package com.pxfintech.chat_service.controller;

import com.pxfintech.chat_service.dto.request.SendMessageRequest;
import com.pxfintech.chat_service.dto.response.MessageResponse;
import com.pxfintech.chat_service.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestAttribute String userId,
            @Valid @RequestBody SendMessageRequest request) {
        log.info("REST request to send message to conversation: {}", request.getConversationId());
        MessageResponse response = messageService.sendMessage(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Slice<MessageResponse>> getMessages(
            @RequestAttribute String userId,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("REST request to get messages for conversation: {}", conversationId);
        Slice<MessageResponse> messages = messageService.getMessages(
                conversationId, userId, before, limit);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{conversationId}/{messageId}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {
        log.info("REST request to get message: {}", messageId);
        MessageResponse response = messageService.getMessage(conversationId, messageId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{conversationId}/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @RequestAttribute String userId,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "false") boolean deleteForEveryone) {
        log.info("REST request to delete message: {} (everyone: {})", messageId, deleteForEveryone);
        messageService.deleteMessage(userId, conversationId, messageId, deleteForEveryone);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{conversationId}/{messageId}")
    public ResponseEntity<Void> editMessage(
            @RequestAttribute String userId,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @RequestBody String newContent) {
        log.info("REST request to edit message: {}", messageId);
        messageService.editMessage(userId, conversationId, messageId, newContent);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread/{conversationId}")
    public ResponseEntity<Long> getUnreadCount(
            @RequestAttribute String userId,
            @PathVariable UUID conversationId) {
        long count = messageService.getUnreadCount(userId, conversationId);
        return ResponseEntity.ok(count);
    }
}