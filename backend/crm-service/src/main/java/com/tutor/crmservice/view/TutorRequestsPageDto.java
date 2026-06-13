package com.tutor.crmservice.view;

import java.util.List;

public record TutorRequestsPageDto(
        List<StatDto> stats,
        List<RequestDto> requests,
        TodayDto today,
        QualityDto quality,
        FunnelDto funnel
) {
    public record StatDto(String icon, String value, String label, String sublabel, String tone) {
    }

    public record RequestDto(String id,
                             String initials,
                             String studentName,
                             String gradeGoal,
                             String subject,
                             String status,
                             String description,
                             String price,
                             String format,
                             String preferredTime,
                             String tone,
                             boolean highlighted,
                             String stage,
                             String source,
                             String deadline,
                             boolean overdue,
                             int leadScore,
                             String nextAction,
                             String rejectionReason,
                             String threadId,
                             String scheduledLessonId,
                             String scheduledLessonAt,
                             String completenessLabel) {
    }

    public record TodayDto(String title, String summary, List<TodayEventDto> events) {
    }

    public record TodayEventDto(String time, String text, String tone) {
    }

    public record QualityDto(String label, String value, String text, int percent) {
    }

    public record FunnelDto(int newCount,
                            int viewedCount,
                            int respondedCount,
                            int contactCount,
                            int trialCount,
                            int studentCount,
                            int closedCount,
                            int trialConversionPercent,
                            int studentConversionPercent,
                            int overdueCount,
                            String averageResponseTime) {
    }
}
