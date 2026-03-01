package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.dto.response.TokenIntrospectionResponse;
import com.pxfintech.authentication_service.dto.response.TokenResponse;
import com.pxfintech.authentication_service.exception.InvalidTokenException;
import com.pxfintech.authentication_service.exception.TokenExpiredException;
import com.pxfintech.authentication_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistService blacklistService;

    @Override
    public TokenResponse createAccessToken(String userId, String clientId, String scope) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, clientId, scope);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, clientId);

        log.info("Access token created for user: {} and client: {}", userId, clientId);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .scope(scope)
                .userId(userId)
                .build();
    }

    @Override
    public TokenResponse refreshAccessToken(String refreshToken, String clientId) {
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        // Check if token is blacklisted
        if (blacklistService.isTokenBlacklisted(refreshToken)) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        // Verify token type
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        // Verify client ID
        String tokenClientId = jwtTokenProvider.getClientIdFromToken(refreshToken);
        if (!clientId.equals(tokenClientId)) {
            throw new InvalidTokenException("Token was not issued for this client");
        }

        // Check expiration
        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new TokenExpiredException("Refresh token has expired");
        }

        // Get user ID from token
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String scope = jwtTokenProvider.getScope(refreshToken);

        // Blacklist the old refresh token
        blacklistService.blacklistToken(refreshToken, "REFRESHED", userId, clientId);

        // Create new tokens
        return createAccessToken(userId, clientId, scope);
    }

    @Override
    public TokenIntrospectionResponse introspectToken(String token) {
        boolean active = false;
        String userId = null;
        String clientId = null;
        Long expiresAt = null;
        Long issuedAt = null;
        String scope = null;
        String tokenType = null;

        try {
            if (jwtTokenProvider.validateToken(token) && !blacklistService.isTokenBlacklisted(token)) {
                active = true;
                userId = jwtTokenProvider.getUserIdFromToken(token);
                clientId = jwtTokenProvider.getClientIdFromToken(token);
                scope = jwtTokenProvider.getScope(token);
                tokenType = jwtTokenProvider.getTokenType(token);

                Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
                Date issued = jwtTokenProvider.getClaimFromToken(token, claims -> claims.getIssuedAt());

                expiresAt = expiration.getTime() / 1000;
                issuedAt = issued.getTime() / 1000;
            }
        } catch (Exception e) {
            log.debug("Token introspection failed: {}", e.getMessage());
        }

        return TokenIntrospectionResponse.builder()
                .active(active)
                .clientId(clientId)
                .userId(userId)
                .expiresAt(expiresAt)
                .issuedAt(issuedAt)
                .scope(scope)
                .tokenType(tokenType)
                .build();
    }

    @Override
    public void revokeToken(String token, String userId, String clientId) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new InvalidTokenException("Invalid token");
        }

        blacklistService.blacklistToken(token, "REVOKED", userId, clientId);
        log.info("Token revoked for user: {}", userId);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            return jwtTokenProvider.validateToken(token) && !blacklistService.isTokenBlacklisted(token);
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }
}