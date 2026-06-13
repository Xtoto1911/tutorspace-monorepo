package com.tutor.crmservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    public void publishApplicationAccepted(UUID clientUserId, UUID applicationId, String tutorName, String subjectName) {
        sendToClient(clientUserId, "APPLICATION_ACCEPTED", "Заявка принята",
                "Репетитор " + tutorName + " принял вашу заявку на занятие по предмету \"" + subjectName + "\".",
                metadata("tutorName", tutorName, "subjectName", subjectName, "applicationId", applicationId));
    }

    public void publishApplicationRejected(UUID clientUserId, UUID applicationId, String tutorName, String subjectName, String reason) {
        sendToClient(clientUserId, "APPLICATION_REJECTED", "Заявка отклонена",
                "Репетитор " + tutorName + " отклонил вашу заявку. Причина: " + reason,
                metadata("tutorName", tutorName, "subjectName", subjectName, "reason", reason, "applicationId", applicationId));
    }

    public void publishLessonCreated(UUID clientUserId, UUID applicationId, UUID lessonId, String tutorName, String subjectName, String startTime) {
        sendToClient(clientUserId, "LESSON_CREATED", "Занятие запланировано",
                "Занятие по предмету \"" + subjectName + "\" с репетитором " + tutorName + " запланировано на " + startTime + ".",
                metadata("tutorName", tutorName, "subjectName", subjectName, "startTime", startTime, "applicationId", applicationId, "lessonId", lessonId));
    }

    public void publishRescheduleRequested(UUID tutorUserId, UUID applicationId, UUID lessonId, String lessonTitle, String message) {
        sendToTutor(tutorUserId, "RESCHEDULE_REQUESTED", "Запрошен перенос",
                "Клиент попросил перенести занятие \"" + lessonTitle + "\": " + message,
                metadata("applicationId", applicationId, "lessonId", lessonId));
    }

    public void publishLessonRescheduled(UUID clientUserId, UUID applicationId, UUID lessonId, String subjectName, String startTime) {
        sendToClient(clientUserId, "LESSON_RESCHEDULED", "Занятие перенесено",
                "Занятие по предмету \"" + subjectName + "\" перенесено на " + startTime + ".",
                metadata("subjectName", subjectName, "startTime", startTime, "applicationId", applicationId, "lessonId", lessonId));
    }

    public void publishTutorMessage(UUID clientUserId, UUID threadId, UUID applicationId, String tutorName) {
        sendToClient(clientUserId, "TUTOR_MESSAGE_SENT", "Новое сообщение",
                "Репетитор " + tutorName + " отправил сообщение.",
                metadata("threadId", threadId, "applicationId", applicationId, "tutorName", tutorName));
    }

    public void publishClientMessage(UUID tutorUserId, UUID threadId, UUID applicationId, String studentName) {
        sendToTutor(tutorUserId, "CLIENT_MESSAGE_SENT", "Новое сообщение",
                "Клиент " + studentName + " отправил сообщение.",
                metadata("threadId", threadId, "applicationId", applicationId, "studentName", studentName));
    }

    private void sendToClient(UUID userId, String eventType, String title, String message, Map<String, String> metadata) {
        send("client-notifications", userId, eventType, title, message, metadata);
    }

    private void sendToTutor(UUID userId, String eventType, String title, String message, Map<String, String> metadata) {
        send("tutor-notifications", userId, eventType, title, message, metadata);
    }

    private void send(String topic, UUID userId, String eventType, String title, String message, Map<String, String> metadata) {
        var payload = new NotificationPayload(userId, title, message, metadata);
        var envelope = new NotificationEventEnvelope<>(eventType, "crm-service", payload);

        try {
            kafkaTemplate.send(topic, userId.toString(), envelope)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event for user {}: {}", eventType, userId, ex.getMessage());
                        } else {
                            log.info("Published {} event for user {}", eventType, userId);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing {} event for user {}: {}", eventType, userId, e.getMessage());
        }
    }

    private Map<String, String> metadata(Object... pairs) {
        Map<String, String> result = new HashMap<>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            Object key = pairs[index];
            Object value = pairs[index + 1];
            if (key != null && value != null) {
                result.put(key.toString(), value.toString());
            }
        }
        return result;
    }
}
