package com.pxfintech.chat_service.security;

import com.pxfintech.chat_service.service.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthenticator implements ChannelInterceptor {

    private final TokenValidationService tokenValidationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && tokenValidationService.validateToken(token)) {
                String userId = tokenValidationService.getUserIdFromToken(token);

                // Create authentication
                UserDetails userDetails = User.builder()
                        .username(userId)
                        .password("")
                        .roles("USER")
                        .build();

                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                accessor.setUser(auth);
                log.info("WebSocket authenticated for user: {}", userId);
            } else {
                log.warn("WebSocket connection attempt with invalid token");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Check native headers first
        Map<String, Object> nativeHeaders = (Map<String, Object>) accessor.getHeader("nativeHeaders");
        if (nativeHeaders != null && nativeHeaders.containsKey("Authorization")) {
            Object authHeader = nativeHeaders.get("Authorization");
            if (authHeader instanceof ArrayList && !((ArrayList) authHeader).isEmpty()) {
                String token = (String) ((ArrayList) authHeader).get(0);
                if (token.startsWith("Bearer ")) {
                    return token.substring(7);
                }
                return token;
            }
        }

        // Check simple headers
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Check query parameters
        String token = accessor.getFirstNativeHeader("token");
        if (token != null) {
            return token;
        }

        return null;
    }
}