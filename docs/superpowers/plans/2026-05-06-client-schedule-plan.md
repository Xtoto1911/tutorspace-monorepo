# Client Schedule Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `/app/client/schedule` into a real client schedule screen backed by live CRM data, with clean empty states, working CTA links, refresh, and a minimal reschedule request flow.

**Architecture:** Keep the page on one aggregated `GET /api/client/schedule` read model from `crm-service`, extend that DTO instead of splitting the page into many frontend fetches, and add one minimal write endpoint for reschedule requests. On the frontend, keep the page component as the main container, move only route/empty-state decisions into a small pure helper for testability, and wire all actions from live DTO fields.

**Tech Stack:** React 19 + TypeScript + custom Rollup/Vitest frontend, Spring Boot 3.5.10 + Java 21 + JPA/H2 backend, Docker Compose, Chrome DevTools MCP.

**Repo note:** This workspace is not a git repository, so commit steps are intentionally omitted.

---

## File Map

- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/ClientCrmController.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/ClientSchedulePageDto.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleRequest.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleRequestRepository.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleService.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/service/RequestLessonRescheduleDto.java`
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonRescheduleServiceTest.java`
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`
- Modify: `frontend/src/shared/types.ts`
- Modify: `frontend/src/shared/api/client.ts`
- Create: `frontend/src/shared/api/client.test.ts`
- Create: `frontend/src/pages/app/client-schedule-helpers.ts`
- Create: `frontend/src/pages/app/client-schedule-helpers.test.ts`
- Modify: `frontend/src/pages/app/ClientSchedulePage.tsx`
- Modify: `frontend/src/styles/global.css`
- Modify: `frontend/FRONTEND_BACKEND_CONTRACT.md`

### Task 1: Persist reschedule requests in CRM

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleRequest.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleRequestRepository.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleService.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/service/RequestLessonRescheduleDto.java`
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonRescheduleServiceTest.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/ClientCrmController.java`

- [ ] **Step 1: Write the failing backend tests**

Create `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonRescheduleServiceTest.java`:

```java
package com.tutor.crmservice.lesson;

import com.tutor.crmservice.service.RequestLessonRescheduleDto;
import com.tutor.crmservice.shared.ConflictException;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonRescheduleServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock LessonRescheduleRequestRepository rescheduleRequestRepository;

    @InjectMocks
    LessonRescheduleService service;

    @Test
    void createRequest_forOwnLesson_savesPendingRequest() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setClientUserId(clientId);
        lesson.setTutorUserId(UUID.randomUUID());
        lesson.setApplicationId(UUID.randomUUID());
        lesson.setSubjectName("Математика");
        lesson.setStartsAt(LocalDateTime.of(2026, 5, 7, 18, 0));

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(rescheduleRequestRepository.findTopByLessonIdOrderByCreatedAtDesc(lessonId)).thenReturn(Optional.empty());
        when(rescheduleRequestRepository.save(any(LessonRescheduleRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonRescheduleRequest saved = service.createClientRequest(
                clientId,
                lessonId,
                new RequestLessonRescheduleDto("Можно на 19:00?", LocalDateTime.of(2026, 5, 7, 19, 0))
        );

        assertEquals("PENDING", saved.getStatus());
        assertEquals("Можно на 19:00?", saved.getMessage());
    }

    @Test
    void createRequest_forForeignLesson_throwsNotFound() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setClientUserId(UUID.randomUUID());

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThrows(ResourceNotFoundException.class, () ->
                service.createClientRequest(UUID.randomUUID(), lessonId, new RequestLessonRescheduleDto("message", null)));
    }

    @Test
    void createRequest_whenPendingAlreadyExists_throwsConflict() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setClientUserId(clientId);

        LessonRescheduleRequest existing = new LessonRescheduleRequest();
        existing.setStatus("PENDING");

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(rescheduleRequestRepository.findTopByLessonIdOrderByCreatedAtDesc(lessonId)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () ->
                service.createClientRequest(clientId, lessonId, new RequestLessonRescheduleDto("message", null)));
    }
}
```

- [ ] **Step 2: Run the focused backend test to verify it fails**

Run: `mvn test -Dtest=LessonRescheduleServiceTest`

Workdir: `backend/crm-service`

Expected: FAIL with missing symbols for `LessonRescheduleService`, `LessonRescheduleRequestRepository`, `LessonRescheduleRequest`, and `RequestLessonRescheduleDto`.

- [ ] **Step 3: Add the minimal reschedule entity, repository, DTO, and service**

Create `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleRequest.java`:

```java
package com.tutor.crmservice.lesson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lesson_reschedule_requests")
@Getter
@Setter
@NoArgsConstructor
public class LessonRescheduleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID lessonId;

    @Column(nullable = false)
    private UUID applicationId;

    @Column(nullable = false)
    private UUID clientUserId;

    @Column(nullable = false)
    private UUID tutorUserId;

    @Column(nullable = false)
    private String lessonTitle;

    @Column(nullable = false)
    private LocalDateTime lessonStartsAt;

    @Column(nullable = false, length = 1000)
    private String message;

    private LocalDateTime proposedStartAt;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
