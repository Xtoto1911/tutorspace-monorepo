package com.tutor.authservice.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserProfileEventPublisherTest {

    @Mock
    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, UserProfileChangedEvent> kafkaTemplate;

    @InjectMocks
    private UserProfileEventPublisher publisher;

    @Test
    void publishesEventToUserProfileEventsTopicKeyedByUserId() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-15T10:00:00Z");

        publisher.publish(userId, "Пётр", now);

        ArgumentCaptor<UserProfileChangedEvent> captor = ArgumentCaptor.forClass(UserProfileChangedEvent.class);
        verify(kafkaTemplate).send(eq(UserProfileChangedEvent.TOPIC), eq(userId.toString()), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().name()).isEqualTo("Пётр");
        assertThat(captor.getValue().changedAt()).isEqualTo(now);
    }
}
