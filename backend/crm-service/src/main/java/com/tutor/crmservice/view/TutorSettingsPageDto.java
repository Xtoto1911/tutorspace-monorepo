package com.tutor.crmservice.view;

import java.util.List;
import java.math.BigDecimal;

public record TutorSettingsPageDto(
        ProfileDto profile,
        List<String> subjects,
        List<String> availableSubjects,
        boolean isHidden,
        List<ToggleDto> visibility,
        List<ToggleDto> notifications,
        ModerationDto moderation
) {
    public record ProfileDto(String name, String city, String headline, String about, String initials, String moderationStatus, Integer experienceYears, BigDecimal pricePerHour) {
    }

    public record ToggleDto(String title, String description, boolean enabled) {
    }

    public record ModerationDto(String status, String description) {
    }
}
