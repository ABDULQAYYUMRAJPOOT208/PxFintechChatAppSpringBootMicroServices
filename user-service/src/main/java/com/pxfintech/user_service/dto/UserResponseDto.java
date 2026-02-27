package com.pxfintech.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
