package com.pxfintech.chat_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pxfintech.chat_service.dto.request.SendMessageRequest;
import com.pxfintech.chat_service.dto.response.MessageResponse;
import com.pxfintech.chat_service.exception.MessageNotFoundException;
import com.pxfintech.chat_service.model.Message;
import com.pxfintech.chat_service.repository.MessageRepository;
import com.pxfintech.chat_service.repository.UserConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserConversationRepository userConversationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.messages}")
    private String messagesTopic;

    @Override
    @Transactional
    public MessageResponse sendMessage(String userId, SendMessageRequest request) {
        log.info("Sending message to conversation: {} from user: {}",
                request.getConversationId(), userId);

        // Create message
        Instant now = Instant.now();
        UUID messageId = UUID.randomUUID();

        Message message = Message.builder()
                .key(Message.MessageKey.builder()
                        .conversationId(request.getConversationId())
                        .createdAt(now)
                        .messageId(messageId)
                        .build())
                .senderId(UUID.fromString(userId))
                .content(request.getContent())
                .messageType(Message.MessageType.valueOf(request.getMessageType()))
                .mediaUrl(request.getMediaUrl())
                .metadata(request.getMetadata())
                .replyToId(request.getReplyToId())
                .forwardedFrom(request.getForwardedFrom())
                .status(Message.MessageStatus.SENT)
                .createdAt(now)
                .updatedAt(now)
                .deletedFor(new HashSet<>())
                .build();

        // Save to Cassandra
        messageRepository.save(message);

        // Update conversation last message
        updateConversationLastMessage(request.getConversationId(), message);

        // Publish to Kafka for async processing
        kafkaTemplate.send(messagesTopic, messageId.toString(), message);

        // Send real-time via WebSocket
        MessageResponse response = mapToResponse(message, 0, 0);
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.getConversationId(),
                response);

        log.info("Message sent successfully with ID: {}", messageId);

        return response;
    }

    @Override
    public MessageResponse getMessage(UUID conversationId, UUID messageId) {
        Message.Key key = Message.Key.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .build();

        Message message = messageRepository.findById(key)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));

        return mapToResponse(message, 0, 0);
    }

    @Override
    public Slice<MessageResponse> getMessages(UUID conversationId, String userId,
                                              Instant before, int limit) {
        log.debug("Getting messages for conversation: {} before: {}", conversationId, before);

        Pageable pageable = PageRequest.of(0, limit);

        Slice<Message> messages = messageRepository.findByKeyConversationIdAndKeyCreatedAtLessThan(
                conversationId, before != null ? before : Instant.now(), pageable);

        return messages.map(message -> {
            int deliveredCount = getDeliveredCount(message);
            int readCount = getReadCount(message);
            return mapToResponse(message, deliveredCount, readCount);
        });
    }

    @Override
    @Transactional
    public void deleteMessage(String userId, UUID conversationId, UUID messageId,
                              boolean deleteForEveryone) {
        log.info("Deleting message: {} for user: {} (everyone: {})",
                messageId, userId, deleteForEveryone);

        Message.Key key = Message.Key.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .build();

        Message message = messageRepository.findById(key)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));

        if (deleteForEveryone) {
            // Check if user is sender or admin
            if (!message.getSenderId().toString().equals(userId)) {
                throw new SecurityException("Only sender can delete for everyone");
            }
            messageRepository.delete(message);
        } else {
            // Delete only for this user
            message.getDeletedFor().add(UUID.fromString(userId));
            messageRepository.save(message);
        }

        // Notify via WebSocket
        Map<String, Object> deleteEvent = new HashMap<>();
        deleteEvent.put("type", "MESSAGE_DELETED");
        deleteEvent.put("messageId", messageId);
        deleteEvent.put("conversationId", conversationId);
        deleteEvent.put("deleteForEveryone", deleteForEveryone);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                deleteEvent);
    }

    @Override
    public void markAsDelivered(UUID conversationId, UUID messageId, String userId) {
        messageRepository.markAsDelivered(conversationId, messageId);

        // Send delivery receipt
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("type", "DELIVERED");
        receipt.put("messageId", messageId);
        receipt.put("userId", userId);
        receipt.put("timestamp", Instant.now());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                receipt);
    }

    @Override
    public void markAsRead(UUID conversationId, UUID messageId, String userId) {
        messageRepository.markAsRead(conversationId, messageId);

        // Reset unread count
        userConversationRepository.markAsRead(
                UUID.fromString(userId),
                conversationId,
                messageId,
                Instant.now());

        // Send read receipt
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("type", "READ");
        receipt.put("messageId", messageId);
        receipt.put("userId", userId);
        receipt.put("timestamp", Instant.now());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                receipt);
    }

    @Override
    public long getUnreadCount(String userId, UUID conversationId) {
        return messageRepository.countNewMessages(conversationId,
                getLastReadTime(userId, conversationId));
    }

    @Override
    @Transactional
    public void editMessage(String userId, UUID conversationId, UUID messageId, String newContent) {
        Message.Key key = Message.Key.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .build();

        Message message = messageRepository.findById(key)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));

        if (!message.getSenderId().toString().equals(userId)) {
            throw new SecurityException("Only sender can edit message");
        }

        message.setContent(newContent);
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);

        // Notify via WebSocket
        Map<String, Object> editEvent = new HashMap<>();
        editEvent.put("type", "MESSAGE_EDITED");
        editEvent.put("messageId", messageId);
        editEvent.put("conversationId", conversationId);
        editEvent.put("newContent", newContent);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                editEvent);
    }

    private void updateConversationLastMessage(UUID conversationId, Message message) {
        // This would call ConversationRepository to update last message
        // conversationRepository.updateLastMessage(...);
    }

    private int getDeliveredCount(Message message) {
        // Implementation would query delivery receipts
        return 0;
    }

    private int getReadCount(Message message) {
        // Implementation would query delivery receipts
        return 0;
    }

    private Instant getLastReadTime(String userId, UUID conversationId) {
        // Implementation would get last read time from user_conversations
        return Instant.now().minusSeconds(3600); // Placeholder
    }

    private MessageResponse mapToResponse(Message message, int deliveredCount, int readCount) {
        return MessageResponse.builder()
                .id(message.getKey().getMessageId())
                .conversationId(message.getKey().getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .mediaUrl(message.getMediaUrl())
                .status(message.getStatus().name())
                .replyToId(message.getReplyToId())
                .forwardedFrom(message.getForwardedFrom())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .deliveredCount(deliveredCount)
                .readCount(readCount)
                .isDeleted(message.getDeletedFor() != null && !message.getDeletedFor().isEmpty())
                .build();
    }
}