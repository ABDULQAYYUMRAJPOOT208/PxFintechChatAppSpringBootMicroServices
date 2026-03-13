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

@Document(collection = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private Map<NotificationType, ChannelPreference> typePreferences;

    private QuietHours quietHours;

    private List<String> optedOutChannels;

    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelPreference {
        private boolean enabled;
        private NotificationChannel primaryChannel;
        private List<NotificationChannel> channels;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuietHours {
        private boolean enabled;
        private String startTime; // "22:00"
        private String endTime;   // "08:00"
        private String timezone;
    }

    public enum NotificationType {
        TRANSACTION_ALERT, PAYMENT_RECEIVED, PAYMENT_SENT,
        OTP, WELCOME, PROMOTION, SECURITY_ALERT
    }

    public enum NotificationChannel {
        PUSH, SMS, EMAIL, IN_APP
    }
}