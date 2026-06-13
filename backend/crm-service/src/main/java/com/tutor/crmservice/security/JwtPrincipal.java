package com.tutor.crmservice.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String email, String role) {
}
