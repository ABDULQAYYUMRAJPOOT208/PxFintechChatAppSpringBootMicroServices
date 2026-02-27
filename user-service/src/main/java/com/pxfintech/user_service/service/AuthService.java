package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.*;

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
