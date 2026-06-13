package com.tutor.marketplaceservice.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewPublicationRestrictionRepository extends JpaRepository<ReviewPublicationRestriction, UUID> {
    boolean existsByUserIdAndActiveTrue(UUID userId);
    Optional<ReviewPublicationRestriction> findByUserId(UUID userId);
}
