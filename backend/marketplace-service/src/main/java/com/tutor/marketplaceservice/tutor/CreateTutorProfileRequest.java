package com.tutor.marketplaceservice.tutor;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateTutorProfileRequest(
        @NotNull(message = "User id is required")
        UUID userId,

        @NotBlank(message = "Slug is required")
        @Size(max = 120, message = "Slug must be at most 120 characters")
        String slug,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must be at most 255 characters")
        String fullName,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "Experience is required")
        @Min(value = 0, message = "Experience must not be negative")
        Integer experienceYears,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Price must be positive")
        BigDecimal pricePerHour,

        @Size(max = 1000, message = "Photo URL must be at most 1000 characters")
        String photoUrl,

        @Size(max = 120, message = "City must be at most 120 characters")
        String city,

        @Size(max = 10, message = "Education must contain at most 10 entries")
        List<@NotBlank @Size(max = 500) String> education,

        @Size(max = 10, message = "Highlights must contain at most 10 entries")
        List<@NotBlank @Size(max = 500) String> highlights,

        @Size(max = 15, message = "Tags must contain at most 15 entries")
        List<@NotBlank @Size(max = 120) String> tags,

        @PositiveOrZero(message = "Response time must not be negative")
        Integer responseTimeMinutes,

        @NotEmpty(message = "At least one subject is required")
        Set<UUID> subjectIds
) {
}
