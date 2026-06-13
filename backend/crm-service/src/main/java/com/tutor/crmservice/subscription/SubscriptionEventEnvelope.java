package com.tutor.crmservice.subscription;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionEventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        T payload
) {}
