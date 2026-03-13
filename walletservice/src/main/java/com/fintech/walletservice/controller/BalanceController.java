package com.fintech.walletservice.controller;

import com.fintech.walletservice.dto.request.CreditRequest;
import com.fintech.walletservice.dto.request.DebitRequest;
import com.fintech.walletservice.dto.response.BalanceResponse;
import com.fintech.walletservice.dto.response.WalletResponse;
import com.fintech.walletservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/balance")
@RequiredArgsConstructor
@Slf4j
public class BalanceController {

    private final WalletService walletService;

    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID userId) {
        log.info("REST request to get balance for user: {}", userId);
        BalanceResponse response = walletService.getBalance(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<BalanceResponse> getMyBalance(@RequestAttribute String userId) {
        log.info("REST request to get balance for current user");
        BalanceResponse response = walletService.getBalance(UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/debit")
    public ResponseEntity<WalletResponse> debit(@Valid @RequestBody DebitRequest request) {
        log.info("REST request to debit wallet for user: {} amount: {}",
                request.getUserId(), request.getAmount());
        WalletResponse response = walletService.debit(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/credit")
    public ResponseEntity<WalletResponse> credit(@Valid @RequestBody CreditRequest request) {
        log.info("REST request to credit wallet for user: {} amount: {}",
                request.getUserId(), request.getAmount());
        WalletResponse response = walletService.credit(request);
        return ResponseEntity.ok(response);
    }
}