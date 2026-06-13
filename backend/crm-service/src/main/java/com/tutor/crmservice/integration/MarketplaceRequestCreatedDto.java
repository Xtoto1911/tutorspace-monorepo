package com.tutor.crmservice.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record MarketplaceRequestCreatedDto(
        UUID requestId,
        UUID clientUserId,
        UUID tutorProfileId,
        UUID tutorUserId,
        String tutorSlug,
        String tutorFullName,
        UUID subjectId,
        String subjectName,
        BigDecimal pricePerHour,
        String studentName,
        String studentGrade,
        LocalDateTime desiredStartTime,
        String comment,
        Instant createdAt
) {
}
