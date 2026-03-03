package com.pxfintech.authentication_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IpFilterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final List<String> whitelist;
    private final List<String> blacklist;
    private final boolean enableWhitelist;
    private final boolean enableBlacklist;

    private static final String DYNAMIC_BLACKLIST_PREFIX = "ip:blacklist:dynamic:";

    public IpFilterService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${security.ip.whitelist:}") String whitelistStr,
            @Value("${security.ip.blacklist:}") String blacklistStr,
            @Value("${security.ip.whitelist.enabled:false}") boolean enableWhitelist,
            @Value("${security.ip.blacklist.enabled:true}") boolean enableBlacklist) {

        this.redisTemplate = redisTemplate;
        this.whitelist = Arrays.asList(whitelistStr.split(","));
        this.blacklist = Arrays.asList(blacklistStr.split(","));
        this.enableWhitelist = enableWhitelist;
        this.enableBlacklist = enableBlacklist;
    }

    /**
     * Check if IP is allowed
     */
    public boolean isIpAllowed(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return false;
        }

        // Check whitelist first if enabled
        if (enableWhitelist) {
            if (!isIpInWhitelist(ipAddress)) {
                log.debug("IP {} not in whitelist", ipAddress);
                return false;
            }
        }

        // Check blacklist if enabled
        if (enableBlacklist) {
            if (isIpInBlacklist(ipAddress) || isIpDynamicallyBlacklisted(ipAddress)) {
                log.debug("IP {} is blacklisted", ipAddress);
                return false;
            }
        }

        return true;
    }

    /**
     * Add IP to dynamic blacklist
     */
    public void addToBlacklist(String ipAddress, String reason, int durationMinutes) {
        String key = DYNAMIC_BLACKLIST_PREFIX + ipAddress;
        redisTemplate.opsForValue().set(key, reason, durationMinutes, TimeUnit.MINUTES);
        log.warn("IP {} added to dynamic blacklist. Reason: {}, Duration: {} minutes",
                ipAddress, reason, durationMinutes);
    }

    /**
     * Remove IP from dynamic blacklist
     */
    public void removeFromBlacklist(String ipAddress) {
        String key = DYNAMIC_BLACKLIST_PREFIX + ipAddress;
        redisTemplate.delete(key);
        log.info("IP {} removed from dynamic blacklist", ipAddress);
    }

    /**
     * Get all dynamic blacklisted IPs
     */
    public List<String> getDynamicBlacklist() {
        // Implementation would scan Redis keys
        return List.of();
    }

    private boolean isIpInWhitelist(String ipAddress) {
        return whitelist.stream()
                .filter(StringUtils::hasText)
                .anyMatch(pattern -> matchesPattern(ipAddress, pattern));
    }

    private boolean isIpInBlacklist(String ipAddress) {
        return blacklist.stream()
                .filter(StringUtils::hasText)
                .anyMatch(pattern -> matchesPattern(ipAddress, pattern));
    }

    private boolean isIpDynamicallyBlacklisted(String ipAddress) {
        String key = DYNAMIC_BLACKLIST_PREFIX + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private boolean matchesPattern(String ipAddress, String pattern) {
        if (pattern.contains("*")) {
            // Simple wildcard matching
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            return Pattern.matches(regex, ipAddress);
        } else if (pattern.contains("/")) {
            // CIDR notation
            return isIpInCidr(ipAddress, pattern);
        } else {
            // Exact match
            return pattern.equals(ipAddress);
        }
    }

    private boolean isIpInCidr(String ipAddress, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefix = Integer.parseInt(parts[1]);

            long ip = ipToLong(ipAddress);
            long networkIp = ipToLong(network);
            long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;

            return (ip & mask) == (networkIp & mask);
        } catch (Exception e) {
            log.error("Invalid CIDR notation: {}", cidr);
            return false;
        }
    }

    private long ipToLong(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        return (Long.parseLong(octets[0]) << 24) +
                (Long.parseLong(octets[1]) << 16) +
                (Long.parseLong(octets[2]) << 8) +
                Long.parseLong(octets[3]);
    }
}