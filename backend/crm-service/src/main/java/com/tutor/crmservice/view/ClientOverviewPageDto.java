package com.tutor.crmservice.view;

import java.util.List;

public record ClientOverviewPageDto(
        int activeApplications,
        List<ApplicationDto> recentApplications,
        LessonDto nextLesson,
        ConversationDto recentConversation
) {
    public record ApplicationDto(
            String id,
            String tutorName,
            String tutorSlug,
            String subject,
            String status,
            String statusTone
    ) {
    }

    public record LessonDto(
            String id,
            String applicationId,
            String title,
            String tutorName,
            String subject,
            String format,
            String startsAt
    ) {
    }

    public record ConversationDto(
            String threadId,
            String participantName,
            String subject,
            String preview,
            String updatedAt
    ) {
    }
}
