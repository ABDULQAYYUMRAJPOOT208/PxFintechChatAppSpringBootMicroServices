package com.fintech.payment_service.controller;

import com.fintech.payment_service.dto.request.P2PTransferRequest;
import com.fintech.payment_service.dto.response.PaymentResponse;
import com.fintech.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/p2p")
    public ResponseEntity<PaymentResponse> sendMoney(
            @RequestAttribute String userId,
            @Valid @RequestBody P2PTransferRequest request) {
        log.info("REST request for P2P transfer from user: {}", userId);
        PaymentResponse response = paymentService.processP2PTransfer(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String transactionId) {
        log.info("REST request for payment status: {}", transactionId);
        PaymentResponse response = paymentService.getPaymentStatus(transactionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @RequestAttribute String userId,
            @PathVariable String transactionId,
            @RequestParam String reason) {
        log.info("REST request for refund: {}", transactionId);
        PaymentResponse response = paymentService.refundPayment(userId, transactionId, reason);
        return ResponseEntity.ok(response);
    }
}