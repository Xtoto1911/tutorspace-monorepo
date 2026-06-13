package com.tutor.adminservice.moderation;

import com.tutor.adminservice.audit.AuditLogService;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.page.AdminPageService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class ModerationControllerHttpSecurityTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private MarketplaceServiceClient marketplaceClient;

    @MockitoBean
    private AuthServiceClient authClient;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private AdminPageService adminPageService;

    @Test
    void unsupportedAdminUserActionReturns422OverHttp() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        var request = new HttpEntity<>(Map.of("action", "remove_role"), headers);

        var response = restTemplate.exchange(
                "/api/admin/users/{id}",
                HttpMethod.PATCH,
                request,
                String.class,
                UUID.randomUUID()
        );

        assertEquals(422, response.getStatusCode().value());
    }

    private String adminToken() {
        return Jwts.builder()
                .subject("admin@tutorspace.local")
                .claim("role", "ROLE_ADMIN")
                .claim("userId", UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
    }
}
