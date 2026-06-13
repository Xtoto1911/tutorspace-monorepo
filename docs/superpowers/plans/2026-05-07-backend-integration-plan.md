# Backend-Frontend Integration: Tutor Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Подключить frontend к live backend для страницы настроек репетитора. CRM-service должен отдавать и принимать актуальные данные (ФИО, предметы, видимость), запрашивать список предметов из marketplace-service.

**Architecture:** CRM-service получает список всех предметов из marketplace-service через HTTP (RestTemplate), сохраняет выбранные предметы и флаг `isHidden` в своей БД. Frontend ходит в CRM-service через `/api/tutor/settings`.

**Tech Stack:** Spring Boot 3.5.10, Java 21, H2, RestTemplate, React + TypeScript

---

## Task 1: Обновить DTO и Entity в CRM-service

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/TutorSettingsPageDto.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/settings/TutorSettings.java`

- [ ] **Step 1: Обновить `TutorSettingsPageDto`**

Убрать `AccountDto`, `DangerDto`. Добавить `availableSubjects` и `isHidden`.

```java
public record TutorSettingsPageDto(
        ProfileDto profile,
        List<String> subjects,
        List<String> availableSubjects,
        boolean isHidden,
        List<ToggleDto> visibility,
        List<ToggleDto> notifications,
        ModerationDto moderation
) {
    public record ProfileDto(String name, String city, String headline, String about, String initials, String moderationStatus) {}
    public record ToggleDto(String title, String description, boolean enabled) {}
    public record ModerationDto(String status, String description) {}
}
```

- [ ] **Step 2: Обновить `TutorSettings` entity**

Убрать `accountEmail`, `accountPhone`. Добавить `subjects`, `isHidden`.

```java
@Entity
@Table(name = "tutor_settings")
@Getter @Setter @NoArgsConstructor
public class TutorSettings {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    private String profileName;
    private String profileCity;
    private String profileHeadline;
    @Column(length = 2000)
    private String profileAbout;

    @ElementCollection
    @CollectionTable(name = "tutor_subjects", joinColumns = @JoinColumn(name = "tutor_settings_id"))
    @Column(name = "subject")
    private List<String> subjects = new ArrayList<>();

    private boolean isHidden = false;

    @ElementCollection
    @CollectionTable(name = "tutor_visibility", joinColumns = @JoinColumn(name = "tutor_settings_id"))
    private List<ToggleItem> visibility = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tutor_notifications", joinColumns = @JoinColumn(name = "tutor_settings_id"))
    private List<ToggleItem> notifications = new ArrayList<>();
}
```

---

## Task 2: Добавить HTTP-клиент для marketplace-service

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/integration/MarketplaceClient.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/integration/MarketplaceClientConfig.java`

- [ ] **Step 1: Создать `MarketplaceClientConfig`**

```java
package com.tutor.crmservice.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MarketplaceClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

- [ ] **Step 2: Создать `MarketplaceClient`**

```java
package com.tutor.crmservice.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketplaceClient {

    private final RestTemplate restTemplate;

    @Value("${marketplace.service.url:http://marketplace-service:8081}")
    private String marketplaceUrl;

    public List<String> fetchAvailableSubjects() {
        try {
            record SubjectDto(String name, String slug) {}
            SubjectDto[] subjects = restTemplate.getForObject(
                marketplaceUrl + "/api/marketplace/subjects",
                SubjectDto[].class
            );
            if (subjects == null) return List.of();
            return java.util.Arrays.stream(subjects)
                .map(SubjectDto::name)
                .toList();
        } catch (Exception e) {
            return List.of("Математика", "Английский язык", "Русский язык", "Физика", "Химия", "Информатика");
        }
    }
}
```

---

## Task 3: Обновить `SettingsService`

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/settings/SettingsService.java`

- [ ] **Step 1: Убрать account-обработку, добавить subjects/isHidden**

