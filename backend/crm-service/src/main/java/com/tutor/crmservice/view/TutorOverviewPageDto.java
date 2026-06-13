package com.tutor.crmservice.view;

public record TutorOverviewPageDto(
        int newRequests,
        int activeStudents,
        int upcomingLessons,
        LessonDto nextLesson
) {
    public record LessonDto(
            String id,
            String applicationId,
            String studentName,
            String title,
            String subject,
            String format,
            String startsAt
    ) {
    }
}
