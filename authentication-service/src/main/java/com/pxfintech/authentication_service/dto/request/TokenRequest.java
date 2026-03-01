package com.pxfintech.authentication_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRequest {

    @NotBlank(message = "Grant type is required")
    private String grantType;

    private String code;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String codeVerifier;
}