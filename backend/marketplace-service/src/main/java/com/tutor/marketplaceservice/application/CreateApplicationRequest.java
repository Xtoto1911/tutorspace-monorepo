package com.tutor.marketplaceservice.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateApplicationRequest(
        @NotNull(message = "Client user id is required")
        UUID clientUserId,

        @NotNull(message = "Subject id is required")
        UUID subjectId,

        @NotNull(message = "Desired start time is required")
        LocalDateTime desiredStartTime,

        @Size(max = 255, message = "Student name must be at most 255 characters")
        String studentName,

        @Size(max = 100, message = "Student grade must be at most 100 characters")
        String studentGrade,

        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String comment
) {
}
