package com.tutor.crmservice.integration;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MarketplaceTutorProfileDto(
        UUID id,
        UUID userId,
        String slug,
        String fullName,
        String description,
        Integer experienceYears,
        BigDecimal pricePerHour,
        String photoUrl,
        Double rating,
        Integer reviewsCount,
        String status,
        List<SubjectDto> subjects
) {
    public record SubjectDto(UUID id, String name, String slug) {
    }
}
