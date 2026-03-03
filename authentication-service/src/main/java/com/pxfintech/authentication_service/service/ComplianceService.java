package com.pxfintech.authentication_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceService {

    private final AuditLogService auditLogService;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${compliance.gdpr.enabled:true}")
    private boolean gdprEnabled;

    @Value("${compliance.data-retention-days:90}")
    private int dataRetentionDays;

    public ComplianceService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Generate GDPR data export for user
     */
    public Map<String, Object> exportUserData(String userId) {
        log.info("Exporting user data for GDPR request: {}", userId);

        Map<String, Object> userData = new HashMap<>();
        userData.put("export_date", LocalDateTime.now());
        userData.put("user_id", userId);

        // Collect all user data from various sources
        userData.put("profile", getUserProfile(userId));
        userData.put("social_accounts", getSocialAccounts(userId));
        userData.put("login_history", getLoginHistory(userId));
        userData.put("consents", getUserConsents(userId));
        userData.put("activity_log", getUserActivity(userId));

        // Save to file for download
        String filename = saveUserDataToFile(userId, userData);
        userData.put("download_url", "/api/v1/compliance/download/" + filename);

        // Log the export
        auditLogService.builder()
                .withAction("GDPR_EXPORT")
                .withUserId(userId)
                .withStatus("COMPLETED")
                .log();

        return userData;
    }

    /**
     * Delete user data (right to be forgotten)
     */
    public void deleteUserData(String userId) {
        log.info("Deleting user data for GDPR right to be forgotten: {}", userId);

        // Delete from all data stores
        deleteUserProfile(userId);
        deleteSocialAccounts(userId);
        deleteLoginHistory(userId);
        deleteUserConsents(userId);
        deleteUserActivity(userId);

        // Log the deletion
        auditLogService.builder()
                .withAction("GDPR_DELETE")
                .withUserId(userId)
                .withStatus("COMPLETED")
                .log();

        // Send deletion confirmation
        kafkaTemplate.send("user-deletion-events", userId,
                "User data deleted at " + LocalDateTime.now());
    }

    /**
     * Generate compliance report
     */
    public String generateComplianceReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating compliance report from {} to {}", startDate, endDate);

        Map<String, Object> report = new HashMap<>();
        report.put("report_period", startDate + " to " + endDate);
        report.put("generated_at", LocalDateTime.now());

        // Collect compliance metrics
        report.put("total_logins", getTotalLogins(startDate, endDate));
        report.put("failed_logins", getFailedLogins(startDate, endDate));
        report.put("data_exports", getDataExports(startDate, endDate));
        report.put("data_deletions", getDataDeletions(startDate, endDate));
        report.put("security_incidents", getSecurityIncidents(startDate, endDate));

        // Save report
        String filename = "compliance_report_" + startDate + "_" + endDate + ".json";
        saveReportToFile(filename, report);

        return filename;
    }

    /**
     * Get user consent status
     */
    public Map<String, Object> getUserConsents(String userId) {
        String consentKey = "consent:" + userId;
        Map<Object, Object> consents = redisTemplate.opsForHash().entries(consentKey);

        Map<String, Object> result = new HashMap<>();
        result.put("marketing_consent", consents.getOrDefault("marketing", false));
        result.put("data_processing_consent", consents.getOrDefault("processing", false));
        result.put("third_party_sharing", consents.getOrDefault("sharing", false));
        result.put("consent_date", consents.get("consent_date"));

        return result;
    }

    /**
     * Update user consent
     */
    public void updateUserConsent(String userId, String consentType, boolean value) {
        String consentKey = "consent:" + userId;
        redisTemplate.opsForHash().put(consentKey, consentType, String.valueOf(value));
        redisTemplate.opsForHash().put(consentKey, "consent_date", LocalDateTime.now().toString());
        redisTemplate.expire(consentKey, dataRetentionDays, java.util.concurrent.TimeUnit.DAYS);

        auditLogService.builder()
                .withAction("CONSENT_UPDATE")
                .withUserId(userId)
                .withDetails(Map.of(consentType, value))
                .withStatus("UPDATED")
                .log();
    }

    /**
     * Run data retention policy
     */
    public void enforceDataRetention() {
        log.info("Enforcing data retention policy ({} days)", dataRetentionDays);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(dataRetentionDays);

        // Delete old login history
        deleteOldLoginHistory(cutoffDate);

        // Delete old activity logs
        deleteOldActivityLogs(cutoffDate);

        // Archive old audit logs
        archiveOldAuditLogs(cutoffDate);

        log.info("Data retention policy enforced");
    }

    private Map<String, Object> getUserProfile(String userId) {
        // Implementation to get user profile from User Service
        Map<String, Object> profile = new HashMap<>();
        profile.put("user_id", userId);
        profile.put("retrieved_at", LocalDateTime.now());
        return profile;
    }

    private List<Map<String, Object>> getSocialAccounts(String userId) {
        // Implementation to get linked social accounts
        return new ArrayList<>();
    }

    private List<Map<String, Object>> getLoginHistory(String userId) {
        // Implementation to get login history
        return new ArrayList<>();
    }

    private List<Map<String, Object>> getUserActivity(String userId) {
        // Implementation to get user activity
        return new ArrayList<>();
    }

    private long getTotalLogins(LocalDate startDate, LocalDate endDate) {
        // Implementation to count logins
        return 0;
    }

    private long getFailedLogins(LocalDate startDate, LocalDate endDate) {
        // Implementation to count failed logins
        return 0;
    }

    private long getDataExports(LocalDate startDate, LocalDate endDate) {
        // Implementation to count data exports
        return 0;
    }

    private long getDataDeletions(LocalDate startDate, LocalDate endDate) {
        // Implementation to count deletions
        return 0;
    }

    private long getSecurityIncidents(LocalDate startDate, LocalDate endDate) {
        // Implementation to count security incidents
        return 0;
    }

    private String saveUserDataToFile(String userId, Map<String, Object> data) {
        String filename = "export_" + userId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".json";

        try (PrintWriter out = new PrintWriter(new FileWriter("/tmp/" + filename))) {
            out.write(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            log.error("Failed to save user data to file: {}", e.getMessage());
        }

        return filename;
    }

    private void saveReportToFile(String filename, Map<String, Object> report) {
        try (PrintWriter out = new PrintWriter(new FileWriter("/tmp/" + filename))) {
            out.write(objectMapper.writeValueAsString(report));
        } catch (Exception e) {
            log.error("Failed to save report to file: {}", e.getMessage());
        }
    }

    private void deleteUserProfile(String userId) {
        // Implementation
    }

    private void deleteSocialAccounts(String userId) {
        // Implementation
    }

    private void deleteLoginHistory(String userId) {
        // Implementation
    }

    private void deleteUserConsents(String userId) {
        // Implementation
    }

    private void deleteUserActivity(String userId) {
        // Implementation
    }

    private void deleteOldLoginHistory(LocalDateTime cutoffDate) {
        // Implementation
    }

    private void deleteOldActivityLogs(LocalDateTime cutoffDate) {
        // Implementation
    }

    private void archiveOldAuditLogs(LocalDateTime cutoffDate) {
        // Implementation
    }
}