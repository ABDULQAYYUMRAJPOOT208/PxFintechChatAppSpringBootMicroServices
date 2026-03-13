package com.pxfintech.chat_service.service;

import com.pxfintech.chat_service.dto.request.SendMessageRequest;
import com.pxfintech.chat_service.dto.response.MessageResponse;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.UUID;

public interface MessageService {

    MessageResponse sendMessage(String userId, SendMessageRequest request);

    MessageResponse getMessage(UUID conversationId, UUID messageId);

    Slice<MessageResponse> getMessages(UUID conversationId, String userId,
                                       Instant before, int limit);

    void deleteMessage(String userId, UUID conversationId, UUID messageId,
                       boolean deleteForEveryone);

    void markAsDelivered(UUID conversationId, UUID messageId, String userId);

    void markAsRead(UUID conversationId, UUID messageId, String userId);

    long getUnreadCount(String userId, UUID conversationId);

    void editMessage(String userId, UUID conversationId, UUID messageId, String newContent);
}