```

Create `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleRequestRepository.java`:

```java
package com.tutor.crmservice.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRescheduleRequestRepository extends JpaRepository<LessonRescheduleRequest, UUID> {
    Optional<LessonRescheduleRequest> findTopByLessonIdOrderByCreatedAtDesc(UUID lessonId);
    List<LessonRescheduleRequest> findByClientUserIdOrderByCreatedAtDesc(UUID clientUserId);
}
```

Create `backend/crm-service/src/main/java/com/tutor/crmservice/service/RequestLessonRescheduleDto.java`:

```java
package com.tutor.crmservice.service;

import java.time.LocalDateTime;

public record RequestLessonRescheduleDto(String message, LocalDateTime proposedStartAt) {
}
```

Create `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleService.java`:

```java
package com.tutor.crmservice.lesson;

import com.tutor.crmservice.service.RequestLessonRescheduleDto;
import com.tutor.crmservice.shared.ConflictException;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonRescheduleService {

    private final LessonRepository lessonRepository;
    private final LessonRescheduleRequestRepository rescheduleRequestRepository;

    @Transactional
    public LessonRescheduleRequest createClientRequest(UUID clientUserId, UUID lessonId, RequestLessonRescheduleDto input) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Урок не найден."));

        if (!lesson.getClientUserId().equals(clientUserId)) {
            throw new ResourceNotFoundException("Урок не найден.");
        }

        rescheduleRequestRepository.findTopByLessonIdOrderByCreatedAtDesc(lessonId)
                .filter(existing -> "PENDING".equals(existing.getStatus()))
                .ifPresent(existing -> {
                    throw new ConflictException("Запрос на перенос уже ожидает ответа.");
                });

        LessonRescheduleRequest request = new LessonRescheduleRequest();
        request.setLessonId(lesson.getId());
        request.setApplicationId(lesson.getApplicationId());
        request.setClientUserId(lesson.getClientUserId());
        request.setTutorUserId(lesson.getTutorUserId());
        request.setLessonTitle(lesson.getTitle());
        request.setLessonStartsAt(lesson.getStartsAt());
        request.setMessage(input.message());
        request.setProposedStartAt(input.proposedStartAt());
        request.setStatus("PENDING");
        return rescheduleRequestRepository.save(request);
    }
}
```

Modify `backend/crm-service/src/main/java/com/tutor/crmservice/controller/ClientCrmController.java`:

```java
private final LessonRescheduleService lessonRescheduleService;

@PostMapping("/schedule/lessons/{lessonId}/reschedule-request")
public Map<String, String> requestReschedule(@AuthenticationPrincipal JwtPrincipal principal,
                                             @PathVariable UUID lessonId,
                                             @RequestBody RequestLessonRescheduleDto payload) {
    LessonRescheduleRequest request = lessonRescheduleService.createClientRequest(principal.userId(), lessonId, payload);
    return Map.of(
            "id", request.getId().toString(),
            "status", request.getStatus()
    );
}
```

- [ ] **Step 4: Run the focused backend test to verify it passes**

Run: `mvn test -Dtest=LessonRescheduleServiceTest`

Workdir: `backend/crm-service`

Expected: PASS with 3 tests run and `BUILD SUCCESS`.

### Task 2: Enrich the live schedule DTO and page read model

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/ClientSchedulePageDto.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`
- Modify: `frontend/FRONTEND_BACKEND_CONTRACT.md`

- [ ] **Step 1: Write the failing read-model tests**

Create `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`:

