package com.fintech.notificationservice.dto.request;

import com.fintech.notificationservice.model.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class SendNotificationRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Notification type is required")
    private Notification.NotificationType type;

    private Notification.NotificationChannel channel;

    private String templateId;

    private Map<String, Object> variables;

    private Map<String, Object> data;

    private Instant scheduledAt;

    private Boolean highPriority;

    private String customTitle;

    private String customContent;
}