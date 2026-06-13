package com.tutor.marketplaceservice.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReviewComplaintRequest(
        @NotBlank(message = "Complaint reason is required")
        @Size(max = 200, message = "Complaint reason must be at most 200 characters")
        String reason,

        @Size(max = 2000, message = "Complaint description must be at most 2000 characters")
        String description
) {
}
