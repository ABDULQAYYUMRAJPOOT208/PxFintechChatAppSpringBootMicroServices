package com.fintech.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(def = "{'userId': 1, 'createdAt': -1}")
public class Notification {

    @Id
    private String id;

    @Indexed
    private String notificationId;

    @Indexed
    private String userId;

    private NotificationType type;

    private NotificationChannel channel;

    private String title;

    private String content;

    private Map<String, Object> data;

    private NotificationStatus status;

    private String providerResponse;

    private Integer retryCount;

    private Instant scheduledAt;

    private Instant sentAt;

    private Instant deliveredAt;

    private Instant readAt;

    @Indexed
    private Instant createdAt;

    public enum NotificationType {
        TRANSACTION_ALERT, PAYMENT_RECEIVED, PAYMENT_SENT,
        OTP, WELCOME, PROMOTION, SECURITY_ALERT
    }

    public enum NotificationChannel {
        PUSH, SMS, EMAIL, IN_APP, WEBHOOK
    }

    public enum NotificationStatus {
        PENDING, SENT, DELIVERED, READ, FAILED, CANCELLED
    }
}