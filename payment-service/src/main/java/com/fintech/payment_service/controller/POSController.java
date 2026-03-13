package com.fintech.payment_service.controller;

import com.fintech.payment_service.dto.request.POSPaymentRequest;
import com.fintech.payment_service.dto.response.PaymentResponse;
import com.fintech.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pos")
@RequiredArgsConstructor
@Slf4j
public class POSController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> processPOSPayment(
            @RequestAttribute String userId,
            @Valid @RequestBody POSPaymentRequest request) {
        log.info("REST request for POS payment from user: {} at merchant: {}",
                userId, request.getMerchantId());
        PaymentResponse response = paymentService.processPOSPayment(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/qr/generate")
    public ResponseEntity<QRCodeResponse> generateQRCode(
            @RequestAttribute String userId,
            @RequestParam String amount,
            @RequestParam(defaultValue = "USD") String currency) {
        log.info("REST request to generate QR code for amount: {} {}", amount, currency);
        QRCodeResponse response = paymentService.generateQRCode(userId, amount, currency);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/qr/pay")
    public ResponseEntity<PaymentResponse> processQRPayment(
            @RequestAttribute String userId,
            @RequestParam String qrData) {
        log.info("REST request to process QR payment");
        PaymentResponse response = paymentService.processQRPayment(userId, qrData);
        return ResponseEntity.ok(response);
    }
}