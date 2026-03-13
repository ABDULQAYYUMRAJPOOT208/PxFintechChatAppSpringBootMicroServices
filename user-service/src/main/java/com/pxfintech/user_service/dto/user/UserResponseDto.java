package com.pxfintech.user_service.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class UserResponseDto {
    private String id;
    private String phoneNumber;
    private String fullName;
    private String email;
    private Boolean isVerified;
    private Boolean isOnline;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
