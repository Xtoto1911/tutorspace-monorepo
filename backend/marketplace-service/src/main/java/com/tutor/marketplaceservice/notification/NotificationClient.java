package com.tutor.marketplaceservice.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.notification-service-url:}")
    private String notificationServiceUrl;

    public void notifyRequestCreated(UUID tutorUserId, String tutorName, String subjectName) {
        if (!StringUtils.hasText(notificationServiceUrl)) {
            return;
        }

        try {
            restClientBuilder.build()
                    .post()
                    .uri(notificationServiceUrl + "/internal/notifications")
                    .body(Map.of(
                            "userId", tutorUserId.toString(),
                            "eventType", "REQUEST_CREATED",
                            "title", "Новая заявка",
                            "message", "Клиент отправил заявку на занятие по предмету \"" + subjectName + "\".",
                            "metadata", Map.of(
                                    "tutorName", tutorName,
                                    "subjectName", subjectName
                            )
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Notification request failed for tutor {}: {}", tutorUserId, exception.getMessage());
        }
    }
}
