package com.fintech.walletservice.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LockAspect {

    private final DistributedLockManager lockManager;

    @Around("@annotation(lockable)")
    public Object acquireLock(ProceedingJoinPoint joinPoint, Lockable lockable) throws Throwable {
        Object[] args = joinPoint.getArgs();
        UUID walletId = null;

        // Extract walletId from arguments
        for (Object arg : args) {
            if (arg instanceof UUID) {
                walletId = (UUID) arg;
                break;
            }
            if (arg instanceof String) {
                try {
                    walletId = UUID.fromString((String) arg);
                    break;
                } catch (IllegalArgumentException e) {
                    // Not a UUID, continue
                }
            }
        }

        if (walletId == null) {
            log.warn("No walletId found for lockable method: {}", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        String lockValue = null;
        try {
            lockValue = lockManager.acquireLock(walletId);
            return joinPoint.proceed();
        } finally {
            if (lockValue != null) {
                lockManager.releaseLock(walletId, lockValue);
            }
        }
    }
}