package com.pxfintech.authentication_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String userId, String clientId, String scope) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("clientId", clientId);
        claims.put("scope", scope);
        claims.put("tokenType", "access");
        claims.put("jti", UUID.randomUUID().toString());

        return createToken(claims, userId, accessTokenExpiration);
    }

    public String generateRefreshToken(String userId, String clientId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("clientId", clientId);
        claims.put("tokenType", "refresh");
        claims.put("jti", UUID.randomUUID().toString());

        return createToken(claims, userId, refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiry) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getTokenId(String token) {
        return getClaimFromToken(token, claims -> claims.get("jti", String.class));
    }

    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("userId", String.class));
    }

    public String getClientIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("clientId", String.class));
    }

    public String getTokenType(String token) {
        return getClaimFromToken(token, claims -> claims.get("tokenType", String.class));
    }

    public String getScope(String token) {
        return getClaimFromToken(token, claims -> claims.get("scope", String.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public LocalDateTime getExpirationDateTime(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return Instant.ofEpochMilli(expiration.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}