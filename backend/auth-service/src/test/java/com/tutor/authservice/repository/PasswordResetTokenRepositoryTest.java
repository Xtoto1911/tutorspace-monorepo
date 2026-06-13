package com.tutor.authservice.repository;

import com.tutor.authservice.domain.PasswordResetToken;
import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.domain.UserStatus;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void findsTokenWithPessimisticWriteLockInsideTransaction() throws Exception {
        User user = new User();
        user.setEmail("locking@example.com");
        user.setName("Locking User");
        user.setPassword("encoded-password");
        user.setRole(Role.ROLE_CLIENT);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash("a".repeat(64));
        token.setExpiresAt(Instant.now().plusSeconds(1800));
        passwordResetTokenRepository.saveAndFlush(token);

        PasswordResetToken found = passwordResetTokenRepository
                .findForUpdateByTokenHash(token.getTokenHash())
                .orElseThrow();

        Method method = PasswordResetTokenRepository.class.getMethod(
                "findForUpdateByTokenHash",
                String.class
        );
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(found.getId()).isEqualTo(token.getId());
    }
}
