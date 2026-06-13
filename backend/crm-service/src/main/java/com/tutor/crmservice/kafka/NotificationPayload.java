package com.tutor.crmservice.kafka;

import java.util.Map;
import java.util.UUID;

public record NotificationPayload(
    UUID userId,
    String title,
    String message,
    Map<String, String> metadata
) {}
