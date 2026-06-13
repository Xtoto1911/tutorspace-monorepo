package com.tutor.subscriptionservice.yookassa;

public record YooKassaPayment(
        String id,
        String status,
        boolean paid,
        boolean test,
        int amountRub,
        String currency,
        String confirmationUrl,
        String cancellationReason
) {}
