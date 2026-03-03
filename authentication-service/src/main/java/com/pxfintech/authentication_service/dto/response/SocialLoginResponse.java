package com.pxfintech.authentication_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SocialLoginResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("is_new_user")
    private Boolean isNewUser;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("needs_phone_verification")
    private Boolean needsPhoneVerification;
}