package com.fintech.walletservice.repository;

import com.fintech.walletservice.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdWithLock(@Param("userId") UUID userId);

    Optional<Wallet> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.id = :walletId")
    int incrementBalance(@Param("walletId") UUID walletId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount WHERE w.id = :walletId AND w.balance >= :amount")
    int decrementBalance(@Param("walletId") UUID walletId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.dailyUsed = :dailyUsed, w.monthlyUsed = :monthlyUsed WHERE w.id = :walletId")
    int updateUsage(@Param("walletId") UUID walletId,
                    @Param("dailyUsed") BigDecimal dailyUsed,
                    @Param("monthlyUsed") BigDecimal monthlyUsed);

    @Modifying
    @Query("UPDATE Wallet w SET w.dailyUsed = 0, w.lastDailyReset = :today WHERE w.lastDailyReset < :today")
    int resetDailyLimits(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE Wallet w SET w.monthlyUsed = 0, w.lastMonthlyReset = :firstDay WHERE w.lastMonthlyReset < :firstDay")
    int resetMonthlyLimits(@Param("firstDay") LocalDate firstDay);
}