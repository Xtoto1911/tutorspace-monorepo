package com.tutor.marketplaceservice.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
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
    void shouldPublishRequestCreatedEvent() {
        var tutorUserId = UUID.randomUUID();
        var marketplaceRequestId = UUID.randomUUID();
        var applicationId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishRequestCreated(tutorUserId, marketplaceRequestId, applicationId, "anna", "Анна", "Математика");

        ArgumentCaptor<NotificationEventEnvelope> eventCaptor = ArgumentCaptor.forClass(NotificationEventEnvelope.class);
        verify(kafkaTemplate).send(eq("tutor-notifications"), eq(tutorUserId.toString()), eventCaptor.capture());
        NotificationPayload payload = (NotificationPayload) eventCaptor.getValue().payload();
        assertThat(payload.metadata()).containsEntry("marketplaceRequestId", marketplaceRequestId.toString());
        assertThat(payload.metadata()).containsEntry("applicationId", applicationId.toString());
    }
}
