package com.fintech.payment_service.service;

import com.fintech.payment_service.dto.request.P2PTransferRequest;
import com.fintech.payment_service.dto.request.POSPaymentRequest;
import com.fintech.payment_service.dto.response.PaymentResponse;
import com.fintech.payment_service.dto.response.QRCodeResponse;

public interface PaymentService {

    PaymentResponse processP2PTransfer(String userId, P2PTransferRequest request);

    PaymentResponse processPOSPayment(String userId, POSPaymentRequest request);

    PaymentResponse processQRPayment(String userId, String qrData);

    QRCodeResponse generateQRCode(String userId, String amount, String currency);

    PaymentResponse getPaymentStatus(String transactionId);

    PaymentResponse refundPayment(String userId, String transactionId, String reason);
}