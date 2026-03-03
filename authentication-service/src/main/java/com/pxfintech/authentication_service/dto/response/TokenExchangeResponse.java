package com.pxfintech.authentication_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenExchangeResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("issued_token_type")
    private String issuedTokenType;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;

    private String scope;
}