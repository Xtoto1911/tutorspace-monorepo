package com.tutor.subscriptionservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventPublisher {

    private static final String TOPIC = "crm-events";

    private final KafkaTemplate<String, SubscriptionEventEnvelope<SubscriptionActivatedPayload>> kafkaTemplate;

    public void publishSubscriptionActivated(UUID tutorUserId, String tariffCode, Instant activeUntil) {
        var payload = new SubscriptionActivatedPayload(tutorUserId, tariffCode, activeUntil);
        var envelope = new SubscriptionEventEnvelope<>("SUBSCRIPTION_ACTIVATED", "subscription-service", payload);

        try {
            kafkaTemplate.send(TOPIC, tutorUserId.toString(), envelope)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish SUBSCRIPTION_ACTIVATED for tutor {}: {}", tutorUserId, exception.getMessage());
                        } else {
                            log.info("Published SUBSCRIPTION_ACTIVATED for tutor {}", tutorUserId);
                        }
                    });
        } catch (Exception exception) {
            log.error("Exception publishing SUBSCRIPTION_ACTIVATED for tutor {}: {}", tutorUserId, exception.getMessage());
        }
    }
}
