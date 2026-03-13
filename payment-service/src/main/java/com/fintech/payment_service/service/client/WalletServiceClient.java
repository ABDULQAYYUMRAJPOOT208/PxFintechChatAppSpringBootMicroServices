package com.fintech.payment_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "wallet-service", url = "${service.wallet.url}")
public interface WalletServiceClient {

    @PostMapping("/wallets/debit")
    WalletResponse debit(@RequestParam UUID userId,
                         @RequestParam BigDecimal amount,
                         @RequestParam String currency,
                         @RequestParam String idempotencyKey);

    @PostMapping("/wallets/credit")
    WalletResponse credit(@RequestParam UUID userId,
                          @RequestParam BigDecimal amount,
                          @RequestParam String currency,
                          @RequestParam String idempotencyKey);

    @GetMapping("/wallets/balance/{userId}")
    BalanceResponse getBalance(@PathVariable UUID userId);

    @lombok.Data
    class WalletResponse {
        private boolean success;
        private String message;
        private String transactionId;
        private BigDecimal newBalance;
    }

    @lombok.Data
    class BalanceResponse {
        private UUID userId;
        private BigDecimal balance;
        private String currency;
    }
}