```java
package com.tutor.crmservice.view;

import com.tutor.crmservice.lesson.Lesson;
import com.tutor.crmservice.lesson.LessonRepository;
import com.tutor.crmservice.lesson.LessonRescheduleRequest;
import com.tutor.crmservice.lesson.LessonRescheduleRequestRepository;
import com.tutor.crmservice.message.MessageRepository;
import com.tutor.crmservice.message.MessageThread;
import com.tutor.crmservice.message.MessageThreadRepository;
import com.tutor.crmservice.request.CrmApplicationRepository;
import com.tutor.crmservice.student.StudentCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageViewServiceTest {

    @Mock CrmApplicationRepository crmApplicationRepository;
    @Mock StudentCardRepository studentCardRepository;
    @Mock LessonRepository lessonRepository;
    @Mock MessageThreadRepository messageThreadRepository;
    @Mock MessageRepository messageRepository;
    @Mock LessonRescheduleRequestRepository lessonRescheduleRequestRepository;

    @InjectMocks
    PageViewService service;

    @Test
    void getClientSchedulePage_mapsLessonActionsAndPeriod() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setApplicationId(applicationId);
        lesson.setClientUserId(clientId);
        lesson.setTutorName("Анна Смирнова");
        lesson.setSubjectName("Математика");
        lesson.setFormat("Онлайн");
        lesson.setTitle("Математика с Анной Смирновой");
        lesson.setStartsAt(LocalDateTime.of(2026, 5, 7, 18, 0));
        lesson.setDurationMinutes(60);

        MessageThread thread = new MessageThread();
        thread.setId(UUID.randomUUID());
        thread.setApplicationId(applicationId);

        when(lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientId)).thenReturn(List.of(lesson));
        when(messageThreadRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(thread));
        when(lessonRescheduleRequestRepository.findByClientUserIdOrderByCreatedAtDesc(clientId)).thenReturn(List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(1, page.events().size());
        assertEquals(thread.getId().toString(), page.events().get(0).threadId());
        assertEquals(applicationId.toString(), page.events().get(0).applicationId());
        assertTrue(page.events().get(0).canReschedule());
        assertFalse(page.emptyState().show());
        assertEquals(1, page.upcomingLessons().size());
    }

    @Test
    void getClientSchedulePage_whenNoLessons_returnsEmptyState() {
        UUID clientId = UUID.randomUUID();

        when(lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientId)).thenReturn(List.of());
        when(lessonRescheduleRequestRepository.findByClientUserIdOrderByCreatedAtDesc(clientId)).thenReturn(List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertTrue(page.emptyState().show());
        assertEquals("Уроков пока нет", page.emptyState().title());
        assertEquals(0, page.events().size());
        assertEquals(0, page.upcomingLessons().size());
    }

    @Test
    void getClientSchedulePage_surfacesPendingRescheduleRequests() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setApplicationId(applicationId);
        lesson.setClientUserId(clientId);
        lesson.setTutorName("Марина Белова");
        lesson.setSubjectName("Английский язык");
        lesson.setFormat("Онлайн");
        lesson.setTitle("Английский язык с Мариной Беловой");
        lesson.setStartsAt(LocalDateTime.of(2026, 5, 8, 17, 0));
        lesson.setDurationMinutes(60);

        LessonRescheduleRequest request = new LessonRescheduleRequest();
        request.setId(UUID.randomUUID());
        request.setLessonId(lessonId);
        request.setApplicationId(applicationId);
        request.setLessonTitle("Английский язык");
        request.setLessonStartsAt(lesson.getStartsAt());
        request.setStatus("PENDING");

        when(lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientId)).thenReturn(List.of(lesson));
        when(messageThreadRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        when(lessonRescheduleRequestRepository.findByClientUserIdOrderByCreatedAtDesc(clientId)).thenReturn(List.of(request));

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(1, page.rescheduleRequests().size());
        assertEquals(request.getId().toString(), page.rescheduleRequests().get(0).id());
        assertEquals("ожидает ответа", page.rescheduleRequests().get(0).status());
    }
}
```

- [ ] **Step 2: Run the focused read-model test to verify it fails**

Run: `mvn test -Dtest=PageViewServiceTest`

Workdir: `backend/crm-service`

Expected: FAIL with missing constructor arguments and missing record fields on `ClientSchedulePageDto`.

- [ ] **Step 3: Extend the DTO and map real schedule data**

Modify `backend/crm-service/src/main/java/com/tutor/crmservice/view/ClientSchedulePageDto.java`:

