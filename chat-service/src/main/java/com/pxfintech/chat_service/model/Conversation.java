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
@Table("conversations")
public class Conversation {

    @PrimaryKey
    private UUID id;

    private ConversationType type;

    private String name;

    private UUID createdBy;

    private Instant createdAt;

    private Instant updatedAt;

    private UUID lastMessageId;

    private String lastMessageContent;

    private Instant lastMessageTime;

    private UUID lastMessageSender;

    private Boolean isArchived;

    private String metadata; // JSON for group icon, description, etc.

    public enum ConversationType {
        ONE_TO_ONE, GROUP
    }
}