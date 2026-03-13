package com.fintech.walletservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    private BigDecimal balance;

    private String currency;

    private String status;

    @JsonProperty("daily_limit")
    private BigDecimal dailyLimit;

    @JsonProperty("monthly_limit")
    private BigDecimal monthlyLimit;

    @JsonProperty("daily_used")
    private BigDecimal dailyUsed;

    @JsonProperty("monthly_used")
    private BigDecimal monthlyUsed;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}