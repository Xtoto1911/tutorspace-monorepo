package com.tutor.authservice.repository;

import com.tutor.authservice.domain.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<PasswordResetToken> findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId,
            Instant now
    );

    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(UUID userId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from PasswordResetToken token
            where token.user.id = :userId
              and (token.usedAt is not null or token.expiresAt <= :now)
            """)
    int deleteUsedOrExpiredByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
