package com.fintech.payment_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.payment_service.dto.request.P2PTransferRequest;
import com.fintech.payment_service.model.Transaction;
import com.fintech.payment_service.repository.TransactionRepository;
import com.fintech.payment_service.service.IdempotencyService;
import com.fintech.payment_service.service.client.WalletServiceClient;
import com.fintech.payment_service.service.client.NotificationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final TransactionRepository transactionRepository;
    private final WalletServiceClient walletServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final IdempotencyService idempotencyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String SAGA_LOG_PREFIX = "saga:";

    /**
     * Execute P2P transfer SAGA
     */
    @Transactional
    public SagaResult executeP2PTransfer(String userId, P2PTransferRequest request) {
        log.info("Starting P2P transfer SAGA for user: {} amount: {}", userId, request.getAmount());

        // Check idempotency
        Transaction existing = idempotencyService.getProcessedTransaction(request.getIdempotencyKey());
        if (existing != null) {
            log.info("Idempotent request - returning existing transaction: {}", existing.getTransactionId());
            return SagaResult.success(existing);
        }

        // Create transaction record
        Transaction transaction = createTransaction(userId, request);

        try {
            // Step 1: Debit sender wallet
            logStep(transaction, "DEBIT_SENDER", "STARTED");
            WalletResponse debitResponse = walletServiceClient.debit(
                    UUID.fromString(userId),
                    request.getAmount(),
                    request.getCurrency(),
                    request.getIdempotencyKey + ":debit"
            );

            if (!debitResponse.isSuccess()) {
                throw new SagaException("Failed to debit sender: " + debitResponse.getMessage());
            }
            logStep(transaction, "DEBIT_SENDER", "COMPLETED");

            // Step 2: Credit receiver wallet
            logStep(transaction, "CREDIT_RECEIVER", "STARTED");
            WalletResponse creditResponse = walletServiceClient.credit(
                    request.getReceiverId(),
                    request.getAmount(),
                    request.getCurrency(),
                    request.getIdempotencyKey + ":credit"
            );

            if (!creditResponse.isSuccess()) {
                throw new SagaException("Failed to credit receiver: " + creditResponse.getMessage());
            }
            logStep(transaction, "CREDIT_RECEIVER", "COMPLETED");

            // Step 3: Mark transaction as success
            transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);
            logStep(transaction, "TRANSACTION_COMPLETE", "SUCCESS");

            // Store idempotency key
            idempotencyService.storeIdempotencyKey(request.getIdempotencyKey(), transaction.getTransactionId());

            // Send notifications
            sendNotifications(transaction);

            // Publish event
            publishTransactionEvent(transaction, "SUCCESS");

            log.info("P2P transfer SAGA completed successfully for transaction: {}",
                    transaction.getTransactionId());

            return SagaResult.success(transaction);

        } catch (Exception e) {
            log.error("SAGA failed for transaction: {} - {}", transaction.getTransactionId(), e.getMessage());

            // Compensating transaction
            compensate(transaction, e);

            return SagaResult.failure(transaction, e.getMessage());
        }
    }

    /**
     * Compensating transaction - rollback on failure
     */
    private void compensate(Transaction transaction, Exception cause) {
        log.info("Starting compensation for transaction: {}", transaction.getTransactionId());

        try {
            // If sender was debited but receiver not credited, refund sender
            if (transaction.getStatus() == Transaction.TransactionStatus.PROCESSING) {
                logStep(transaction, "COMPENSATION", "STARTED");

                walletServiceClient.credit(
                        transaction.getSenderId(),
                        transaction.getAmount(),
                        transaction.getCurrency(),
                        "compensate:" + transaction.getTransactionId()
                );

                logStep(transaction, "COMPENSATION", "COMPLETED");
            }

            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setErrorMessage(cause.getMessage());
            transactionRepository.save(transaction);

            log.info("Compensation completed for transaction: {}", transaction.getTransactionId());

        } catch (Exception e) {
            log.error("Compensation failed for transaction: {} - {}",
                    transaction.getTransactionId(), e.getMessage());
            // Manual intervention required
            markForManualReview(transaction);
        }
    }

    private Transaction createTransaction(String userId, P2PTransferRequest request) {
        Transaction transaction = Transaction.builder()
                .senderId(UUID.fromString(userId))
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .idempotencyKey(request.getIdempotencyKey())
                .transactionType(Transaction.TransactionType.P2P)
                .status(Transaction.TransactionStatus.PROCESSING)
                .metadata(request.getMetadata())
                .build();

        return transactionRepository.save(transaction);
    }

    private void logStep(Transaction transaction, String step, String status) {
        String sagaKey = SAGA_LOG_PREFIX + transaction.getTransactionId();

        Map<String, Object> stepLog = new HashMap<>();
        stepLog.put("step", step);
        stepLog.put("status", status);
        stepLog.put("timestamp", Instant.now().toString());

        // Store in Redis for real-time tracking
        // Could also store in database for persistence

        log.info("SAGA step - Transaction: {} Step: {} Status: {}",
                transaction.getTransactionId(), step, status);
    }

    private void sendNotifications(Transaction transaction) {
        try {
            // Notify sender
            notificationServiceClient.sendNotification(
                    transaction.getSenderId().toString(),
                    "PAYMENT_SENT",
                    Map.of(
                            "amount", transaction.getAmount(),
                            "receiver", transaction.getReceiverId(),
                            "transactionId", transaction.getTransactionId()
                    )
            );

            // Notify receiver
            notificationServiceClient.sendNotification(
                    transaction.getReceiverId().toString(),
                    "PAYMENT_RECEIVED",
                    Map.of(
                            "amount", transaction.getAmount(),
                            "sender", transaction.getSenderId(),
                            "transactionId", transaction.getTransactionId()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send notifications for transaction: {}",
                    transaction.getTransactionId(), e);
        }
    }

    private void publishTransactionEvent(Transaction transaction, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("transactionId", transaction.getTransactionId());
        event.put("eventType", eventType);
        event.put("userId", transaction.getSenderId());
        event.put("amount", transaction.getAmount());
        event.put("timestamp", Instant.now().toString());

        kafkaTemplate.send("payment-transactions", transaction.getTransactionId(), event);
    }

    private void markForManualReview(Transaction transaction) {
        // Mark transaction for manual review
        log.error("Transaction {} requires manual review", transaction.getTransactionId());
        // Could store in database with a flag or send to admin queue
    }

    @lombok.Data
    @lombok.Builder
    public static class SagaResult {
        private boolean success;
        private Transaction transaction;
        private String message;

        public static SagaResult success(Transaction transaction) {
            return SagaResult.builder()
                    .success(true)
                    .transaction(transaction)
                    .build();
        }

        public static SagaResult failure(Transaction transaction, String message) {
            return SagaResult.builder()
                    .success(false)
                    .transaction(transaction)
                    .message(message)
                    .build();
        }
    }

    public static class SagaException extends RuntimeException {
        public SagaException(String message) {
            super(message);
        }
    }
}