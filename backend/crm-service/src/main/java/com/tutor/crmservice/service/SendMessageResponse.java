package com.tutor.crmservice.service;

public record SendMessageResponse(
        String id,
        String threadId,
        String text,
        String status
) {
}
