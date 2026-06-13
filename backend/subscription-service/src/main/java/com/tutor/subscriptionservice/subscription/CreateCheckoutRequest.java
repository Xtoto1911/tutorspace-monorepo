package com.tutor.subscriptionservice.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCheckoutRequest(
        @NotNull UUID tutorUserId,
        @NotBlank String tariffCode
) {}
