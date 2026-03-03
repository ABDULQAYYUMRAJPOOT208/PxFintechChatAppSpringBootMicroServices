package com.pxfintech.authentication_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ClientRegistrationRequest {

    @NotBlank(message = "Client name is required")
    private String clientName;

    @NotBlank(message = "Client type is required")
    private String clientType;

    @NotEmpty(message = "At least one grant type is required")
    private List<String> grantTypes;

    private List<String> redirectUris;

    @NotEmpty(message = "At least one scope is required")
    private List<String> scopes;

    private List<String> authenticationMethods;

    private Integer accessTokenValidity = 3600;

    private Integer refreshTokenValidity = 86400;

    @NotNull(message = "Require proof key flag is required")
    private Boolean requireProofKey;

    @NotNull(message = "Require consent flag is required")
    private Boolean requireAuthorizationConsent;
}