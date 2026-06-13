package com.tutor.subscriptionservice.subscription;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionActivationResult(
        UUID subscriptionId,
        UUID paymentId,
        UUID periodId,
        UUID tutorUserId,
        String tariffCode,
        Instant activeUntil
) {}
