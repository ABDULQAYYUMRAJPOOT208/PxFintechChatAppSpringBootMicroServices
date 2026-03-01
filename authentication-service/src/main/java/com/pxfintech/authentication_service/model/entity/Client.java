package com.pxfintech.authentication_service.model.entity;

import com.pxfintech.authentication_service.model.enums.ClientType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "oauth2_clients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", unique = true, nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_secret", nullable = false, length = 255)
    private String clientSecret;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 20)
    private ClientType clientType;

    @Column(name = "grant_types", columnDefinition = "TEXT[]")
    private List<String> grantTypes;

    @Column(name = "redirect_uris", columnDefinition = "TEXT[]")
    private List<String> redirectUris;

    @Column(name = "scopes", columnDefinition = "TEXT[]")
    private List<String> scopes;

    @Column(name = "authentication_methods", columnDefinition = "TEXT[]")
    private List<String> authenticationMethods;

    @Column(name = "access_token_validity")
    private Integer accessTokenValidity;

    @Column(name = "refresh_token_validity")
    private Integer refreshTokenValidity;

    @Column(name = "require_proof_key")
    private Boolean requireProofKey;

    @Column(name = "require_authorization_consent")
    private Boolean requireAuthorizationConsent;

    @Column(name = "is_active")
    private Boolean isActive;

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