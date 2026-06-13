package com.tutor.notificationservice.notification;

import java.util.Map;

public record NotificationIngestDto(
        String userId,
        String eventType,
        String title,
        String message,
        Map<String, String> metadata
) {
}
