package com.pxfintech.user_service.service;

import com.pxfintech.user_service.dto.user.UserResponseDto;
import com.pxfintech.user_service.model.User;
import com.pxfintech.user_service.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImp implements UserService {

    private final UserRepo userRepository;

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