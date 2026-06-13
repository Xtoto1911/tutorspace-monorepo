package com.tutor.crmservice.view;

import java.util.List;

public record ClientApplicationsPageDto(
        List<MetricDto> metrics,
        List<String> tabs,
        List<ApplicationCardDto> applications,
        SideLessonDto nextLesson,
        List<ProgressDto> matchProgress,
        List<String> recommendations
) {
    public record MetricDto(String value, String label, String tone) {
    }

    public record ApplicationCardDto(String id,
                                     String initials,
                                     String name,
                                     String subject,
                                     List<String> chips,
                                     String status,
                                     String statusTone,
                                     String price,
                                     String note,
                                     String action) {
    }

    public record SideLessonDto(String title, String pill, String teacher, String text) {
    }

    public record ProgressDto(String label, int value) {
    }
}
