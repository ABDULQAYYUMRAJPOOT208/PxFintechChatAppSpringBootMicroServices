package com.fintech.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

    @Id
    private String id;

    @Indexed(unique = true)
    private String templateId;

    private String name;

    private String description;

    private NotificationType type;

    private Map<NotificationChannel, ChannelContent> channelContent;

    private List<String> requiredVariables;

    private Map<String, Object> defaultVariables;

    private String version;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelContent {
        private String subject;      // For email
        private String title;         // For push
        private String body;          // For all channels
        private String smsText;       // For SMS
        private String htmlContent;   // For email HTML
        private Map<String, String> data; // For push data payload
    }

    public enum NotificationType {
        TRANSACTION_ALERT, PAYMENT_RECEIVED, PAYMENT_SENT,
        OTP, WELCOME, PROMOTION, SECURITY_ALERT
    }

    public enum NotificationChannel {
        PUSH, SMS, EMAIL, IN_APP
    }
}