package com.fintech.gateway.limiter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class KeyResolver {

    public String resolveKey(ServerWebExchange exchange) {
        // Try to get user ID from authentication
        String userId = exchange.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }

        // Fallback to client IP
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            ip = forwardedFor.split(",")[0].trim();
        }

        return "ip:" + ip;
    }

    public String getUserType(ServerWebExchange exchange) {
        // Determine user type for rate limit tiers
        String userId = exchange.getAttribute("userId");
        if (userId != null) {
            // Check if premium user (would call User Service in real implementation)
            return "authenticated"; // or "premium" for premium users
        }
        return "public";
    }
}