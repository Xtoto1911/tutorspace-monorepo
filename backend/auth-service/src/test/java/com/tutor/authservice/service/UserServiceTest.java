package com.tutor.authservice.service;

import com.tutor.authservice.config.PasswordResetProperties;
import com.tutor.authservice.domain.PasswordResetToken;
import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.domain.UserSession;
import com.tutor.authservice.domain.UserStatus;
import com.tutor.authservice.dto.JwtAuthenticationDto;
import com.tutor.authservice.dto.PasswordResetConfirmDto;
import com.tutor.authservice.dto.PasswordResetRequestDto;
import com.tutor.authservice.dto.PasswordResetResponseDto;
import com.tutor.authservice.dto.RefreshTokenDto;
import com.tutor.authservice.dto.RegisterRequestDto;
import com.tutor.authservice.exception.InvalidRoleRegistrationException;
import com.tutor.authservice.exception.InvalidPasswordResetTokenException;
import com.tutor.authservice.exception.UserNotFoundException;
import com.tutor.authservice.repository.PasswordResetTokenRepository;
import com.tutor.authservice.repository.UserRepository;
import com.tutor.authservice.repository.UserSessionRepository;
import com.tutor.authservice.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordResetProperties passwordResetProperties;

    @InjectMocks
    private UserService userService;

    @Test
    void registerCreatesActivePublicUser() {
        RegisterRequestDto dto = registerRequest(Role.ROLE_CLIENT);
        JwtAuthenticationDto expectedTokens = new JwtAuthenticationDto();
        expectedTokens.setToken("access");
        expectedTokens.setRefreshToken("refresh");
        Instant expiresAt = Instant.parse("2026-04-20T22:00:00Z");

        when(userRepository.existsByEmail("client@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed-password");
        when(jwtService.generateAuthToken(any(User.class))).thenReturn(expectedTokens);
        when(jwtService.getExpirationFromToken("refresh")).thenReturn(expiresAt);

        JwtAuthenticationDto tokens = userService.register(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession savedSession = sessionCaptor.getValue();

        assertThat(tokens).isSameAs(expectedTokens);
        assertThat(savedUser.getEmail()).isEqualTo("client@example.com");
        assertThat(savedUser.getName()).isEqualTo("Client User");
        assertThat(savedUser.getPassword()).isEqualTo("hashed-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_CLIENT);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedSession.getUser()).isSameAs(savedUser);
        assertThat(savedSession.getRefreshTokenHash()).hasSize(64);
        assertThat(savedSession.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void registerRejectsAdminRoleFromPublicEndpoint() {
        RegisterRequestDto dto = registerRequest(Role.ROLE_ADMIN);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(InvalidRoleRegistrationException.class)
                .hasMessageContaining("ROLE_ADMIN")
                .hasMessageContaining("Регистрация");

        verifyNoInteractions(userRepository, userSessionRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerRejectsModeratorRoleFromPublicEndpoint() {
        RegisterRequestDto dto = registerRequest(Role.ROLE_MODERATOR);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(InvalidRoleRegistrationException.class)
                .hasMessageContaining("ROLE_MODERATOR")
                .hasMessageContaining("Регистрация");

        verifyNoInteractions(userRepository, userSessionRepository, passwordEncoder, jwtService);
    }

    @Test
    void createModeratorAccountCreatesActiveModeratorWithoutIssuingSession() {
        when(userRepository.existsByEmail("moderator@example.com")).thenReturn(false);
        when(passwordEncoder.encode("moderator-password")).thenReturn("hashed-moderator-password");

        User moderator = userService.createModeratorAccount("moderator@example.com", "moderator-password", "Модератор");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(moderator).isSameAs(savedUser);
        assertThat(savedUser.getEmail()).isEqualTo("moderator@example.com");
        assertThat(savedUser.getName()).isEqualTo("Модератор");
        assertThat(savedUser.getPassword()).isEqualTo("hashed-moderator-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_MODERATOR);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verifyNoInteractions(userSessionRepository, jwtService);
    }

    @Test
    void refreshTokenRevokesOldSessionAndCreatesNewOne() {
        User user = activeUser();
        UserSession oldSession = activeSession(user);
        JwtAuthenticationDto expectedTokens = tokens("new-access", "new-refresh");
        Instant newExpiresAt = Instant.parse("2026-04-21T22:00:00Z");

        when(jwtService.validateJwtToken("old-refresh")).thenReturn(true);
        when(userSessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(oldSession));
        when(jwtService.getEmailFromToken("old-refresh")).thenReturn("client@example.com");
        when(jwtService.generateAuthToken(user)).thenReturn(expectedTokens);
        when(jwtService.getExpirationFromToken("new-refresh")).thenReturn(newExpiresAt);

        JwtAuthenticationDto tokens = userService.refreshToken(refreshToken("old-refresh"));

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession newSession = sessionCaptor.getValue();

        assertThat(tokens).isSameAs(expectedTokens);
        assertThat(oldSession.getRevokedAt()).isNotNull();
        assertThat(newSession.getUser()).isSameAs(user);
        assertThat(newSession.getRefreshTokenHash()).hasSize(64);
        assertThat(newSession.getExpiresAt()).isEqualTo(newExpiresAt);
    }

    @Test
    void logoutRevokesActiveSession() {
        User user = activeUser();
        UserSession session = activeSession(user);

        when(jwtService.validateJwtToken("refresh")).thenReturn(true);
        when(userSessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(session));

        userService.logout(refreshToken("refresh"));

        assertThat(session.getRevokedAt()).isNotNull();
    }

    @Test
    void updateRoleChangesUserRole() {
        User user = activeUser();
        UUID userId = user.getId();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User updatedUser = userService.updateRole(userId, Role.ROLE_MODERATOR);

        assertThat(updatedUser).isSameAs(user);
        assertThat(user.getRole()).isEqualTo(Role.ROLE_MODERATOR);
    }

    @Test
    void updateStatusBlocksUserAndRevokesActiveSessions() {
        User user = activeUser();
        UUID userId = user.getId();
        UserSession firstSession = activeSession(user);
        UserSession secondSession = activeSession(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(userId))
                .thenReturn(List.of(firstSession, secondSession));

        User updatedUser = userService.updateStatus(userId, UserStatus.BLOCKED);

        assertThat(updatedUser).isSameAs(user);
        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(firstSession.getRevokedAt()).isNotNull();
        assertThat(secondSession.getRevokedAt()).isNotNull();
    }

    @Test
    void shouldListUsersForAdminOrderedByEmail() {
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@tutorspace.local");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        User moderator = new User();
        moderator.setId(UUID.randomUUID());
        moderator.setEmail("moderator@tutorspace.local");
        moderator.setRole(Role.ROLE_MODERATOR);
        moderator.setStatus(UserStatus.ACTIVE);

        when(userRepository.findAllByOrderByEmailAsc()).thenReturn(List.of(admin, moderator));

        List<User> result = userService.listUsersForAdmin();

        assertThat(result)
                .extracting(User::getEmail)
                .containsExactly("admin@tutorspace.local", "moderator@tutorspace.local");
        verify(userRepository).findAllByOrderByEmailAsc();
    }

    @Test
    void getUserByIdThrowsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void passwordResetRequestForKnownAccountCreatesHashedToken() {
        User user = activeUser();
        when(userRepository.findByEmailForPasswordReset("client@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(UUID.class), any(Instant.class)
        )).thenReturn(Optional.empty());
        when(passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of());
        when(passwordResetProperties.isExposeToken()).thenReturn(true);

        PasswordResetResponseDto response = userService.requestPasswordReset(
                passwordResetRequest("client@example.com")
        );

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertThat(response.getMessage()).isEqualTo("Если аккаунт существует, инструкции по сбросу пароля созданы.");
        assertThat(response.getTestToken()).isNotBlank();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).isEqualTo(sha256(response.getTestToken()));
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getExpiresAt()).isAfter(savedToken.getCreatedAt());
        assertThat(savedToken.getUsedAt()).isNull();
        verify(userRepository).findByEmailForPasswordReset("client@example.com");
        verify(userRepository, never()).findByEmail("client@example.com");
    }

    @Test
    void passwordResetRequestForUnknownAccountReturnsNeutralResponseAndPersistsNothing() {
        when(userRepository.findByEmailForPasswordReset("missing@example.com")).thenReturn(Optional.empty());

        PasswordResetResponseDto response = userService.requestPasswordReset(
                passwordResetRequest("missing@example.com")
        );

        assertThat(response.getMessage()).isEqualTo("Если аккаунт существует, инструкции по сбросу пароля созданы.");
        assertThat(response.getTestToken()).isNull();
        verifyNoInteractions(passwordResetTokenRepository);
    }

    @Test
    void passwordResetRequestNeverStoresRawToken() {
        User user = activeUser();
        when(userRepository.findByEmailForPasswordReset("client@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(UUID.class), any(Instant.class)
        )).thenReturn(Optional.empty());
        when(passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of());
        when(passwordResetProperties.isExposeToken()).thenReturn(true);

        PasswordResetResponseDto response = userService.requestPasswordReset(
                passwordResetRequest("client@example.com")
        );

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash())
                .isNotEqualTo(response.getTestToken())
                .isEqualTo(sha256(response.getTestToken()));
    }

    @Test
    void passwordResetRequestInsideCooldownKeepsCurrentTokenAndCreatesNothing() {
        User user = activeUser();
        PasswordResetToken currentToken = activePasswordResetToken(user, "current-token");
        currentToken.setCreatedAt(Instant.now().minusSeconds(60));
        when(userRepository.findByEmailForPasswordReset("client@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(UUID.class), any(Instant.class)
        )).thenReturn(Optional.of(currentToken));

        PasswordResetResponseDto response = userService.requestPasswordReset(
                passwordResetRequest("client@example.com")
        );

        assertThat(response.getMessage()).isEqualTo("Если аккаунт существует, инструкции по сбросу пароля созданы.");
        assertThat(response.getTestToken()).isNull();
        assertThat(currentToken.getUsedAt()).isNull();
        verify(passwordResetTokenRepository).deleteUsedOrExpiredByUserId(any(UUID.class), any(Instant.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(passwordResetTokenRepository, never()).findByUserIdAndUsedAtIsNull(any(UUID.class));
    }

    @Test
    void passwordResetRequestCleansOldUsedAndExpiredTokens() {
        User user = activeUser();
        when(userRepository.findByEmailForPasswordReset("client@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(UUID.class), any(Instant.class)
        )).thenReturn(Optional.empty());
        when(passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of());

        userService.requestPasswordReset(passwordResetRequest("client@example.com"));

        verify(passwordResetTokenRepository).deleteUsedOrExpiredByUserId(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                any(Instant.class)
        );
    }

    @Test
    void passwordResetRequestAfterCooldownInvalidatesPriorTokenAndCreatesNewOne() {
        User user = activeUser();
        PasswordResetToken priorToken = activePasswordResetToken(user, "prior-token");
        priorToken.setCreatedAt(Instant.now().minusSeconds(180));
        when(userRepository.findByEmailForPasswordReset("client@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(UUID.class), any(Instant.class)
        )).thenReturn(Optional.of(priorToken));
        when(passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId()))
                .thenReturn(List.of(priorToken));

        PasswordResetResponseDto response = userService.requestPasswordReset(
                passwordResetRequest("client@example.com")
        );

        assertThat(response.getMessage()).isEqualTo("Если аккаунт существует, инструкции по сбросу пароля созданы.");
        assertThat(priorToken.getUsedAt()).isNotNull();
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void passwordResetConfirmEncodesPasswordMarksTokenUsedAndRevokesSessions() {
        User user = activeUser();
        PasswordResetToken resetToken = activePasswordResetToken(user, "raw-reset-token");
        UserSession firstSession = activeSession(user);
        UserSession secondSession = activeSession(user);
        when(passwordResetTokenRepository.findForUpdateByTokenHash(sha256("raw-reset-token")))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
                .thenReturn(List.of(firstSession, secondSession));

        PasswordResetResponseDto response = userService.confirmPasswordReset(
                passwordResetConfirm("raw-reset-token", "new-password")
        );

        assertThat(response.getMessage()).isEqualTo("Пароль успешно обновлен.");
        assertThat(response.getTestToken()).isNull();
        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(resetToken.getUsedAt()).isNotNull();
        assertThat(firstSession.getRevokedAt()).isNotNull();
        assertThat(secondSession.getRevokedAt()).isNotNull();
        verify(passwordEncoder).encode("new-password");
        verify(passwordResetTokenRepository).findForUpdateByTokenHash(sha256("raw-reset-token"));
    }

    @Test
    void passwordResetConfirmRejectsExpiredToken() {
        User user = activeUser();
        PasswordResetToken resetToken = activePasswordResetToken(user, "expired-token");
        resetToken.setExpiresAt(Instant.now().minusSeconds(1));
        when(passwordResetTokenRepository.findForUpdateByTokenHash(sha256("expired-token")))
                .thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> userService.confirmPasswordReset(
                passwordResetConfirm("expired-token", "new-password")
        ))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessage("Ссылка для сброса пароля устарела или недействительна.");

        verify(passwordEncoder, never()).encode(anyString());
        verifyNoInteractions(userSessionRepository);
    }

    @Test
    void passwordResetConfirmRejectsUsedToken() {
        User user = activeUser();
        PasswordResetToken resetToken = activePasswordResetToken(user, "used-token");
        resetToken.setUsedAt(Instant.now().minusSeconds(1));
        when(passwordResetTokenRepository.findForUpdateByTokenHash(sha256("used-token")))
                .thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> userService.confirmPasswordReset(
                passwordResetConfirm("used-token", "new-password")
        ))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessage("Ссылка для сброса пароля устарела или недействительна.");

        verify(passwordEncoder, never()).encode(anyString());
        verifyNoInteractions(userSessionRepository);
    }

    private RegisterRequestDto registerRequest(Role role) {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setEmail("client@example.com");
        dto.setPassword("password");
        dto.setName("Client User");
        dto.setRole(role);
        return dto;
    }

    private RefreshTokenDto refreshToken(String token) {
        RefreshTokenDto dto = new RefreshTokenDto();
        dto.setRefreshToken(token);
        return dto;
    }

    private PasswordResetRequestDto passwordResetRequest(String email) {
        PasswordResetRequestDto dto = new PasswordResetRequestDto();
        dto.setEmail(email);
        return dto;
    }

    private PasswordResetConfirmDto passwordResetConfirm(String token, String password) {
        PasswordResetConfirmDto dto = new PasswordResetConfirmDto();
        dto.setToken(token);
        dto.setPassword(password);
        return dto;
    }

    private JwtAuthenticationDto tokens(String accessToken, String refreshToken) {
        JwtAuthenticationDto dto = new JwtAuthenticationDto();
        dto.setToken(accessToken);
        dto.setRefreshToken(refreshToken);
        return dto;
    }

    private User activeUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("client@example.com");
        user.setName("Client User");
        user.setPassword("hashed-password");
        user.setRole(Role.ROLE_CLIENT);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private UserSession activeSession(User user) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash("old-hash");
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        return session;
    }

    private PasswordResetToken activePasswordResetToken(User user, String rawToken) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(rawToken));
        token.setCreatedAt(Instant.now().minusSeconds(60));
        token.setExpiresAt(Instant.now().plusSeconds(1800));
        return token;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
