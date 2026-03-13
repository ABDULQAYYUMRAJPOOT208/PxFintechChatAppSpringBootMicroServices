package com.fintech.payment_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.payment_service.model.Transaction;
import com.fintech.payment_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.idempotency.ttl}")
    private long idempotencyTtl;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    /**
     * Check if request with idempotency key was already processed
     * Returns existing transaction if found, null otherwise
     */
    public Transaction getProcessedTransaction(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }

        // Check Redis first
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        String transactionId = redisTemplate.opsForValue().get(key);

        if (transactionId != null) {
            log.info("Found cached idempotency key: {} -> transaction: {}",
                    idempotencyKey, transactionId);
            return transactionRepository.findByTransactionId(transactionId).orElse(null);
        }

        // Check database
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            // Cache it for next time
            cacheIdempotencyKey(idempotencyKey, existing.get().getTransactionId());
            return existing.get();
        }

        return null;
    }

    /**
     * Store idempotency key after successful processing
     */
    public void storeIdempotencyKey(String idempotencyKey, String transactionId) {
        if (idempotencyKey == null) {
            return;
        }

        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, transactionId, idempotencyTtl, TimeUnit.SECONDS);
        log.debug("Stored idempotency key: {} -> transaction: {}", idempotencyKey, transactionId);
    }

    /**
     * Cache the idempotency key in Redis
     */
    private void cacheIdempotencyKey(String idempotencyKey, String transactionId) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, transactionId, idempotencyTtl, TimeUnit.SECONDS);
    }

    /**
     * Generate cache key for request
     */
    public String generateKey(String userId, String operation, Object request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String data = userId + ":" + operation + ":" + requestJson;
            return Integer.toHexString(data.hashCode());
        } catch (JsonProcessingException e) {
            log.error("Failed to generate idempotency key", e);
            return UUID.randomUUID().toString();
        }
    }
}