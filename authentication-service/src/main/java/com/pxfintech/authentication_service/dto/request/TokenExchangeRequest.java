package com.pxfintech.authentication_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenExchangeRequest {

    @NotBlank(message = "subject token is required")
    @JsonProperty("subject_token")
    private String subjectToken;

    @JsonProperty("subject_token_type")
    private String subjectTokenType = "urn:ietf:params:oauth:token-type:access_token";

    @JsonProperty("actor_token")
    private String actorToken;

    @JsonProperty("actor_token_type")
    private String actorTokenType;

    @JsonProperty("requested_token_type")
    private String requestedTokenType = "urn:ietf:params:oauth:token-type:access_token";

    private String scope;

    private String audience;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;
}