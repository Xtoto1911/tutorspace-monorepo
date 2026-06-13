package com.tutor.subscriptionservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionActivatedPayload(
        UUID tutorUserId,
        String tariffCode,
        Instant activeUntil
) {}
