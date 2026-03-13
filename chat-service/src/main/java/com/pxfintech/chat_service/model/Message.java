package com.pxfintech.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("messages")
public class Message {

    @PrimaryKey
    private MessageKey key;

    private UUID senderId;

    private String content;

    @CassandraType(type = CassandraType.Name.TEXT)
    private MessageType messageType;

    private String mediaUrl;

    private String metadata; // JSON for additional data

    @CassandraType(type = CassandraType.Name.TEXT)
    private MessageStatus status;

    private UUID replyToId;

    private UUID forwardedFrom;

    private Instant createdAt;

    private Instant updatedAt;

    private Set<UUID> deletedFor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageKey {
        private UUID conversationId;
        private Instant createdAt;
        private UUID messageId;
    }

    public enum MessageType {
        TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION, CONTACT
    }

    public enum MessageStatus {
        SENT, DELIVERED, READ
    }
}