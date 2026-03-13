package com.fintech.payment_service.service;

import com.fintech.payment_service.dto.request.P2PTransferRequest;
import com.fintech.payment_service.dto.request.POSPaymentRequest;
import com.fintech.payment_service.dto.response.PaymentResponse;
import com.fintech.payment_service.dto.response.QRCodeResponse;
import com.fintech.payment_service.exception.InsufficientFundsException;
import com.fintech.payment_service.exception.PaymentFailedException;
import com.fintech.payment_service.exception.TransactionNotFoundException;
import com.fintech.payment_service.model.Transaction;
import com.fintech.payment_service.repository.TransactionRepository;
import com.fintech.payment_service.saga.SagaOrchestrator;
import com.fintech.payment_service.service.client.WalletServiceClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final SagaOrchestrator sagaOrchestrator;
    private final TransactionRepository transactionRepository;
    private final WalletServiceClient walletServiceClient;
    private final FraudDetectionService fraudDetectionService;
    private final IdempotencyService idempotencyService;

    @Value("${payment.min-amount}")
    private BigDecimal minAmount;

    @Value("${payment.max-amount}")
    private BigDecimal maxAmount;

    @Override
    public PaymentResponse processP2PTransfer(String userId, P2PTransferRequest request) {
        log.info("Processing P2P transfer from user: {} to: {} amount: {}",
                userId, request.getReceiverId(), request.getAmount());

        // Validate amount
        validateAmount(request.getAmount());

        // Check fraud
        FraudDetectionService.FraudDetectionResult fraudResult =
                fraudDetectionService.analyzeP2PTransfer(userId, request);

        if (!fraudResult.isAllowed()) {
            log.warn("P2P transfer blocked by fraud detection: {}", fraudResult);
            throw new PaymentFailedException("Transaction blocked by fraud detection: " + fraudResult.getAction());
        }

        // Execute SAGA
        SagaOrchestrator.SagaResult result = sagaOrchestrator.executeP2PTransfer(userId, request);

        if (!result.isSuccess()) {
            throw new PaymentFailedException("Payment failed: " + result.getMessage());
        }

        Transaction transaction = result.getTransaction();

        return mapToResponse(transaction);
    }

    @Override
    public PaymentResponse processPOSPayment(String userId, POSPaymentRequest request) {
        log.info("Processing POS payment from user: {} to merchant: {} amount: {}",
                userId, request.getMerchantId(), request.getAmount());

        validateAmount(request.getAmount());

        // Check fraud
        FraudDetectionService.FraudDetectionResult fraudResult =
                fraudDetectionService.analyzePOSPayment(userId, request);

        if (!fraudResult.isAllowed()) {
            throw new PaymentFailedException("POS payment blocked by fraud detection");
        }

        // For POS, we'll use a simplified flow (will be expanded with actual POS integration)
        // In real implementation, this would interact with a payment terminal API

        P2PTransferRequest p2pRequest = new P2PTransferRequest();
        p2pRequest.setReceiverId(request.getMerchantId());
        p2pRequest.setAmount(request.getAmount());
        p2pRequest.setCurrency(request.getCurrency());
        p2pRequest.setDescription(request.getDescription());
        p2pRequest.setIdempotencyKey(request.getIdempotencyKey());

        SagaOrchestrator.SagaResult result = sagaOrchestrator.executeP2PTransfer(userId, p2pRequest);

        if (!result.isSuccess()) {
            throw new PaymentFailedException("POS payment failed: " + result.getMessage());
        }

        return mapToResponse(result.getTransaction());
    }

    @Override
    public PaymentResponse processQRPayment(String userId, String qrData) {
        log.info("Processing QR payment from user: {} with data: {}", userId, qrData);

        // Parse QR data (format: PAY|merchantId|amount|currency|ref)
        String[] parts = qrData.split("\\|");
        if (parts.length < 4 || !"PAY".equals(parts[0])) {
            throw new PaymentFailedException("Invalid QR code data");
        }

        UUID merchantId = UUID.fromString(parts[1]);
        BigDecimal amount = new BigDecimal(parts[2]);
        String currency = parts[3];
        String reference = parts.length > 4 ? parts[4] : null;

        validateAmount(amount);

        P2PTransferRequest request = new P2PTransferRequest();
        request.setReceiverId(merchantId);
        request.setAmount(amount);
        request.setCurrency(currency);
        request.setDescription("QR Payment " + (reference != null ? reference : ""));
        request.setIdempotencyKey("qr:" + UUID.randomUUID().toString());

        SagaOrchestrator.SagaResult result = sagaOrchestrator.executeP2PTransfer(userId, request);

        if (!result.isSuccess()) {
            throw new PaymentFailedException("QR payment failed: " + result.getMessage());
        }

        return mapToResponse(result.getTransaction());
    }

    @Override
    public <QRCodeWriter> QRCodeResponse generateQRCode(String userId, String amountStr, String currency) {
        log.info("Generating QR code for user: {} amount: {}", userId, amountStr);

        BigDecimal amount = new BigDecimal(amountStr);
        validateAmount(amount);

        // Create QR data
        String qrData = String.format("PAY|%s|%s|%s|%d",
                userId, amountStr, currency, System.currentTimeMillis());

        try {
            // Generate QR code image
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            String qrBase64 = Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());

            return QRCodeResponse.builder()
                    .qrCode(qrBase64)
                    .qrData(qrData)
                    .paymentId("QR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .expiresIn(300) // 5 minutes
                    .amount(amountStr)
                    .currency(currency)
                    .build();

        } catch (WriterException e) {
            log.error("Failed to generate QR code", e);
            throw new PaymentFailedException("Failed to generate QR code");
        }
    }

    @Override
    public PaymentResponse getPaymentStatus(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));

        return mapToResponse(transaction);
    }

    @Override
    public PaymentResponse refundPayment(String userId, String transactionId, String reason) {
        Transaction originalTransaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));

        // Verify user is either sender or receiver
        if (!originalTransaction.getSenderId().toString().equals(userId) &&
                !originalTransaction.getReceiverId().toString().equals(userId)) {
            throw new PaymentFailedException("User not authorized to refund this transaction");
        }

        // Create refund transaction
        P2PTransferRequest refundRequest = new P2PTransferRequest();
        refundRequest.setReceiverId(originalTransaction.getSenderId()); // Send money back to sender
        refundRequest.setAmount(originalTransaction.getAmount());
        refundRequest.setCurrency(originalTransaction.getCurrency());
        refundRequest.setDescription("Refund: " + reason);
        refundRequest.setIdempotencyKey("refund:" + transactionId);

        SagaOrchestrator.SagaResult result = sagaOrchestrator.executeP2PTransfer(
                originalTransaction.getReceiverId().toString(), refundRequest);

        if (!result.isSuccess()) {
            throw new PaymentFailedException("Refund failed: " + result.getMessage());
        }

        return mapToResponse(result.getTransaction());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(minAmount) < 0) {
            throw new PaymentFailedException("Amount below minimum: " + minAmount);
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw new PaymentFailedException("Amount above maximum: " + maxAmount);
        }
    }

    private PaymentResponse mapToResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .senderId(transaction.getSenderId())
                .receiverId(transaction.getReceiverId())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .message(transaction.getErrorMessage())
                .build();
    }
}