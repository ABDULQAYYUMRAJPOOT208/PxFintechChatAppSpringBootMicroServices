package com.fintech.gateway.limiter;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class RateLimiterService {

    @Value("${rate-limiter.public.replenishRate:10}")
    private int publicReplenishRate;

    @Value("${rate-limiter.public.burstCapacity:20}")
    private int publicBurstCapacity;

    @Value("${rate-limiter.authenticated.replenishRate:100}")
    private int authenticatedReplenishRate;

    @Value("${rate-limiter.authenticated.burstCapacity:200}")
    private int authenticatedBurstCapacity;

    @Value("${rate-limiter.premium.replenishRate:500}")
    private int premiumReplenishRate;

    @Value("${rate-limiter.premium.burstCapacity:1000}")
    private int premiumBurstCapacity;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private LettuceBasedProxyManager<byte[]> proxyManager;
    private final ConcurrentMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        RedisURI redisUri = RedisURI.Builder.redis(redisHost, redisPort).build();
        RedisClient redisClient = RedisClient.create(redisUri);
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(
                RedisCodec.of(ByteArrayCodec.INSTANCE, ByteArrayCodec.INSTANCE));

        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMaxSize(Duration.ofMinutes(1)))
                .build();
    }

    public boolean tryConsume(String key, String userType) {
        Bucket bucket = resolveBucket(key, userType);
        return bucket.tryConsume(1);
    }

    private Bucket resolveBucket(String key, String userType) {
        byte[] cacheKey = key.getBytes();

        BucketConfiguration configuration = getConfiguration(userType);

        return proxyManager.builder()
                .build(cacheKey, () -> configuration);
    }

    private BucketConfiguration getConfiguration(String userType) {
        Bandwidth limit;

        switch (userType) {
            case "premium":
                limit = Bandwidth.classic(premiumBurstCapacity, Refill.intervally(
                        premiumReplenishRate, Duration.ofSeconds(1)));
                break;
            case "authenticated":
                limit = Bandwidth.classic(authenticatedBurstCapacity, Refill.intervally(
                        authenticatedReplenishRate, Duration.ofSeconds(1)));
                break;
            default:
                limit = Bandwidth.classic(publicBurstCapacity, Refill.intervally(
                        publicReplenishRate, Duration.ofSeconds(1)));
        }

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    public long getAvailableTokens(String key, String userType) {
        Bucket bucket = resolveBucket(key, userType);
        return bucket.getAvailableTokens();
    }

    public void resetLimit(String key) {
        localBuckets.remove(key);
    }
}