package com.fintech.payment_service.service;

import com.fintech.payment_service.dto.request.P2PTransferRequest;
import com.fintech.payment_service.dto.request.POSPaymentRequest;
import com.fintech.payment_service.model.FraudAlert;
import com.fintech.payment_service.repository.FraudAlertRepository;
import com.fintech.payment_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.fraud.enabled}")
    private boolean fraudDetectionEnabled;

    private static final String VELOCITY_PREFIX = "fraud:velocity:";
    private static final String AMOUNT_PREFIX = "fraud:amount:";
    private static final String LOCATION_PREFIX = "fraud:location:";

    /**
     * Analyze P2P transfer for fraud
     */
    public FraudDetectionResult analyzeP2PTransfer(String userId, P2PTransferRequest request) {
        if (!fraudDetectionEnabled) {
            return FraudDetectionResult.allowed();
        }

        int riskScore = 0;
        Map<String, Object> flags = new HashMap<>();

        // 1. Velocity check - too many transactions in short time
        long recentCount = transactionRepository.countBySenderIdAndStatusAndCreatedAtAfter(
                UUID.fromString(userId),
                Transaction.TransactionStatus.SUCCESS,
                Instant.now().minus(1, ChronoUnit.HOURS));

        if (recentCount > 10) {
            riskScore += 30;
            flags.put("high_velocity", recentCount);
        } else if (recentCount > 5) {
            riskScore += 15;
            flags.put("medium_velocity", recentCount);
        }

        // 2. Amount check - unusually large amount
        BigDecimal avgAmount = getAverageTransactionAmount(userId);
        if (request.getAmount().compareTo(avgAmount.multiply(new BigDecimal("3"))) > 0) {
            riskScore += 25;
            flags.put("unusual_amount", request.getAmount());
        }

        // 3. Daily limit check
        BigDecimal dailyTotal = transactionRepository.getTotalSentSince(
                UUID.fromString(userId),
                Instant.now().truncatedTo(ChronoUnit.DAYS));

        if (dailyTotal != null && dailyTotal.add(request.getAmount()).compareTo(new BigDecimal("5000")) > 0) {
            riskScore += 20;
            flags.put("daily_limit_exceeded", dailyTotal);
        }

        // 4. New receiver check
        String receiverKey = VELOCITY_PREFIX + "receiver:" + userId + ":" + request.getReceiverId();
        Boolean hasSentBefore = redisTemplate.hasKey(receiverKey);
        if (Boolean.FALSE.equals(hasSentBefore)) {
            riskScore += 10;
            flags.put("new_receiver", true);
        }

        return determineAction(riskScore, flags, userId, request);
    }

    /**
     * Analyze POS payment for fraud
     */
    public FraudDetectionResult analyzePOSPayment(String userId, POSPaymentRequest request) {
        if (!fraudDetectionEnabled) {
            return FraudDetectionResult.allowed();
        }

        int riskScore = 0;
        Map<String, Object> flags = new HashMap<>();

        // 1. Location mismatch (if user usually transacts in different location)
        String lastLocation = redisTemplate.opsForValue().get(LOCATION_PREFIX + userId);
        if (lastLocation != null && request.getLocation() != null
                && !lastLocation.equals(request.getLocation())) {
            riskScore += 40;
            flags.put("location_mismatch", lastLocation + " vs " + request.getLocation());
        }

        // 2. Unusual transaction time
        int hour = Instant.now().atZone(java.time.ZoneId.systemDefault()).getHour();
        if (hour < 6 || hour > 23) {
            riskScore += 15;
            flags.put("unusual_time", hour);
        }

        // 3. Amount check for POS
        if (request.getAmount().compareTo(new BigDecimal("1000")) > 0) {
            riskScore += 25;
            flags.put("high_value_pos", request.getAmount());
        }

        return determineAction(riskScore, flags, userId, request);
    }

    private FraudDetectionResult determineAction(int riskScore, Map<String, Object> flags,
                                                 String userId, Object request) {
        FraudAlert.AlertSeverity severity;
        String action;

        if (riskScore >= 70) {
            severity = FraudAlert.AlertSeverity.CRITICAL;
            action = "BLOCK";
            log.error("Critical fraud detected for user: {} with risk score: {}", userId, riskScore);
        } else if (riskScore >= 50) {
            severity = FraudAlert.AlertSeverity.HIGH;
            action = "REVIEW";
            log.warn("High fraud risk for user: {} with risk score: {}", userId, riskScore);
        } else if (riskScore >= 30) {
            severity = FraudAlert.AlertSeverity.MEDIUM;
            action = "FLAG";
            log.info("Medium fraud risk for user: {} with risk score: {}", userId, riskScore);
        } else {
            severity = FraudAlert.AlertSeverity.LOW;
            action = "ALLOW";
        }

        // Create alert if risk is medium or higher
        if (riskScore >= 30) {
            createFraudAlert(userId, severity, riskScore, flags, action);
        }

        // Publish to Kafka for real-time monitoring
        publishFraudEvent(userId, riskScore, flags, action);

        return FraudDetectionResult.builder()
                .allowed(action.equals("ALLOW"))
                .riskScore(riskScore)
                .flags(flags)
                .action(action)
                .build();
    }

    private BigDecimal getAverageTransactionAmount(String userId) {
        String key = AMOUNT_PREFIX + userId;
        String avgStr = redisTemplate.opsForValue().get(key);

        if (avgStr != null) {
            return new BigDecimal(avgStr);
        }
        return new BigDecimal("50"); // Default average
    }

    private void createFraudAlert(String userId, FraudAlert.AlertSeverity severity,
                                  int riskScore, Map<String, Object> flags, String action) {
        FraudAlert alert = FraudAlert.builder()
                .alertId("ALERT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId(UUID.fromString(userId))
                .alertType(FraudAlert.AlertType.PATTERN)
                .severity(severity)
                .score(new BigDecimal(riskScore))
                .description(flags.toString())
                .metadata(flags.toString())
                .actionTaken(action)
                .build();

        fraudAlertRepository.save(alert);
    }

    private void publishFraudEvent(String userId, int riskScore,
                                   Map<String, Object> flags, String action) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("riskScore", riskScore);
        event.put("flags", flags);
        event.put("action", action);
        event.put("timestamp", Instant.now().toString());

        kafkaTemplate.send("fraud-alerts", userId, event);
    }

    @lombok.Builder
    @lombok.Data
    public static class FraudDetectionResult {
        private boolean allowed;
        private int riskScore;
        private Map<String, Object> flags;
        private String action;

        public static FraudDetectionResult allowed() {
            return FraudDetectionResult.builder()
                    .allowed(true)
                    .riskScore(0)
                    .action("ALLOW")
                    .build();
        }
    }
}