package com.tutor.authservice.repository;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void findsPasswordResetUserWithPessimisticWriteLockInsideTransaction() throws Exception {
        User user = new User();
        user.setEmail("reset-lock@example.com");
        user.setName("Reset Lock User");
        user.setPassword("encoded-password");
        user.setRole(Role.ROLE_CLIENT);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        User found = userRepository.findByEmailForPasswordReset(user.getEmail()).orElseThrow();

        Method method = UserRepository.class.getMethod(
                "findByEmailForPasswordReset",
                String.class
        );
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(found.getId()).isEqualTo(user.getId());
    }
}
