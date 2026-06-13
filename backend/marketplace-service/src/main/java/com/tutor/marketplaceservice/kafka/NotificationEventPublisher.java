package com.tutor.marketplaceservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    public void publishRequestCreated(UUID tutorUserId, UUID requestId, UUID applicationId, String tutorSlug, String tutorName, String subjectName) {
        var payload = new NotificationPayload(
                tutorUserId,
                "Новая заявка",
                "Клиент отправил заявку на занятие по предмету \"" + subjectName + "\".",
                Map.of(
                        "tutorName", tutorName,
                        "subjectName", subjectName,
                        "marketplaceRequestId", requestId.toString(),
                        "applicationId", applicationId.toString(),
                        "tutorSlug", tutorSlug
                )
        );
        var envelope = new NotificationEventEnvelope<>("REQUEST_CREATED", "marketplace-service", payload);

        try {
            kafkaTemplate.send("tutor-notifications", tutorUserId.toString(), envelope)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish RequestCreated event for tutor {}: {}", tutorUserId, ex.getMessage());
                        } else {
                            log.info("Published RequestCreated event for tutor {}", tutorUserId);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing RequestCreated event for tutor {}: {}", tutorUserId, e.getMessage());
        }
    }
}
