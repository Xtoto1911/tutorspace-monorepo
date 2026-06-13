package com.tutor.authservice.security.jwt;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank(message = "JWT secret must be configured")
    private String secret;

    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Duration refreshTokenTtl = Duration.ofDays(1);
}
