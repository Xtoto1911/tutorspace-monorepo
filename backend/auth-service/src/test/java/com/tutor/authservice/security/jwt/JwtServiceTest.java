package com.tutor.authservice.security.jwt;

import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.dto.JwtAuthenticationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenTtl(Duration.ofHours(24));

        jwtService = new JwtService(properties);
    }

    @Test
    void generateAuthTokenIncludesUserIdRoleAndNameClaims() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("admin@example.com");
        user.setName("Admin User");
        user.setRole(Role.ROLE_ADMIN);

        JwtAuthenticationDto tokens = jwtService.generateAuthToken(user);

        assertThat(jwtService.validateJwtToken(tokens.getToken())).isTrue();
        assertThat(jwtService.getEmailFromToken(tokens.getToken())).isEqualTo("admin@example.com");
        assertThat(jwtService.getUserIdFromToken(tokens.getToken())).isEqualTo(userId);
        assertThat(jwtService.getRoleFromToken(tokens.getToken())).isEqualTo("ROLE_ADMIN");
        assertThat(tokens.getName()).isEqualTo("Admin User");
        assertThat(jwtService.getExpirationFromToken(tokens.getRefreshToken())).isAfter(java.time.Instant.now());
    }
}
