package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.auth.AuthResponse;
import com.pxfintech.user_service.dto.otp.VerifyOTPRequest;
import com.pxfintech.user_service.dto.event.UserEvent;
import com.pxfintech.user_service.dto.user.UserLoginRequestDto;
import com.pxfintech.user_service.dto.user.UserRegisterRequestDto;
import com.pxfintech.user_service.dto.user.UserResponseDto;
import com.pxfintech.user_service.model.User;
import com.pxfintech.user_service.repo.UserRepo;
import com.pxfintech.user_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImp implements AuthService {

    private final UserRepo userRepo;
    private final OTPService otpService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserEventProducer userEventProducer;

    @Override
    public UserResponseDto register(UserRegisterRequestDto request) {
        log.info("Registering user with phone: {}", request.getPhoneNumber());

        if (userRepo.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("User already exists with this phone number");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsVerified(false);

        User savedUser = userRepo.save(user);

        otpService.generateAndSendOTP(savedUser.getPhoneNumber());

        userEventProducer.sendUserRegistrationEvent(UserEvent.builder()
                .userId(savedUser.getId())
                .phoneNumber(savedUser.getPhoneNumber())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .eventType("USER_REGISTERED")
                .timestamp(LocalDateTime.now())
                .build());

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
        User user = userRepo.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsVerified()) {
            throw new RuntimeException("Please verify your phone umber first");
        }

        String token = jwtTokenProvider.generateToken(user.getPhoneNumber(), user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getPhoneNumber());

        return AuthResponse.builder()
                .message("OTP sent successfully")
                .phoneNumber(request.getPhoneNumber())
                .success(true)
                .accessToken(token)
                .refreshToken(refreshToken)
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
            userEventProducer.sendUserStatusEvent(user.getId(), user.getPhoneNumber(), "USER_OFFLINE");
        });

        log.info("User logged out: {}", phoneNumber);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (jwtTokenProvider.validateToken(refreshToken) && jwtTokenProvider.isRefreshToken(refreshToken)) {
            String phoneNumber = jwtTokenProvider.getPhoneNumberFromToken(refreshToken);
            User user = userRepo.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newAccessToken = jwtTokenProvider.generateToken(user.getPhoneNumber(), user.getId());
            return AuthResponse.builder()
                    .success(true)
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .phoneNumber(phoneNumber)
                    .message("Token refreshed successfully")
                    .build();
        }
        throw new RuntimeException("Invalid refresh token");
    }

    @Override
    public UserResponseDto[] getAllUsers() {
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
                        user.getCreatedAt()))
                .toArray(UserResponseDto[]::new);
        return users;
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOTPRequest request) {
        log.info("Verifying OTP for phone: {}", request.getPhoneNumber());

        boolean isValid = otpService.validateOTP(request.getPhoneNumber(), request.getOtp());

        if (!isValid) {
            return AuthResponse.builder().success(false).message("Invalid or expired OTP")
                    .phoneNumber(request.getPhoneNumber())
                    .build();
        }
        User user = userRepo.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsVerified(true);
        user.setIsOnline(true);
        userRepo.save(user);

        userEventProducer.sendUserStatusEvent(user.getId(), user.getPhoneNumber(), "USER_ONLINE");

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
    public void resendOtp(String phoneNumber) {
        log.info("Resending OTP to: {}", phoneNumber);

        if (!userRepo.existsByPhoneNumber(phoneNumber)) {
            throw new RuntimeException("User not found");
        }
        otpService.generateAndSendOTP(phoneNumber);
        log.info("OTP resent to: {}", phoneNumber);
    }

}
