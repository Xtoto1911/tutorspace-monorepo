package com.tutor.authservice.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin-seed")
public record AdminSeedProperties(
        boolean enabled,
        @Email String email,
        @NotBlank String password
) {
}
