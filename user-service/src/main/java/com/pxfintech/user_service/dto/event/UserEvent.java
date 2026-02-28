package com.pxfintech.user_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEvent {
    private String userId;
    private String phoneNumber;
    private String eventType; // USER_REGISTERED, USER_ONLINE, USER_OFFLINE
    private LocalDateTime timestamp;
    private String fullName;
    private String email;
}
