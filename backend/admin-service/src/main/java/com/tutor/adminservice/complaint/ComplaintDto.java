package com.tutor.adminservice.complaint;

import java.time.Instant;
import java.util.UUID;

public record ComplaintDto(
    UUID id,
    UUID reportedUserId,
    UUID reporterUserId,
    ComplaintTargetType targetType,
    UUID targetId,
    String targetSnapshot,
    ComplaintCategory category,
    String reason,
    String description,
    ComplaintPriority priority,
    ComplaintStatus status,
    Instant createdAt,
    Instant moderatedAt,
    UUID moderatorId,
    String resolutionNotes
) {
    public static ComplaintDto from(Complaint c) {
        return new ComplaintDto(
            c.getId(), c.getReportedUserId(), c.getReporterUserId(),
            c.getTargetType(), c.getTargetId(), c.getTargetSnapshot(),
            c.getCategory(), c.getReason(), c.getDescription(),
            c.getPriority(), c.getStatus(), c.getCreatedAt(),
            c.getModeratedAt(), c.getModeratorId(), c.getResolutionNotes()
        );
    }
}