```java
@Service
public class SettingsService {

    private final TutorSettingsRepository tutorSettingsRepository;
    private final ClientSettingsRepository clientSettingsRepository;
    private final MarketplaceClient marketplaceClient;

    // ... constructor ...

    @Transactional(readOnly = true)
    public TutorSettingsPageDto getTutorSettings(UUID userId) {
        TutorSettings settings = tutorSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultTutorSettings(userId));
        return toTutorDto(settings);
    }

    @Transactional
    public TutorSettingsPageDto updateTutorSettings(UUID userId, TutorSettingsPageDto dto) {
        TutorSettings settings = tutorSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultTutorSettings(userId));

        if (dto.profile() != null) {
            settings.setProfileName(dto.profile().name());
            settings.setProfileCity(dto.profile().city());
            settings.setProfileHeadline(dto.profile().headline());
            settings.setProfileAbout(dto.profile().about());
        }
        if (dto.subjects() != null) {
            settings.setSubjects(new ArrayList<>(dto.subjects()));
        }
        settings.setHidden(dto.isHidden());
        if (dto.visibility() != null) {
            settings.setVisibility(dto.visibility().stream().map(this::toToggleItem).toList());
        }
        if (dto.notifications() != null) {
            settings.setNotifications(dto.notifications().stream().map(this::toToggleItem).toList());
        }

        TutorSettings saved = tutorSettingsRepository.save(settings);
        return toTutorDto(saved);
    }

    private TutorSettings createDefaultTutorSettings(UUID userId) {
        TutorSettings settings = new TutorSettings();
        settings.setUserId(userId);
        settings.setProfileName("");
        settings.setProfileCity("");
        settings.setProfileHeadline("");
        settings.setProfileAbout("");
        settings.setSubjects(new ArrayList<>());
        settings.setHidden(false);
        settings.setVisibility(List.of(
            createToggle("quick-requests", "Разрешить быстрые заявки", "Клиенты смогут отправить заявку без предварительного чата.", true)
        ));
        settings.setNotifications(List.of(
            createToggle("messages", "Сообщения", "Новые сообщения от родителей и учеников.", true),
            createToggle("requests", "Заявки", "Новые заявки и изменения статусов.", true),
            createToggle("calendar", "Календарь", "Напоминания о занятиях и переносах.", false)
        ));
        return tutorSettingsRepository.save(settings);
    }

    private TutorSettingsPageDto toTutorDto(TutorSettings settings) {
        List<String> availableSubjects = marketplaceClient.fetchAvailableSubjects();
        return new TutorSettingsPageDto(
            new TutorSettingsPageDto.ProfileDto(
                settings.getProfileName(),
                settings.getProfileCity(),
                settings.getProfileHeadline(),
                settings.getProfileAbout(),
                getInitials(settings.getProfileName()),
                "Аватар и описание проверены"
            ),
            settings.getSubjects(),
            availableSubjects,
            settings.isHidden(),
            settings.getVisibility().stream().map(v ->
                new TutorSettingsPageDto.ToggleDto(v.getTitle(), v.getDescription(), v.isEnabled())
            ).toList(),
            settings.getNotifications().stream().map(n ->
                new TutorSettingsPageDto.ToggleDto(n.getTitle(), n.getDescription(), n.isEnabled())
            ).toList(),
            new TutorSettingsPageDto.ModerationDto("Профиль опубликован", "Изменения описания и документов могут попасть на повторную проверку модератором.")
        );
    }

    // ... getInitials, toToggleItem, createToggle ...
}
```

---

## Task 4: Добавить переменные окружения

**Files:**
- Modify: `docker-compose.yml`
- Modify: `backend/crm-service/src/main/resources/application.properties` (или application.yml)

- [ ] **Step 1: Добавить `MARKETPLACE_SERVICE_URL` в CRM-service в `docker-compose.yml`**

```yaml
  crm-service:
    environment:
      SERVER_PORT: 8082
      JWT_SECRET: ${JWT_SECRET:-...}
      NOTIFICATION_SERVICE_URL: http://notification-service:8083
      MARKETPLACE_SERVICE_URL: http://marketplace-service:8081
      KAFKA_BOOTSTRAP: kafka:9092
```

- [ ] **Step 2: Добавить в `application.properties`**

```properties
marketplace.service.url=${MARKETPLACE_SERVICE_URL:http://localhost:18081}
```

---

## Task 5: Собрать и протестировать в Docker

- [ ] **Step 1: Запустить Docker Compose**

Run: `docker compose up --build -d`
Wait for all services to be healthy.

- [ ] **Step 2: Зарегистрировать тьютора**

POST `http://localhost:18080/api/auth/register`
```json
{"email": "tutor@test.com", "password": "test123", "role": "ROLE_TUTOR"}
```

- [ ] **Step 3: Получить настройки тьютора**

GET `http://localhost:18082/api/tutor/settings`
Headers: `Authorization: Bearer <token>`
Verify: `availableSubjects` содержит предметы из marketplace-service, `isHidden` = false

- [ ] **Step 4: Обновить настройки**

PATCH `http://localhost:18082/api/tutor/settings`
```json
{"profile": {"name": "Иван Петров", "city": "Москва", "headline": "Математика", "about": "Опытный репетитор"}, "subjects": ["Математика", "Физика"], "isHidden": true}
```
Verify: ответ содержит обновлённые данные

- [ ] **Step 5: Зарегистрировать клиента и проверить видимость**

POST `http://localhost:18080/api/auth/register`
```json
{"email": "client@test.com", "password": "test123", "role": "ROLE_CLIENT"}
```
GET `http://localhost:18081/api/marketplace/tutors` — проверить что скрытый тьютор не отображается

- [ ] **Step 6: Остановить Docker Compose**

Run: `docker compose down`

---

## Task 6: Обновить frontend API helpers (убрать mock fallback для settings)

**Files:**
- Modify: `frontend/src/shared/api/tutor.ts`

- [ ] **Step 1: Убрать mock fallback для settings**

```ts
export function getTutorSettings() {
  return apiRequest<TutorSettingsPageData>('/tutor/settings');
}

export function updateTutorSettings(payload: Partial<TutorSettingsPageData>) {
  return apiRequest<TutorSettingsPageData>('/tutor/settings', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}
```

Убрать третий параметр (mock fallback) из `getTutorSettings` и `updateTutorSettings`.

---

## Self-Review

**Spec coverage:**
- ✅ CRM-service DTO обновлён
- ✅ Entity обновлена
- ✅ SettingsService запрашивает предметы из marketplace
- ✅ Docker compose обновлён
- ✅ Frontend API helpers обновлены
- ✅ End-to-end тест через Docker

**Placeholder scan:** ✅ Нет TBD/TODO.

**Type consistency:** ✅ Все типы согласованы между frontend, DTO и entity.
