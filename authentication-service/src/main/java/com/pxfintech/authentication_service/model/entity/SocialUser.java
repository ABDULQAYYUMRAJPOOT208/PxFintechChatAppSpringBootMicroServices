package com.pxfintech.authentication_service.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "social_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId; // Reference to user in User Service

    @Column(name = "provider", nullable = false, length = 20)
    private String provider; // google, facebook, apple

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "locale", length = 10)
    private String locale;

    @Column(name = "verified_email")
    private Boolean verifiedEmail;

    @Column(name = "raw_attributes", columnDefinition = "TEXT")
    private String rawAttributes; // JSON string of all attributes

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "is_linked")
    private Boolean isLinked;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}