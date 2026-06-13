package com.tutor.crmservice.view;

import java.util.List;

public record TutorWorkspaceProfilePageDto(
        List<StatDto> stats,
        IdentityDto identity,
        PublicCardDto publicCard,
        List<FieldDto> fields,
        List<SubjectDto> subjects,
        List<ChecklistDto> checklist
) {
    public record StatDto(String icon, String value, String label, String sublabel, String tone) {
    }

    public record IdentityDto(String name, String role, String city, String avatarInitials, boolean verified, String status, int completion, Integer experienceYears, Integer pricePerHour) {
    }

    public record PublicCardDto(String title, String subtitle, String rating, String price, String response, String views, List<String> chips) {
    }

    public record FieldDto(String id, String label, String value, String type) {
    }

    public record SubjectDto(String title, String meta, String price, String tone) {
    }

    public record ChecklistDto(String title, String text, boolean done) {
    }
}
