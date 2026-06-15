package com.tutor.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.domain.UserStatus;
import com.tutor.authservice.dto.UpdateProfileNameRequest;
import com.tutor.authservice.security.CustomUserServiceImpl;
import com.tutor.authservice.security.jwt.JwtService;
import com.tutor.authservice.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerMeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserServiceImpl customUserServiceImpl;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void patchMeUpdatesNameAndReturnsPublicDto() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("tutor@example.com");
        user.setName("Пётр");
        user.setRole(Role.ROLE_TUTOR);
        user.setStatus(UserStatus.ACTIVE);
        when(userService.updateProfileName(eq(userId), eq("Пётр"))).thenReturn(user);

        authenticateAs(user);

        mockMvc.perform(patch("/api/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileNameRequest("Пётр"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("tutor@example.com"))
                .andExpect(jsonPath("$.name").value("Пётр"))
                .andExpect(jsonPath("$.role").value("ROLE_TUTOR"));

        verify(userService).updateProfileName(userId, "Пётр");
    }

    @Test
    void patchMeRejectsInvalidNameWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        authenticateAs(user);

        mockMvc.perform(patch("/api/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"А\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void patchMeWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(patch("/api/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Пётр\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void patchMeReturns400WhenServiceThrowsIllegalArgument() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userService.updateProfileName(eq(userId), eq("А ")))
                .thenThrow(new IllegalArgumentException("ФИО должно содержать от 2 до 100 символов."));
        authenticateAs(user);

        mockMvc.perform(patch("/api/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"А \"}"))
                .andExpect(status().isBadRequest());
    }

    private void authenticateAs(User user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
