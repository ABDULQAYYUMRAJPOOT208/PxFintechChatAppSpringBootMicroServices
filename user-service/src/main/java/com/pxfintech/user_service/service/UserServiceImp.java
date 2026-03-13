package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.event.UserEvent;
import com.pxfintech.user_service.dto.user.SocialUserRegisterRequestDto;
import com.pxfintech.user_service.dto.user.UserRegisterRequestDto;
import com.pxfintech.user_service.dto.user.UserResponseDto;
import com.pxfintech.user_service.model.User;
import com.pxfintech.user_service.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImp implements UserService {

    private final UserRepo userRepository;
    private final UserEventProducer userEventProducer;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return mapToUserResponse(user);
    }

    @Override
    public boolean userExists(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public UserResponseDto createUser(UserRegisterRequestDto requestDto) {
        if (userRepository.existsByPhoneNumber(requestDto.getPhoneNumber())) {
            throw new RuntimeException("User with phone number " + requestDto.getPhoneNumber() + " already exists.");
        }

        User user = new User();
        user.setPhoneNumber(requestDto.getPhoneNumber());
        user.setFullName(requestDto.getFullName());
        user.setEmail(requestDto.getEmail());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setIsVerified(false);
        user.setIsOnline(false);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        sendUserCreatedEvent(savedUser);
        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponseDto createSocialUser(SocialUserRegisterRequestDto requestDto) {
        if (requestDto.getEmail() != null && userRepository.existsByEmail(requestDto.getEmail())) {
            throw new RuntimeException("User with email " + requestDto.getEmail() + " already exists.");
        }

        User user = new User();
        user.setFullName(requestDto.getFullName());
        user.setEmail(requestDto.getEmail());
        user.setProfilePicture(requestDto.getProfilePicture());
        user.setAuthProvider(requestDto.getAuthProvider());
        user.setIsVerified(requestDto.getIsVerified());
        user.setIsOnline(false);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        sendUserCreatedEvent(savedUser);
        return mapToUserResponse(savedUser);
    }

    private void sendUserCreatedEvent(User user) {
        UserEvent userEvent = UserEvent.builder()
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .eventType("USER_REGISTERED")
                .timestamp(LocalDateTime.now())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
        userEventProducer.sendUserRegistrationEvent(userEvent);
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
