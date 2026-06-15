# Tutor Name Propagation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a tutor renames themselves in `/app/tutor/profile`, the new name appears everywhere — workspace header chip, every existing application, every existing lesson, every existing message thread — within seconds.

**Architecture:** `auth-service` becomes the single source of truth for `User.name`. It exposes `PATCH /api/auth/me` and publishes a `UserProfileChanged` Kafka event. `crm-service` consumes the event and updates the three denormalised snapshot columns (`crm_applications.tutor_name`, `lessons.tutor_name`, `message_threads.participant_name`). The frontend updates its local session after a successful rename and dispatches a window event so the workspace header refreshes immediately.

**Tech Stack:** Spring Boot 3.5.10, Java 21, Spring Kafka, JPA/Hibernate, React 18 + Vite + TypeScript, Vitest.

**Working directory:** repo root. All paths are relative to the repo root unless prefixed with `backend/` or `frontend/`.

**Spec:** `docs/superpowers/specs/2026-06-15-tutor-name-propagation-design.md`

---

## File map

### `backend/auth-service` (modify)

- `pom.xml` — already has `spring-kafka` and `spring-kafka-test`; no change.
- `src/main/resources/application.yaml` — add `spring.kafka.*` producer block.
- `src/main/java/com/tutor/authservice/dto/UpdateProfileNameRequest.java` (new).
- `src/main/java/com/tutor/authservice/dto/UserPublicDto.java` (new).
- `src/main/java/com/tutor/authservice/kafka/UserProfileChangedEvent.java` (new).
- `src/main/java/com/tutor/authservice/kafka/UserProfileEventPublisher.java` (new).
- `src/main/java/com/tutor/authservice/service/UserService.java` — add `updateProfileName`.
- `src/main/java/com/tutor/authservice/controller/AuthController.java` — add `PATCH /me`.

### `backend/auth-service` (tests)

- `src/test/java/com/tutor/authservice/service/UserServiceTest.java` — add cases for `updateProfileName`.
- `src/test/java/com/tutor/authservice/controller/AuthControllerTest.java` (new) — `PATCH /me` happy path + 400 + 401.
- `src/test/java/com/tutor/authservice/kafka/UserProfileEventPublisherTest.java` (new).

### `backend/crm-service` (modify)

- `src/main/resources/application.yaml` — register a second `ConcurrentKafkaListenerContainerFactory` for the new topic and a `DefaultErrorHandler` with DLT.
- `src/main/java/com/tutor/crmservice/request/CrmApplicationRepository.java` — add `updateTutorNameByTutorUserId`.
- `src/main/java/com/tutor/crmservice/lesson/LessonRepository.java` — add `updateTutorNameByTutorUserId`.
- `src/main/java/com/tutor/crmservice/message/MessageThreadRepository.java` — add `updateParticipantNameByTutorUserId`.
- `src/main/java/com/tutor/crmservice/kafka/UserProfileChangedEvent.java` (new, crm local copy).
- `src/main/java/com/tutor/crmservice/kafka/UserProfileChangedListener.java` (new).
- `src/main/java/com/tutor/crmservice/kafka/KafkaErrorHandlingConfig.java` (new).

### `backend/crm-service` (tests)

- `src/test/java/com/tutor/crmservice/kafka/UserProfileChangedListenerTest.java` (new).
- `src/test/java/com/tutor/crmservice/kafka/TutorNamePropagationServiceTest.java` (new).

### `frontend/src` (modify)

- `shared/api/auth.ts` (new).
- `shared/auth/session.ts` — add `updateSessionUserName`.
- `components/layout-helpers.ts` — add `profileChangedEvent`.
- `pages/app/TutorWorkspaceProfilePage.tsx` — call auth first, then CRM; emit event.
- `components/layout.tsx` — listen for the event and re-derive `workspaceUser`.

### `frontend/src` (tests)

- `shared/api/auth.test.ts` (new).
- `shared/auth/session.test.ts` (new) — for `updateSessionUserName`.
- `components/layout-helpers.test.ts` — assert the new `profileChangedEvent` constant.

---

## Phase 1 — auth-service

### Task 1: Add Kafka producer config to `auth-service`

**Files:**
- Modify: `backend/auth-service/src/main/resources/application.yaml`

- [ ] **Step 1: Add the `spring.kafka` block to `application.yaml`**

Append (after the existing `spring:` keys, before the `jwt:` block):

```yaml
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
```

Note: `add.type.headers: false` is intentional — the consumer (crm-service) cannot resolve `com.tutor.authservice.kafka.UserProfileChangedEvent` because auth-service is not on its classpath. The consumer will be configured with a `default-type` pointing at its own local copy.

- [ ] **Step 2: Verify the service still starts**

```bash
cd backend/auth-service
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS` (no compilation errors, no missing bean).

- [ ] **Step 3: Commit**

```bash
git add backend/auth-service/src/main/resources/application.yaml
git commit -m "chore(auth): add spring.kafka producer config for user-profile-events"
```

---

### Task 2: Create the `UserProfileChangedEvent` record

