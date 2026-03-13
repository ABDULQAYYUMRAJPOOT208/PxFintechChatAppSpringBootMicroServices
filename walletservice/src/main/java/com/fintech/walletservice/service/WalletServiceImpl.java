package com.fintech.walletservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.walletservice.dto.request.CreateWalletRequest;
import com.fintech.walletservice.dto.request.CreditRequest;
import com.fintech.walletservice.dto.request.DebitRequest;
import com.fintech.walletservice.dto.response.BalanceResponse;
import com.fintech.walletservice.dto.response.WalletResponse;
import com.fintech.walletservice.exception.InsufficientBalanceException;
import com.fintech.walletservice.exception.LimitExceededException;
import com.fintech.walletservice.exception.WalletFrozenException;
import com.fintech.walletservice.exception.WalletNotFoundException;
import com.fintech.walletservice.lock.Lockable;
import com.fintech.walletservice.model.Wallet;
import com.fintech.walletservice.model.WalletTransaction;
import com.fintech.walletservice.repository.TransactionRepository;
import com.fintech.walletservice.repository.WalletRepository;
import com.fintech.walletservice.sharding.Shardable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${wallet.default-currency}")
    private String defaultCurrency;

    @Value("${wallet.default-daily-limit}")
    private BigDecimal defaultDailyLimit;

    @Value("${wallet.default-monthly-limit}")
    private BigDecimal defaultMonthlyLimit;

    @Override
    @Shardable
    @Transactional
    public WalletResponse createWallet(String userId, CreateWalletRequest request) {
        log.info("Creating wallet for user: {}", userId);

        UUID userUuid = UUID.fromString(userId);

        // Check if wallet already exists
        if (walletRepository.findByUserId(userUuid).isPresent()) {
            throw new IllegalArgumentException("Wallet already exists for user: " + userId);
        }

        // Create new wallet
        Wallet wallet = Wallet.builder()
                .userId(userUuid)
                .currency(request.getCurrency() != null ? request.getCurrency() : defaultCurrency)
                .dailyLimit(request.getDailyLimit() != null ?
                        request.getDailyLimit() : defaultDailyLimit)
                .monthlyLimit(request.getMonthlyLimit() != null ?
                        request.getMonthlyLimit() : defaultMonthlyLimit)
                .balance(BigDecimal.ZERO)
                .dailyUsed(BigDecimal.ZERO)
                .monthlyUsed(BigDecimal.ZERO)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        // Publish wallet created event
        publishWalletEvent(savedWallet, "WALLET_CREATED");

        log.info("Wallet created successfully with ID: {}", savedWallet.getId());

        return mapToResponse(savedWallet);
    }

    @Override
    @Shardable
    @Cacheable(value = "wallets", key = "#userId")
    public WalletResponse getWallet(UUID userId) {
        log.debug("Getting wallet for user: {}", userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        return mapToResponse(wallet);
    }

    @Override
    @Shardable
    @Cacheable(value = "balances", key = "#userId")
    public BalanceResponse getBalance(UUID userId) {
        log.debug("Getting balance for user: {}", userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        resetLimitsIfNeeded(wallet);

        BigDecimal availableBalance = calculateAvailableBalance(wallet);
        BigDecimal dailyRemaining = wallet.getDailyLimit().subtract(wallet.getDailyUsed());
        BigDecimal monthlyRemaining = wallet.getMonthlyLimit().subtract(wallet.getMonthlyUsed());

        return BalanceResponse.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .availableBalance(availableBalance)
                .dailyRemaining(dailyRemaining)
                .monthlyRemaining(monthlyRemaining)
                .build();
    }

    @Override
    @Shardable
    @Lockable
    @Transactional
    @CacheEvict(value = {"wallets", "balances"}, key = "#request.userId")
    public WalletResponse debit(DebitRequest request) {
        log.info("Processing debit for user: {} amount: {}", request.getUserId(), request.getAmount());

        // Check idempotency
        if (transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            log.info("Idempotent request - skipping duplicate debit: {}", request.getIdempotencyKey());
            return getWallet(request.getUserId());
        }

        Wallet wallet = walletRepository.findByUserIdWithLock(request.getUserId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + request.getUserId()));

        // Check if wallet is frozen
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new WalletFrozenException("Wallet is " + wallet.getStatus());
        }

        // Reset limits if needed
        resetLimitsIfNeeded(wallet);

        // Check sufficient balance
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Available: %s, Requested: %s",
                            wallet.getBalance(), request.getAmount()));
        }

        // Check daily limit
        BigDecimal newDailyUsed = wallet.getDailyUsed().add(request.getAmount());
        if (newDailyUsed.compareTo(wallet.getDailyLimit()) > 0) {
            throw new LimitExceededException("Daily limit exceeded. Remaining: " +
                    wallet.getDailyLimit().subtract(wallet.getDailyUsed()));
        }

        // Check monthly limit
        BigDecimal newMonthlyUsed = wallet.getMonthlyUsed().add(request.getAmount());
        if (newMonthlyUsed.compareTo(wallet.getMonthlyLimit()) > 0) {
            throw new LimitExceededException("Monthly limit exceeded. Remaining: " +
                    wallet.getMonthlyLimit().subtract(wallet.getMonthlyUsed()));
        }

        // Record balance before
        BigDecimal balanceBefore = wallet.getBalance();

        // Update wallet
        wallet.setBalance(balanceBefore.subtract(request.getAmount()));
        wallet.setDailyUsed(newDailyUsed);
        wallet.setMonthlyUsed(newMonthlyUsed);

        Wallet updatedWallet = walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .transactionId("DEB" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase())
                .idempotencyKey(request.getIdempotencyKey())
                .transactionType(WalletTransaction.TransactionType.DEBIT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(updatedWallet.getBalance())
                .currency(request.getCurrency())
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .referenceId(request.getReferenceId())
                .metadata(request.getMetadata())
                .build();

        transactionRepository.save(transaction);

        // Publish events
        publishBalanceChange(updatedWallet, transaction, "DEBIT");
        publishWalletEvent(updatedWallet, "BALANCE_UPDATED");

        log.info("Debit processed successfully. New balance: {}", updatedWallet.getBalance());

        return mapToResponse(updatedWallet);
    }

    @Override
    @Shardable
    @Lockable
    @Transactional
    @CacheEvict(value = {"wallets", "balances"}, key = "#request.userId")
    public WalletResponse credit(CreditRequest request) {
        log.info("Processing credit for user: {} amount: {}", request.getUserId(), request.getAmount());

        // Check idempotency
        if (transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            log.info("Idempotent request - skipping duplicate credit: {}", request.getIdempotencyKey());
            return getWallet(request.getUserId());
        }

        Wallet wallet = walletRepository.findByUserIdWithLock(request.getUserId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + request.getUserId()));

        // Check if wallet is frozen
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new WalletFrozenException("Wallet is " + wallet.getStatus());
        }

        // Record balance before
        BigDecimal balanceBefore = wallet.getBalance();

        // Update wallet
        wallet.setBalance(balanceBefore.add(request.getAmount()));

        Wallet updatedWallet = walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .transactionId("CRE" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase())
                .idempotencyKey(request.getIdempotencyKey())
                .transactionType(WalletTransaction.TransactionType.CREDIT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(updatedWallet.getBalance())
                .currency(request.getCurrency())
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .referenceId(request.getReferenceId())
                .metadata(request.getMetadata())
                .build();

        transactionRepository.save(transaction);

        // Publish events
        publishBalanceChange(updatedWallet, transaction, "CREDIT");
        publishWalletEvent(updatedWallet, "BALANCE_UPDATED");

        log.info("Credit processed successfully. New balance: {}", updatedWallet.getBalance());

        return mapToResponse(updatedWallet);
    }

    @Override
    @Shardable
    @Transactional
    @CacheEvict(value = {"wallets", "balances"}, key = "#userId")
    public WalletResponse freezeWallet(UUID userId, String reason) {
        log.info("Freezing wallet for user: {}", userId);

        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        wallet.setStatus(Wallet.WalletStatus.FROZEN);
        wallet.setFrozenReason(reason);
        wallet.setFrozenAt(Instant.now());

        Wallet updatedWallet = walletRepository.save(wallet);

        publishWalletEvent(updatedWallet, "WALLET_FROZEN");

        log.info("Wallet frozen successfully");

        return mapToResponse(updatedWallet);
    }

    @Override
    @Shardable
    @Transactional
    @CacheEvict(value = {"wallets", "balances"}, key = "#userId")
    public WalletResponse unfreezeWallet(UUID userId) {
        log.info("Unfreezing wallet for user: {}", userId);

        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        wallet.setFrozenReason(null);
        wallet.setFrozenAt(null);

        Wallet updatedWallet = walletRepository.save(wallet);

        publishWalletEvent(updatedWallet, "WALLET_UNFROZEN");

        log.info("Wallet unfrozen successfully");

        return mapToResponse(updatedWallet);
    }

    @Override
    @Shardable
    @Transactional
    @CacheEvict(value = {"wallets", "balances"}, key = "#userId")
    public WalletResponse updateLimits(UUID userId, BigDecimal dailyLimit, BigDecimal monthlyLimit) {
        log.info("Updating limits for user: {}", userId);

        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        if (dailyLimit != null) {
            wallet.setDailyLimit(dailyLimit);
        }

        if (monthlyLimit != null) {
            wallet.setMonthlyLimit(monthlyLimit);
        }

        Wallet updatedWallet = walletRepository.save(wallet);

        publishWalletEvent(updatedWallet, "LIMITS_UPDATED");

        log.info("Limits updated successfully");

        return mapToResponse(updatedWallet);
    }

    private void resetLimitsIfNeeded(Wallet wallet) {
        LocalDate today = LocalDate.now();

        // Reset daily limit
        if (wallet.getLastDailyReset() == null || wallet.getLastDailyReset().isBefore(today)) {
            wallet.setDailyUsed(BigDecimal.ZERO);
            wallet.setLastDailyReset(today);
        }

        // Reset monthly limit
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        if (wallet.getLastMonthlyReset() == null || wallet.getLastMonthlyReset().isBefore(firstOfMonth)) {
            wallet.setMonthlyUsed(BigDecimal.ZERO);
            wallet.setLastMonthlyReset(firstOfMonth);
        }
    }

    private BigDecimal calculateAvailableBalance(Wallet wallet) {
        // Subtract any pending holds
        // For now, just return balance
        return wallet.getBalance();
    }

    private void publishBalanceChange(Wallet wallet, WalletTransaction transaction, String type) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", wallet.getUserId());
        event.put("walletId", wallet.getId());
        event.put("transactionId", transaction.getTransactionId());
        event.put("type", type);
        event.put("amount", transaction.getAmount());
        event.put("balance", wallet.getBalance());
        event.put("timestamp", Instant.now().toString());

        kafkaTemplate.send("balance-changes", wallet.getUserId().toString(), event);
    }

    private void publishWalletEvent(Wallet wallet, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", wallet.getUserId());
        event.put("walletId", wallet.getId());
        event.put("eventType", eventType);
        event.put("balance", wallet.getBalance());
        event.put("status", wallet.getStatus());
        event.put("timestamp", Instant.now().toString());

        kafkaTemplate.send("wallet-events", wallet.getUserId().toString(), event);
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .dailyLimit(wallet.getDailyLimit())
                .monthlyLimit(wallet.getMonthlyLimit())
                .dailyUsed(wallet.getDailyUsed())
                .monthlyUsed(wallet.getMonthlyUsed())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}