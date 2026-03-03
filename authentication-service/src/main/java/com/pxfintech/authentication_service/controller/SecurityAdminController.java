package com.pxfintech.authentication_service.controller;

import com.pxfintech.authentication_service.dto.request.RateLimitConfigRequest;
import com.pxfintech.authentication_service.dto.response.RateLimitStatusResponse;
import com.pxfintech.authentication_service.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/security/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class SecurityAdminController {

    private final RateLimitingService rateLimitingService;
    private final BruteForceProtectionService bruteForceService;
    private final IpFilterService ipFilterService;
    private final SuspiciousActivityDetector suspiciousActivityDetector;
    private final ComplianceService complianceService;
    private final AuditLogService auditLogService;

    // ==================== RATE LIMITING ====================

    @GetMapping("/ratelimit/status/{key}")
    public ResponseEntity<RateLimitStatusResponse> getRateLimitStatus(
            @PathVariable String key,
            @RequestParam int limit,
            @RequestParam int duration) {
        return ResponseEntity.ok(rateLimitingService.getRateLimitStatus(key, limit, duration));
    }

    @DeleteMapping("/ratelimit/clear/{key}")
    public ResponseEntity<Void> clearRateLimit(@PathVariable String key) {
        rateLimitingService.clearRateLimit(key);
        return ResponseEntity.ok().build();
    }

    // ==================== BRUTE FORCE ====================

    @GetMapping("/bruteforce/attempts/{username}")
    public ResponseEntity<Long> getAttemptCount(@PathVariable String username) {
        return ResponseEntity.ok(bruteForceService.getAttemptCount(username));
    }

    @PostMapping("/bruteforce/reset/{username}")
    public ResponseEntity<Void> resetAttempts(@PathVariable String username) {
        bruteForceService.resetAttempts(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bruteforce/block/user/{username}")
    public ResponseEntity<Void> blockUser(@PathVariable String username, @RequestParam String reason) {
        bruteForceService.blockUser(username, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bruteforce/block/ip/{ipAddress}")
    public ResponseEntity<Void> blockIp(@PathVariable String ipAddress, @RequestParam String reason) {
        bruteForceService.blockIp(ipAddress, reason);
        return ResponseEntity.ok().build();
    }

    // ==================== IP FILTER ====================

    @GetMapping("/ip/check/{ipAddress}")
    public ResponseEntity<Boolean> checkIpAllowed(@PathVariable String ipAddress) {
        return ResponseEntity.ok(ipFilterService.isIpAllowed(ipAddress));
    }

    @PostMapping("/ip/blacklist/{ipAddress}")
    public ResponseEntity<Void> addToBlacklist(
            @PathVariable String ipAddress,
            @RequestParam String reason,
            @RequestParam int durationMinutes) {
        ipFilterService.addToBlacklist(ipAddress, reason, durationMinutes);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/ip/blacklist/{ipAddress}")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable String ipAddress) {
        ipFilterService.removeFromBlacklist(ipAddress);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ip/blacklist")
    public ResponseEntity<List<String>> getDynamicBlacklist() {
        return ResponseEntity.ok(ipFilterService.getDynamicBlacklist());
    }

    // ==================== COMPLIANCE ====================

    @PostMapping("/compliance/export/{userId}")
    public ResponseEntity<Map<String, Object>> exportUserData(@PathVariable String userId) {
        return ResponseEntity.ok(complianceService.exportUserData(userId));
    }

    @DeleteMapping("/compliance/delete/{userId}")
    public ResponseEntity<Void> deleteUserData(@PathVariable String userId) {
        complianceService.deleteUserData(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/compliance/report")
    public ResponseEntity<String> generateComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String filename = complianceService.generateComplianceReport(startDate, endDate);
        return ResponseEntity.ok("Report generated: " + filename);
    }

    @GetMapping("/compliance/consent/{userId}")
    public ResponseEntity<Map<String, Object>> getUserConsents(@PathVariable String userId) {
        return ResponseEntity.ok(complianceService.getUserConsents(userId));
    }

    @PostMapping("/compliance/consent/{userId}")
    public ResponseEntity<Void> updateUserConsent(
            @PathVariable String userId,
            @RequestParam String consentType,
            @RequestParam boolean value) {
        complianceService.updateUserConsent(userId, consentType, value);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/compliance/enforce-retention")
    public ResponseEntity<Void> enforceDataRetention() {
        complianceService.enforceDataRetention();
        return ResponseEntity.ok().build();
    }

    // ==================== AUDIT LOGS ====================

    @GetMapping("/audit/search")
    public ResponseEntity<List<AuditLogService.AuditEvent>> searchAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        // Implementation would search audit logs
        return ResponseEntity.ok(List.of());
    }

    // ==================== SECURITY DASHBOARD ====================

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getSecurityDashboard() {
        Map<String, Object> dashboard = new java.util.HashMap<>();

        dashboard.put("timestamp", LocalDateTime.now());
        dashboard.put("blocked_ips", ipFilterService.getDynamicBlacklist().size());
        dashboard.put("active_rate_limits", "N/A"); // Implementation needed
        dashboard.put("failed_attempts_today", 0); // Implementation needed
        dashboard.put("suspicious_activities_today", 0); // Implementation needed

        return ResponseEntity.ok(dashboard);
    }
}