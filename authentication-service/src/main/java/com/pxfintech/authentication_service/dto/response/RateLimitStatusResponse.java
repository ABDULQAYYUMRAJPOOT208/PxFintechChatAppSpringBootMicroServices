package com.pxfintech.authentication_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RateLimitStatusResponse {

    @JsonProperty("remaining")
    private Long remaining;

    @JsonProperty("limit")
    private Long limit;

    @JsonProperty("reset_in_seconds")
    private Long resetInSeconds;

    @JsonProperty("retry_after_seconds")
    private Long retryAfterSeconds;

    @JsonProperty("is_rate_limited")
    private Boolean isRateLimited;
}