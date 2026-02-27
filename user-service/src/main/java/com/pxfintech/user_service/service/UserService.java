package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.UserResponseDto;

public interface UserService {
    UserResponseDto getUserById(String userId);
    boolean userExists(String phoneNumber);
}