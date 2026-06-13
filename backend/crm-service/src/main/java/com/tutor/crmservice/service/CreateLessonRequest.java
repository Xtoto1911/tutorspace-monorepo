package com.tutor.crmservice.service;

import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record CreateLessonRequest(
        UUID applicationId,
        UUID studentId,
        LocalDateTime startsAt,
        @Min(value = 30, message = "Lesson duration must be at least 30 minutes")
        Integer durationMinutes,
        SlotRequest slot,
        String studentName,
        String subject,
        String format,
        RecurrenceRequest recurrence
) {
    public record SlotRequest(LocalDate date, LocalTime startTime, Integer durationMinutes) {}

    public record RecurrenceRequest(RecurrenceType type, Integer occurrences) {}

    public enum RecurrenceType {
        NONE,
        WEEKLY
    }
}
