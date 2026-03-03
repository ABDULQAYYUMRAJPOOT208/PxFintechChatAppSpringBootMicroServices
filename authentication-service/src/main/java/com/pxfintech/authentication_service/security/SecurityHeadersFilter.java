package com.pxfintech.authentication_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${security.headers.hsts.max-age:31536000}")
    private String hstsMaxAge;

    @Value("${security.headers.csp.default-src:default-src 'self'}")
    private String cspPolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // HSTS (HTTP Strict Transport Security)
        response.setHeader("Strict-Transport-Security",
                "max-age=" + hstsMaxAge + "; includeSubDomains; preload");

        // Content Security Policy
        response.setHeader("Content-Security-Policy", buildCspPolicy());

        // X-Content-Type-Options (prevent MIME sniffing)
        response.setHeader("X-Content-Type-Options", "nosniff");

        // X-Frame-Options (prevent clickjacking)
        response.setHeader("X-Frame-Options", "DENY");

        // X-XSS-Protection
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy (formerly Feature Policy)
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=()");

        // Cache Control for sensitive endpoints
        if (isSensitiveEndpoint(request.getRequestURI())) {
            response.setHeader("Cache-Control",
                    "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        // Remove server header
        response.setHeader("Server", "");

        filterChain.doFilter(request, response);
    }

    private String buildCspPolicy() {
        return cspPolicy + "; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://apis.google.com; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https:; " +
                "connect-src 'self' https:; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'";
    }

    private boolean isSensitiveEndpoint(String uri) {
        return uri.contains("/auth/") ||
                uri.contains("/oauth2/") ||
                uri.contains("/token") ||
                uri.contains("/userinfo");
    }
}