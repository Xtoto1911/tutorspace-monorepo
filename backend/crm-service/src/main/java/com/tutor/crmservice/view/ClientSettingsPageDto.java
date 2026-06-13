package com.tutor.crmservice.view;

import java.util.List;

public record ClientSettingsPageDto(
        List<MetricDto> metrics,
        ProfileDto profile,
        List<StudentDto> students,
        List<ToggleDto> notifications,
        List<ToggleDto> privacy,
        List<String> searchPreferences
) {
    public record MetricDto(String icon, String value, String label, String sublabel, String tone) {
    }

    public record ProfileDto(String parentName, String phone, String email, String city, String timezone, String language) {
    }

    public record StudentDto(String id, String name, String details, String goal, String tone) {
    }

    public record ToggleDto(String id, String title, String description, boolean enabled) {
    }
}
