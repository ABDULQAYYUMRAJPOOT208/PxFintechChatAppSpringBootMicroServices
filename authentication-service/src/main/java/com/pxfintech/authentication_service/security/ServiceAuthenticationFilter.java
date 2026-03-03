package com.pxfintech.authentication_service.security;

import com.pxfintech.authentication_service.service.ServiceTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    private final ServiceTokenService serviceTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String serviceToken = extractServiceToken(request);

        if (serviceToken != null) {
            try {
                // Validate service token
                if (serviceTokenService.validateServiceToken(serviceToken, null, null)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    "service",
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    request.setAttribute("serviceAuthenticated", true);
                    log.debug("Service authenticated successfully");
                }
            } catch (Exception e) {
                log.error("Service authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractServiceToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("X-Service-Token");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Service ")) {
            return headerAuth.substring(8);
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/oauth2/token") ||
                path.contains("/oauth2/authorize") ||
                path.contains("/actuator");
    }
}