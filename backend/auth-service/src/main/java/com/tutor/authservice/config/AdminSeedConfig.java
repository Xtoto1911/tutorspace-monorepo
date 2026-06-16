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
                    new SeedUser(UUID.fromString("55555555-5555-5555-5555-555555555555"), "irina@tutorspace.local", "Ирина", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("66666666-6666-6666-6666-666666666666"), "olga@tutorspace.local", "Ольга", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("77777777-7777-7777-7777-777777777777"), "sergey@tutorspace.local", "Сергей", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("88888888-8888-8888-8888-888888888888"), "elena@tutorspace.local", "Елена", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("99999999-9999-9999-9999-999999999999"), "dmitry@tutorspace.local", "Дмитрий", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "natalia@tutorspace.local", "Наталья", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01"), "andrey@tutorspace.local", "Андрей", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02"), "ksenia@tutorspace.local", "Ксения", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03"), "mikhail@tutorspace.local", "Михаил", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04"), "tatiana@tutorspace.local", "Татьяна", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05"), "alexey@tutorspace.local", "Алексей", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06"), "vera@tutorspace.local", "Вера", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07"), "boris@tutorspace.local", "Борис", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08"), "polina@tutorspace.local", "Полина", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09"), "victor@tutorspace.local", "Виктор", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0a"), "lidia@tutorspace.local", "Лидия", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0b"), "artem@tutorspace.local", "Артём", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0c"), "svetlana@tutorspace.local", "Светлана", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0d"), "kirill@tutorspace.local", "Кирилл", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0e"), "julia@tutorspace.local", "Юлия", "tutor12345", Role.ROLE_TUTOR),
                    new SeedUser(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0f"), "maxim@tutorspace.local", "Максим", "tutor12345", Role.ROLE_TUTOR)
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

            List<SeedUser> clients = List.of(
                    new SeedUser(null, "client01@tutorspace.local", "Алексей Соколов", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client02@tutorspace.local", "Мария Кузнецова", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client03@tutorspace.local", "Дмитрий Орлов", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client04@tutorspace.local", "Екатерина Никитина", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client05@tutorspace.local", "Иван Морозов", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client06@tutorspace.local", "Ольга Васильева", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client07@tutorspace.local", "Сергей Лебедев", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client08@tutorspace.local", "Елена Соколова", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client09@tutorspace.local", "Андрей Козлов", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client10@tutorspace.local", "Ксения Новикова", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client11@tutorspace.local", "Михаил Морозов", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client12@tutorspace.local", "Татьяна Волкова", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client13@tutorspace.local", "Алексей Соловьёв", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client14@tutorspace.local", "Вера Лебедева", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client15@tutorspace.local", "Борис Козлов", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client16@tutorspace.local", "Полина Новикова", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client17@tutorspace.local", "Виктор Соколов", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client18@tutorspace.local", "Лидия Морозова", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client19@tutorspace.local", "Артём Волков", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client20@tutorspace.local", "Светлана Лебедева", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client21@tutorspace.local", "Кирилл Козлов", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client22@tutorspace.local", "Юлия Соловьёва", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client23@tutorspace.local", "Максим Никитин", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client24@tutorspace.local", "Анна Орлова", "client12345", Role.ROLE_CLIENT),
                    new SeedUser(null, "client25@tutorspace.local", "Павел Кузнецов", "client12345", Role.ROLE_CLIENT)
            );

            clients.forEach(seed -> createUserIfMissing(
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
