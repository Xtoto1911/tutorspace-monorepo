package com.tutor.authservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileEventPublisher {

    private final KafkaTemplate<String, UserProfileChangedEvent> kafkaTemplate;

    public void publish(UUID userId, String name, Instant changedAt) {
        UserProfileChangedEvent event = new UserProfileChangedEvent(userId, name, changedAt);
        try {
            kafkaTemplate.send(UserProfileChangedEvent.TOPIC, userId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish UserProfileChanged for user {}: {}", userId, ex.getMessage());
                        } else {
                            log.info("Published UserProfileChanged for user {}", userId);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing UserProfileChanged for user {}: {}", userId, e.getMessage());
        }
    }
}
