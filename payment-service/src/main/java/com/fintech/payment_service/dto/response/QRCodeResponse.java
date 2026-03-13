package com.fintech.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QRCodeResponse {

    @JsonProperty("qr_code")
    private String qrCode; // Base64 encoded QR image

    @JsonProperty("qr_data")
    private String qrData; // The data encoded in QR

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("expires_in")
    private long expiresIn; // seconds

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;
}