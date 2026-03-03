package com.pxfintech.authentication_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pxfintech.authentication_service.dto.response.RateLimitStatusResponse;
import com.pxfintech.authentication_service.service.RateLimitingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    @Value("${security.ratelimit.default.limit:100}")
    private int defaultLimit;

    @Value("${security.ratelimit.default.duration:60}")
    private int defaultDuration;

    @Value("${security.ratelimit.auth.limit:5}")
    private int authLimit;

    @Value("${security.ratelimit.auth.duration:300}")
    private int authDuration;

    private static final Map<String, RateLimitConfig> ENDPOINT_LIMITS = new HashMap<>();

    static {
        // Auth endpoints - stricter limits
        ENDPOINT_LIMITS.put("/auth/login", new RateLimitConfig(5, 300));
        ENDPOINT_LIMITS.put("/auth/register", new RateLimitConfig(3, 3600));
        ENDPOINT_LIMITS.put("/auth/verify-otp", new RateLimitConfig(10, 300));
        ENDPOINT_LIMITS.put("/auth/resend-otp", new RateLimitConfig(3, 3600));

        // OAuth2 endpoints
        ENDPOINT_LIMITS.put("/oauth2/token", new RateLimitConfig(20, 60));
        ENDPOINT_LIMITS.put("/oauth2/authorize", new RateLimitConfig(20, 60));

        // Service endpoints
        ENDPOINT_LIMITS.put("/service/token", new RateLimitConfig(100, 60));
        ENDPOINT_LIMITS.put("/service/exchange", new RateLimitConfig(100, 60));

        // User info endpoints
        ENDPOINT_LIMITS.put("/users/profile", new RateLimitConfig(50, 60));
        ENDPOINT_LIMITS.put("/users/search", new RateLimitConfig(30, 60));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for certain endpoints
        if (shouldSkipRateLimiting(endpoint)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if IP is blocked
        if (rateLimitingService.isBlocked(clientIp)) {
            sendRateLimitResponse(response, "IP is temporarily blocked due to suspicious activity");
            return;
        }

        // Get rate limit config for endpoint
        RateLimitConfig config = getRateLimitConfig(endpoint);

        // Create key combining IP and endpoint
        String key = clientIp + ":" + endpoint + ":" + method;

        // Check rate limit
        if (rateLimitingService.isRateLimited(key, config.limit, config.duration)) {
            RateLimitStatusResponse status = rateLimitingService.getRateLimitStatus(
                    key, config.limit, config.duration);

            response.setHeader("X-RateLimit-Limit", String.valueOf(config.limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemaining()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(status.getResetInSeconds()));

            sendRateLimitResponse(response, "Rate limit exceeded. Please try again later.");
            return;
        }

        // Add rate limit headers
        RateLimitStatusResponse status = rateLimitingService.getRateLimitStatus(
                key, config.limit, config.duration);

        response.setHeader("X-RateLimit-Limit", String.valueOf(config.limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemaining()));

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean shouldSkipRateLimiting(String endpoint) {
        return endpoint.startsWith("/actuator") ||
                endpoint.startsWith("/swagger") ||
                endpoint.startsWith("/api-docs");
    }

    private RateLimitConfig getRateLimitConfig(String endpoint) {
        for (Map.Entry<String, RateLimitConfig> entry : ENDPOINT_LIMITS.entrySet()) {
            if (endpoint.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new RateLimitConfig(defaultLimit, defaultDuration);
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", message);
        errorResponse.put("path", response.getHeader("X-Forwarded-Path"));

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @lombok.Value
    private static class RateLimitConfig {
        int limit;
        int duration;
    }
}