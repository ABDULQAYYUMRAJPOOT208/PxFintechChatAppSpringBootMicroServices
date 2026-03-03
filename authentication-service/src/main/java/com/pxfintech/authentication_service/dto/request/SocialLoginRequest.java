package com.pxfintech.authentication_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocialLoginRequest {

    @NotBlank(message = "Provider is required")
    private String provider; // google, facebook, apple

    @NotBlank(message = "Access token is required")
    private String accessToken;

    private String idToken; // For Google/Apple

    private String deviceId;

    private String deviceName;

    private Boolean linkAccount = false; // Whether to link to existing account

    private String existingPhoneNumber; // If linking to existing account
}