```java
public record ClientSchedulePageDto(
        List<MetricDto> metrics,
        PeriodDto period,
        List<WeekDayDto> weekDays,
        List<String> timeSlots,
        List<EventDto> events,
        List<RescheduleDto> rescheduleRequests,
        List<MiniMonthDayDto> miniMonthDays,
        List<UpcomingLessonDto> upcomingLessons,
        EmptyStateDto emptyState
) {
    public record PeriodDto(String from, String to, String label) {
    }

    public record EventDto(
            String id,
            String lessonId,
            String applicationId,
            String threadId,
            String title,
            String meta,
            String time,
            String startsAt,
            String endsAt,
            String tone,
            int column,
            int row,
            boolean canJoin,
            boolean canReschedule,
            boolean canMessageTutor
    ) {
    }

    public record RescheduleDto(String id, String lessonId, String time, String title, String status, String tone) {
    }

    public record UpcomingLessonDto(
            String id,
            String lessonId,
            String applicationId,
            String threadId,
            String time,
            String title,
            String details,
            boolean canJoin,
            boolean canReschedule,
            boolean canMessageTutor
    ) {
    }

    public record EmptyStateDto(boolean show, String title, String text) {
    }
}
```

Modify `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java` by injecting `LessonRescheduleRequestRepository`, mapping message threads by `applicationId`, and replacing fake schedule values with data-driven fields:

```java
private final LessonRescheduleRequestRepository lessonRescheduleRequestRepository;

@Transactional(readOnly = true)
public ClientSchedulePageDto getClientSchedulePage(UUID clientUserId) {
    List<Lesson> lessons = lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientUserId);
    List<LessonRescheduleRequest> requests = lessonRescheduleRequestRepository.findByClientUserIdOrderByCreatedAtDesc(clientUserId);
    LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

    List<ClientSchedulePageDto.EventDto> events = lessons.stream()
            .map(lesson -> {
                MessageThread thread = messageThreadRepository.findByApplicationId(lesson.getApplicationId()).orElse(null);
                int dayIndex = Math.max(0, Math.min(4, lesson.getStartsAt().getDayOfWeek().getValue() - 1));
                int row = Math.max(0, Math.min(4, (lesson.getStartsAt().getHour() - 12) / 2));
                LocalDateTime endsAt = lesson.getStartsAt().plusMinutes(lesson.getDurationMinutes());
                return new ClientSchedulePageDto.EventDto(
                        lesson.getId().toString(),
                        lesson.getId().toString(),
                        lesson.getApplicationId().toString(),
                        thread == null ? null : thread.getId().toString(),
                        lesson.getSubjectName(),
                        lesson.getTutorName(),
                        TIME_FORMAT.format(lesson.getStartsAt()),
                        lesson.getStartsAt().toString(),
                        endsAt.toString(),
                        "indigo",
                        dayIndex + 1,
                        row,
                        true,
                        true,
                        thread != null
                );
            })
            .toList();

    List<ClientSchedulePageDto.RescheduleDto> rescheduleDtos = requests.stream()
            .map(request -> new ClientSchedulePageDto.RescheduleDto(
                    request.getId().toString(),
                    request.getLessonId().toString(),
                    TIME_FORMAT.format(request.getLessonStartsAt()),
                    request.getLessonTitle(),
                    "PENDING".equals(request.getStatus()) ? "ожидает ответа" : "обновлено",
                    "PENDING".equals(request.getStatus()) ? "orange" : "blue"
            ))
            .toList();

    List<ClientSchedulePageDto.UpcomingLessonDto> upcoming = lessons.stream()
            .filter(lesson -> lesson.getStartsAt().isAfter(LocalDateTime.now().minusHours(1)))
            .limit(2)
            .map(lesson -> {
                MessageThread thread = messageThreadRepository.findByApplicationId(lesson.getApplicationId()).orElse(null);
                return new ClientSchedulePageDto.UpcomingLessonDto(
                        lesson.getId().toString(),
                        lesson.getId().toString(),
                        lesson.getApplicationId().toString(),
                        thread == null ? null : thread.getId().toString(),
                        TIME_FORMAT.format(lesson.getStartsAt()),
                        lesson.getSubjectName(),
                        lesson.getTutorName() + " · " + lesson.getFormat().toLowerCase(RU),
                        true,
                        true,
                        thread != null
                );
            })
            .toList();

    return new ClientSchedulePageDto(
            List.of(
                    new ClientSchedulePageDto.MetricDto("С", String.valueOf(lessons.stream().filter(lesson -> lesson.getStartsAt().toLocalDate().equals(LocalDate.now())).count()), "Сегодня", "урока", "indigo"),
                    new ClientSchedulePageDto.MetricDto("Н", String.valueOf(lessons.size()), "Неделя", "занятий", "violet"),
                    new ClientSchedulePageDto.MetricDto("Д", "0", "Домашка", "задания", "amber"),
                    new ClientSchedulePageDto.MetricDto("₽", rescheduleDtos.isEmpty() ? "ok" : String.valueOf(rescheduleDtos.size()), "Переносы", rescheduleDtos.isEmpty() ? "не требуются" : "ожидают", "emerald")
            ),
            new ClientSchedulePageDto.PeriodDto(weekStart.toString(), weekStart.plusDays(4).toString(), weekStart.format(DAY_FORMAT) + "–" + weekStart.plusDays(4).format(DAY_FORMAT)),
            buildClientWeekDays(weekStart),
            List.of("12:00", "14:00", "16:00", "18:00", "20:00"),
            events,
            rescheduleDtos,
            buildMiniMonthDays(weekStart),
            upcoming,
            new ClientSchedulePageDto.EmptyStateDto(lessons.isEmpty(), "Уроков пока нет", "Когда репетитор создаст урок, он появится в расписании.")
    );
}
```

