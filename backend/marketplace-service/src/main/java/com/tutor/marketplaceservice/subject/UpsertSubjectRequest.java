package com.tutor.marketplaceservice.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertSubjectRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Size(max = 120, message = "Slug must be at most 120 characters")
        String slug,

        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description
) {
}
