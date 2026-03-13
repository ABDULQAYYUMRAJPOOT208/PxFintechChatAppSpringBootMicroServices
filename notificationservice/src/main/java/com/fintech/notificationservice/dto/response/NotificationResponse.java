package com.fintech.notificationservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {

    @JsonProperty("notification_id")
    private String notificationId;

    @JsonProperty("user_id")
    private String userId;

    private String type;

    private String channel;

    private String title;

    private String content;

    private String status;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("sent_at")
    private Instant sentAt;

    @JsonProperty("delivered_at")
    private Instant deliveredAt;

    private String message;
}