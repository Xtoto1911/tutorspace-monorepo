package com.tutor.crmservice.view;

import java.util.List;

public record ClientSchedulePageDto(
        List<MetricDto> metrics,
        PeriodDto period,
        List<WeekDayDto> weekDays,
        List<String> timeSlots,
        List<EventDto> events,
        List<RescheduleDto> rescheduleRequests,
        List<MiniMonthDayDto> miniMonthDays,
        List<UpcomingLessonDto> upcomingLessons,
        EmptyStateDto emptyState
) {
    public record MetricDto(String icon, String value, String label, String caption, String tone) {
    }

    public record PeriodDto(String from, String to, String label) {
    }

    public record WeekDayDto(String day, String date, boolean active) {
    }

    public record EventDto(
            String id,
            String lessonId,
            String applicationId,
            String threadId,
            String title,
            String meta,
            String time,
            String startsAt,
            String endsAt,
            String tone,
            int column,
            int row,
            boolean canJoin,
            boolean canReschedule,
            boolean canMessageTutor
    ) {
    }

    public record RescheduleDto(String id, String lessonId, String time, String startsAt, String title, String status, String tone) {
    }

    public record MiniMonthDayDto(String date, boolean active) {
    }

    public record UpcomingLessonDto(
            String id,
            String lessonId,
            String applicationId,
            String threadId,
            String time,
            String title,
            String details,
            boolean canJoin,
            boolean canReschedule,
            boolean canMessageTutor
    ) {
    }

    public record EmptyStateDto(boolean show, String title, String text) {
    }
}
