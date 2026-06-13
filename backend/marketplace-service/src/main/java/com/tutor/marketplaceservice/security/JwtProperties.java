package com.tutor.marketplaceservice.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank(message = "JWT secret must be configured")
    private String secret;
}
