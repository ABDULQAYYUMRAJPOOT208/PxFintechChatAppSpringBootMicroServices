package com.pxfintech.user_service.controller;

import com.pxfintech.user_service.dto.user.SocialUserRegisterRequestDto;
import com.pxfintech.user_service.dto.user.UserRegisterRequestDto;
import com.pxfintech.user_service.dto.user.UserResponseDto;
import com.pxfintech.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRegisterRequestDto requestDto) {
        UserResponseDto response = userService.createUser(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/social")
    public ResponseEntity<UserResponseDto> createSocialUser(@Valid @RequestBody SocialUserRegisterRequestDto requestDto) {
        UserResponseDto response = userService.createSocialUser(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

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