Update `frontend/FRONTEND_BACKEND_CONTRACT.md` in the `GET /api/client/schedule` section so the documented contract matches the expanded DTO fields and the page is no longer described as static.

- [ ] **Step 4: Run the focused read-model test to verify it passes**

Run: `mvn test -Dtest=PageViewServiceTest`

Workdir: `backend/crm-service`

Expected: PASS with 3 tests run and `BUILD SUCCESS`.

### Task 3: Wire the frontend page to the live DTO and real actions

**Files:**
- Modify: `frontend/src/shared/types.ts`
- Modify: `frontend/src/shared/api/client.ts`
- Create: `frontend/src/shared/api/client.test.ts`
- Create: `frontend/src/pages/app/client-schedule-helpers.ts`
- Create: `frontend/src/pages/app/client-schedule-helpers.test.ts`
- Modify: `frontend/src/pages/app/ClientSchedulePage.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Write the failing frontend unit tests**

Create `frontend/src/shared/api/client.test.ts`:

```ts
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { apiRequest } = vi.hoisted(() => ({
  apiRequest: vi.fn(),
}));

vi.mock('./http', () => ({ apiRequest }));

import { getClientSchedulePage, requestClientLessonReschedule } from './client';

describe('client schedule API', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    apiRequest.mockResolvedValue({ ok: true });
  });

  it('requests the live schedule page DTO from /client/schedule', async () => {
    await getClientSchedulePage();

    expect(apiRequest).toHaveBeenCalledWith('/client/schedule', undefined, expect.anything());
  });

  it('posts a reschedule request to the lesson action endpoint', async () => {
    await requestClientLessonReschedule('lesson-1', {
      message: 'Можно на 19:00?',
      proposedStartAt: '2026-05-07T19:00:00',
    });

    expect(apiRequest).toHaveBeenCalledWith(
      '/client/schedule/lessons/lesson-1/reschedule-request',
      {
        method: 'POST',
        body: JSON.stringify({ message: 'Можно на 19:00?', proposedStartAt: '2026-05-07T19:00:00' }),
      },
      expect.any(Function),
    );
  });
});
```

Create `frontend/src/pages/app/client-schedule-helpers.test.ts`:

```ts
import { describe, expect, it } from 'vitest';

import { getClientSchedulePrimaryLink, shouldShowClientScheduleEmptyState } from './client-schedule-helpers';

