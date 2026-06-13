package com.tutor.crmservice.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record RequestLessonRescheduleDto(
        @NotBlank(message = "Сообщение для переноса обязательно.")
        @Size(max = 1000, message = "Сообщение для переноса не должно превышать 1000 символов.")
        String message,
        LocalDateTime proposedStartAt
) {
}
