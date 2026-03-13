package com.pxfintech.chat_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {

    private UUID id;

    private String type;

    private String name;

    private List<ParticipantInfo> participants;

    @JsonProperty("last_message")
    private MessagePreview lastMessage;

    @JsonProperty("unread_count")
    private int unreadCount;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("is_archived")
    private boolean isArchived;

    private String metadata;

    @Data
    @Builder
    public static class ParticipantInfo {
        private UUID userId;
        private String role;
        private boolean isActive;
    }

    @Data
    @Builder
    public static class MessagePreview {
        private UUID id;
        private String content;
        private UUID senderId;
        private Instant sentAt;
    }
}