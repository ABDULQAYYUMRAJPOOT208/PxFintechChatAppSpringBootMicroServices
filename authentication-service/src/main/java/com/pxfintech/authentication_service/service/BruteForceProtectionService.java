package com.pxfintech.authentication_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BruteForceProtectionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitingService rateLimitingService;

    @Value("${security.bruteforce.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.bruteforce.block-minutes:30}")
    private int blockMinutes;

    @Value("${security.bruteforce.notification-threshold:10}")
    private int notificationThreshold;

    private static final String ATTEMPT_PREFIX = "bf:attempt:";
    private static final String BLOCK_PREFIX = "bf:block:";
    private static final String NOTIFY_PREFIX = "bf:notify:";

    /**
     * Record failed login attempt
     */
    public void recordFailedAttempt(String username, String ipAddress) {
        String userKey = ATTEMPT_PREFIX + username;
        String ipKey = ATTEMPT_PREFIX + ipAddress;

        // Increment attempts for user
        Long userAttempts = redisTemplate.opsForValue().increment(userKey);
        if (userAttempts == 1) {
            redisTemplate.expire(userKey, blockMinutes, TimeUnit.MINUTES);
        }

        // Increment attempts for IP
        Long ipAttempts = redisTemplate.opsForValue().increment(ipKey);
        if (ipAttempts == 1) {
            redisTemplate.expire(ipKey, blockMinutes, TimeUnit.MINUTES);
        }

        log.warn("Failed login attempt for user: {} from IP: {} (attempts: user={}, ip={})",
                username, ipAddress, userAttempts, ipAttempts);

        // Check if should block
        if (userAttempts >= maxAttempts) {
            blockUser(username, "Max attempts exceeded");
        }

        if (ipAttempts >= maxAttempts * 2) { // IP gets more attempts
            blockIp(ipAddress, "Suspicious activity from IP");
        }

        // Check if should notify admin
        if (userAttempts >= notificationThreshold || ipAttempts >= notificationThreshold) {
            notifySecurityTeam(username, ipAddress, userAttempts, ipAttempts);
        }
    }

    /**
     * Record successful login
     */
    public void recordSuccessfulLogin(String username, String ipAddress) {
        String userKey = ATTEMPT_PREFIX + username;
        String ipKey = ATTEMPT_PREFIX + ipAddress;

        // Clear failed attempts on successful login
        redisTemplate.delete(userKey);
        redisTemplate.delete(ipKey);

        log.info("Successful login for user: {} from IP: {}", username, ipAddress);
    }

    /**
     * Check if user is blocked
     */
    public boolean isBlocked(String username) {
        String blockKey = BLOCK_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
    }

    /**
     * Check if IP is blocked
     */
    public boolean isIpBlocked(String ipAddress) {
        String blockKey = BLOCK_PREFIX + "ip:" + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
    }

    /**
     * Block a user
     */
    public void blockUser(String username, String reason) {
        String blockKey = BLOCK_PREFIX + username;
        redisTemplate.opsForValue().set(blockKey, reason, blockMinutes, TimeUnit.MINUTES);

        // Also block by IP if needed
        log.warn("User blocked: {} - Reason: {}", username, reason);

        // Trigger security alert
        triggerSecurityAlert("USER_BLOCKED", username, reason);
    }

    /**
     * Block an IP address
     */
    public void blockIp(String ipAddress, String reason) {
        String blockKey = BLOCK_PREFIX + "ip:" + ipAddress;
        redisTemplate.opsForValue().set(blockKey, reason, blockMinutes * 2, TimeUnit.MINUTES);

        log.warn("IP blocked: {} - Reason: {}", ipAddress, reason);

        // Trigger security alert
        triggerSecurityAlert("IP_BLOCKED", ipAddress, reason);
    }

    /**
     * Get attempt count for user
     */
    public Long getAttemptCount(String username) {
        String userKey = ATTEMPT_PREFIX + username;
        String attempts = redisTemplate.opsForValue().get(userKey);
        return attempts != null ? Long.parseLong(attempts) : 0L;
    }

    /**
     * Reset attempts for user
     */
    public void resetAttempts(String username) {
        String userKey = ATTEMPT_PREFIX + username;
        redisTemplate.delete(userKey);
        log.info("Reset attempts for user: {}", username);
    }

    private void notifySecurityTeam(String username, String ipAddress,
                                    Long userAttempts, Long ipAttempts) {
        String notifyKey = NOTIFY_PREFIX + username + ":" + ipAddress;

        // Check if already notified recently (avoid spam)
        Boolean alreadyNotified = redisTemplate.hasKey(notifyKey);
        if (Boolean.TRUE.equals(alreadyNotified)) {
            return;
        }

        // Send notification (email, Slack, etc.)
        log.error("SECURITY ALERT: Multiple failed attempts - User: {}, IP: {}, " +
                        "User attempts: {}, IP attempts: {}",
                username, ipAddress, userAttempts, ipAttempts);

        // Store notification to avoid duplicates
        redisTemplate.opsForValue().set(notifyKey, "notified", 60, TimeUnit.MINUTES);
    }

    private void triggerSecurityAlert(String type, String target, String reason) {
        // Send to security monitoring system
        log.info("Security alert triggered: {} - {} - {}", type, target, reason);

        // Could send to SIEM, Slack, Email, etc.
    }
}