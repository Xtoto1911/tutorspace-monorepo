package com.tutor.authservice.repository;

import com.tutor.authservice.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    List<UserSession> findByUserIdAndRevokedAtIsNull(UUID userId);
}
