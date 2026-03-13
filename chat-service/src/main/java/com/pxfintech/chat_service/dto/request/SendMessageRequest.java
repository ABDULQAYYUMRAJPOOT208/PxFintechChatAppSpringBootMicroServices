package com.pxfintech.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SendMessageRequest {

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @NotBlank(message = "Message content is required")
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    private String content;

    private String messageType = "TEXT";

    private String mediaUrl;

    private UUID replyToId;

    private UUID forwardedFrom;

    private String metadata;

    private String clientMessageId; // For idempotency
}