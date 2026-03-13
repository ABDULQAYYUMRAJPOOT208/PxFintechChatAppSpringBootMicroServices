package com.fintech.walletservice.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockManager {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${wallet.lock.timeout:5000}")
    private long lockTimeout;

    @Value("${wallet.lock.retry-attempts:3}")
    private int retryAttempts;

    @Value("${wallet.lock.retry-delay:100}")
    private long retryDelay;

    private static final String LOCK_PREFIX = "lock:wallet:";

    public String acquireLock(UUID walletId) {
        String lockKey = LOCK_PREFIX + walletId;
        String lockValue = UUID.randomUUID().toString();

        for (int i = 0; i < retryAttempts; i++) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, lockTimeout, TimeUnit.MILLISECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Lock acquired for wallet: {} with value: {}", walletId, lockValue);
                return lockValue;
            }

            log.debug("Failed to acquire lock for wallet: {}, attempt: {}", walletId, i + 1);

            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while acquiring lock", e);
            }
        }

        throw new RuntimeException("Failed to acquire lock for wallet: " + walletId);
    }

    public boolean releaseLock(UUID walletId, String lockValue) {
        String lockKey = LOCK_PREFIX + walletId;
        String currentValue = redisTemplate.opsForValue().get(lockKey);

        if (lockValue.equals(currentValue)) {
            Boolean deleted = redisTemplate.delete(lockKey);
            log.debug("Lock released for wallet: {}", walletId);
            return Boolean.TRUE.equals(deleted);
        }

        log.warn("Cannot release lock - lock value mismatch for wallet: {}", walletId);
        return false;
    }

    public boolean isLocked(UUID walletId) {
        String lockKey = LOCK_PREFIX + walletId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
}