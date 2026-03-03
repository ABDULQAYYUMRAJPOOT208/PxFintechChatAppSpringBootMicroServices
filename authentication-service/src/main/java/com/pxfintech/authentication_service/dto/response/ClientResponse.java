package com.pxfintech.authentication_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ClientResponse {

    private UUID id;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("client_type")
    private String clientType;

    @JsonProperty("grant_types")
    private List<String> grantTypes;

    @JsonProperty("redirect_uris")
    private List<String> redirectUris;

    @JsonProperty("scopes")
    private List<String> scopes;

    @JsonProperty("authentication_methods")
    private List<String> authenticationMethods;

    @JsonProperty("access_token_validity")
    private Integer accessTokenValidity;

    @JsonProperty("refresh_token_validity")
    private Integer refreshTokenValidity;

    @JsonProperty("require_proof_key")
    private Boolean requireProofKey;

    @JsonProperty("require_consent")
    private Boolean requireAuthorizationConsent;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}