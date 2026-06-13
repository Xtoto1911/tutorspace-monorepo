package com.tutor.crmservice.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
        @NotBlank(message = "Student name is required")
        @Size(max = 255, message = "Student name is too long")
        String name,
        @NotBlank(message = "Student details are required")
        @Size(max = 255, message = "Student details are too long")
        String details,
        @NotBlank(message = "Student subject is required")
        @Size(max = 255, message = "Student subject is too long")
        String subject,
        @Size(max = 500, message = "Student note is too long")
        String note
) {
}
