package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.dto.request.ServiceTokenRequest;
import com.pxfintech.authentication_service.dto.request.TokenExchangeRequest;
import com.pxfintech.authentication_service.dto.response.TokenExchangeResponse;
import com.pxfintech.authentication_service.dto.response.TokenResponse;
import com.pxfintech.authentication_service.exception.InvalidTokenException;
import com.pxfintech.authentication_service.exception.ServiceNotFoundException;
import com.pxfintech.authentication_service.model.entity.ServiceRegistry;
import com.pxfintech.authentication_service.repository.ServiceRegistryRepository;
import com.pxfintech.authentication_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceTokenServiceImpl implements ServiceTokenService {

    private final ServiceRegistryRepository serviceRegistryRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final BlacklistService blacklistService;

    @Value("${jwt.service-token-expiration:3600}")
    private Long serviceTokenExpiration;

    private static final String SERVICE_TOKEN_PREFIX = "service:token:";
    private static final String SERVICE_RATE_LIMIT_PREFIX = "service:ratelimit:";
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public TokenResponse generateServiceToken(ServiceTokenRequest request) {
        log.info("Generating service token for: {}", request.getServiceName());

        // Validate service credentials
        ServiceRegistry service = serviceRegistryRepository.findByServiceName(request.getServiceName())
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + request.getServiceName()));

        if (!service.getIsActive()) {
            throw new InvalidTokenException("Service account is deactivated");
        }

        if (!passwordEncoder.matches(request.getServiceSecret(), service.getServiceSecret())) {
            throw new InvalidTokenException("Invalid service credentials");
        }

        // Check rate limit
        if (!checkRateLimit(service)) {
            throw new InvalidTokenException("Rate limit exceeded for service: " + request.getServiceName());
        }

        // Validate target service if specified
        if (request.getTargetService() != null && !isTargetAllowed(service, request.getTargetService())) {
            throw new InvalidTokenException("Target service not allowed: " + request.getTargetService());
        }

        // Validate scope
        String scope = request.getScope() != null ? request.getScope() : "service";
        if (!isScopeAllowed(service, scope)) {
            throw new InvalidTokenException("Scope not allowed: " + scope);
        }

        // Generate service token
        Map<String, Object> claims = new HashMap<>();
        claims.put("serviceId", service.getServiceId());
        claims.put("serviceName", service.getServiceName());
        claims.put("type", "service");
        claims.put("tokenType", "service");
        claims.put("scope", scope);
        if (request.getTargetService() != null) {
            claims.put("targetService", request.getTargetService());
        }
        claims.put("jti", generateTokenId());

        String accessToken = jwtTokenProvider.createTokenWithCustomClaims(claims, service.getServiceName(), serviceTokenExpiration);

        // Store token info in Valkey for introspection
        storeTokenInfo(accessToken, service, scope);

        log.info("Service token generated for: {} with scope: {}", request.getServiceName(), scope);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(serviceTokenExpiration)
                .scope(scope)
                .build();
    }

    @Override
    public TokenExchangeResponse exchangeToken(TokenExchangeRequest request) {
        log.info("Exchanging token of type: {}", request.getSubjectTokenType());

        // Validate the subject token
        if (!jwtTokenProvider.validateToken(request.getSubjectToken())) {
            throw new InvalidTokenException("Invalid subject token");
        }

        // Check if token is blacklisted
        if (blacklistService.isTokenBlacklisted(request.getSubjectToken())) {
            throw new InvalidTokenException("Subject token has been revoked");
        }

        // Extract information from subject token
        String subject = jwtTokenProvider.getSubjectFromToken(request.getSubjectToken());
        String tokenType = jwtTokenProvider.getClaimFromToken(request.getSubjectToken(),
                claims -> claims.get("tokenType", String.class));
        String userId = jwtTokenProvider.getClaimFromToken(request.getSubjectToken(),
                claims -> claims.get("userId", String.class));
        String originalScope = jwtTokenProvider.getClaimFromToken(request.getSubjectToken(),
                claims -> claims.get("scope", String.class));

        // Determine new scope (requested scope or subset of original)
        String newScope = determineScope(originalScope, request.getScope());

        // Create exchanged token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("originalTokenType", tokenType);
        claims.put("exchanged", true);
        claims.put("exchangeTime", System.currentTimeMillis());
        claims.put("scope", newScope);
        claims.put("jti", generateTokenId());

        if (request.getAudience() != null) {
            claims.put("audience", request.getAudience());
        }

        if (request.getActorToken() != null) {
            // Handle delegation/impersonation
            validateActorToken(request.getActorToken());
            claims.put("actor", extractActorInfo(request.getActorToken()));
        }

        String exchangedToken = jwtTokenProvider.createTokenWithCustomClaims(
                claims, subject, serviceTokenExpiration);

        log.info("Token exchanged successfully for subject: {}", subject);

        return TokenExchangeResponse.builder()
                .accessToken(exchangedToken)
                .issuedTokenType(request.getRequestedTokenType())
                .tokenType("Bearer")
                .expiresIn(serviceTokenExpiration)
                .scope(newScope)
                .build();
    }

    @Override
    public boolean validateServiceToken(String token, String requiredScope, String targetService) {
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                return false;
            }

            // Check if token is blacklisted
            if (blacklistService.isTokenBlacklisted(token)) {
                return false;
            }

            // Verify it's a service token
            String tokenType = jwtTokenProvider.getClaimFromToken(token,
                    claims -> claims.get("tokenType", String.class));
            if (!"service".equals(tokenType)) {
                return false;
            }

            // Check scope
            String scope = jwtTokenProvider.getClaimFromToken(token,
                    claims -> claims.get("scope", String.class));
            if (requiredScope != null && !scope.contains(requiredScope)) {
                return false;
            }

            // Check target service if specified
            if (targetService != null) {
                String tokenTarget = jwtTokenProvider.getClaimFromToken(token,
                        claims -> claims.get("targetService", String.class));
                if (tokenTarget != null && !tokenTarget.equals(targetService)) {
                    return false;
                }
            }

            // Check if service is still active
            String serviceName = jwtTokenProvider.getSubjectFromToken(token);
            ServiceRegistry service = serviceRegistryRepository.findByServiceName(serviceName).orElse(null);
            if (service == null || !service.getIsActive()) {
                return false;
            }

            // Check rate limit
            if (!checkRateLimit(service)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Service token validation error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void registerService(String serviceName, String description, String[] allowedScopes) {
        log.info("Registering new service: {}", serviceName);

        if (serviceRegistryRepository.existsByServiceName(serviceName)) {
            throw new InvalidTokenException("Service already exists: " + serviceName);
        }

        String serviceId = generateServiceId(serviceName);
        String serviceSecret = generateServiceSecret();

        ServiceRegistry service = ServiceRegistry.builder()
                .serviceName(serviceName)
                .serviceId(serviceId)
                .serviceSecret(passwordEncoder.encode(serviceSecret))
                .description(description)
                .allowedScopes(allowedScopes)
                .isActive(true)
                .rateLimit(1000) // Default rate limit
                .build();

        serviceRegistryRepository.save(service);

        log.info("Service registered successfully. Service ID: {}, Secret: {}", serviceId, serviceSecret);
        // In production, return the secret securely to the caller
    }

    @Override
    @Transactional
    public void rotateServiceSecret(String serviceName) {
        ServiceRegistry service = serviceRegistryRepository.findByServiceName(serviceName)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceName));

        String newSecret = generateServiceSecret();
        service.setServiceSecret(passwordEncoder.encode(newSecret));
        service.setUpdatedAt(LocalDateTime.now());

        serviceRegistryRepository.save(service);

        log.info("Service secret rotated for: {}. New secret: {}", serviceName, newSecret);
    }

    private boolean checkRateLimit(ServiceRegistry service) {
        String key = SERVICE_RATE_LIMIT_PREFIX + service.getServiceId();
        String current = redisTemplate.opsForValue().get(key);

        if (current == null) {
            redisTemplate.opsForValue().set(key, "1", 1, TimeUnit.MINUTES);
            return true;
        }

        int count = Integer.parseInt(current);
        if (count >= service.getRateLimit()) {
            return false;
        }

        redisTemplate.opsForValue().increment(key);
        return true;
    }

    private boolean isTargetAllowed(ServiceRegistry service, String targetService) {
        if (service.getAllowedTargets() == null || service.getAllowedTargets().length == 0) {
            return true; // No restrictions
        }
        return Arrays.asList(service.getAllowedTargets()).contains(targetService);
    }

    private boolean isScopeAllowed(ServiceRegistry service, String scope) {
        if (service.getAllowedScopes() == null || service.getAllowedScopes().length == 0) {
            return true; // No restrictions
        }
        return Arrays.asList(service.getAllowedScopes()).contains(scope);
    }

    private String determineScope(String originalScope, String requestedScope) {
        if (requestedScope == null) {
            return originalScope;
        }

        // Ensure requested scope is a subset of original scope
        String[] originalScopes = originalScope.split(" ");
        String[] requestedScopes = requestedScope.split(" ");

        for (String req : requestedScopes) {
            boolean found = false;
            for (String orig : originalScopes) {
                if (orig.equals(req)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new InvalidTokenException("Requested scope not allowed: " + req);
            }
        }

        return requestedScope;
    }

    private void validateActorToken(String actorToken) {
        if (!jwtTokenProvider.validateToken(actorToken)) {
            throw new InvalidTokenException("Invalid actor token");
        }
    }

    private Map<String, Object> extractActorInfo(String actorToken) {
        Map<String, Object> actorInfo = new HashMap<>();
        actorInfo.put("id", jwtTokenProvider.getUserIdFromToken(actorToken));
        actorInfo.put("type", jwtTokenProvider.getClaimFromToken(actorToken,
                claims -> claims.get("tokenType", String.class)));
        return actorInfo;
    }

    private void storeTokenInfo(String token, ServiceRegistry service, String scope) {
        String tokenId = jwtTokenProvider.getTokenId(token);
        String key = SERVICE_TOKEN_PREFIX + tokenId;

        Map<String, String> tokenInfo = new HashMap<>();
        tokenInfo.put("serviceId", service.getServiceId());
        tokenInfo.put("serviceName", service.getServiceName());
        tokenInfo.put("scope", scope);
        tokenInfo.put("issuedAt", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(key, tokenInfo);
        redisTemplate.expire(key, serviceTokenExpiration, TimeUnit.SECONDS);
    }

    private String generateServiceId(String serviceName) {
        String prefix = serviceName.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String random = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + random;
    }

    private String generateServiceSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateTokenId() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}