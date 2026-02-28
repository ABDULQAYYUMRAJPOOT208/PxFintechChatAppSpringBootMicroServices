package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.user.UserResponseDto;

public interface UserService {
    UserResponseDto getUserById(String userId);
    boolean userExists(String phoneNumber);
}