describe('client schedule helpers', () => {
  it('prefers application details when applicationId is present', () => {
    expect(getClientSchedulePrimaryLink({ applicationId: 'app-1', threadId: 'thread-1' })).toBe('/app/client/applications/app-1');
  });

  it('falls back to messages when only threadId is present', () => {
    expect(getClientSchedulePrimaryLink({ threadId: 'thread-1' })).toBe('/app/client/messages');
  });

  it('shows the empty state only when there are no events, lessons, or reschedule requests', () => {
    expect(shouldShowClientScheduleEmptyState({ events: [], upcomingLessons: [], rescheduleRequests: [] })).toBe(true);
    expect(shouldShowClientScheduleEmptyState({ events: [{ id: 'evt-1' }], upcomingLessons: [], rescheduleRequests: [] })).toBe(false);
  });
});
```

- [ ] **Step 2: Run the focused frontend tests to verify they fail**

Run: `npm test -- client.test.ts client-schedule-helpers.test.ts`

Workdir: `frontend`

Expected: FAIL with missing exports for `requestClientLessonReschedule`, `getClientSchedulePrimaryLink`, and `shouldShowClientScheduleEmptyState`.

- [ ] **Step 3: Extend types, API helpers, page logic, and styles**

Modify `frontend/src/shared/types.ts` so `ClientSchedulePageData` can drive real CTA and empty states:

```ts
export type ClientSchedulePageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    caption: string;
    tone: 'indigo' | 'violet' | 'amber' | 'emerald';
  }>;
  period: {
    from: string;
    to: string;
    label: string;
  };
  weekDays: Array<{
    day: string;
    date: string;
    active?: boolean;
  }>;
  timeSlots: string[];
  events: Array<{
    id: string;
    lessonId: string;
    applicationId?: string | null;
    threadId?: string | null;
    title: string;
    meta: string;
    time: string;
    startsAt: string;
    endsAt: string;
    tone: 'indigo' | 'violet' | 'blue' | 'green' | 'orange';
    column: number;
    row: number;
    canJoin: boolean;
    canReschedule: boolean;
    canMessageTutor: boolean;
  }>;
  rescheduleRequests: Array<{
    id: string;
    lessonId: string;
    time: string;
    title: string;
    status: string;
    tone: 'orange' | 'blue';
  }>;
  miniMonthDays: Array<{
    date: string;
    active?: boolean;
  }>;
  upcomingLessons: Array<{
    id: string;
    lessonId: string;
    applicationId?: string | null;
    threadId?: string | null;
    time: string;
    title: string;
    details: string;
    canJoin: boolean;
    canReschedule: boolean;
    canMessageTutor: boolean;
  }>;
  emptyState: {
    show: boolean;
    title: string;
    text: string;
  };
};
```

Create `frontend/src/pages/app/client-schedule-helpers.ts`:

```ts
export function getClientSchedulePrimaryLink(input: { applicationId?: string | null; threadId?: string | null }) {
  if (input.applicationId) {
    return `/app/client/applications/${input.applicationId}`;
  }

  if (input.threadId) {
    return '/app/client/messages';
  }

  return '/app/client/applications';
}

export function shouldShowClientScheduleEmptyState(input: {
  events: Array<{ id: string }>;
  upcomingLessons: Array<{ id: string }>;
  rescheduleRequests: Array<{ id: string }>;
}) {
  return input.events.length === 0 && input.upcomingLessons.length === 0 && input.rescheduleRequests.length === 0;
}
```

Modify `frontend/src/shared/api/client.ts`:

```ts
export function getClientSchedulePage() {
  return apiRequest<ClientSchedulePageData>('/client/schedule', undefined, () => clientSchedulePageFallback);
}

export function requestClientLessonReschedule(
  lessonId: string,
  payload: { message: string; proposedStartAt?: string },
) {
  return apiRequest<{ id: string; status: string }>(
    `/client/schedule/lessons/${lessonId}/reschedule-request`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    () => ({ id: `reschedule-${Date.now().toString(36)}`, status: 'PENDING' }),
  );
}
```

Modify `frontend/src/pages/app/ClientSchedulePage.tsx` so the page uses explicit loading/error state, refetches on sync, routes CTA from live IDs, shows empty states, and posts reschedule requests:

```tsx
const emergencyFallback: ClientSchedulePageData = {
  metrics: [],
  period: { from: '', to: '', label: 'Неделя' },
  weekDays: [],
  timeSlots: ['12:00', '14:00', '16:00', '18:00', '20:00'],
  events: [],
  rescheduleRequests: [],
  miniMonthDays: [],
  upcomingLessons: [],
  emptyState: {
    show: true,
    title: 'Не удалось загрузить расписание',
    text: 'Повторите попытку синхронизации чуть позже.',
  },
};

const [pageData, setPageData] = useState<ClientSchedulePageData>(emergencyFallback);
const [isLoading, setIsLoading] = useState(true);
const [isSubmitting, setIsSubmitting] = useState(false);
const [error, setError] = useState<string | null>(null);

