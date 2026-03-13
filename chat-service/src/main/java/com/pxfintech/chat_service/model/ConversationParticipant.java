package com.pxfintech.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("conversation_participants")
public class ConversationParticipant {

    @PrimaryKey
    private ParticipantKey key;

    private ParticipantRole role;

    private Instant joinedAt;

    private Instant leftAt;

    private Boolean isActive;

    private UUID lastReadMessageId;

    private Instant lastReadTime;

    private Boolean notificationEnabled;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantKey {
        private UUID conversationId;
        private UUID userId;
    }

    public enum ParticipantRole {
        ADMIN, MEMBER
    }
}