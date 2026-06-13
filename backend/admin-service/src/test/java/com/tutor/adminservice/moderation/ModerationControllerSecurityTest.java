package com.tutor.adminservice.moderation;

import com.tutor.adminservice.audit.AuditLogService;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.config.SecurityConfig;
import com.tutor.adminservice.page.AdminPageService;
import com.tutor.adminservice.security.JwtFilter;
import com.tutor.adminservice.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModerationController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtService.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class ModerationControllerSecurityTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketplaceServiceClient marketplaceClient;

    @MockitoBean
    private AuthServiceClient authClient;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private AdminPageService adminPageService;

    @Test
    void unsupportedAdminUserActionReachesControllerAndReturns422() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType("application/json")
                        .content("{\"action\":\"remove_role\"}"))
                .andExpect(status().isUnprocessableEntity());
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
