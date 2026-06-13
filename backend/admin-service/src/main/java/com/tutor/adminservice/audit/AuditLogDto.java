package com.tutor.adminservice.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
    UUID id,
    UUID adminUserId,
    String actionType,
    String targetType,
    UUID targetId,
    String details,
    Instant createdAt
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
            log.getId(), log.getAdminUserId(), log.getActionType(),
            log.getTargetType(), log.getTargetId(), log.getDetails(),
            log.getCreatedAt()
        );
    }
}
