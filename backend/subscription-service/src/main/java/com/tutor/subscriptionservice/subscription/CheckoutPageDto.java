package com.tutor.subscriptionservice.subscription;

import java.util.UUID;

public record CheckoutPageDto(
        UUID paymentId,
        String tariffCode,
        String tariffName,
        int amountRub,
        String currency,
        int durationDays,
        PaymentStatus status,
        String providerStatus,
        String confirmationUrl,
        boolean testMode
) {
}
