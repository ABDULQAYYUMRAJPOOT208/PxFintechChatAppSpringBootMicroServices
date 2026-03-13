package com.fintech.walletservice.scheduler;

import com.fintech.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitResetScheduler {

    private final WalletRepository walletRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void resetDailyLimits() {
        log.info("Resetting daily limits for all wallets");
        LocalDate today = LocalDate.now();
        int updated = walletRepository.resetDailyLimits(today);
        log.info("Reset daily limits for {} wallets", updated);
    }

    @Scheduled(cron = "0 5 0 1 * ?") // Run at 12:05 AM on the 1st of every month
    public void resetMonthlyLimits() {
        log.info("Resetting monthly limits for all wallets");
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        int updated = walletRepository.resetMonthlyLimits(firstDay);
        log.info("Reset monthly limits for {} wallets", updated);
    }
}