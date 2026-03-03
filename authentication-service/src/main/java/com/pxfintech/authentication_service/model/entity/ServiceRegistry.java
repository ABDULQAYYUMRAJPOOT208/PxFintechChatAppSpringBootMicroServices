package com.pxfintech.authentication_service.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_registry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_name", unique = true, nullable = false, length = 100)
    private String serviceName;

    @Column(name = "service_id", unique = true, nullable = false, length = 100)
    private String serviceId;

    @Column(name = "service_secret", nullable = false, length = 255)
    private String serviceSecret;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "allowed_scopes", columnDefinition = "TEXT[]")
    private String[] allowedScopes;

    @Column(name = "allowed_targets", columnDefinition = "TEXT[]")
    private String[] allowedTargets;

    @Column(name = "ip_whitelist", columnDefinition = "TEXT[]")
    private String[] ipWhitelist;

    @Column(name = "rate_limit")
    private Integer rateLimit;

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