package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.model.entity.BlacklistedToken;
import com.pxfintech.authentication_service.repository.TokenBlacklistRepository;
import com.pxfintech.authentication_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistService {

    private final TokenBlacklistRepository blacklistRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Transactional
    public void blacklistToken(String token, String reason, String userId, String clientId) {
        String tokenId = jwtTokenProvider.getTokenId(token);
        String tokenType = jwtTokenProvider.getTokenType(token);
        LocalDateTime expirationTime = jwtTokenProvider.getExpirationDateTime(token);

        // Store in database for persistence
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .tokenId(tokenId)
                .tokenType(tokenType)
                .expirationTime(expirationTime)
                .blacklistReason(reason)
                .userId(userId != null ? java.util.UUID.fromString(userId) : null)
                .clientId(clientId)
                .build();

        blacklistRepository.save(blacklistedToken);

        // Also cache in Valkey for quick lookup
        String key = BLACKLIST_PREFIX + tokenId;
        long ttl = calculateTtl(expirationTime);
        redisTemplate.opsForValue().set(key, reason, ttl, TimeUnit.SECONDS);

        log.info("Token blacklisted: {} with reason: {}", tokenId, reason);
    }

    public boolean isTokenBlacklisted(String token) {
        String tokenId = jwtTokenProvider.getTokenId(token);

        // Check Valkey first (fast path)
        String key = BLACKLIST_PREFIX + tokenId;
        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            return true;
        }

        // Fallback to database check
        return blacklistRepository.existsByTokenId(tokenId);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupExpiredBlacklistEntries() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = blacklistRepository.deleteExpiredTokens(now);
        log.info("Cleaned up {} expired blacklist entries", deletedCount);
    }

    private long calculateTtl(LocalDateTime expirationTime) {
        LocalDateTime now = LocalDateTime.now();
        if (expirationTime.isBefore(now)) {
            return 0;
        }
        return java.time.Duration.between(now, expirationTime).getSeconds();
    }
}