async function loadPage() {
  setIsLoading(true);
  setError(null);
  try {
    setPageData(await getClientSchedulePage());
  } catch (loadError) {
    setPageData(emergencyFallback);
    setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить расписание.');
  } finally {
    setIsLoading(false);
  }
}

useEffect(() => {
  void loadPage();
}, []);

async function handleReschedule(lessonId: string) {
  setIsSubmitting(true);
  setError(null);
  try {
    await requestClientLessonReschedule(lessonId, { message: 'Нужен перенос занятия.' });
    await loadPage();
  } catch (submitError) {
    setError(submitError instanceof Error ? submitError.message : 'Не удалось отправить запрос на перенос.');
  } finally {
    setIsSubmitting(false);
  }
}

const nextLesson = pageData.upcomingLessons[0] ?? null;
const primaryLessonLink = nextLesson ? getClientSchedulePrimaryLink(nextLesson) : '/app/client/applications';
const showEmptyState = shouldShowClientScheduleEmptyState(pageData);
```

Update the relevant JSX branches so:

- loading renders a calm loading state;
- `error` renders an inline message;
- empty screen uses `pageData.emptyState.title` and `pageData.emptyState.text`;
- `Синхронизировать` calls `loadPage()`;
- `Открыть урок` links to `primaryLessonLink`;
- `Запросить перенос` calls `handleReschedule(nextLesson.lessonId)` and respects `isSubmitting`;
- `Написать репетитору` only shows when `nextLesson?.canMessageTutor` is true;
- fake `Окно / своб.` and fake homework summary are removed or replaced by empty-state copy.

Modify `frontend/src/styles/global.css` to support:

```css
.schedule-empty-state,
.schedule-inline-error,
.schedule-loading-state {
  border: 1px solid color-mix(in oklab, var(--border) 85%, white);
  border-radius: 24px;
  background: var(--surface);
  padding: 24px;
}

.schedule-inline-error {
  color: var(--text-strong);
  background: color-mix(in oklab, var(--surface) 80%, #fff4f4);
}

.schedule-reschedule-action[disabled] {
  opacity: 0.6;
  cursor: wait;
}
```

- [ ] **Step 4: Run the focused frontend tests to verify they pass**

Run: `npm test -- client.test.ts client-schedule-helpers.test.ts`

Workdir: `frontend`

Expected: PASS with both files green.

### Task 4: Verify the full slice end to end

**Files:**
- Verify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/ClientCrmController.java`
- Verify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`
- Verify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/ClientSchedulePageDto.java`
- Verify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleRequest.java`
- Verify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonRescheduleService.java`
- Verify: `frontend/src/shared/types.ts`
- Verify: `frontend/src/shared/api/client.ts`
- Verify: `frontend/src/pages/app/client-schedule-helpers.ts`
- Verify: `frontend/src/pages/app/ClientSchedulePage.tsx`
- Verify: `frontend/src/styles/global.css`

- [ ] **Step 1: Run the focused backend suite**

Run: `mvn test -Dtest=LessonRescheduleServiceTest,PageViewServiceTest`

Workdir: `backend/crm-service`

Expected: PASS with `BUILD SUCCESS`.

- [ ] **Step 2: Run the focused frontend suite**

Run: `npm test -- client.test.ts client-schedule-helpers.test.ts`

Workdir: `frontend`

Expected: PASS.

- [ ] **Step 3: Run the frontend build**

Run: `npm run build`

Workdir: `frontend`

Expected: successful TypeScript check and Rollup bundle build.

- [ ] **Step 4: Rebuild and restart the full local stack**

Run: `docker compose up --build -d`

Workdir: `C:\JavaMy\newDiploma`

Expected: all containers `Up` in `docker compose ps`.

- [ ] **Step 5: Verify the screen manually through DevTools MCP**

Open: `http://127.0.0.1:4173/app/client/schedule`

Verify:

- sign in as a client if the route guard redirects to `/auth/sign-in`;
- the page loads live data instead of decorative placeholders;
- `Синхронизировать` triggers a refresh without breaking layout;
- `Открыть урок` and `Написать репетитору` route to working destinations;
- `Запросить перенос` creates a pending request and the side panel updates;
- empty-state copy still looks correct for accounts with no lessons.

- [ ] **Step 6: Update the plan checklist inline if verification required any small follow-up fixes**

If anything fails, fix only the failing slice, then rerun the exact verification command that proves the claim.
