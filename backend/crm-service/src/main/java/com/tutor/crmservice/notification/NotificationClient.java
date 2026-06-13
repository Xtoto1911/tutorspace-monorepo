package com.tutor.crmservice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient.Builder restClientBuilder;

    @Value("${app.notification-service-url:}")
    private String notificationServiceUrl;

    public void notifyApplicationAccepted(UUID clientUserId, String tutorName, String subjectName) {
        send(clientUserId, "APPLICATION_ACCEPTED", "Заявка принята",
                "Репетитор " + tutorName + " принял вашу заявку на занятие по предмету \"" + subjectName + "\".",
                Map.of("tutorName", tutorName, "subjectName", subjectName));
    }

    public void notifyApplicationRejected(UUID clientUserId, String tutorName, String subjectName, String reason) {
        send(clientUserId, "APPLICATION_REJECTED", "Заявка отклонена",
                "Репетитор " + tutorName + " отклонил вашу заявку. Причина: " + reason,
                Map.of("tutorName", tutorName, "subjectName", subjectName, "reason", reason));
    }

    public void notifyLessonCreated(UUID clientUserId, String tutorName, String subjectName, String startTime) {
        send(clientUserId, "LESSON_CREATED", "Занятие запланировано",
                "Занятие по предмету \"" + subjectName + "\" с репетитором " + tutorName + " запланировано на " + startTime + ".",
                Map.of("tutorName", tutorName, "subjectName", subjectName, "startTime", startTime));
    }

    private void send(UUID userId, String eventType, String title, String message, Map<String, String> metadata) {
        if (!StringUtils.hasText(notificationServiceUrl)) {
            return;
        }

        try {
            restClientBuilder.build()
                    .post()
                    .uri(notificationServiceUrl + "/internal/notifications")
                    .body(Map.of(
                            "userId", userId.toString(),
                            "eventType", eventType,
                            "title", title,
                            "message", message,
                            "metadata", metadata
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Notification request failed for user {}: {}", userId, exception.getMessage());
        }
    }
}
