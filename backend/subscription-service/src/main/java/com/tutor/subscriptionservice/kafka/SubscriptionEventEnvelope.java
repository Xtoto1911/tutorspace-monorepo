package com.tutor.subscriptionservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionEventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        T payload
) {
    public SubscriptionEventEnvelope(String eventType, String producer, T payload) {
        this(UUID.randomUUID(), eventType, Instant.now(), producer, payload);
    }
}