**Files:**
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/kafka/UserProfileChangedEvent.java`

- [ ] **Step 1: Create the record**

```java
package com.tutor.authservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record UserProfileChangedEvent(
        UUID userId,
        String name,
        Instant changedAt
) {
    public static final String TOPIC = "user-profile-events";
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd backend/auth-service
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add backend/auth-service/src/main/java/com/tutor/authservice/kafka/UserProfileChangedEvent.java
git commit -m "feat(auth): add UserProfileChangedEvent kafka record"
```

---

### Task 3: Create the `UserProfileEventPublisher`

**Files:**
- Create: `backend/auth-service/src/test/java/com/tutor/authservice/kafka/UserProfileEventPublisherTest.java`
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/kafka/UserProfileEventPublisher.java`

- [ ] **Step 1: Write the failing test**

`backend/auth-service/src/test/java/com/tutor/authservice/kafka/UserProfileEventPublisherTest.java`:

```java
package com.tutor.authservice.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserProfileEventPublisherTest {

    @Mock
    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, UserProfileChangedEvent> kafkaTemplate;

    @InjectMocks
    private UserProfileEventPublisher publisher;

    @Test
    void publishesEventToUserProfileEventsTopicKeyedByUserId() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-15T10:00:00Z");

        publisher.publish(userId, "Пётр", now);

        ArgumentCaptor<UserProfileChangedEvent> captor = ArgumentCaptor.forClass(UserProfileChangedEvent.class);
        verify(kafkaTemplate).send(eq(UserProfileChangedEvent.TOPIC), eq(userId.toString()), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().name()).isEqualTo("Пётр");
        assertThat(captor.getValue().changedAt()).isEqualTo(now);
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

```bash
cd backend/auth-service
mvn -q -Dtest=UserProfileEventPublisherTest test
```

Expected: `BUILD FAILURE` (or compilation error for missing class `UserProfileEventPublisher`).

- [ ] **Step 3: Implement the publisher**

`backend/auth-service/src/main/java/com/tutor/authservice/kafka/UserProfileEventPublisher.java`:

```java
package com.tutor.authservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileEventPublisher {

    private final KafkaTemplate<String, UserProfileChangedEvent> kafkaTemplate;

    public void publish(UUID userId, String name, Instant changedAt) {
        UserProfileChangedEvent event = new UserProfileChangedEvent(userId, name, changedAt);
        try {
            kafkaTemplate.send(UserProfileChangedEvent.TOPIC, userId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish UserProfileChanged for user {}: {}", userId, ex.getMessage());
                        } else {
                            log.info("Published UserProfileChanged for user {}", userId);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing UserProfileChanged for user {}: {}", userId, e.getMessage());
        }
    }
}
```

- [ ] **Step 4: Run the test and verify it passes**

```bash
cd backend/auth-service
mvn -q -Dtest=UserProfileEventPublisherTest test
```

Expected: `BUILD SUCCESS`, 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add backend/auth-service/src/main/java/com/tutor/authservice/kafka/UserProfileEventPublisher.java \
        backend/auth-service/src/test/java/com/tutor/authservice/kafka/UserProfileEventPublisherTest.java
git commit -m "feat(auth): add UserProfileEventPublisher for UserProfileChanged"
```

---

### Task 4: Add the DTOs

**Files:**
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/dto/UpdateProfileNameRequest.java`
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/dto/UserPublicDto.java`

- [ ] **Step 1: Create `UpdateProfileNameRequest`**

`backend/auth-service/src/main/java/com/tutor/authservice/dto/UpdateProfileNameRequest.java`:

```java
package com.tutor.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileNameRequest(
        @NotBlank
        @Size(min = 2, max = 100, message = "ФИО должно содержать от 2 до 100 символов")
        String name
) {}
```

- [ ] **Step 2: Create `UserPublicDto`**

`backend/auth-service/src/main/java/com/tutor/authservice/dto/UserPublicDto.java`:

```java
package com.tutor.authservice.dto;

import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.domain.UserStatus;

import java.util.UUID;

public record UserPublicDto(
        UUID id,
        String email,
        String name,
        Role role,
        UserStatus status
) {
    public static UserPublicDto from(User user) {
        return new UserPublicDto(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getStatus());
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd backend/auth-service
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add backend/auth-service/src/main/java/com/tutor/authservice/dto/UpdateProfileNameRequest.java \
        backend/auth-service/src/main/java/com/tutor/authservice/dto/UserPublicDto.java
git commit -m "feat(auth): add UpdateProfileNameRequest and UserPublicDto"
```

---

### Task 5: Add `UserService.updateProfileName` (TDD)

**Files:**
- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/service/UserService.java`
- Modify: `backend/auth-service/src/test/java/com/tutor/authservice/service/UserServiceTest.java`

- [ ] **Step 1: Add the failing test to `UserServiceTest`**

Open `backend/auth-service/src/test/java/com/tutor/authservice/service/UserServiceTest.java` and:

1. Add imports:
```java
import com.tutor.authservice.kafka.UserProfileEventPublisher;
import org.mockito.ArgumentMatchers;
```

2. Add a new `@Mock` next to the existing mocks (search for `@Mock` to find the right place):
```java
@Mock
private UserProfileEventPublisher userProfileEventPublisher;
```

3. Append the new tests at the end of the class (before the closing brace):

```java
    @Test
    void updateProfileNamePersistsNormalizedNameAndPublishesEvent() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("tutor@example.com");
        user.setName("  Пётр   Сергеев ");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.updateProfileName(userId, "  Пётр   Сергеев ");

        assertThat(result.getName()).isEqualTo("Пётр Сергеев");
        verify(userRepository).save(user);
        verify(userProfileEventPublisher).publish(eq(userId), eq("Пётр Сергеев"), any(Instant.class));
    }

    @Test
    void updateProfileNameRejectsBlankValue() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfileName(userId, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ФИО");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(userProfileEventPublisher);
    }

    @Test
    void updateProfileNameRejectsTooLongValue() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        String longName = "А".repeat(101);

        assertThatThrownBy(() -> userService.updateProfileName(userId, longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(userProfileEventPublisher);
    }

    @Test
    void updateProfileNameThrowsWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfileName(userId, "Пётр"))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(userProfileEventPublisher);
    }
