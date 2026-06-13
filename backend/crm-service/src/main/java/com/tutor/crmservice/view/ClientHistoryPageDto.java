package com.tutor.crmservice.view;

import java.util.List;

public record ClientHistoryPageDto(
        List<StatDto> stats,
        List<String> filters,
        String activeMonth,
        List<EventDto> events,
        List<SummaryRowDto> monthSummary,
        List<CompletedLessonDto> completedLessons,
        EmptyStateDto emptyState
) {
    public record StatDto(String icon, String value, String label, String sublabel, String tone) {
    }

    public record EventDto(String id, String type, String date, String title, String text, String status, String tone) {
    }

    public record SummaryRowDto(String title, String caption, String value, String tone) {
    }

    public record CompletedLessonDto(String lessonId, String date, String tutor, String subject, String result, String href) {
    }

    public record EmptyStateDto(boolean show, String title, String text) {
    }
}
