package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.user.SocialUserRegisterRequestDto;
import com.pxfintech.user_service.dto.user.UserRegisterRequestDto;
import com.pxfintech.user_service.dto.user.UserResponseDto;

public interface UserService {
    UserResponseDto getUserById(String userId);
    boolean userExists(String phoneNumber);
    UserResponseDto createUser(UserRegisterRequestDto requestDto);
    UserResponseDto createSocialUser(SocialUserRegisterRequestDto requestDto);
}
