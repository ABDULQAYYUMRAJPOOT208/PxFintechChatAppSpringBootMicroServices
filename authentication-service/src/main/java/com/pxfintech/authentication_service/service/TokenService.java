package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.dto.response.TokenIntrospectionResponse;
import com.pxfintech.authentication_service.dto.response.TokenResponse;

public interface TokenService {

    TokenResponse createAccessToken(String userId, String clientId, String scope);

    TokenResponse refreshAccessToken(String refreshToken, String clientId);

    TokenIntrospectionResponse introspectToken(String token);

    void revokeToken(String token, String userId, String clientId);

    boolean validateToken(String token);
}