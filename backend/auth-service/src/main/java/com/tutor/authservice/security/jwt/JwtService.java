package com.tutor.authservice.security.jwt;

import com.tutor.authservice.domain.User;
import com.tutor.authservice.dto.JwtAuthenticationDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtAuthenticationDto generateAuthToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId().toString());
        claims.put("name", user.getName());

        JwtAuthenticationDto jwtDto = new JwtAuthenticationDto();
        jwtDto.setToken(generateAccessToken(claims, user.getEmail()));
        jwtDto.setRefreshToken(generateRefreshToken(claims, user.getEmail()));
        jwtDto.setName(user.getName());

        return jwtDto;
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.error("Expired JwtEx", ex);
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JwtEx", ex);
        } catch (MalformedJwtException ex) {
            log.error("Malformed JwtEx", ex);
        } catch (SecurityException ex) {
            log.error("Security ex", ex);
        } catch (Exception ex) {
            log.error("invalid ex", ex);
        }
        return false;
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(getClaims(token).get("userId", String.class));
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Instant getExpirationFromToken(String token) {
        return getClaims(token).getExpiration().toInstant();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String generateAccessToken(Map<String, Object> claims, String email) {
        Date date = Date.from(Instant.now().plus(jwtProperties.getAccessTokenTtl()));
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .expiration(date)
                .signWith(getSigningKey())
                .compact();
    }

    private String generateRefreshToken(Map<String, Object> claims, String email) {
        Date date = Date.from(Instant.now().plus(jwtProperties.getRefreshTokenTtl()));
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .expiration(date)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }


}
