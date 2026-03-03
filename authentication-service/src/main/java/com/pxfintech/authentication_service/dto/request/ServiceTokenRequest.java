package com.pxfintech.authentication_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServiceTokenRequest {

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotBlank(message = "Service secret is required")
    private String serviceSecret;

    private String targetService;

    private String scope;
}