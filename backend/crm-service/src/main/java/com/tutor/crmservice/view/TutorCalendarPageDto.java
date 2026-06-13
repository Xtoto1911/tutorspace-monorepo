package com.tutor.crmservice.view;

import java.util.List;

public record TutorCalendarPageDto(
        List<StatDto> stats,
        List<WeekDayDto> weekDays,
        List<String> timeSlots,
        List<EventDto> events,
        NextLessonDto nextLesson,
        List<UpcomingEventDto> upcomingEvents,
        List<MonthDayDto> monthDays,
        List<SlotDto> slots,
        List<PendingRequestDto> pendingRequests
) {
    public record StatDto(String icon, String value, String label, String sublabel, String tone) {
    }

    public record WeekDayDto(String day, String date, boolean active) {
    }

    public record EventDto(String id,
                           String title,
                           String subtitle,
                           String tone,
                           int column,
                           int row,
                           int rows,
                           String startsAt,
                           String seriesId,
                           String recurrenceLabel,
                           String applicationId,
                           String threadId,
                           String studentId) {
    }

    public record NextLessonDto(String time, String title, String description) {
    }

    public record UpcomingEventDto(String time, String text, String tone) {
    }

    public record MonthDayDto(String day, boolean muted, boolean selected) {
    }

    public record PendingRequestDto(String applicationId,
                                    String studentId,
                                    String studentName,
                                    String subject,
                                    String format,
                                    String desiredStartTime,
                                    String gradeGoal,
                                    String threadId) {
    }
}
