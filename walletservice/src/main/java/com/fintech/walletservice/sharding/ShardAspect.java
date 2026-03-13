package com.fintech.walletservice.sharding;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class ShardAspect {

    @Around("@annotation(shardable)")
    public Object routeToShard(ProceedingJoinPoint joinPoint, Shardable shardable) throws Throwable {
        Object[] args = joinPoint.getArgs();
        UUID userId = null;

        // Extract userId from arguments
        for (Object arg : args) {
            if (arg instanceof UUID) {
                userId = (UUID) arg;
                break;
            }
            if (arg instanceof String) {
                try {
                    userId = UUID.fromString((String) arg);
                    break;
                } catch (IllegalArgumentException e) {
                    // Not a UUID, continue
                }
            }
        }

        if (userId != null) {
            try {
                ShardManager.setShardKey(userId);
                return joinPoint.proceed();
            } finally {
                ShardManager.clearShardKey();
            }
        }

        return joinPoint.proceed();
    }
}