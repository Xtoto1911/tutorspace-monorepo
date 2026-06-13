package com.tutor.authservice.config;

import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.domain.UserStatus;
import com.tutor.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminSeedProperties.class)
public class AdminSeedConfig {

    @Bean
    CommandLineRunner adminSeedRunner(AdminSeedProperties properties,
                                      UserRepository userRepository,
                                      JdbcTemplate jdbcTemplate,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            if (!properties.enabled()) {
                return;
            }

            createUserIfMissing(
                    userRepository,
                    jdbcTemplate,
                    passwordEncoder,
                    null,
                    properties.email(),
                    "Администратор",
                    properties.password(),
                    Role.ROLE_ADMIN
            );

            List<SeedUser> tutors = List.of(
                    new SeedUser(UUID.fromString("11111111-1111-1111-1111-111111111111"), "anna@tutorspace.local", "Анна", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("22222222-2222-2222-2222-222222222222"), "ivan@tutorspace.local", "Иван", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("33333333-3333-3333-3333-333333333333"), "maria@tutorspace.local", "Мария", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("44444444-4444-4444-4444-444444444444"), "pavel@tutorspace.local", "Павел", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("55555555-5555-5555-5555-555555555555"), "irina@tutorspace.local", "Ирина", "tutor12345", Role.ROLE_TUTOR)
            );

            tutors.forEach(seed -> createUserIfMissing(
                    userRepository,
                    jdbcTemplate,
                    passwordEncoder,
                    seed.id(),
                    seed.email(),
                    seed.name(),
                    seed.password(),
                    seed.role()
            ));
        };
    }

    private void createUserIfMissing(UserRepository userRepository,
                                     JdbcTemplate jdbcTemplate,
                                     PasswordEncoder passwordEncoder,
                                     UUID id,
                                     String email,
                                     String name,
                                     String password,
                                     Role role) {
        var existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getName() == null) {
                user.setName(name);
                userRepository.save(user);
            }
            return;
        }

        String encodedPassword = passwordEncoder.encode(password);

        if (id != null) {
            jdbcTemplate.update(
                    "insert into users (id, email, name, password, role, status) values (?, ?, ?, ?, ?, ?)",
                    id,
                    email,
                    name,
                    encodedPassword,
                    role.name(),
                    UserStatus.ACTIVE.name()
            );
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    private record SeedUser(UUID id, String email, String name, String password, Role role) {
    }
}
