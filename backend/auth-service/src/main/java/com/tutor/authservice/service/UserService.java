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
import com.tutor.authservice.dto.UserCredentialsDto;
import com.tutor.authservice.exception.EmailAlreadyExistsException;
import com.tutor.authservice.exception.InvalidPasswordResetTokenException;
import com.tutor.authservice.exception.InvalidRoleRegistrationException;
import com.tutor.authservice.exception.UserNotFoundException;
import com.tutor.authservice.repository.PasswordResetTokenRepository;
import com.tutor.authservice.repository.UserRepository;
import com.tutor.authservice.repository.UserSessionRepository;
import com.tutor.authservice.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration PASSWORD_RESET_COOLDOWN = Duration.ofMinutes(2);

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetProperties passwordResetProperties;


    @Transactional
    public JwtAuthenticationDto register(RegisterRequestDto dto) {
        if (dto.getRole() == null || !dto.getRole().canRegisterPublicly()) {
            throw new InvalidRoleRegistrationException(dto.getRole());
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException(dto.getEmail());
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(normalizeName(dto.getName(), dto.getEmail()));
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
        log.info("User registered : {}", dto.getEmail());

        return issueSessionTokens(user);
    }

    @Transactional
    public User createModeratorAccount(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setName(normalizeName(name, email));
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.ROLE_MODERATOR);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
        log.info("Moderator account created: {}", email);

        return user;
    }

    @Transactional
    public JwtAuthenticationDto login(UserCredentialsDto dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            JwtAuthenticationDto tokens = issueSessionTokens(user);

            log.info("User logged in: {}", dto.getEmail());

            return tokens;
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Неверный email или пароль.");
        }
    }

    @Transactional
    public JwtAuthenticationDto refreshToken(RefreshTokenDto dto) {
        String refreshToken = dto.getRefreshToken();
        validateRefreshToken(refreshToken);

        UserSession session = findActiveSession(refreshToken);
        User user = session.getUser();
        String email = jwtService.getEmailFromToken(refreshToken);

        if (!email.equals(user.getEmail())) {
            throw new BadCredentialsException("Неверный владелец токена.");
        }

        if (!user.isAccountNonLocked() || !user.isEnabled()) {
            throw new LockedException("Аккаунт заблокирован. Обратитесь в поддержку.");
        }

        session.revoke(Instant.now());

        return issueSessionTokens(user);
    }

    @Transactional
    public void logout(RefreshTokenDto dto) {
        String refreshToken = dto.getRefreshToken();
        validateRefreshToken(refreshToken);

        UserSession session = findActiveSession(refreshToken);
        session.revoke(Instant.now());
    }

    @Transactional
    public PasswordResetResponseDto requestPasswordReset(PasswordResetRequestDto dto) {
        String message = "Если аккаунт существует, инструкции по сбросу пароля созданы.";
        User user = userRepository.findByEmailForPasswordReset(dto.getEmail()).orElse(null);

        if (user == null) {
            return new PasswordResetResponseDto(message, null);
        }

        Instant now = Instant.now();
        passwordResetTokenRepository.deleteUsedOrExpiredByUserId(user.getId(), now);

        Optional<PasswordResetToken> latestActiveToken =
                passwordResetTokenRepository
                        .findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                                user.getId(),
                                now
                        );
        if (latestActiveToken
                .filter(token -> !token.getCreatedAt().isBefore(now.minus(PASSWORD_RESET_COOLDOWN)))
                .isPresent()) {
            return new PasswordResetResponseDto(message, null);
        }

        passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())
                .forEach(token -> token.setUsedAt(now));

        String rawToken = generatePasswordResetToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plus(PASSWORD_RESET_TOKEN_TTL));
        passwordResetTokenRepository.save(resetToken);

        String exposedToken = passwordResetProperties.isExposeToken() ? rawToken : null;
        return new PasswordResetResponseDto(message, exposedToken);
    }

    @Transactional
    public PasswordResetResponseDto confirmPasswordReset(PasswordResetConfirmDto dto) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findForUpdateByTokenHash(hashToken(dto.getToken()))
                .orElseThrow(InvalidPasswordResetTokenException::new);

        if (!resetToken.isActive(now)) {
            throw new InvalidPasswordResetTokenException();
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        resetToken.setUsedAt(now);
        revokeActiveSessions(user, now);

        return new PasswordResetResponseDto("Пароль успешно обновлен.", null);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return findUserById(userId);
    }

    @Transactional(readOnly = true)
    public List<User> listUsersForAdmin() {
        return userRepository.findAllByOrderByEmailAsc();
    }

    @Transactional
    public User updateRole(UUID userId, Role role) {
        User user = findUserById(userId);
        user.setRole(role);
        log.info("User role updated: userId={}, role={}", userId, role);
        return user;
    }

    @Transactional
    public User updateStatus(UUID userId, UserStatus status) {
        User user = findUserById(userId);
        user.setStatus(status);

        if (UserStatus.BLOCKED.equals(status)) {
            revokeActiveSessions(user, Instant.now());
        }

        log.info("User status updated: userId={}, status={}", userId, status);
        return user;
    }

    private JwtAuthenticationDto issueSessionTokens(User user) {
        JwtAuthenticationDto tokens = jwtService.generateAuthToken(user);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(hashRefreshToken(tokens.getRefreshToken()));
        session.setExpiresAt(jwtService.getExpirationFromToken(tokens.getRefreshToken()));
        userSessionRepository.save(session);

        return tokens;
    }

    private void validateRefreshToken(String refreshToken) {
        if (!jwtService.validateJwtToken(refreshToken)) {
            throw new BadCredentialsException("Недействительный refresh-токен. Войдите заново.");
        }
    }

    private UserSession findActiveSession(String refreshToken) {
        UserSession session = userSessionRepository.findByRefreshTokenHash(hashRefreshToken(refreshToken))
                .orElseThrow(() -> new BadCredentialsException("Недействительный refresh-токен. Войдите заново."));

        if (!session.isActive(Instant.now())) {
            throw new BadCredentialsException("Недействительный refresh-токен. Войдите заново.");
        }

        return session;
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void revokeActiveSessions(User user, Instant now) {
        userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())
                .forEach(session -> session.revoke(now));
    }

    private String hashRefreshToken(String refreshToken) {
        return hashToken(refreshToken);
    }

    private String generatePasswordResetToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeName(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (email == null || email.isBlank()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
