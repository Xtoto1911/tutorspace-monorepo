package com.tutor.crmservice.view;

public record TutorLessonCandidateDto(
        String applicationId,
        String studentId,
        String studentName,
        String subject,
        String format,
        String desiredStartTime,
        String gradeGoal
) {
}
