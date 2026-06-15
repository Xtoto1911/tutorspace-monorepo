package com.tutor.authservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record UserProfileChangedEvent(
        UUID userId,
        String name,
        Instant changedAt
) {
    public static final String TOPIC = "user-profile-events";
}
