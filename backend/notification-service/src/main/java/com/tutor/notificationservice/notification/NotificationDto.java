package com.tutor.notificationservice.notification;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String eventType,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt,
        Map<String, String> metadata,
        String actionUrl
) {
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getMetadata(),
                actionUrl(notification.getEventType(), notification.getMetadata())
        );
    }

    private static String actionUrl(String eventType, Map<String, String> metadata) {
        String threadId = metadata == null ? null : metadata.get("threadId");
        String applicationId = metadata == null ? null : metadata.get("applicationId");

        return switch (eventType) {
            case "REQUEST_CREATED" -> "/app/tutor/requests";
            case "CLIENT_MESSAGE_SENT" -> threadId == null ? "/app/tutor/messages" : "/app/tutor/messages?threadId=" + threadId;
            case "APPLICATION_ACCEPTED", "APPLICATION_REJECTED" ->
                    applicationId == null ? "/app/client/applications" : "/app/client/applications/" + applicationId;
            case "LESSON_CREATED", "LESSON_RESCHEDULED" -> "/app/client/schedule";
            case "RESCHEDULE_REQUESTED" -> "/app/tutor/calendar";
            case "TUTOR_MESSAGE_SENT" -> threadId == null ? "/app/client/messages" : "/app/client/messages?threadId=" + threadId;
            default -> null;
        };
    }
}
