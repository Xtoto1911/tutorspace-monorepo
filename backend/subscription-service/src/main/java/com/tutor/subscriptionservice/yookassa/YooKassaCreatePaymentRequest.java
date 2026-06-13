package com.tutor.subscriptionservice.yookassa;

import java.util.UUID;

public record YooKassaCreatePaymentRequest(
        UUID localPaymentId,
        UUID tutorUserId,
        String tariffCode,
        String tariffName,
        int amountRub
) {}
