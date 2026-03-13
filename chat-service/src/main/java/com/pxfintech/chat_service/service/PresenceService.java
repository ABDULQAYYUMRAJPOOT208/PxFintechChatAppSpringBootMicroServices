package com.pxfintech.chat_service.service;

import java.time.Instant;

public interface PresenceService {

    void userConnected(String userId, String sessionId);

    void userDisconnected(String sessionId);

    boolean isUserOnline(String userId);

    Instant getLastSeen(String userId);

    void updateLastSeen(String userId);

    int getOnlineCount();
}