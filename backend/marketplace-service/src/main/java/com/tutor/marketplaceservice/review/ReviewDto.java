package com.tutor.marketplaceservice.review;

import java.time.Instant;
import java.util.UUID;

public record ReviewDto(
        UUID id,
        UUID tutorProfileId,
        UUID clientUserId,
        Integer rating,
        String text,
        ReviewStatus status,
        Instant createdAt,
        Instant updatedAt,
        String authorName,
        String tutorName,
        String tutorSlug
) {
    public static ReviewDto from(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getTutorProfile().getId(),
                review.getClientUserId(),
                review.getRating(),
                review.getText(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                "Клиент TutorSpace",
                review.getTutorProfile().getFullName(),
                review.getTutorProfile().getSlug()
        );
    }
}
