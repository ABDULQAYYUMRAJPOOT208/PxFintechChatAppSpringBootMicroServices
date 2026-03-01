package com.pxfintech.authentication_service.repository;

import com.pxfintech.authentication_service.model.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<BlacklistedToken, UUID> {

    Optional<BlacklistedToken> findByTokenId(String tokenId);

    boolean existsByTokenId(String tokenId);

    @Modifying
    @Query("DELETE FROM BlacklistedToken b WHERE b.expirationTime < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}