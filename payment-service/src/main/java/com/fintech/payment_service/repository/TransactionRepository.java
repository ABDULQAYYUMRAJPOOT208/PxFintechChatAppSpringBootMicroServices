package com.fintech.payment_service.repository;

import com.fintech.payment_service.model.Transaction;
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
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
            UUID senderId, UUID receiverId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.senderId = :userId OR t.receiverId = :userId) " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findUserTransactionsInDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
            "WHERE t.senderId = :userId " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt >= :since")
    BigDecimal getTotalSentSince(@Param("userId") UUID userId,
                                 @Param("since") Instant since);

    long countBySenderIdAndStatusAndCreatedAtAfter(
            UUID senderId, Transaction.TransactionStatus status, Instant createdAt);
}