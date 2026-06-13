package com.tutor.marketplaceservice.review;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateReviewRequest(
        @NotNull(message = "Tutor profile id is required")
        UUID tutorProfileId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @NotBlank(message = "Review text is required")
        @Size(max = 2000, message = "Review text must be at most 2000 characters")
        String text
) {
}
