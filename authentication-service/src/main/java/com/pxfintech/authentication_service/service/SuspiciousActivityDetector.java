package com.pxfintech.authentication_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuspiciousActivityDetector {

    private final RedisTemplate<String, String> redisTemplate;
    private final AuditLogService auditLogService;
    private final IpFilterService ipFilterService;

    private static final String ACTIVITY_PREFIX = "activity:";
    private static final String GEO_PREFIX = "geo:";
    private static final String DEVICE_PREFIX = "device:";

    /**
     * Analyze login attempt for suspicious patterns
     */
    public boolean isSuspiciousLogin(String userId, String ipAddress,
                                     String userAgent, String deviceId) {
        int riskScore = 0;
        Map<String, Object> reasons = new HashMap<>();

        // Check for impossible travel
        if (hasImpossibleTravel(userId, ipAddress)) {
            riskScore += 50;
            reasons.put("impossible_travel", true);
        }

        // Check for multiple devices
        if (hasMultipleDevices(userId, deviceId)) {
            riskScore += 20;
            reasons.put("multiple_devices", true);
        }

        // Check for suspicious IP
        if (isSuspiciousIp(ipAddress)) {
            riskScore += 30;
            reasons.put("suspicious_ip", true);
        }

        // Check for unusual time
        if (isUnusualTime(userId)) {
            riskScore += 10;
            reasons.put("unusual_time", true);
        }

        // Check for rapid attempts
        if (isRapidAttempt(userId)) {
            riskScore += 25;
            reasons.put("rapid_attempt", true);
        }

        boolean isSuspicious = riskScore >= 50;

        if (isSuspicious) {
            log.warn("Suspicious login detected for user: {} - Risk score: {} - Reasons: {}",
                    userId, riskScore, reasons);

            auditLogService.builder()
                    .withAction("SUSPICIOUS_LOGIN")
                    .withUserId(userId)
                    .withIpAddress(ipAddress)
                    .withUserAgent(userAgent)
                    .withDetails(reasons)
                    .withStatus("DETECTED")
                    .log();

            // Block if very high risk
            if (riskScore >= 80) {
                ipFilterService.addToBlacklist(ipAddress,
                        "High risk login attempt", 60);
            }
        }

        // Store activity for future analysis
        storeActivity(userId, ipAddress, deviceId);

        return isSuspicious;
    }

    private boolean hasImpossibleTravel(String userId, String currentIp) {
        String lastLocationKey = ACTIVITY_PREFIX + "last:ip:" + userId;
        String lastLocation = redisTemplate.opsForValue().get(lastLocationKey);

        if (lastLocation != null && !lastLocation.equals(currentIp)) {
            // Check time between logins
            String lastTimeKey = ACTIVITY_PREFIX + "last:time:" + userId;
            String lastTimeStr = redisTemplate.opsForValue().get(lastTimeKey);

            if (lastTimeStr != null) {
                long lastTime = Long.parseLong(lastTimeStr);
                long currentTime = System.currentTimeMillis() / 1000;

                // If less than 1 hour between logins from different locations
                if (currentTime - lastTime < 3600) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasMultipleDevices(String userId, String currentDeviceId) {
        String devicesKey = DEVICE_PREFIX + userId;
        String devicesStr = redisTemplate.opsForValue().get(devicesKey);

        if (devicesStr != null) {
            String[] devices = devicesStr.split(",");
            // If more than 3 devices in last 24 hours
            return devices.length >= 3 && !devicesStr.contains(currentDeviceId);
        }

        return false;
    }

    private boolean isSuspiciousIp(String ipAddress) {
        // Check against known proxy/VPN lists
        String proxyKey = GEO_PREFIX + "proxy:" + ipAddress;
        Boolean isProxy = redisTemplate.hasKey(proxyKey);

        // Check against high-risk countries
        String countryKey = GEO_PREFIX + "country:" + ipAddress;
        String country = redisTemplate.opsForValue().get(countryKey);

        return Boolean.TRUE.equals(isProxy) ||
                ("RU".equals(country) || "CN".equals(country)); // Example high-risk countries
    }

    private boolean isUnusualTime(String userId) {
        String lastTimeKey = ACTIVITY_PREFIX + "avg:time:" + userId;
        String avgTimeStr = redisTemplate.opsForValue().get(lastTimeKey);

        if (avgTimeStr != null) {
            int avgHour = Integer.parseInt(avgTimeStr);
            int currentHour = LocalDateTime.now().getHour();

            // If login time differs from average by more than 6 hours
            return Math.abs(currentHour - avgHour) > 6;
        }

        return false;
    }

    private boolean isRapidAttempt(String userId) {
        String attemptKey = ACTIVITY_PREFIX + "attempts:" + userId;
        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);

        if (attemptsStr != null) {
            int attempts = Integer.parseInt(attemptsStr);
            return attempts > 3; // More than 3 attempts in 5 minutes
        }

        return false;
    }

    private void storeActivity(String userId, String ipAddress, String deviceId) {
        // Store last IP
        redisTemplate.opsForValue().set(
                ACTIVITY_PREFIX + "last:ip:" + userId,
                ipAddress,
                30, TimeUnit.DAYS);

        // Store last time
        redisTemplate.opsForValue().set(
                ACTIVITY_PREFIX + "last:time:" + userId,
                String.valueOf(System.currentTimeMillis() / 1000),
                30, TimeUnit.DAYS);

        // Update devices
        String devicesKey = DEVICE_PREFIX + userId;
        redisTemplate.opsForList().leftPush(devicesKey, deviceId);
        redisTemplate.expire(devicesKey, 1, TimeUnit.DAYS);

        // Trim to last 10 devices
        redisTemplate.opsForList().trim(devicesKey, 0, 9);

        // Increment attempt counter (expires in 5 minutes)
        String attemptKey = ACTIVITY_PREFIX + "attempts:" + userId;
        redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, 5, TimeUnit.MINUTES);
    }
}