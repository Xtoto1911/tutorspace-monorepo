package com.tutor.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileNameRequest(
        @NotBlank
        @Size(min = 2, max = 100, message = "ФИО должно содержать от 2 до 100 символов")
        String name
) {}
