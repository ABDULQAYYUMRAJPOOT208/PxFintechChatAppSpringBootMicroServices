package com.pxfintech.user_service.controller;

import com.pxfintech.user_service.dto.user.UserResponseDto;
import com.pxfintech.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDto> getCurrentUser(@RequestAttribute String userId) {
        UserResponseDto response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable String userId) {
        UserResponseDto response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exists/{phoneNumber}")
    public ResponseEntity<Boolean> userExists(@PathVariable String phoneNumber) {
        return ResponseEntity.ok(userService.userExists(phoneNumber));
    }
}