```

Also add the imports for `eq`, `any`, `never`, `verifyNoInteractions` — they may already be there. Confirm with the file's current imports.

- [ ] **Step 2: Run the new tests and verify they fail**

```bash
cd backend/auth-service
mvn -q -Dtest=UserServiceTest test
```

Expected: `BUILD FAILURE` (compilation error for `updateProfileName` not existing).

- [ ] **Step 3: Implement `updateProfileName` in `UserService`**

Open `backend/auth-service/src/main/java/com/tutor/authservice/service/UserService.java`.

1. Add the publisher field and constructor injection. Replace the existing field block:

```java
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetProperties passwordResetProperties;
```

with:

```java
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetProperties passwordResetProperties;
    private final com.tutor.authservice.kafka.UserProfileEventPublisher userProfileEventPublisher;
```

(`@RequiredArgsConstructor` already injects all final fields — no constructor change needed.)

2. Add a constant near the top of the class (just below the existing `Duration` constants):

```java
    private static final int PROFILE_NAME_MIN = 2;
    private static final int PROFILE_NAME_MAX = 100;
```

3. Add the method at the end of the class:

```java
    @Transactional
    public User updateProfileName(UUID userId, String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("ФИО обязательно для заполнения.");
        }
        String normalized = normalizeProfileName(rawName);
        if (normalized.length() < PROFILE_NAME_MIN || normalized.length() > PROFILE_NAME_MAX) {
            throw new IllegalArgumentException(
                    "ФИО должно содержать от " + PROFILE_NAME_MIN + " до " + PROFILE_NAME_MAX + " символов.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setName(normalized);
        User saved = userRepository.save(user);
        log.info("Updated profile name for user {}", userId);
        userProfileEventPublisher.publish(userId, normalized, Instant.now());
        return saved;
    }

    private String normalizeProfileName(String raw) {
        return raw.trim().replaceAll("\\s+", " ");
    }
```

- [ ] **Step 4: Run the tests and verify they pass**

```bash
cd backend/auth-service
mvn -q -Dtest=UserServiceTest test
```

Expected: `BUILD SUCCESS`, all `UserServiceTest` cases pass (the existing 30+ tests plus the 4 new ones).

- [ ] **Step 5: Commit**

```bash
git add backend/auth-service/src/main/java/com/tutor/authservice/service/UserService.java \
        backend/auth-service/src/test/java/com/tutor/authservice/service/UserServiceTest.java
git commit -m "feat(auth): add UserService.updateProfileName with event publish"
```

---

### Task 6: Add the `PATCH /me` endpoint to `AuthController`

**Files:**
- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/controller/AuthController.java`
- Create: `backend/auth-service/src/test/java/com/tutor/authservice/controller/AuthControllerTest.java`

- [ ] **Step 1: Write the failing test**

`backend/auth-service/src/test/java/com/tutor/authservice/controller/AuthControllerTest.java`:

```java
package com.tutor.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.domain.UserStatus;
import com.tutor.authservice.dto.UpdateProfileNameRequest;
import com.tutor.authservice.dto.UserPublicDto;
import com.tutor.authservice.exception.UserNotFoundException;
import com.tutor.authservice.security.JwtPrincipal;
import com.tutor.authservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

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

        authenticateAs(userId);
        try {
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
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void patchMeRejectsInvalidNameWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticateAs(userId);
        try {
            mockMvc.perform(patch("/api/auth/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"А\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        } finally {
            SecurityContextHolder.clearContext();
        }
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

    private void authenticateAs(UUID userId) {
        JwtPrincipal principal = new JwtPrincipal(userId, "tutor@example.com", Role.ROLE_TUTOR);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
```

- [ ] **Step 2: Add the test dependencies to `pom.xml` (only if not already there)**

Check `backend/auth-service/pom.xml` for `spring-security-test`. If absent, add to `<dependencies>`:

```xml
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependencies>
```

- [ ] **Step 3: Run the test and verify it fails**

```bash
cd backend/auth-service
mvn -q -Dtest=AuthControllerTest test
```

Expected: `BUILD FAILURE` (no `PATCH /me` endpoint, missing bean wiring).

- [ ] **Step 4: Inspect `SecurityConfig` to confirm the new route is protected**

Open `backend/auth-service/src/main/java/com/tutor/authservice/config/SecurityConfig.java`. Confirm that the existing matcher list covers `/api/auth/**` and the new `PATCH /me` will be authenticated. (The default for any unspecified route is "authenticated".) No change needed if the existing rules already require authentication for `/api/auth/me`.

If the `permitAll()` list includes `/api/auth/**`, add `/api/auth/me` to the authenticated list explicitly. Match the pattern already used in the file for the refresh/logout routes.

- [ ] **Step 5: Add the endpoint to `AuthController`**

Open `backend/auth-service/src/main/java/com/tutor/authservice/controller/AuthController.java`.

1. Add imports:
```java
import com.tutor.authservice.dto.UpdateProfileNameRequest;
import com.tutor.authservice.dto.UserPublicDto;
import com.tutor.authservice.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
```

2. Add the method at the end of the class:

```java
    @PatchMapping("/me")
    public UserPublicDto updateMe(@AuthenticationPrincipal JwtPrincipal principal,
                                   @Valid @RequestBody UpdateProfileNameRequest request) {
        return UserPublicDto.from(userService.updateProfileName(principal.userId(), request.name()));
    }
```

- [ ] **Step 6: Run the test and verify it passes**

```bash
cd backend/auth-service
mvn -q -Dtest=AuthControllerTest test
```

Expected: `BUILD SUCCESS`, 3 tests passed.

If the test fails on `SecurityContextHolder` setup, it means `JwtPrincipal` is not on the classpath for tests — check the import path `com.tutor.authservice.security.JwtPrincipal` and confirm it exists in `backend/auth-service/src/main/java/com/tutor/authservice/security/JwtPrincipal.java`.

- [ ] **Step 7: Run the whole auth-service test suite**

```bash
cd backend/auth-service
mvn -q test
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 8: Commit**

```bash
git add backend/auth-service/pom.xml \
        backend/auth-service/src/main/java/com/tutor/authservice/controller/AuthController.java \
        backend/auth-service/src/main/java/com/tutor/authservice/config/SecurityConfig.java \
        backend/auth-service/src/test/java/com/tutor/authservice/controller/AuthControllerTest.java
git commit -m "feat(auth): expose PATCH /api/auth/me for tutor name changes"
```

---

## Phase 2 — crm-service

### Task 7: Add the `updateTutorNameByTutorUserId` repository method to `CrmApplicationRepository`

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/request/CrmApplicationRepository.java`

- [ ] **Step 1: Add the modifying query method**

Open the file and add new imports:

```java
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

Add a new method inside the interface:

```java
    @Modifying
    @Query("UPDATE CrmApplication a SET a.tutorName = :newName WHERE a.tutorUserId = :userId AND a.tutorName <> :newName")
    int updateTutorNameByTutorUserId(@Param("userId") java.util.UUID userId, @Param("newName") String newName);
```

- [ ] **Step 2: Verify compilation**

```bash
cd backend/crm-service
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/request/CrmApplicationRepository.java
git commit -m "feat(crm): add bulk update tutor name on CrmApplicationRepository"
```

---

### Task 8: Add the equivalent method to `LessonRepository` and `MessageThreadRepository`

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRepository.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/message/MessageThreadRepository.java`

- [ ] **Step 1: Update `LessonRepository`**

Open the file. Add the new method inside the interface:

```java
    @Modifying
    @Query("UPDATE Lesson l SET l.tutorName = :newName WHERE l.tutorUserId = :userId AND l.tutorName <> :newName")
    int updateTutorNameByTutorUserId(@Param("userId") java.util.UUID userId, @Param("newName") String newName);
```

`@Modifying` and `@Param` are already imported in this file (used by `findByIdForUpdate`).

- [ ] **Step 2: Update `MessageThreadRepository`**

Open the file. Add new imports:

```java
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

Add a new method inside the interface:

```java
    @Modifying
    @Query("UPDATE MessageThread t SET t.participantName = :newName WHERE t.tutorUserId = :userId AND t.participantName <> :newName")
    int updateParticipantNameByTutorUserId(@Param("userId") java.util.UUID userId, @Param("newName") String newName);
```

- [ ] **Step 3: Verify compilation**

```bash
cd backend/crm-service
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRepository.java \
        backend/crm-service/src/main/java/com/tutor/crmservice/message/MessageThreadRepository.java
git commit -m "feat(crm): add bulk update tutor name on Lesson and MessageThread repositories"
```

---

### Task 9: Create the `TutorNamePropagationService` (TDD)

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/TutorNamePropagationService.java`
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/kafka/TutorNamePropagationServiceTest.java`

- [ ] **Step 1: Write the failing test**

`backend/crm-service/src/test/java/com/tutor/crmservice/kafka/TutorNamePropagationServiceTest.java`:

```java
package com.tutor.crmservice.kafka;

import com.tutor.crmservice.lesson.LessonRepository;
import com.tutor.crmservice.message.MessageThreadRepository;
import com.tutor.crmservice.request.CrmApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorNamePropagationServiceTest {

    @Mock private CrmApplicationRepository crmApplicationRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private MessageThreadRepository messageThreadRepository;

    @InjectMocks
    private TutorNamePropagationService service;

    @Test
    void propagateTutorNameUpdatesAllThreeTablesForUser() {
        UUID userId = UUID.randomUUID();
        when(crmApplicationRepository.updateTutorNameByTutorUserId(userId, "Пётр")).thenReturn(3);
        when(lessonRepository.updateTutorNameByTutorUserId(userId, "Пётр")).thenReturn(2);
        when(messageThreadRepository.updateParticipantNameByTutorUserId(userId, "Пётр")).thenReturn(1);

        service.propagateTutorName(userId, "Пётр");

        verify(crmApplicationRepository).updateTutorNameByTutorUserId(userId, "Пётр");
        verify(lessonRepository).updateTutorNameByTutorUserId(userId, "Пётр");
        verify(messageThreadRepository).updateParticipantNameByTutorUserId(userId, "Пётр");
    }

    @Test
    void propagateTutorNameSkipsRepositoriesWhenNameIsNull() {
        UUID userId = UUID.randomUUID();
        service.propagateTutorName(userId, null);

        verify(crmApplicationRepository, never()).updateTutorNameByTutorUserId(userId, null);
        verify(lessonRepository, never()).updateTutorNameByTutorUserId(userId, null);
        verify(messageThreadRepository, never()).updateParticipantNameByTutorUserId(userId, null);
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

```bash
cd backend/crm-service
mvn -q -Dtest=TutorNamePropagationServiceTest test
```

Expected: `BUILD FAILURE` (missing class).

- [ ] **Step 3: Implement the service**

`backend/crm-service/src/main/java/com/tutor/crmservice/kafka/TutorNamePropagationService.java`:

```java
package com.tutor.crmservice.kafka;

import com.tutor.crmservice.lesson.LessonRepository;
import com.tutor.crmservice.message.MessageThreadRepository;
import com.tutor.crmservice.request.CrmApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TutorNamePropagationService {

    private final CrmApplicationRepository crmApplicationRepository;
    private final LessonRepository lessonRepository;
    private final MessageThreadRepository messageThreadRepository;

    @Transactional
    public void propagateTutorName(UUID userId, String newName) {
        if (newName == null || newName.isBlank()) {
            log.warn("Skipping tutor name propagation for user {} because the name is empty", userId);
            return;
        }
        int applications = crmApplicationRepository.updateTutorNameByTutorUserId(userId, newName);
        int lessons = lessonRepository.updateTutorNameByTutorUserId(userId, newName);
        int threads = messageThreadRepository.updateParticipantNameByTutorUserId(userId, newName);
        log.info("Propagated tutor name '{}' to user {}: {} applications, {} lessons, {} threads",
                newName, userId, applications, lessons, threads);
    }
}
```

- [ ] **Step 4: Run the test and verify it passes**

```bash
cd backend/crm-service
mvn -q -Dtest=TutorNamePropagationServiceTest test
```

Expected: `BUILD SUCCESS`, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/kafka/TutorNamePropagationService.java \
        backend/crm-service/src/test/java/com/tutor/crmservice/kafka/TutorNamePropagationServiceTest.java
git commit -m "feat(crm): add TutorNamePropagationService to update tutor name snapshots"
```

---

### Task 10: Create the crm-local `UserProfileChangedEvent` and `UserProfileChangedListener`

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/UserProfileChangedEvent.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/UserProfileChangedListener.java`
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/kafka/UserProfileChangedListenerTest.java`

- [ ] **Step 1: Create the crm-local event record**

`backend/crm-service/src/main/java/com/tutor/crmservice/kafka/UserProfileChangedEvent.java`:

```java
package com.tutor.crmservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record UserProfileChangedEvent(
        UUID userId,
        String name,
        Instant changedAt
) {}
```

- [ ] **Step 2: Write the failing listener test**

`backend/crm-service/src/test/java/com/tutor/crmservice/kafka/UserProfileChangedListenerTest.java`:

```java
package com.tutor.crmservice.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserProfileChangedListenerTest {

    @Mock
    private TutorNamePropagationService propagationService;

    @InjectMocks
    private UserProfileChangedListener listener;

    @Test
    void onUserProfileChangedDelegatesToPropagationService() {
        UUID userId = UUID.randomUUID();
        UserProfileChangedEvent event = new UserProfileChangedEvent(userId, "Пётр", Instant.parse("2026-06-15T10:00:00Z"));

        listener.onUserProfileChanged(event);

        verify(propagationService).propagateTutorName(userId, "Пётр");
    }

    @Test
    void onUserProfileChangedIgnoresEventsWithNullUserId() {
        UserProfileChangedEvent event = new UserProfileChangedEvent(null, "Пётр", Instant.now());

        listener.onUserProfileChanged(event);

        verifyNoInteractions(propagationService);
    }
}
```

- [ ] **Step 3: Run the test and verify it fails**

```bash
cd backend/crm-service
mvn -q -Dtest=UserProfileChangedListenerTest test
```

Expected: `BUILD FAILURE` (missing class).

- [ ] **Step 4: Implement the listener**

`backend/crm-service/src/main/java/com/tutor/crmservice/kafka/UserProfileChangedListener.java`:

```java
package com.tutor.crmservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileChangedListener {

    private final TutorNamePropagationService propagationService;

    @KafkaListener(
            topics = "user-profile-events",
            groupId = "crm-user-profile-changed",
            containerFactory = "userProfileListenerContainerFactory"
    )
    public void onUserProfileChanged(UserProfileChangedEvent event) {
        if (event == null || event.userId() == null) {
            log.warn("Ignoring UserProfileChanged event with no userId: {}", event);
            return;
        }
        log.debug("Received UserProfileChanged for user {} (changedAt={})", event.userId(), event.changedAt());
        propagationService.propagateTutorName(event.userId(), event.name());
    }
}
```

- [ ] **Step 5: Run the test and verify it passes**

```bash
cd backend/crm-service
mvn -q -Dtest=UserProfileChangedListenerTest test
```

Expected: `BUILD SUCCESS`, 2 tests passed.

- [ ] **Step 6: Commit**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/kafka/UserProfileChangedEvent.java \
        backend/crm-service/src/main/java/com/tutor/crmservice/kafka/UserProfileChangedListener.java \
        backend/crm-service/src/test/java/com/tutor/crmservice/kafka/UserProfileChangedListenerTest.java
git commit -m "feat(crm): consume UserProfileChanged from user-profile-events topic"
```

---

### Task 11: Configure the user-profile listener container factory and DLT

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/KafkaErrorHandlingConfig.java`
- Modify: `backend/crm-service/src/main/resources/application.yaml`

- [ ] **Step 1: Create the consumer factory, container factory, and error handler**

`backend/crm-service/src/main/java/com/tutor/crmservice/kafka/KafkaErrorHandlingConfig.java`:

```java
package com.tutor.crmservice.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaErrorHandlingConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.application.name:crm-service}")
    private String appName;

    @Bean
    public ConsumerFactory<String, UserProfileChangedEvent> userProfileConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "crm-user-profile-changed");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.tutor.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.tutor.crmservice.kafka.UserProfileChangedEvent");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserProfileChangedEvent> userProfileListenerContainerFactory(
            ConsumerFactory<String, UserProfileChangedEvent> userProfileConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, UserProfileChangedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userProfileConsumerFactory);

        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", record.partition())
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 5L));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd backend/crm-service
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run the full crm-service test suite (regression check)**

