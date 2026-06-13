package com.tutor.subscriptionservice.subscription;

import java.time.Instant;
import java.util.UUID;

public record CurrentSubscriptionDto(
        UUID tutorUserId,
        boolean active,
        String tariffCode,
        Instant activeUntil
) {
    public static CurrentSubscriptionDto inactive(UUID tutorUserId) {
        return new CurrentSubscriptionDto(tutorUserId, false, null, null);
    }
}
