package com.pxfintech.authentication_service.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class RateLimitConfigRequest {

    @NotBlank(message = "Endpoint pattern is required")
    private String endpointPattern;

    @NotNull(message = "Limit is required")
    @Min(value = 1, message = "Limit must be at least 1")
    private Integer limit;

    @NotNull(message = "Duration in seconds is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;

    private String clientId; // null for global limits

    private String httpMethod; // GET, POST, PUT, DELETE, null for all

    @Pattern(regexp = "^(IP|CLIENT|USER|GLOBAL)$",
            message = "Type must be IP, CLIENT, USER, or GLOBAL")
    private String type = "GLOBAL";
}