package com.pxfintech.authentication_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenIntrospectionResponse {

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("exp")
    private Long expiresAt;

    @JsonProperty("iat")
    private Long issuedAt;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("token_type")
    private String tokenType;
}