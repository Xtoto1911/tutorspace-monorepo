package com.tutor.marketplaceservice.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByTutorProfileIdAndStatusOrderByCreatedAtDesc(UUID tutorProfileId, ReviewStatus status);
    List<Review> findByTutorProfileIdAndStatus(UUID tutorProfileId, ReviewStatus status);
    @EntityGraph(attributePaths = "tutorProfile")
    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
    Optional<Review> findByTutorProfileIdAndClientUserId(UUID tutorProfileId, UUID clientUserId);
}
