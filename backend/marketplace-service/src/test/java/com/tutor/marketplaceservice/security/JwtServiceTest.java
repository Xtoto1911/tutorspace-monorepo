package com.tutor.marketplaceservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        jwtService = new JwtService(properties);
    }

    @Test
    void parsesAuthServiceCompatibleToken() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .claims(Map.of(
                        "userId", userId.toString(),
                        "role", "ROLE_CLIENT"
                ))
                .subject("client@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey())
                .compact();

        JwtPrincipal principal = jwtService.getPrincipal(token);

        assertThat(jwtService.validateJwtToken(token)).isTrue();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("client@example.com");
        assertThat(principal.role()).isEqualTo("ROLE_CLIENT");
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
}
