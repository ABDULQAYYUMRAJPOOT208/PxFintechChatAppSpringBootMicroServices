package com.fintech.walletservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BalanceResponse {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("wallet_id")
    private UUID walletId;

    private BigDecimal balance;

    private String currency;

    @JsonProperty("available_balance")
    private BigDecimal availableBalance;

    @JsonProperty("daily_remaining")
    private BigDecimal dailyRemaining;

    @JsonProperty("monthly_remaining")
    private BigDecimal monthlyRemaining;
}