```bash
cd backend/crm-service
mvn -q test
```

Expected: `BUILD SUCCESS`, all tests pass. Pay attention to `PageViewServiceTest`, `LessonServiceTest`, `MessageServiceTest`, and `ApplicationLifecycleServiceTest` — they read `tutorName` from the snapshots, so as long as we have not changed the read paths, they stay green.

- [ ] **Step 4: Commit**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/kafka/KafkaErrorHandlingConfig.java
git commit -m "feat(crm): add Kafka error handling and listener container factory for user-profile-events"
```

---

## Phase 3 — Frontend

### Task 12: Add `updateSessionUserName` to `session.ts`

**Files:**
- Modify: `frontend/src/shared/auth/session.ts`
- Modify: `frontend/src/shared/auth/session.test.ts` (new — for the new function)

- [ ] **Step 1: Write the failing test**

Create `frontend/src/shared/auth/session.test.ts`:

```ts
import { beforeEach, describe, expect, it } from 'vitest';
import {
  readAuthSession,
  updateSessionUserName,
  writeAuthSession,
} from './session';
import type { AuthSession } from '../types';

const baseSession: AuthSession = {
  token: 'token-1',
  refreshToken: 'refresh-1',
  user: {
    id: 'user-1',
    email: 'tutor@example.com',
    name: 'Иван',
    role: 'tutor',
  },
};

