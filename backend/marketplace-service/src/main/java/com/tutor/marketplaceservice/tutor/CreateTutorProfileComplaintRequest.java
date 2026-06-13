package com.tutor.marketplaceservice.tutor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTutorProfileComplaintRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
}
