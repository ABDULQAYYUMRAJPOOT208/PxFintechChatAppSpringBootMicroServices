package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.*;
import com.pxfintech.user_service.model.User;
import com.pxfintech.user_service.repo.UserRepo;
import com.pxfintech.user_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImp implements AuthService {

    private final UserRepo userRepo;
private  final OTPService otpService;
private  final JwtTokenProvider jwtTokenProvider;
    @Override
    public UserResponseDto register(UserRegisterRequestDto request) {
        log.info("Registering user with phone: {}", request.getPhoneNumber());

        if(userRepo.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("User already exists with this phone number");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(request.getPassword());
        user.setIsVerified(false);

        User savedUser = userRepo.save(user);

        otpService.generateAndSendOTP(savedUser.getPhoneNumber());

        UserResponseDto response = new UserResponseDto();
        response.setId(savedUser.getId());
        response.setEmail(savedUser.getEmail());
        response.setFullName(savedUser.getFullName());
        response.setPhoneNumber(savedUser.getPhoneNumber());
        response.setIsVerified(savedUser.getIsVerified());

        log.info("User registered successfully with ID: {}", savedUser.getId());
        return response;
    }

    @Override
    public AuthResponse login(UserLoginRequestDto request) {
        log.info("Login attempt for phone: {}", request.getPhoneNumber());
        User user = userRepo.findByPhoneNumber(request.getPhoneNumber()).orElseThrow(()-> new RuntimeException("User not found"));
//        log.info("User for login >> {}",user);
        if(!user.getIsVerified()){
            throw new RuntimeException("Please verify your phone umber first");
        }


        String otp = otpService.generateAndSendOTP(request.getPhoneNumber());

        log.info("Login OTP: {} sent to: {}", otp, request.getPhoneNumber());

        return AuthResponse.builder()
                .message("OTP sent successfully")
                .phoneNumber(request.getPhoneNumber())
                .success(true)
                .build();

    }
    @Override
    @Transactional
    public void logout(String token) {
        // Extract token (remove "Bearer " prefix)
        String jwtToken = token.substring(7);

        String phoneNumber = jwtTokenProvider.getPhoneNumberFromToken(jwtToken);

        // Update user online status
        userRepo.findByPhoneNumber(phoneNumber).ifPresent(user -> {
            user.setIsOnline(false);
            userRepo.save(user);
        });

        log.info("User logged out: {}", phoneNumber);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        return null;
    }

    @Override
    public UserResponseDto[] getAllUsers(){
        UserResponseDto[] users = userRepo.findAll()
                .stream()
                .map(user -> new UserResponseDto(
                        user.getId(),
                        user.getPhoneNumber(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getIsVerified(),
                        user.getIsOnline(),
                        user.getLastLoginAt(),
                        user.getCreatedAt()
                ))
                .toArray(UserResponseDto[]::new);
        return users;
    }


    @Override
    @Transactional
    public AuthResponse verifyOtp (VerifyOTPRequest request)
    {
        log.info("Verifying OTP for phone: {}",request.getPhoneNumber());

        boolean isValid = otpService.validateOTP(request.getPhoneNumber(),request.getOtp());

        if(!isValid)
        {
            return AuthResponse.builder().success(false).
                    message("Invalid or expired OTP")
                    .phoneNumber(request.getPhoneNumber())
                    .build();
        }
        // Find and update user
        User user = userRepo.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsVerified(true);
        userRepo.save(user);

        // Convert to response DTO
        UserResponseDto userResponse = new UserResponseDto();
        userResponse.setId(user.getId());
        userResponse.setPhoneNumber(user.getPhoneNumber());
        userResponse.setFullName(user.getFullName());
        userResponse.setEmail(user.getEmail());
        userResponse.setIsVerified(user.getIsVerified());
        userResponse.setCreatedAt(user.getCreatedAt());

        log.info("User verified successfully: {}", user.getId());

        return AuthResponse.builder()
                .success(true)
                .message("OTP verified successfully")
                .phoneNumber(request.getPhoneNumber())
                .user(userResponse)
                .build();
    }
    @Override
    public void resendOtp(String phoneNumber)
    {
        log.info("Resending OTP to: {}", phoneNumber);

        if(!userRepo.existsByPhoneNumber(phoneNumber))
        {
            throw new RuntimeException("User not found");
        }
        otpService.generateAndSendOTP(phoneNumber);
        log.info("OTP resent to: {}", phoneNumber);
    }

    private UserResponseDto mapToUserResponse(User user) {
        UserResponseDto response = new UserResponseDto();
        response.setId(user.getId());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setIsVerified(user.getIsVerified());
        response.setIsOnline(user.getIsOnline());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }



}

