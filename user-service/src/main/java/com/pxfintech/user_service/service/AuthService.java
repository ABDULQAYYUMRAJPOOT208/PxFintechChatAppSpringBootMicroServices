package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.auth.AuthResponse;
import com.pxfintech.user_service.dto.otp.VerifyOTPRequest;
import com.pxfintech.user_service.dto.user.UserLoginRequestDto;
import com.pxfintech.user_service.dto.user.UserRegisterRequestDto;
import com.pxfintech.user_service.dto.user.UserResponseDto;

public interface AuthService {
    UserResponseDto register(UserRegisterRequestDto request);
    AuthResponse login(UserLoginRequestDto request);
    UserResponseDto[] getAllUsers();
    AuthResponse verifyOtp(VerifyOTPRequest request);
    AuthResponse refreshToken(String refreshToken);
//    public AuthResponse login(LoginRequest)
    void logout(String token);
    void resendOtp(String phoneNumber);

}
