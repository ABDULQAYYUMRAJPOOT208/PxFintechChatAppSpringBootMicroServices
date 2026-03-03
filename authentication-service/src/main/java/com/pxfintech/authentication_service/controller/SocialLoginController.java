package com.pxfintech.authentication_service.controller;

import com.pxfintech.authentication_service.dto.request.SocialLoginRequest;
import com.pxfintech.authentication_service.dto.response.SocialLoginResponse;
import com.pxfintech.authentication_service.service.SocialLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
@Slf4j
public class SocialLoginController {

    private final SocialLoginService socialLoginService;

    @PostMapping("/google")
    public ResponseEntity<SocialLoginResponse> loginWithGoogle(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Google login request");
        SocialLoginResponse response = socialLoginService.authenticateWithGoogle(
                request.getIdToken(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/facebook")
    public ResponseEntity<SocialLoginResponse> loginWithFacebook(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Facebook login request");
        SocialLoginResponse response = socialLoginService.authenticateWithFacebook(
                request.getAccessToken(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apple")
    public ResponseEntity<SocialLoginResponse> loginWithApple(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Apple login request");
        SocialLoginResponse response = socialLoginService.authenticateWithApple(
                request.getIdToken(), request.getAccessToken(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/link")
    public ResponseEntity<SocialLoginResponse> linkSocialAccount(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SocialLoginRequest request) {
        log.info("Linking social account for user: {}", userId);
        SocialLoginResponse response = socialLoginService.linkSocialAccount(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unlink/{provider}")
    public ResponseEntity<Void> unlinkSocialAccount(
            @AuthenticationPrincipal String userId,
            @PathVariable String provider) {
        log.info("Unlinking social account {} for user: {}", provider, userId);
        socialLoginService.unlinkSocialAccount(userId, provider);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{provider}")
    public ResponseEntity<Boolean> checkSocialAccountLinked(
            @AuthenticationPrincipal String userId,
            @PathVariable String provider) {
        boolean isLinked = socialLoginService.isSocialAccountLinked(userId, provider);
        return ResponseEntity.ok(isLinked);
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<SocialLoginResponse> oauth2Success(@AuthenticationPrincipal OAuth2User principal) {
        log.info("OAuth2 login success for user: {}", principal.getName());

        // This endpoint is called after successful OAuth2 login
        // You can create or retrieve user and return JWT tokens

        return ResponseEntity.ok(SocialLoginResponse.builder()
                .message("OAuth2 login successful")
                .build());
    }
}