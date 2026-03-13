package com.fintech.walletservice.service;

import com.fintech.walletservice.dto.request.CreateWalletRequest;
import com.fintech.walletservice.dto.request.DebitRequest;
import com.fintech.walletservice.dto.request.CreditRequest;
import com.fintech.walletservice.dto.response.BalanceResponse;
import com.fintech.walletservice.dto.response.WalletResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    WalletResponse createWallet(String userId, CreateWalletRequest request);

    WalletResponse getWallet(UUID userId);

    BalanceResponse getBalance(UUID userId);

    WalletResponse debit(DebitRequest request);

    WalletResponse credit(CreditRequest request);

    WalletResponse freezeWallet(UUID userId, String reason);

    WalletResponse unfreezeWallet(UUID userId);

    WalletResponse updateLimits(UUID userId, BigDecimal dailyLimit, BigDecimal monthlyLimit);
}