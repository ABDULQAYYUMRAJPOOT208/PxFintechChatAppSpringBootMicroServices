package com.fintech.walletservice.repository;

import com.fintech.walletservice.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findByTransactionId(String transactionId);

    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    @Query("SELECT t FROM WalletTransaction t WHERE t.wallet.id = :walletId " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    List<WalletTransaction> findTransactionsInDateRange(
            @Param("walletId") UUID walletId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT SUM(t.amount) FROM WalletTransaction t " +
            "WHERE t.wallet.id = :walletId " +
            "AND t.transactionType = 'DEBIT' " +
            "AND t.status = 'COMPLETED' " +
            "AND t.createdAt >= :since")
    BigDecimal sumDebitsSince(@Param("walletId") UUID walletId, @Param("since") Instant since);
}