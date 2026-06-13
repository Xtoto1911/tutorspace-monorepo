package com.tutor.adminservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final JwtService jwtService = new JwtService();

    @Test
    void validatesAuthServiceTokenAndExtractsUserIdClaim() {
        var userId = UUID.randomUUID();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        var token = Jwts.builder()
                .claims(Map.of(
                        "role", "ROLE_ADMIN",
                        "userId", userId.toString()
                ))
                .subject("admin@tutorspace.local")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals("ROLE_ADMIN", jwtService.extractRole(token));
    }
}
