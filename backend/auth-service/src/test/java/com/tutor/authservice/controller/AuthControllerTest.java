package com.tutor.authservice.controller;

import com.tutor.authservice.dto.PasswordResetResponseDto;
import com.tutor.authservice.exception.InvalidPasswordResetTokenException;
import com.tutor.authservice.exceptionHandler.GlobalExceptionHandler;
import com.tutor.authservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private static final String NEUTRAL_MESSAGE =
            "If the account exists, password reset instructions have been created";

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void passwordResetReturnsSameNeutralMessageForDistinctEmails() throws Exception {
        when(userService.requestPasswordReset(any()))
                .thenReturn(new PasswordResetResponseDto(NEUTRAL_MESSAGE, null));

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"known@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(NEUTRAL_MESSAGE));

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(NEUTRAL_MESSAGE));
    }

    @Test
    void passwordResetRejectsInvalidEmailWithMessageAndFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email format"))
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    @Test
    void passwordResetConfirmReturnsStableMessageForInvalidToken() throws Exception {
        when(userService.confirmPasswordReset(any()))
                .thenThrow(new InvalidPasswordResetTokenException());

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"unknown-token","password":"new-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"message":"Invalid or expired password reset token"}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void passwordResetConfirmRejectsShortPasswordWithMessageAndFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"reset-token","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters"))
                .andExpect(jsonPath("$.password").value("Password must be at least 8 characters"));
    }
}
