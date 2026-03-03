package com.pxfintech.authentication_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pxfintech.authentication_service.dto.request.SocialLoginRequest;
import com.pxfintech.authentication_service.dto.response.SocialLoginResponse;
import com.pxfintech.authentication_service.exception.InvalidTokenException;
import com.pxfintech.authentication_service.model.entity.SocialUser;
import com.pxfintech.authentication_service.repository.SocialUserRepository;
import com.pxfintech.authentication_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialLoginServiceImpl implements SocialLoginService {

    private final SocialUserRepository socialUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String facebookClientSecret;

    @Value("${apple.team-id}")
    private String appleTeamId;

    @Value("${apple.key-id}")
    private String appleKeyId;

    @Value("${apple.client-id}")
    private String appleClientId;

    @Value("${apple.private-key}")
    private String applePrivateKey;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Override
    @Transactional
    public SocialLoginResponse authenticateWithGoogle(String idToken, SocialLoginRequest request) {
        log.info("Authenticating with Google");

        try {
            // Verify Google ID token
            GoogleUserInfo userInfo = verifyGoogleToken(idToken);

            // Check if social user exists
            SocialUser socialUser = socialUserRepository
                    .findByProviderAndProviderId("google", userInfo.getSub())
                    .orElse(null);

            UUID userId;
            boolean isNewUser = false;

            if (socialUser == null) {
                // Create new user in User Service (call via API)
                userId = createUserFromSocialProfile(userInfo, "google");
                isNewUser = true;

                // Save social user mapping
                socialUser = SocialUser.builder()
                        .userId(userId)
                        .provider("google")
                        .providerId(userInfo.getSub())
                        .email(userInfo.getEmail())
                        .name(userInfo.getName())
                        .profilePictureUrl(userInfo.getPicture())
                        .locale(userInfo.getLocale())
                        .verifiedEmail(userInfo.getEmailVerified())
                        .rawAttributes(objectMapper.writeValueAsString(userInfo))
                        .isLinked(false)
                        .lastLoginAt(LocalDateTime.now())
                        .build();

                socialUserRepository.save(socialUser);
            } else {
                userId = socialUser.getUserId();
                socialUser.setLastLoginAt(LocalDateTime.now());
                socialUserRepository.save(socialUser);
            }

            // Generate JWT tokens
            String accessToken = jwtTokenProvider.generateAccessToken(userId.toString(), "fintech-mobile-app", "openid profile email");
            String refreshToken = jwtTokenProvider.generateRefreshToken(userId.toString(), "fintech-mobile-app");

            return SocialLoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                    .isNewUser(isNewUser)
                    .userId(userId.toString())
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .provider("google")
                    .needsPhoneVerification(isNewUser)
                    .build();

        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage());
            throw new InvalidTokenException("Google authentication failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SocialLoginResponse authenticateWithFacebook(String accessToken, SocialLoginRequest request) {
        log.info("Authenticating with Facebook");

        try {
            // Verify Facebook access token
            FacebookUserInfo userInfo = verifyFacebookToken(accessToken);

            // Check if social user exists
            SocialUser socialUser = socialUserRepository
                    .findByProviderAndProviderId("facebook", userInfo.getId())
                    .orElse(null);

            UUID userId;
            boolean isNewUser = false;

            if (socialUser == null) {
                // Create new user in User Service
                userId = createUserFromSocialProfile(userInfo, "facebook");
                isNewUser = true;

                // Save social user mapping
                socialUser = SocialUser.builder()
                        .userId(userId)
                        .provider("facebook")
                        .providerId(userInfo.getId())
                        .email(userInfo.getEmail())
                        .name(userInfo.getName())
                        .profilePictureUrl(userInfo.getPicture() != null ?
                                userInfo.getPicture().getData().getUrl() : null)
                        .verifiedEmail(true)
                        .rawAttributes(objectMapper.writeValueAsString(userInfo))
                        .isLinked(false)
                        .lastLoginAt(LocalDateTime.now())
                        .build();

                socialUserRepository.save(socialUser);
            } else {
                userId = socialUser.getUserId();
                socialUser.setLastLoginAt(LocalDateTime.now());
                socialUserRepository.save(socialUser);
            }

            // Generate JWT tokens
            String jwtAccessToken = jwtTokenProvider.generateAccessToken(userId.toString(), "fintech-mobile-app", "openid profile email");
            String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userId.toString(), "fintech-mobile-app");

            return SocialLoginResponse.builder()
                    .accessToken(jwtAccessToken)
                    .refreshToken(jwtRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                    .isNewUser(isNewUser)
                    .userId(userId.toString())
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .provider("facebook")
                    .needsPhoneVerification(isNewUser)
                    .build();

        } catch (Exception e) {
            log.error("Facebook authentication failed: {}", e.getMessage());
            throw new InvalidTokenException("Facebook authentication failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SocialLoginResponse authenticateWithApple(String idToken, String authorizationCode, SocialLoginRequest request) {
        log.info("Authenticating with Apple");

        try {
            // Verify Apple identity token
            AppleUserInfo userInfo = verifyAppleToken(idToken, authorizationCode);

            // Check if social user exists
            SocialUser socialUser = socialUserRepository
                    .findByProviderAndProviderId("apple", userInfo.getSub())
                    .orElse(null);

            UUID userId;
            boolean isNewUser = false;

            if (socialUser == null) {
                // Create new user in User Service
                userId = createUserFromSocialProfile(userInfo, "apple");
                isNewUser = true;

                // Save social user mapping
                socialUser = SocialUser.builder()
                        .userId(userId)
                        .provider("apple")
                        .providerId(userInfo.getSub())
                        .email(userInfo.getEmail())
                        .name(userInfo.getName() != null ? userInfo.getName() : "Apple User")
                        .verifiedEmail(userInfo.getEmailVerified() != null ? userInfo.getEmailVerified() : true)
                        .rawAttributes(objectMapper.writeValueAsString(userInfo))
                        .isLinked(false)
                        .lastLoginAt(LocalDateTime.now())
                        .build();

                socialUserRepository.save(socialUser);
            } else {
                userId = socialUser.getUserId();
                socialUser.setLastLoginAt(LocalDateTime.now());
                socialUserRepository.save(socialUser);
            }

            // Generate JWT tokens
            String jwtAccessToken = jwtTokenProvider.generateAccessToken(userId.toString(), "fintech-mobile-app", "openid profile email");
            String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userId.toString(), "fintech-mobile-app");

            return SocialLoginResponse.builder()
                    .accessToken(jwtAccessToken)
                    .refreshToken(jwtRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                    .isNewUser(isNewUser)
                    .userId(userId.toString())
                    .email(userInfo.getEmail())
                    .name(userInfo.getName() != null ? userInfo.getName() : "Apple User")
                    .provider("apple")
                    .needsPhoneVerification(isNewUser)
                    .build();

        } catch (Exception e) {
            log.error("Apple authentication failed: {}", e.getMessage());
            throw new InvalidTokenException("Apple authentication failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SocialLoginResponse linkSocialAccount(String userId, SocialLoginRequest request) {
        log.info("Linking social account {} for user: {}", request.getProvider(), userId);

        try {
            SocialLoginResponse socialResponse = null;

            switch (request.getProvider().toLowerCase()) {
                case "google":
                    socialResponse = authenticateWithGoogle(request.getIdToken(), request);
                    break;
                case "facebook":
                    socialResponse = authenticateWithFacebook(request.getAccessToken(), request);
                    break;
                case "apple":
                    socialResponse = authenticateWithApple(request.getIdToken(), null, request);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported provider: " + request.getProvider());
            }

            // Update social user to be linked to the existing account
            SocialUser socialUser = socialUserRepository
                    .findByProviderAndProviderId(request.getProvider().toLowerCase(),
                            socialResponse.getUserId()) // Note: This is provider ID
                    .orElseThrow(() -> new InvalidTokenException("Social user not found"));

            socialUser.setUserId(UUID.fromString(userId));
            socialUser.setIsLinked(true);
            socialUserRepository.save(socialUser);

            log.info("Social account linked successfully for user: {}", userId);

            return socialResponse;

        } catch (Exception e) {
            log.error("Failed to link social account: {}", e.getMessage());
            throw new InvalidTokenException("Failed to link social account: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void unlinkSocialAccount(String userId, String provider) {
        log.info("Unlinking social account {} for user: {}", provider, userId);

        SocialUser socialUser = socialUserRepository
                .findByUserIdAndProvider(UUID.fromString(userId), provider.toLowerCase())
                .orElseThrow(() -> new InvalidTokenException("Social account not found"));

        socialUserRepository.delete(socialUser);

        log.info("Social account unlinked successfully");
    }

    @Override
    public boolean isSocialAccountLinked(String userId, String provider) {
        return socialUserRepository
                .findByUserIdAndProvider(UUID.fromString(userId), provider.toLowerCase())
                .isPresent();
    }

    private GoogleUserInfo verifyGoogleToken(String idToken) {
        // Verify Google ID token using Google's tokeninfo endpoint
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        ResponseEntity<GoogleUserInfo> response = restTemplate.getForEntity(url, GoogleUserInfo.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new InvalidTokenException("Invalid Google token");
        }

        GoogleUserInfo userInfo = response.getBody();

        // Verify audience matches our client ID
        if (!googleClientId.equals(userInfo.getAud())) {
            throw new InvalidTokenException("Token was not issued for this client");
        }

        return userInfo;
    }

    private FacebookUserInfo verifyFacebookToken(String accessToken) {
        // Verify Facebook access token
        String url = "https://graph.facebook.com/me?fields=id,name,email,picture&access_token=" + accessToken;

        ResponseEntity<FacebookUserInfo> response = restTemplate.getForEntity(url, FacebookUserInfo.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new InvalidTokenException("Invalid Facebook token");
        }

        return response.getBody();
    }

    private AppleUserInfo verifyAppleToken(String idToken, String authorizationCode) {
        // Verify Apple identity token
        // Note: Apple token verification is more complex and involves
        // validating the JWT signature with Apple's public keys

        // For now, we'll decode and verify the JWT
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new InvalidTokenException("Invalid Apple token format");
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            AppleUserInfo userInfo = objectMapper.readValue(payload, AppleUserInfo.class);

            // Verify issuer
            if (!"https://appleid.apple.com".equals(userInfo.getIss())) {
                throw new InvalidTokenException("Invalid issuer");
            }

            // Verify audience
            if (!appleClientId.equals(userInfo.getAud())) {
                throw new InvalidTokenException("Token was not issued for this client");
            }

            return userInfo;

        } catch (Exception e) {
            throw new InvalidTokenException("Apple token verification failed: " + e.getMessage());
        }
    }

    private UUID createUserFromSocialProfile(Object userInfo, String provider) {
        // Call User Service to create a new user
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> userRequest = new HashMap<>();

            if (userInfo instanceof GoogleUserInfo) {
                GoogleUserInfo googleInfo = (GoogleUserInfo) userInfo;
                userRequest.put("fullName", googleInfo.getName());
                userRequest.put("email", googleInfo.getEmail());
                userRequest.put("profilePicture", googleInfo.getPicture());
            } else if (userInfo instanceof FacebookUserInfo) {
                FacebookUserInfo fbInfo = (FacebookUserInfo) userInfo;
                userRequest.put("fullName", fbInfo.getName());
                userRequest.put("email", fbInfo.getEmail());
                userRequest.put("profilePicture", fbInfo.getPicture() != null ?
                        fbInfo.getPicture().getData().getUrl() : null);
            } else if (userInfo instanceof AppleUserInfo) {
                AppleUserInfo appleInfo = (AppleUserInfo) userInfo;
                userRequest.put("fullName", appleInfo.getName() != null ?
                        appleInfo.getName() : "Apple User");
                userRequest.put("email", appleInfo.getEmail());
            }

            userRequest.put("authProvider", provider);
            userRequest.put("isVerified", true);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    userServiceUrl + "/users/social",
                    request,
                    Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String userId = (String) response.getBody().get("id");
                return UUID.fromString(userId);
            } else {
                throw new RuntimeException("Failed to create user in User Service");
            }

        } catch (Exception e) {
            log.error("Failed to create user from social profile: {}", e.getMessage());
            throw new RuntimeException("Failed to create user account", e);
        }
    }

    // Inner classes for provider-specific user info
    @lombok.Data
    public static class GoogleUserInfo {
        private String sub;
        private String name;
        private String givenName;
        private String familyName;
        private String picture;
        private String email;
        private boolean emailVerified;
        private String locale;
        private String aud;
        private String iss;
        private Long exp;
        private Long iat;

        @JsonProperty("given_name")
        public String getGivenName() { return givenName; }

        @JsonProperty("family_name")
        public String getFamilyName() { return familyName; }

        @JsonProperty("email_verified")
        public boolean getEmailVerified() { return emailVerified; }
    }

    @lombok.Data
    public static class FacebookUserInfo {
        private String id;
        private String name;
        private String email;
        private Picture picture;

        @lombok.Data
        public static class Picture {
            private Data data;

            @lombok.Data
            public static class Data {
                private int height;
                private int width;
                private String url;
            }
        }
    }

    @lombok.Data
    public static class AppleUserInfo {
        private String sub;
        private String email;
        private String name;
        private String aud;
        private String iss;
        private Long exp;
        private Long iat;
        private Boolean emailVerified;
        private Boolean isPrivateEmail;

        @JsonProperty("email_verified")
        public Boolean getEmailVerified() { return emailVerified; }

        @JsonProperty("is_private_email")
        public Boolean getIsPrivateEmail() { return isPrivateEmail; }
    }
}
