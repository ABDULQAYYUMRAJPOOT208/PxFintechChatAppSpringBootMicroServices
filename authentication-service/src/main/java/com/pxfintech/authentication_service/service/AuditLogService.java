package com.pxfintech.authentication_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.topic:audit-logs}")
    private String auditTopic;

    @Value("${audit.log.include-headers:true}")
    private boolean includeHeaders;

    public AuditLogService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Async
    public void logEvent(AuditEvent event) {
        try {
            String jsonEvent = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(auditTopic, event.getUserId(), jsonEvent);

            // Also log to local file for redundancy
            log.info("AUDIT: {}", jsonEvent);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event: {}", e.getMessage());
        }
    }

    public AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }

    public class AuditEventBuilder {
        private final Map<String, Object> event = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();

        public AuditEventBuilder withAction(String action) {
            event.put("action", action);
            return this;
        }

        public AuditEventBuilder withUserId(String userId) {
            event.put("userId", userId);
            return this;
        }

        public AuditEventBuilder withUsername(String username) {
            event.put("username", username);
            return this;
        }

        public AuditEventBuilder withIpAddress(String ipAddress) {
            event.put("ipAddress", ipAddress);
            return this;
        }

        public AuditEventBuilder withUserAgent(String userAgent) {
            event.put("userAgent", userAgent);
            return this;
        }

        public AuditEventBuilder withResource(String resource) {
            event.put("resource", resource);
            return this;
        }

        public AuditEventBuilder withStatus(String status) {
            event.put("status", status);
            return this;
        }

        public AuditEventBuilder withDetails(Map<String, Object> details) {
            event.put("details", details);
            return this;
        }

        public AuditEventBuilder withHeader(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public void log() {
            event.put("eventId", UUID.randomUUID().toString());
            event.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            event.put("service", "authentication-service");

            if (includeHeaders && !headers.isEmpty()) {
                event.put("headers", headers);
            }

            AuditEvent auditEvent = new AuditEvent(event);
            logEvent(auditEvent);
        }
    }

    @lombok.Value
    public static class AuditEvent {
        Map<String, Object> data;
    }
}