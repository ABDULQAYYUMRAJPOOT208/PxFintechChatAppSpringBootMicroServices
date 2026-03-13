package com.fintech.payment_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class POSPaymentRequest {

    @NotNull(message = "Merchant ID is required")
    private UUID merchantId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.50", message = "Minimum amount is 0.50")
    @DecimalMax(value = "10000.00", message = "Maximum amount is 10000.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    private String currency;

    @NotBlank(message = "Payment method is required")
    private String paymentMethodType; // CARD, WALLET, QR

    private String cardId; // If paying with saved card

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    private String terminalId;
    private String location;
}