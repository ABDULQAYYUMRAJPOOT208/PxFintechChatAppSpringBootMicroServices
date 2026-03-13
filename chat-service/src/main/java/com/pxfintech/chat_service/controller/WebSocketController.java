package com.pxfintech.chat_service.controller;

import com.pxfintech.chat_service.dto.request.SendMessageRequest;
import com.pxfintech.chat_service.dto.request.TypingIndicatorRequest;
import com.pxfintech.chat_service.dto.response.MessageResponse;
import com.pxfintech.chat_service.service.MessageService;
import com.pxfintech.chat_service.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final MessageService messageService;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    @SendToUser("/queue/reply")
    public MessageResponse sendMessage(Principal principal,
                                       @Payload SendMessageRequest request,
                                       @Header("simpSessionId") String sessionId) {
        log.info("WebSocket message received from user: {}", principal.getName());

        String userId = principal.getName();
        return messageService.sendMessage(userId, request);
    }

    @MessageMapping("/chat.typing")
    public void typingIndicator(Principal principal,
                                @Payload TypingIndicatorRequest request,
                                @Header("simpSessionId") String sessionId) {
        log.debug("Typing indicator from user: {} in conversation: {}",
                principal.getName(), request.getConversationId());

        Map<String, Object> typingEvent = new HashMap<>();
        typingEvent.put("type", "TYPING");
        typingEvent.put("userId", principal.getName());
        typingEvent.put("conversationId", request.getConversationId());
        typingEvent.put("isTyping", request.isTyping());
        typingEvent.put("timestamp", System.currentTimeMillis());

        // Broadcast to all participants in the conversation
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.getConversationId() + "/typing",
                typingEvent);
    }

    @MessageMapping("/chat.read/{conversationId}/{messageId}")
    public void markAsRead(Principal principal,
                           @DestinationVariable UUID conversationId,
                           @DestinationVariable UUID messageId) {
        log.debug("User {} marked message {} as read in conversation {}",
                principal.getName(), messageId, conversationId);

        messageService.markAsRead(conversationId, messageId, principal.getName());
    }

    @MessageMapping("/chat.delivered/{conversationId}/{messageId}")
    public void markAsDelivered(Principal principal,
                                @DestinationVariable UUID conversationId,
                                @DestinationVariable UUID messageId) {
        log.debug("User {} received message {} in conversation {}",
                principal.getName(), messageId, conversationId);

        messageService.markAsDelivered(conversationId, messageId, principal.getName());
    }

    @MessageMapping("/chat.presence")
    @SendToUser("/queue/presence")
    public Map<String, Object> getUserPresence(Principal principal,
                                               @Payload Map<String, String> request) {
        String targetUserId = request.get("userId");
        boolean isOnline = presenceService.isUserOnline(targetUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", targetUserId);
        response.put("isOnline", isOnline);
        response.put("lastSeen", presenceService.getLastSeen(targetUserId));

        return response;
    }

    @MessageMapping("/chat.conversation.join/{conversationId}")
    public void joinConversation(Principal principal,
                                 @DestinationVariable UUID conversationId) {
        log.info("User {} joined conversation: {}", principal.getName(), conversationId);

        // Subscribe user to conversation topic
        // The subscription is handled automatically by STOMP

        // Notify others
        Map<String, Object> joinEvent = new HashMap<>();
        joinEvent.put("type", "USER_JOINED");
        joinEvent.put("userId", principal.getName());
        joinEvent.put("conversationId", conversationId);
        joinEvent.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/events",
                joinEvent);
    }

    @MessageMapping("/chat.conversation.leave/{conversationId}")
    public void leaveConversation(Principal principal,
                                  @DestinationVariable UUID conversationId) {
        log.info("User {} left conversation: {}", principal.getName(), conversationId);

        // Notify others
        Map<String, Object> leaveEvent = new HashMap<>();
        leaveEvent.put("type", "USER_LEFT");
        leaveEvent.put("userId", principal.getName());
        leaveEvent.put("conversationId", conversationId);
        leaveEvent.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/events",
                leaveEvent);
    }
}