package com.pxfintech.authentication_service.repository;

import com.pxfintech.authentication_service.model.entity.SocialUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocialUserRepository extends JpaRepository<SocialUser, UUID> {

    Optional<SocialUser> findByProviderAndProviderId(String provider, String providerId);

    List<SocialUser> findByUserId(UUID userId);

    Optional<SocialUser> findByUserIdAndProvider(UUID userId, String provider);

    boolean existsByProviderAndProviderId(String provider, String providerId);

    @Modifying
    @Query("UPDATE SocialUser s SET s.lastLoginAt = :lastLoginAt WHERE s.id = :id")
    int updateLastLogin(@Param("id") UUID id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Modifying
    @Query("DELETE FROM SocialUser s WHERE s.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}