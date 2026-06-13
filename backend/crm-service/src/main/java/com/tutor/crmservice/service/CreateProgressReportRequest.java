package com.tutor.crmservice.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProgressReportRequest(
        @NotBlank(message = "Report title is required")
        @Size(max = 255, message = "Report title is too long")
        String title,
        @NotBlank(message = "Report summary is required")
        @Size(max = 2000, message = "Report summary is too long")
        String summary,
        @Min(value = 0, message = "Progress cannot be below 0")
        @Max(value = 100, message = "Progress cannot exceed 100")
        int progress
) {
}
