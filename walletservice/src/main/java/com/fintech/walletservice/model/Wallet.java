package com.fintech.walletservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyLimit = new BigDecimal("1000");

    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyLimit = new BigDecimal("10000");

    @Column(name = "daily_used", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyUsed = BigDecimal.ZERO;

    @Column(name = "monthly_used", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyUsed = BigDecimal.ZERO;

    @Column(name = "last_daily_reset")
    private LocalDate lastDailyReset;

    @Column(name = "last_monthly_reset")
    private LocalDate lastMonthlyReset;

    @Column(name = "frozen_reason")
    private String frozenReason;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        LocalDate today = LocalDate.now();
        lastDailyReset = today;
        lastMonthlyReset = today.withDayOfMonth(1);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum WalletStatus {
        ACTIVE, FROZEN, CLOSED
    }
}