package com.tutor.notificationservice.notification;

import com.tutor.notificationservice.security.JwtAuthenticationFilter;
import com.tutor.notificationservice.security.JwtService;
import com.tutor.notificationservice.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {UserNotificationController.class, AdminNotificationController.class},
        properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class UserNotificationControllerSecurityTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void userNotificationsRequireToken() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCanListOnlyOwnNotifications() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(notificationService.listDtosByUser(userId.toString())).thenReturn(List.of(new NotificationDto(
                notificationId,
                "LESSON_CREATED",
                "Занятие запланировано",
                "Занятие появилось в расписании.",
                false,
                LocalDateTime.of(2026, 5, 28, 12, 0),
                Map.of("lessonId", "lesson-1"),
                "/app/client/schedule"
        )));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token("ROLE_CLIENT", userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$[0].actionUrl").value("/app/client/schedule"));

        verify(notificationService).listDtosByUser(eq(userId.toString()));
    }

    @Test
    void tutorCanMarkAllNotificationsRead() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + token("ROLE_TUTOR", userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        verify(notificationService).markAllRead(userId.toString());
    }

    @Test
    void clientCannotReadAdminNotifications() throws Exception {
        mockMvc.perform(get("/api/admin/notifications")
                        .header("Authorization", "Bearer " + token("ROLE_CLIENT", UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAdminNotifications() throws Exception {
        when(notificationService.listAllDtos()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/notifications")
                        .header("Authorization", "Bearer " + token("ROLE_ADMIN", UUID.randomUUID())))
                .andExpect(status().isOk());
    }

    private String token(String role, UUID userId) {
        return Jwts.builder()
                .claims(Map.of(
                        "userId", userId.toString(),
                        "role", role
                ))
                .subject("user@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
}