describe('updateSessionUserName', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('updates the name in the persisted session', () => {
    writeAuthSession(baseSession);
    updateSessionUserName('Пётр');
    const session = readAuthSession();
    expect(session?.user.name).toBe('Пётр');
    expect(session?.user.id).toBe('user-1');
    expect(session?.user.email).toBe('tutor@example.com');
  });

  it('is a no-op when no session is stored', () => {
    updateSessionUserName('Пётр');
    expect(readAuthSession()).toBeNull();
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

```bash
cd frontend
npm test -- session
```

Expected: FAIL with "updateSessionUserName is not a function".

- [ ] **Step 3: Add the function**

Open `frontend/src/shared/auth/session.ts` and add at the end (before the last `getAppRouteForRole` export — the new function should sit with the other session helpers):

```ts
export function updateSessionUserName(name: string) {
  const current = readAuthSession();
  if (!current) {
    return;
  }
  writeAuthSession({
    ...current,
    user: { ...current.user, name },
  });
}
```

- [ ] **Step 4: Run the test and verify it passes**

```bash
cd frontend
npm test -- session
```

Expected: PASS, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/shared/auth/session.ts frontend/src/shared/auth/session.test.ts
git commit -m "feat(frontend): add updateSessionUserName to update the cached auth name"
```

---

### Task 13: Add the `profileChangedEvent` constant

**Files:**
- Modify: `frontend/src/components/layout-helpers.ts`
- Modify: `frontend/src/components/layout-helpers.test.ts`

- [ ] **Step 1: Add the constant**

Open `frontend/src/components/layout-helpers.ts`. Right below the existing `tutorSidebarRefreshEvent` constant, add:

```ts
export const profileChangedEvent = 'tutorspace:profile-changed';
```

- [ ] **Step 2: Add a test for the constant**

Open `frontend/src/components/layout-helpers.test.ts`. Search for the existing `tutor workspace sidebar cards` describe block. Add a new `describe` block near the end of the file (before the last closing `}`):

```ts
describe('profile changed event', () => {
  it('uses a stable event name for refreshing the workspace header', async () => {
    const { profileChangedEvent } = await import('./layout-helpers');
    expect(profileChangedEvent).toBe('tutorspace:profile-changed');
  });
});
```

- [ ] **Step 3: Run the test and verify it passes**

```bash
cd frontend
npm test -- layout-helpers
```

Expected: PASS, the new case plus the existing 11 cases.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/layout-helpers.ts \
        frontend/src/components/layout-helpers.test.ts
git commit -m "feat(frontend): add profileChangedEvent for workspace header refresh"
```

---

### Task 14: Create the `updateAuthProfile` API helper

**Files:**
- Create: `frontend/src/shared/api/auth.ts`
- Create: `frontend/src/shared/api/auth.test.ts`

- [ ] **Step 1: Write the failing test**

`frontend/src/shared/api/auth.test.ts`:

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiRequest } from './http';
import { updateAuthProfile } from './auth';

vi.mock('./http', () => ({
  apiRequest: vi.fn(),
}));

describe('updateAuthProfile', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('PATCHes the new name to /auth/me', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ id: 'user-1', name: 'Пётр' });

    const result = await updateAuthProfile({ name: 'Пётр' });

    expect(apiRequest).toHaveBeenCalledWith('/auth/me', {
      method: 'PATCH',
      body: JSON.stringify({ name: 'Пётр' }),
    });
    expect(result).toEqual({ id: 'user-1', name: 'Пётр' });
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

```bash
cd frontend
npm test -- shared/api/auth
```

Expected: FAIL with "Cannot find module './auth'".

- [ ] **Step 3: Implement the helper**

`frontend/src/shared/api/auth.ts`:

```ts
import { apiRequest } from './http';

export type UpdateAuthProfilePayload = {
  name: string;
};

export type AuthPublicUser = {
  id: string;
  email?: string;
  name: string;
  role?: string;
  status?: string;
};

export function updateAuthProfile(payload: UpdateAuthProfilePayload) {
  return apiRequest<AuthPublicUser>('/auth/me', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}
```

- [ ] **Step 4: Run the test and verify it passes**

```bash
cd frontend
npm test -- shared/api/auth
```

Expected: PASS, 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/shared/api/auth.ts frontend/src/shared/api/auth.test.ts
git commit -m "feat(frontend): add updateAuthProfile PATCH /auth/me helper"
```

---

### Task 15: Update `TutorWorkspaceProfilePage.handleSave`

**Files:**
- Modify: `frontend/src/pages/app/TutorWorkspaceProfilePage.tsx`

- [ ] **Step 1: Update the imports**

Open the file. Extend the existing `tutor` api import block:

```ts
import {
  getTutorSettings,
  getTutorWorkspaceProfile,
  updateTutorSettings,
  updateTutorWorkspaceProfile,
} from '../../shared/api/tutor';
```

becomes:

```ts
import {
  getTutorSettings,
  getTutorWorkspaceProfile,
  updateTutorSettings,
  updateTutorWorkspaceProfile,
} from '../../shared/api/tutor';
import { updateAuthProfile } from '../../shared/api/auth';
import { updateSessionUserName } from '../../shared/auth/session';
import { profileChangedEvent } from '../../components/layout-helpers';
```

- [ ] **Step 2: Replace `handleSave`**

Find the existing `handleSave` function (around line 115). Replace it with:

```ts
  const handleSave = async () => {
    if (!profile || !settings) return;
    if (!validate()) return;

    setSaving(true);
    setStatus('');
    try {
      await updateAuthProfile({ name: profile.identity.name });
      updateSessionUserName(profile.identity.name);
      window.dispatchEvent(new Event(profileChangedEvent));

      const updated = await updateTutorWorkspaceProfile(profile);
      const settingsPayload = buildTutorSettingsFromWorkspaceProfile(updated, settings);
      const updatedSettings = await updateTutorSettings(settingsPayload);
      const refreshedProfile = await getTutorWorkspaceProfile();
      setProfile(refreshedProfile);
      setSettings(updatedSettings);
      setStatus('Профиль и настройки сохранены в CRM.');
    } catch {
      setStatus('Не удалось сохранить профиль и настройки.');
    } finally {
      setSaving(false);
    }
  };
```

- [ ] **Step 3: Run the frontend typecheck**

```bash
cd frontend
npm run typecheck
```

Expected: `tsc --noEmit -p tsconfig.json` completes with no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/app/TutorWorkspaceProfilePage.tsx
git commit -m "feat(frontend): call PATCH /auth/me first on profile save, refresh local session"
```

---

### Task 16: Update the `Layout` to listen for `profileChangedEvent`

**Files:**
- Modify: `frontend/src/components/layout.tsx`

- [ ] **Step 1: Add the import**

Open `frontend/src/components/layout.tsx`. Find the existing import block that pulls from `./layout-helpers`. Add `profileChangedEvent` to it. Search for `tutorSidebarRefreshEvent` to find the right line.

If the line is:

```ts
import {
  tutorSidebarRefreshEvent,
  // ...other helpers
} from './layout-helpers';
```

change to:

```ts
import {
  profileChangedEvent,
  tutorSidebarRefreshEvent,
  // ...other helpers
} from './layout-helpers';
```

- [ ] **Step 2: Add the listener inside `WorkspaceChrome` (or wherever `workspaceUser` is derived)**

Find the function that computes `workspaceUser` (the existing `useMemo` with `name`/`initials`). In the same `useEffect` that subscribes to `tutorSidebarRefreshEvent`, also subscribe to `profileChangedEvent` and re-read the session.

Insert this `useEffect` right after the existing tutor-sidebar `useEffect` (search for `tutorSidebarRefreshEvent` to locate it):

```ts
useEffect(() => {
  const handler = () => {
    const fresh = readAuthSession();
    setSession(fresh);
  };
  window.addEventListener(profileChangedEvent, handler);
  return () => window.removeEventListener(profileChangedEvent, handler);
}, []);
```

- [ ] **Step 3: Confirm `readAuthSession` and `setSession` are accessible in scope**

- `readAuthSession` lives in `frontend/src/shared/auth/session.ts`. Add the import at the top of `layout.tsx` if not already there:
  ```ts
  import { readAuthSession } from '../shared/auth/session';
  ```
- `setSession` is the existing state setter for the `session` `useState`. Confirm that the local `useState<AuthSession | null>(null)` for the session is named `session` (or rename `setSession` to whatever the existing setter is).

- [ ] **Step 4: Run the typecheck**

```bash
cd frontend
npm run typecheck
```

Expected: no errors.

- [ ] **Step 5: Run the full frontend test suite**

```bash
cd frontend
npm test
```

Expected: all tests pass, 172+ tests (the 4 added in earlier tasks included).

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/layout.tsx
git commit -m "feat(frontend): refresh workspace user chip on profileChangedEvent"
```

---

## Phase 4 — End-to-end verification

### Task 17: Run the full local backend and frontend test suites

- [ ] **Step 1: Run the auth-service tests**

```bash
cd backend/auth-service
mvn test
```

Expected: `BUILD SUCCESS`, all tests pass (the existing ~30 tests plus the new `UserProfileEventPublisherTest`, `UserServiceTest` cases, `AuthControllerTest`).

- [ ] **Step 2: Run the crm-service tests**

```bash
cd backend/crm-service
mvn test
```

Expected: `BUILD SUCCESS`, all tests pass (existing tests stay green; new `TutorNamePropagationServiceTest` and `UserProfileChangedListenerTest` pass).

- [ ] **Step 3: Run the marketplace-service tests (regression — should not be affected)**

```bash
cd backend/marketplace-service
mvn test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run the frontend tests**

```bash
cd frontend
npm test
```

Expected: all tests pass.

- [ ] **Step 5: Build the frontend**

```bash
cd frontend
npm run build
```

Expected: `tsc --noEmit -p tsconfig.json && node scripts/build.mjs` completes with no errors.

- [ ] **Step 6: Build the backend Docker images**

```bash
docker compose build auth-service crm-service
```

Expected: images build successfully. (Per AGENTS.md, this is `mvn -B package -DskipTests` per service, so a green build does not prove tests passed — the earlier `mvn test` runs are the evidence.)

---

### Task 18: Manual smoke test (out-of-band)

These are not committed; the operator should run them after the implementation lands.

- [ ] **Step 1: Start the local stack**

```bash
docker compose up -d
```

- [ ] **Step 2: Register a tutor and a client; create an application**

Use the existing `seed` users or register fresh ones through the API. Have the client send an application to the tutor.

- [ ] **Step 3: Open `/app/tutor/profile` as the tutor, change the name, save**

Expected: the workspace header chip shows the new name without a page reload.

- [ ] **Step 4: Open `/app/client/applications` as the client**

Expected: within ~1 second, the application card shows the new name. (Kafka event-driven.)

- [ ] **Step 5: Stop the local stack**

```bash
docker compose down
```

---

### Task 19: Commit and merge

- [ ] **Step 1: Verify the branch state**

```bash
git status
git log --oneline -10
```

Expected: the `feature/tutor-name-propagation` branch with one design spec commit plus the 17 implementation commits, all clean.

- [ ] **Step 2: Push the branch (only if the user asks)**

```bash
git push origin feature/tutor-name-propagation
```

- [ ] **Step 3: Merge into `dev`**

```bash
git checkout dev
git merge --no-ff feature/tutor-name-propagation -m "merge: tutor name propagation across services"
```

Expected: clean merge (no conflicts because all changes are additive).

- [ ] **Step 4: Push `dev` (only if the user asks)**

```bash
git push origin dev
```

---

## Self-review notes (written before publishing)

- **Spec coverage:**
  - "Auth is the single source of truth" → Tasks 5, 6 (auth endpoint + service).
  - "PATCH /api/auth/me" → Task 6.
  - "Validation 2–100 chars" → Task 4 (DTO `@Size`) + Task 5 (`IllegalArgumentException` for service-level checks).
  - "UserProfileChanged Kafka event" → Tasks 2, 3 (auth publisher), Task 10 (crm listener).
  - "Three snapshot updates" → Tasks 7, 8, 9.
  - "Idempotent consumer" → Task 7/8/9 use `AND a.tutorName <> :newName` guard; Task 9 service tests the no-op path.
  - "DLT with 5 retries" → Task 11.
  - "Frontend: auth first, then CRM" → Task 15.
  - "Frontend: local session refresh" → Task 12.
  - "Frontend: header chip refresh" → Tasks 13, 16.
  - "JWT untouched" → no JWT-related tasks.
  - "Marketplace sync untouched" → no marketplace-related tasks.
  - "Tests for both services + frontend" → Tasks 3, 5, 6, 9, 10, 12, 13, 14.
- **No placeholders:** every code block is the actual content. Every commit command has a real message. Every test runs an actual command with a real expected result.
- **Type consistency:** the DTOs (`UpdateProfileNameRequest`, `UserPublicDto`), the event record (`UserProfileChangedEvent` — auth and crm local copies with the same fields), the publisher method (`publish(userId, name, changedAt)`), the listener method (`onUserProfileChanged(event)`), the service method (`propagateTutorName(userId, newName)`), the repository methods (`updateTutorNameByTutorUserId(userId, newName)`) are all consistent across tasks.
- **One ambiguity:** the `crm-service` `application.yaml` already configures a default consumer factory. Task 11's `userProfileConsumerFactory` bean overrides for the new topic but does not touch the existing factory, so `SubscriptionActivatedListener` and any other existing listener continue to use the default. Verified by reading `application.yaml` and `SubscriptionActivatedListener`.
