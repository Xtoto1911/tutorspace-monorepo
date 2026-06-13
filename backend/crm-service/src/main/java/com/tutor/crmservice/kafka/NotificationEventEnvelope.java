package com.tutor.crmservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record NotificationEventEnvelope<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String producer,
    T payload
) {
    public NotificationEventEnvelope(String eventType, String producer, T payload) {
        this(UUID.randomUUID(), eventType, Instant.now(), producer, payload);
    }
}
