package com.pxfintech.chat_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceServiceImpl implements PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String USER_PRESENCE_PREFIX = "presence:user:";
    private static final String SESSION_USER_PREFIX = "presence:session:";
    private static final String LAST_SEEN_PREFIX = "lastseen:";
    private static final int ONLINE_TIMEOUT_SECONDS = 60;

    @Override
    public void userConnected(String userId, String sessionId) {
        log.debug("User connected: {} with session: {}", userId, sessionId);

        // Store session to user mapping
        String sessionKey = SESSION_USER_PREFIX + sessionId;
        redisTemplate.opsForValue().set(sessionKey, userId, ONLINE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Store user presence with heartbeat
        String presenceKey = USER_PRESENCE_PREFIX + userId;
        redisTemplate.opsForValue().set(presenceKey, "online", ONLINE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Broadcast online status
        broadcastPresence(userId, true);
    }

    @Override
    public void userDisconnected(String sessionId) {
        String sessionKey = SESSION_USER_PREFIX + sessionId;
        String userId = redisTemplate.opsForValue().get(sessionKey);

        if (userId != null) {
            log.debug("User disconnected: {} from session: {}", userId, sessionId);

            // Remove session mapping
            redisTemplate.delete(sessionKey);

            // Check if user has other active sessions
            String pattern = SESSION_USER_PREFIX + "*";
            Set<String> sessions = redisTemplate.keys(pattern);
            boolean hasOtherSessions = false;

            if (sessions != null) {
                for (String s : sessions) {
                    String u = redisTemplate.opsForValue().get(s);
                    if (userId.equals(u)) {
                        hasOtherSessions = true;
                        break;
                    }
                }
            }

            if (!hasOtherSessions) {
                // User is completely offline
                String presenceKey = USER_PRESENCE_PREFIX + userId;
                redisTemplate.delete(presenceKey);

                // Update last seen
                updateLastSeen(userId);

                // Broadcast offline status
                broadcastPresence(userId, false);
            }
        }
    }

    @Override
    public boolean isUserOnline(String userId) {
        String presenceKey = USER_PRESENCE_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(presenceKey));
    }

    @Override
    public Instant getLastSeen(String userId) {
        String lastSeenKey = LAST_SEEN_PREFIX + userId;
        String lastSeenStr = redisTemplate.opsForValue().get(lastSeenKey);

        if (lastSeenStr != null) {
            return Instant.parse(lastSeenStr);
        }
        return Instant.now().minusSeconds(3600); // Default to 1 hour ago
    }

    @Override
    public void updateLastSeen(String userId) {
        String lastSeenKey = LAST_SEEN_PREFIX + userId;
        redisTemplate.opsForValue().set(lastSeenKey, Instant.now().toString());
        redisTemplate.expire(lastSeenKey, 7, TimeUnit.DAYS);
    }

    @Override
    public int getOnlineCount() {
        String pattern = USER_PRESENCE_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys.size() : 0;
    }

    // Heartbeat method called periodically from WebSocket keep-alive
    public void heartbeat(String sessionId) {
        String sessionKey = SESSION_USER_PREFIX + sessionId;
        String userId = redisTemplate.opsForValue().get(sessionKey);

        if (userId != null) {
            String presenceKey = USER_PRESENCE_PREFIX + userId;
            redisTemplate.expire(presenceKey, ONLINE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            redisTemplate.expire(sessionKey, ONLINE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void broadcastPresence(String userId, boolean isOnline) {
        Map<String, Object> presenceEvent = new HashMap<>();
        presenceEvent.put("type", "PRESENCE");
        presenceEvent.put("userId", userId);
        presenceEvent.put("isOnline", isOnline);
        presenceEvent.put("timestamp", Instant.now().toEpochMilli());

        // Broadcast to all interested parties (user's friends, etc.)
        // In a real app, you'd send this only to users who have this user in their contacts
        messagingTemplate.convertAndSend("/topic/presence", presenceEvent);
    }
}