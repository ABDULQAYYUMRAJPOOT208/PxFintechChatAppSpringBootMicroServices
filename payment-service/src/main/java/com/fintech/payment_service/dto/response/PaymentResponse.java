package com.fintech.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("intent_id")
    private String intentId;

    private String status;

    private BigDecimal amount;

    private String currency;

    @JsonProperty("sender_id")
    private UUID senderId;

    @JsonProperty("receiver_id")
    private UUID receiverId;

    private String description;

    @JsonProperty("client_secret")
    private String clientSecret; // For card payments

    @JsonProperty("receipt_url")
    private String receiptUrl;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("requires_action")
    private boolean requiresAction;

    @JsonProperty("action_type")
    private String actionType; // 3D_SECURE, OTP

    private String message;
}