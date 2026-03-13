package com.fintech.walletservice.controller;

import com.fintech.walletservice.dto.request.CreateWalletRequest;
import com.fintech.walletservice.dto.response.WalletResponse;
import com.fintech.walletservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @RequestAttribute String userId,
            @Valid @RequestBody CreateWalletRequest request) {
        log.info("REST request to create wallet for user: {}", userId);
        WalletResponse response = walletService.createWallet(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(@RequestAttribute String userId) {
        log.info("REST request to get wallet for user: {}", userId);
        WalletResponse response = walletService.getWallet(UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable UUID userId) {
        log.info("REST request to get wallet for user: {}", userId);
        WalletResponse response = walletService.getWallet(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/freeze")
    public ResponseEntity<WalletResponse> freezeWallet(
            @RequestAttribute String userId,
            @RequestParam String reason) {
        log.info("REST request to freeze wallet for user: {}", userId);
        WalletResponse response = walletService.freezeWallet(UUID.fromString(userId), reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unfreeze")
    public ResponseEntity<WalletResponse> unfreezeWallet(@RequestAttribute String userId) {
        log.info("REST request to unfreeze wallet for user: {}", userId);
        WalletResponse response = walletService.unfreezeWallet(UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/limits")
    public ResponseEntity<WalletResponse> updateLimits(
            @RequestAttribute String userId,
            @RequestParam(required = false) BigDecimal dailyLimit,
            @RequestParam(required = false) BigDecimal monthlyLimit) {
        log.info("REST request to update limits for user: {}", userId);
        WalletResponse response = walletService.updateLimits(
                UUID.fromString(userId), dailyLimit, monthlyLimit);
        return ResponseEntity.ok(response);
    }
}