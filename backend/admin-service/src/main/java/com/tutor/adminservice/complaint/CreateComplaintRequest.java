package com.tutor.adminservice.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateComplaintRequest(
    @NotNull UUID reportedUserId,
    @NotNull ComplaintCategory category,
    @NotBlank String reason,
    String description,
    ComplaintPriority priority,
    ComplaintTargetType targetType,
    UUID targetId,
    String targetSnapshot
) {}
