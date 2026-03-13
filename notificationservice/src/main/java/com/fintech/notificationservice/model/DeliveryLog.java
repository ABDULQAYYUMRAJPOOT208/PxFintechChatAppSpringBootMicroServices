package com.fintech.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "delivery_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLog {

    @Id
    private String id;

    @Indexed
    private String notificationId;

    private String userId;

    private Notification.NotificationChannel channel;

    private String provider;

    private String providerMessageId;

    private DeliveryStatus status;

    private Integer attempt;

    private String error;

    private Instant createdAt;

    public enum DeliveryStatus {
        ATTEMPTED, SUCCESS, FAILED, RETRY
    }
}