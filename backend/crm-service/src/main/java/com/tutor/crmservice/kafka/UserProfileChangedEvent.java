package com.tutor.crmservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record UserProfileChangedEvent(
        UUID userId,
        String name,
        Instant changedAt
) {}
