package com.tutor.crmservice.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message text is required")
        @Size(max = 2000, message = "Message text must be at most 2000 characters")
        String text
) {
}
