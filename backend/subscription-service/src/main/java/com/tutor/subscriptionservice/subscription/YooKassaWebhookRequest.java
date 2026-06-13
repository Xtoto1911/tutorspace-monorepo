package com.tutor.subscriptionservice.subscription;

public record YooKassaWebhookRequest(
        String event,
        YooKassaWebhookObject object,
        String rawPayload
) {
}
