package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.dto.request.ServiceTokenRequest;
import com.pxfintech.authentication_service.dto.request.TokenExchangeRequest;
import com.pxfintech.authentication_service.dto.response.TokenExchangeResponse;
import com.pxfintech.authentication_service.dto.response.TokenResponse;

public interface ServiceTokenService {

    TokenResponse generateServiceToken(ServiceTokenRequest request);

    TokenExchangeResponse exchangeToken(TokenExchangeRequest request);

    boolean validateServiceToken(String token, String requiredScope, String targetService);

    void registerService(String serviceName, String description, String[] allowedScopes);

    void rotateServiceSecret(String serviceName);
}