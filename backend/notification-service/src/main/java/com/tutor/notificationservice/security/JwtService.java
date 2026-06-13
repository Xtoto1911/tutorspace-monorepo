package com.tutor.notificationservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties jwtProperties;

    public boolean validateJwtToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT", ex);
        } catch (UnsupportedJwtException ex) {
            log.debug("Unsupported JWT", ex);
        } catch (MalformedJwtException ex) {
            log.debug("Malformed JWT", ex);
        } catch (SecurityException ex) {
            log.debug("Invalid JWT signature", ex);
        } catch (Exception ex) {
            log.debug("Invalid JWT", ex);
        }
        return false;
    }

    public JwtPrincipal getPrincipal(String token) {
        Claims claims = getClaims(token);
        return new JwtPrincipal(
                UUID.fromString(claims.get("userId", String.class)),
                claims.getSubject(),
                claims.get("role", String.class)
        );
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
