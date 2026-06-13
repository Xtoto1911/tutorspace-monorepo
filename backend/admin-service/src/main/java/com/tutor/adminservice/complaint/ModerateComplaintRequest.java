package com.tutor.adminservice.complaint;

import jakarta.validation.constraints.NotNull;

public record ModerateComplaintRequest(
    @NotNull ComplaintStatus status,
    String resolutionNotes,
    ComplaintModerationAction action
) {}
