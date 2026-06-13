package com.tutor.crmservice.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    @InjectMocks
    private NotificationEventPublisher publisher;

    @Test
    void shouldPublishApplicationAcceptedEvent() {
        var clientUserId = UUID.randomUUID();
        var applicationId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishApplicationAccepted(clientUserId, applicationId, "Анна", "Математика");

        verify(kafkaTemplate).send(eq("client-notifications"), eq(clientUserId.toString()), any(NotificationEventEnvelope.class));
    }

    @Test
    void shouldPublishApplicationRejectedEvent() {
        var clientUserId = UUID.randomUUID();
        var applicationId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishApplicationRejected(clientUserId, applicationId, "Анна", "Математика", "Нет времени");

        verify(kafkaTemplate).send(eq("client-notifications"), eq(clientUserId.toString()), any(NotificationEventEnvelope.class));
    }

    @Test
    void shouldPublishLessonCreatedEvent() {
        var clientUserId = UUID.randomUUID();
        var applicationId = UUID.randomUUID();
        var lessonId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishLessonCreated(clientUserId, applicationId, lessonId, "Анна", "Математика", "2026-05-01 10:00");

        verify(kafkaTemplate).send(eq("client-notifications"), eq(clientUserId.toString()), any(NotificationEventEnvelope.class));
    }

    @Test
    void shouldPublishClientMessageEventToTutorTopic() {
        var tutorUserId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishClientMessage(tutorUserId, UUID.randomUUID(), UUID.randomUUID(), "Мария");

        verify(kafkaTemplate).send(eq("tutor-notifications"), eq(tutorUserId.toString()), any(NotificationEventEnvelope.class));
    }

    @Test
    void shouldPublishLessonRescheduledEventToClientTopic() {
        var clientUserId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishLessonRescheduled(clientUserId, UUID.randomUUID(), UUID.randomUUID(), "Математика", "05.05.2026 19:00");

        verify(kafkaTemplate).send(eq("client-notifications"), eq(clientUserId.toString()), any(NotificationEventEnvelope.class));
    }
}
