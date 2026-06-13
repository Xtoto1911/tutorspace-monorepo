package com.tutor.marketplaceservice.tutor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SyncTutorProfileRequest(
        UUID userId,
        String fullName,
        String city,
        String headline,
        String description,
        Integer experienceYears,
        BigDecimal pricePerHour,
        boolean hidden,
        List<String> subjects
) {
}
