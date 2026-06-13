package com.tutor.subscriptionservice.subscription;

import java.util.UUID;

public record CheckoutResponse(
        UUID paymentId,
        String confirmationUrl,
        PaymentStatus status
) {}
