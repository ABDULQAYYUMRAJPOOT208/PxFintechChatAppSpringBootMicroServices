package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.dto.response.RateLimitStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final String BUCKET_PREFIX = "bucket:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Check if request should be rate limited using token bucket algorithm
     */
    public boolean isRateLimited(String key, int limit, int durationSeconds) {
        String bucketKey = BUCKET_PREFIX + key;

        // Lua script for atomic token bucket operations
        String luaScript =
                "local bucket = redis.call('get', KEYS[1])\n" +
                        "local limit = tonumber(ARGV[1])\n" +
                        "local duration = tonumber(ARGV[2])\n" +
                        "local now = tonumber(ARGV[3])\n" +
                        "local window = duration\n" +
                        "\n" +
                        "if bucket then\n" +
                        "    local tokens = tonumber(bucket)\n" +
                        "    if tokens > 0 then\n" +
                        "        redis.call('decr', KEYS[1])\n" +
                        "        return 0\n" +
                        "    else\n" +
                        "        local ttl = redis.call('ttl', KEYS[1])\n" +
                        "        return ttl\n" +
                        "    end\n" +
                        "else\n" +
                        "    redis.call('set', KEYS[1], limit - 1)\n" +
                        "    redis.call('expire', KEYS[1], window)\n" +
                        "    return 0\n" +
                        "end";

        RedisScript<Long> script = RedisScript.of(luaScript, Long.class);
        Long result = redisTemplate.execute(script,
                Arrays.asList(bucketKey),
                String.valueOf(limit),
                String.valueOf(durationSeconds),
                String.valueOf(System.currentTimeMillis() / 1000));

        return result != null && result > 0;
    }

    /**
     * Get current rate limit status
     */
    public RateLimitStatusResponse getRateLimitStatus(String key, int limit, int durationSeconds) {
        String bucketKey = BUCKET_PREFIX + key;

        String remainingStr = redisTemplate.opsForValue().get(bucketKey);
        Long remaining = remainingStr != null ? Long.parseLong(remainingStr) : limit;

        Long ttl = redisTemplate.getExpire(bucketKey, TimeUnit.SECONDS);
        if (ttl < 0) {
            ttl = 0L;
        }

        boolean isRateLimited = remaining <= 0;

        return RateLimitStatusResponse.builder()
                .remaining(Math.max(0, remaining))
                .limit((long) limit)
                .resetInSeconds(isRateLimited ? ttl : 0)
                .retryAfterSeconds(isRateLimited ? ttl : 0)
                .isRateLimited(isRateLimited)
                .build();
    }

    /**
     * Increment attempt counter for brute force protection
     */
    public long incrementAttempts(String key, int maxAttempts, int blockDurationMinutes) {
        String attemptKey = RATE_LIMIT_PREFIX + "attempt:" + key;

        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts == 1) {
            redisTemplate.expire(attemptKey, blockDurationMinutes, TimeUnit.MINUTES);
        }

        if (attempts >= maxAttempts) {
            // Block the IP/user
            blockKey(key, blockDurationMinutes * 60);
        }

        return attempts;
    }

    /**
     * Block a key (IP, user, client)
     */
    public void blockKey(String key, int blockSeconds) {
        String blockKey = BLACKLIST_PREFIX + key;
        redisTemplate.opsForValue().set(blockKey, "blocked", blockSeconds, TimeUnit.SECONDS);
        log.warn("Key blocked: {} for {} seconds", key, blockSeconds);
    }

    /**
     * Check if a key is blocked
     */
    public boolean isBlocked(String key) {
        String blockKey = BLACKLIST_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
    }

    /**
     * Clear rate limit for a key
     */
    public void clearRateLimit(String key) {
        String bucketKey = BUCKET_PREFIX + key;
        redisTemplate.delete(bucketKey);
    }

    /**
     * Reset all rate limits for testing
     */
    public void resetAllLimits() {
        // Only for testing
    }
}