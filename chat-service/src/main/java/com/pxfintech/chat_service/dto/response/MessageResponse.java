package com.pxfintech.chat_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {

    private UUID id;

    @JsonProperty("conversation_id")
    private UUID conversationId;

    @JsonProperty("sender_id")
    private UUID senderId;

    private String content;

    @JsonProperty("message_type")
    private String messageType;

    @JsonProperty("media_url")
    private String mediaUrl;

    private String status;

    @JsonProperty("reply_to_id")
    private UUID replyToId;

    @JsonProperty("forwarded_from")
    private UUID forwardedFrom;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("delivered_to")
    private int deliveredCount;

    @JsonProperty("read_by")
    private int readCount;

    @JsonProperty("is_deleted")
    private boolean isDeleted;

    @JsonProperty("client_message_id")
    private String clientMessageId; // For matching client-side messages
}