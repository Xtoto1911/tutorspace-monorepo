# Tutor Calendar Platform Student Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let tutors create a lesson from an already accepted platform application directly inside calendar cells while preserving the existing manual booking flow for off-platform students.

**Architecture:** Keep lesson creation on the existing `POST /api/tutor/lessons` contract and add one narrow read endpoint for accepted lesson candidates. Backend filters accepted applications that already have a `StudentCard` and no linked lesson; frontend consumes that list in both lesson-creation modals through a mode switch: `Из принятых заявок` or `Вручную`.

**Tech Stack:** Spring Boot 3.5.10, Java 21, JUnit 5, Mockito, React 19, TypeScript 5, Vitest.

---

## File Structure

**New Files:**
- `backend/crm-service/src/main/java/com/tutor/crmservice/view/TutorLessonCandidateDto.java` — DTO for accepted applications that can still be turned into a lesson.
- `frontend/src/pages/app/tutor-calendar-helpers.ts` — small pure helpers for building payloads and deriving modal state.
- `frontend/src/pages/app/tutor-calendar-helpers.test.ts` — unit tests for helper behavior.

**Modified Files:**
- `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java` — builds the candidate list from accepted applications, student cards, and existing lessons.
- `backend/crm-service/src/main/java/com/tutor/crmservice/controller/TutorCrmController.java` — exposes `GET /api/tutor/lesson-candidates`.
- `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java` — covers accepted-candidate filtering and duplicate exclusion.
- `frontend/src/shared/types.ts` — adds `TutorLessonCandidate`.
- `frontend/src/shared/api/tutor.ts` — adds `getTutorLessonCandidates()`.
- `frontend/src/shared/api/tutor.test.ts` — verifies the new request and fallback behavior.
- `frontend/src/pages/app/TutorCalendarPage.tsx` — loads candidates, adds source-mode switch, and submits either `applicationId` or manual fields in both modals.

**Verification Commands:**
- Backend narrow test: `mvn test "-Dtest=PageViewServiceTest" "-DfailIfNoTests=false"`
- Frontend API/helper tests: `npm test -- tutor.test.ts tutor-calendar-helpers.test.ts`
- Frontend type safety: `npm run typecheck`

**Checkpoint note:** `C:\JavaMy\newDiploma` is not currently exposed as a git worktree, so each task ends with a verification checkpoint instead of a mandatory commit. If git becomes available during execution, use the suggested commit messages.

---

## Task 1: Add backend lesson-candidate projection

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/view/TutorLessonCandidateDto.java`
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`

- [ ] **Step 1: Write the failing backend test for candidate filtering**

Add this test to `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`:

```java
@Test
void getTutorLessonCandidates_returnsAcceptedApplicationsWithoutLessons() {
    PageViewService service = createService();
    UUID tutorId = UUID.randomUUID();

    UUID acceptedId = UUID.randomUUID();
    CrmApplication accepted = new CrmApplication();
    accepted.setId(acceptedId);
    accepted.setTutorUserId(tutorId);
    accepted.setStudentName("Иван Иванов");
    accepted.setStudentGrade("8 класс");
    accepted.setGoal("подготовка к ОГЭ");
    accepted.setSubjectName("Математика");
    accepted.setFormat("Онлайн");
    accepted.setDesiredStartTime(LocalDateTime.of(2026, 5, 8, 17, 0));
    accepted.setStatus(ApplicationStatus.ACCEPTED);

    UUID bookedId = UUID.randomUUID();
    CrmApplication booked = new CrmApplication();
    booked.setId(bookedId);
    booked.setTutorUserId(tutorId);
    booked.setStudentName("Мария Петрова");
    booked.setStudentGrade("7 класс");
    booked.setGoal("повысить успеваемость");
    booked.setSubjectName("Физика");
    booked.setFormat("Онлайн");
    booked.setDesiredStartTime(LocalDateTime.of(2026, 5, 9, 16, 0));
    booked.setStatus(ApplicationStatus.ACCEPTED);

    CrmApplication rejected = new CrmApplication();
    rejected.setId(UUID.randomUUID());
    rejected.setTutorUserId(tutorId);
    rejected.setStudentName("Не Должен Попасть");
    rejected.setStudentGrade("9 класс");
    rejected.setGoal("тест");
    rejected.setSubjectName("Химия");
    rejected.setFormat("Офлайн");
    rejected.setDesiredStartTime(LocalDateTime.of(2026, 5, 10, 15, 0));
    rejected.setStatus(ApplicationStatus.REJECTED);

    StudentCard acceptedCard = new StudentCard();
    acceptedCard.setId(UUID.randomUUID());
    acceptedCard.setApplicationId(acceptedId);
    acceptedCard.setTutorUserId(tutorId);
    acceptedCard.setClientUserId(UUID.randomUUID());
    acceptedCard.setName("Иван Иванов");
    acceptedCard.setDetails("8 класс");
    acceptedCard.setSubject("Математика");
    acceptedCard.setStatus("Активен");
    acceptedCard.setNote("Карточка создана");
    acceptedCard.setProgress(25);

    StudentCard bookedCard = new StudentCard();
    bookedCard.setId(UUID.randomUUID());
    bookedCard.setApplicationId(bookedId);
    bookedCard.setTutorUserId(tutorId);
    bookedCard.setClientUserId(UUID.randomUUID());
    bookedCard.setName("Мария Петрова");
    bookedCard.setDetails("7 класс");
    bookedCard.setSubject("Физика");
    bookedCard.setStatus("Активен");
    bookedCard.setNote("Карточка создана");
    bookedCard.setProgress(30);

    Lesson existingLesson = new Lesson();
    existingLesson.setId(UUID.randomUUID());
    existingLesson.setApplicationId(bookedId);
    existingLesson.setTutorUserId(tutorId);
    existingLesson.setStartsAt(LocalDateTime.of(2026, 5, 9, 16, 0));
    existingLesson.setDurationMinutes(60);

    when(crmApplicationRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorId))
            .thenReturn(List.of(accepted, booked, rejected));
    when(studentCardRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorId))
            .thenReturn(List.of(acceptedCard, bookedCard));
    when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId))
            .thenReturn(List.of(existingLesson));

    List<TutorLessonCandidateDto> candidates = service.getTutorLessonCandidates(tutorId);

    assertEquals(1, candidates.size());
    assertEquals(acceptedId.toString(), candidates.get(0).applicationId());
    assertEquals(acceptedCard.getId().toString(), candidates.get(0).studentId());
    assertEquals("Иван Иванов", candidates.get(0).studentName());
    assertEquals("Математика", candidates.get(0).subject());
    assertEquals("8 класс · подготовка к ОГЭ", candidates.get(0).gradeGoal());
}
```

- [ ] **Step 2: Run the backend test and confirm it fails because the DTO/service method do not exist yet**

Run from `backend/crm-service`:

```powershell
mvn test "-Dtest=PageViewServiceTest" "-DfailIfNoTests=false"
```

Expected: compilation failure mentioning `TutorLessonCandidateDto` and/or `getTutorLessonCandidates`.

- [ ] **Step 3: Create the DTO**

Create `backend/crm-service/src/main/java/com/tutor/crmservice/view/TutorLessonCandidateDto.java`:

```java
package com.tutor.crmservice.view;

public record TutorLessonCandidateDto(
        String applicationId,
        String studentId,
        String studentName,
        String subject,
        String format,
        String desiredStartTime,
        String gradeGoal
) {
}
```

- [ ] **Step 4: Implement candidate filtering in `PageViewService`**

Add this method to `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`:

```java
@Transactional(readOnly = true)
public List<TutorLessonCandidateDto> getTutorLessonCandidates(UUID tutorUserId) {
    List<CrmApplication> applications = crmApplicationRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId);
    Map<UUID, StudentCard> cardsByApplicationId = studentCardRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId)
            .stream()
            .collect(Collectors.toMap(StudentCard::getApplicationId, Function.identity(), (left, right) -> left));
    java.util.Set<UUID> bookedApplicationIds = lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorUserId)
            .stream()
            .map(Lesson::getApplicationId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());

    return applications.stream()
            .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
            .filter(application -> !bookedApplicationIds.contains(application.getId()))
            .filter(application -> cardsByApplicationId.containsKey(application.getId()))
            .map(application -> {
                StudentCard card = cardsByApplicationId.get(application.getId());
                return new TutorLessonCandidateDto(
                        application.getId().toString(),
                        card.getId().toString(),
                        card.getName(),
                        application.getSubjectName(),
                        application.getFormat(),
                        application.getDesiredStartTime().toString(),
                        application.getStudentGrade() + " · " + application.getGoal()
                );
            })
            .toList();
}
```

- [ ] **Step 5: Re-run the backend test and confirm it passes**

Run from `backend/crm-service`:

```powershell
mvn test "-Dtest=PageViewServiceTest" "-DfailIfNoTests=false"
```

Expected: `BUILD SUCCESS` and `Tests run:` output for `PageViewServiceTest`.

- [ ] **Step 6: Verification checkpoint**

If git becomes available:

```powershell
git add src/main/java/com/tutor/crmservice/view/TutorLessonCandidateDto.java src/main/java/com/tutor/crmservice/view/PageViewService.java src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java
git commit -m "feat(crm): add tutor lesson candidates"
```

If git is still unavailable, record the passing `PageViewServiceTest` output in the task notes.

---

## Task 2: Expose the lesson-candidates endpoint

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/TutorCrmController.java`
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`

- [ ] **Step 1: Add a narrow controller method**

Insert this method into `backend/crm-service/src/main/java/com/tutor/crmservice/controller/TutorCrmController.java`:

```java
@GetMapping("/lesson-candidates")
public List<TutorLessonCandidateDto> getLessonCandidates(@AuthenticationPrincipal JwtPrincipal principal) {
    return pageViewService.getTutorLessonCandidates(principal.userId());
}
```

Add the import:

```java
import com.tutor.crmservice.view.TutorLessonCandidateDto;
```

- [ ] **Step 2: Run compile-level backend verification**

Run from `backend/crm-service`:

```powershell
mvn test "-Dtest=PageViewServiceTest" "-DfailIfNoTests=false"
```

Expected: `BUILD SUCCESS`; no new compile errors in `TutorCrmController`.

- [ ] **Step 3: Verification checkpoint**

If git becomes available:

```powershell
git add src/main/java/com/tutor/crmservice/controller/TutorCrmController.java
git commit -m "feat(crm): expose tutor lesson candidates endpoint"
```

If git is still unavailable, capture the successful Maven output in the task notes.

---

## Task 3: Add frontend API contract for lesson candidates

**Files:**
- Modify: `frontend/src/shared/types.ts`
- Modify: `frontend/src/shared/api/tutor.ts`
- Modify: `frontend/src/shared/api/tutor.test.ts`

- [ ] **Step 1: Write the failing frontend API test**

Add this test to `frontend/src/shared/api/tutor.test.ts`:

```ts
it('fetches tutor lesson candidates', async () => {
  await getTutorLessonCandidates();
  expect(apiRequest).toHaveBeenCalledWith(
    '/tutor/lesson-candidates',
    undefined,
    expect.any(Function),
  );
  const fallbackFactory = apiRequest.mock.calls[0]?.[2];
  expect(fallbackFactory()).toEqual([]);
});
```

Update the import list at the top of the test file:

```ts
import {
  getTutorAvailabilitySchedules,
  createTutorAvailabilitySchedule,
  updateTutorAvailabilitySchedule,
  deleteTutorAvailabilitySchedule,
  getTutorSlotOverrides,
  createTutorSlotOverride,
  deleteTutorSlotOverride,
  getTutorCalendarSlots,
  getTutorLessonCandidates,
} from './tutor';
```

- [ ] **Step 2: Run the frontend test and confirm it fails**

Run from `frontend`:

```powershell
npm test -- tutor.test.ts
```

Expected: failure because `getTutorLessonCandidates` is not exported yet.

- [ ] **Step 3: Add the shared type**

Append this type to `frontend/src/shared/types.ts` near `TutorCalendarSlot`:

```ts
export type TutorLessonCandidate = {
  applicationId: string;
  studentId: string;
  studentName: string;
  subject: string;
  format: string;
  desiredStartTime: string;
  gradeGoal: string;
};
```

- [ ] **Step 4: Add the frontend API helper**

Add this function to `frontend/src/shared/api/tutor.ts`:

```ts
export function getTutorLessonCandidates() {
  return apiRequest<TutorLessonCandidate[]>('/tutor/lesson-candidates', undefined, () => []);
}
```

Also extend the import list in the same file:

```ts
import type {
  TutorCalendarPageData,
  TutorRequestsPageData,
  TutorMessagesPageData,
  TutorProfile,
  TutorSettingsPageData,
  TutorSubscriptionPageData,
  TutorTariffCheckoutPageData,
  TutorStudentsPageData,
  TutorWorkspaceProfilePageData,
  AvailabilitySchedule,
  SlotOverride,
  TutorCalendarSlot,
  TutorLessonCandidate,
} from '../types';
```

- [ ] **Step 5: Re-run the frontend API test**

Run from `frontend`:

```powershell
npm test -- tutor.test.ts
```

Expected: `PASS` for the new lesson-candidates API test and the existing tutor API tests.

- [ ] **Step 6: Verification checkpoint**

If git becomes available:

```powershell
git add src/shared/types.ts src/shared/api/tutor.ts src/shared/api/tutor.test.ts
git commit -m "feat(frontend): add lesson candidate api contract"
```

If git is still unavailable, note the successful Vitest run.

---

## Task 4: Add pure frontend helpers for mode-aware lesson payloads

**Files:**
- Create: `frontend/src/pages/app/tutor-calendar-helpers.ts`
- Create: `frontend/src/pages/app/tutor-calendar-helpers.test.ts`

- [ ] **Step 1: Write the helper tests first**

Create `frontend/src/pages/app/tutor-calendar-helpers.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import {
  buildCellLessonPayload,
  buildSlotLessonPayload,
} from './tutor-calendar-helpers';

describe('tutor calendar helpers', () => {
  it('builds a platform payload for a selected calendar cell', () => {
    expect(buildCellLessonPayload({
      mode: 'platform',
      applicationId: 'app-1',
      date: '2026-05-08',
      time: '17:00',
      durationMinutes: 60,
    })).toEqual({
      applicationId: 'app-1',
      startsAt: '2026-05-08T17:00:00',
      durationMinutes: 60,
      slot: null,
    });
  });

  it('builds a manual payload for a selected slot', () => {
    expect(buildSlotLessonPayload({
      mode: 'manual',
      studentName: 'Иван Иванов',
      subject: 'Математика',
      format: 'Онлайн',
      slotDate: '2026-05-08',
      slotStartTime: '17:00',
      slotEndTime: '18:30',
    })).toEqual({
      applicationId: null,
      startsAt: '2026-05-08T17:00:00',
      durationMinutes: 90,
      studentName: 'Иван Иванов',
      subject: 'Математика',
      format: 'Онлайн',
    });
  });
});
```

- [ ] **Step 2: Run the helper test and confirm it fails**

Run from `frontend`:

```powershell
npm test -- tutor-calendar-helpers.test.ts
```

Expected: module-not-found failure because the helper file does not exist yet.

- [ ] **Step 3: Implement the helper file**

Create `frontend/src/pages/app/tutor-calendar-helpers.ts`:

```ts
type PlatformCellInput = {
  mode: 'platform';
  applicationId: string;
  date: string;
  time: string;
  durationMinutes: number;
};

type ManualCellInput = {
  mode: 'manual';
  date: string;
  time: string;
  durationMinutes: number;
  studentName: string;
  subject: string;
  format: string;
};

type ManualSlotInput = {
  mode: 'manual';
  studentName: string;
  subject: string;
  format: string;
  slotDate: string;
  slotStartTime: string;
  slotEndTime: string;
};

type PlatformSlotInput = {
  mode: 'platform';
  applicationId: string;
  slotDate: string;
  slotStartTime: string;
  slotEndTime: string;
};

function getDurationMinutes(startTime: string, endTime: string) {
  const [startH, startM] = startTime.split(':').map(Number);
  const [endH, endM] = endTime.split(':').map(Number);
  return endH * 60 + endM - (startH * 60 + startM);
}

export function buildCellLessonPayload(input: PlatformCellInput | ManualCellInput) {
  const startsAt = `${input.date}T${input.time}:00`;
  if (input.mode === 'platform') {
    return {
      applicationId: input.applicationId,
      startsAt,
      durationMinutes: input.durationMinutes,
      slot: null,
    };
  }

  return {
    applicationId: null,
    startsAt,
    durationMinutes: input.durationMinutes,
    studentName: input.studentName,
    subject: input.subject,
    format: input.format,
  };
}

export function buildSlotLessonPayload(input: PlatformSlotInput | ManualSlotInput) {
  const startsAt = `${input.slotDate}T${input.slotStartTime}:00`;
  const durationMinutes = getDurationMinutes(input.slotStartTime, input.slotEndTime);
  if (input.mode === 'platform') {
    return {
      applicationId: input.applicationId,
      startsAt,
      durationMinutes,
    };
  }

  return {
    applicationId: null,
    startsAt,
    durationMinutes,
    studentName: input.studentName,
    subject: input.subject,
    format: input.format,
  };
}
```

- [ ] **Step 4: Re-run the helper tests**

Run from `frontend`:

```powershell
npm test -- tutor-calendar-helpers.test.ts
```

Expected: `PASS` for both helper scenarios.

- [ ] **Step 5: Verification checkpoint**

If git becomes available:

```powershell
git add src/pages/app/tutor-calendar-helpers.ts src/pages/app/tutor-calendar-helpers.test.ts
git commit -m "test(frontend): add tutor calendar payload helpers"
```

If git is still unavailable, record the successful Vitest run.

---

## Task 5: Integrate platform/manual mode into the “Новое занятие” modal

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Add state for lesson candidates and mode switching**

In `frontend/src/pages/app/TutorCalendarPage.tsx`, add imports:

```ts
import { buildCellLessonPayload, buildSlotLessonPayload } from './tutor-calendar-helpers';
```

Extend the API import:

```ts
import {
  createTutorAvailabilitySchedule,
  createTutorLesson,
  createTutorSlotOverride,
  getTutorCalendar,
  getTutorCalendarSlots,
  getTutorAvailabilitySchedules,
  getTutorLessonCandidates,
  updateTutorAvailabilitySchedule,
  deleteTutorAvailabilitySchedule,
} from '../../shared/api/tutor';
```

Add page state near the other hooks:

```ts
const [lessonCandidates, setLessonCandidates] = useState<TutorLessonCandidate[]>([]);
const [lessonMode, setLessonMode] = useState<'platform' | 'manual'>('platform');
const [bookSlotMode, setBookSlotMode] = useState<'platform' | 'manual'>('platform');
```

Also extend the type import:

```ts
import type { TutorCalendarPageData, AvailabilitySchedule, TutorLessonCandidate } from '../../shared/types';
```

- [ ] **Step 2: Load lesson candidates with the calendar data**

Inside `loadCalendarData`, replace the single request with:

```ts
try {
  const [data, candidates] = await Promise.all([
    getTutorCalendar(from, to),
    getTutorLessonCandidates(),
  ]);
  setPageData(data as TutorCalendarPageData);
  setLessonCandidates(candidates);
} catch {
  setPageData(tutorCalendarPage);
  setLessonCandidates([]);
} finally {
  setIsLoading(false);
}
```

- [ ] **Step 3: Switch the cell-creation submit handler to helper-based payloads**

Replace `handleCreateLessonSubmit` with:

```ts
const handleCreateLessonSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();
  const formData = new FormData(e.currentTarget);
  const durationMinutes = Number(formData.get('duration'));

  const payload = lessonMode === 'platform'
    ? buildCellLessonPayload({
        mode: 'platform',
        applicationId: String(formData.get('applicationId')),
        date: String(formData.get('date')),
        time: String(formData.get('time')),
        durationMinutes,
      })
    : buildCellLessonPayload({
        mode: 'manual',
        date: String(formData.get('date')),
        time: String(formData.get('time')),
        durationMinutes,
        studentName: String(formData.get('studentName')),
        subject: String(formData.get('subject')),
        format: String(formData.get('format')),
      });

  try {
    await createTutorLesson(payload);
    setShowLessonModal(false);
    setSelectedCell(null);
    setLessonMode('platform');
    await loadCalendarData(currentWeekStart);
  } catch (err) {
    alert('Не удалось создать занятие: ' + (err instanceof Error ? err.message : 'Unknown error'));
  }
};
```

- [ ] **Step 4: Update the modal markup**

Inside the `showLessonModal` form, add the source switch and conditional fields:

```tsx
<fieldset className="lesson-source-switch">
  <legend>Источник ученика</legend>
  <label>
    <input
      type="radio"
      name="lessonSource"
      checked={lessonMode === 'platform'}
      onChange={() => setLessonMode('platform')}
    />
    Из принятых заявок
  </label>
  <label>
    <input
      type="radio"
      name="lessonSource"
      checked={lessonMode === 'manual'}
      onChange={() => setLessonMode('manual')}
    />
    Вручную
  </label>
</fieldset>

{lessonMode === 'platform' ? (
  lessonCandidates.length === 0 ? (
    <p className="form-hint">Нет принятых заявок без назначенного урока. Можно переключиться на ручной режим.</p>
  ) : (
    <label>
      Ученик с платформы
      <select name="applicationId" required defaultValue={lessonCandidates[0]?.applicationId ?? ''}>
        {lessonCandidates.map((candidate) => (
          <option key={candidate.applicationId} value={candidate.applicationId}>
            {candidate.studentName} · {candidate.subject} · {candidate.format} · {candidate.gradeGoal}
          </option>
        ))}
      </select>
    </label>
  )
) : (
  <>
    <label>
      Имя ученика
      <input type="text" name="studentName" required placeholder="Иван Иванов" />
    </label>
    <label>
      Предмет
      <input type="text" name="subject" required placeholder="Математика" />
    </label>
    <label>
      Формат
      <select name="format" defaultValue="Онлайн">
        <option value="Онлайн">Онлайн</option>
        <option value="Офлайн">Офлайн</option>
      </select>
    </label>
  </>
)}
```

- [ ] **Step 5: Run frontend verification for the updated modal logic**

Run from `frontend`:

```powershell
npm test -- tutor-calendar-helpers.test.ts tutor.test.ts
npm run typecheck
```

Expected: Vitest passes and TypeScript reports no errors in `TutorCalendarPage.tsx`.

- [ ] **Step 6: Verification checkpoint**

If git becomes available:

```powershell
git add src/pages/app/TutorCalendarPage.tsx
git commit -m "feat(frontend): add platform candidates to lesson modal"
```

If git is still unavailable, store the passing typecheck/test output in the task notes.

---

## Task 6: Integrate platform/manual mode into free-slot booking

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Switch the free-slot submit handler to helper-based payloads**

Replace `handleBookSlot` in `frontend/src/pages/app/TutorCalendarPage.tsx` with:

```ts
const handleBookSlot = async (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();
  if (!selectedSlot) return;

  const formData = new FormData(e.currentTarget);
  const payload = bookSlotMode === 'platform'
    ? buildSlotLessonPayload({
        mode: 'platform',
        applicationId: String(formData.get('applicationId')),
        slotDate: selectedSlot.date,
        slotStartTime: selectedSlot.startTime.slice(0, 5),
        slotEndTime: selectedSlot.endTime.slice(0, 5),
      })
    : buildSlotLessonPayload({
        mode: 'manual',
        studentName: String(formData.get('studentName')),
        subject: String(formData.get('subject')),
        format: String(formData.get('format')),
        slotDate: selectedSlot.date,
        slotStartTime: selectedSlot.startTime.slice(0, 5),
        slotEndTime: selectedSlot.endTime.slice(0, 5),
      });

  try {
    await createTutorLesson(payload);
    setShowBookSlotModal(false);
    setSelectedSlot(null);
    setBookSlotMode('platform');
    await handleSync();
  } catch (err) {
    alert('Не удалось записать ученика: ' + (err instanceof Error ? err.message : 'Unknown error'));
  }
};
```

- [ ] **Step 2: Update the free-slot modal markup**

Replace the static manual fields inside `showBookSlotModal` with:

```tsx
<fieldset className="lesson-source-switch">
  <legend>Источник ученика</legend>
  <label>
    <input
      type="radio"
      name="slotSource"
      checked={bookSlotMode === 'platform'}
      onChange={() => setBookSlotMode('platform')}
    />
    Из принятых заявок
  </label>
  <label>
    <input
      type="radio"
      name="slotSource"
      checked={bookSlotMode === 'manual'}
      onChange={() => setBookSlotMode('manual')}
    />
    Вручную
  </label>
</fieldset>

{bookSlotMode === 'platform' ? (
  lessonCandidates.length === 0 ? (
    <p className="form-hint">Нет принятых заявок без урока. Для этого окна можно записать внешнего ученика вручную.</p>
  ) : (
    <label>
      Ученик с платформы
      <select name="applicationId" required defaultValue={lessonCandidates[0]?.applicationId ?? ''}>
        {lessonCandidates.map((candidate) => (
          <option key={candidate.applicationId} value={candidate.applicationId}>
            {candidate.studentName} · {candidate.subject} · {candidate.format} · {candidate.gradeGoal}
          </option>
        ))}
      </select>
    </label>
  )
) : (
  <>
    <label>
      Имя ученика
      <input type="text" name="studentName" required placeholder="Иван Иванов" />
    </label>
    <label>
      Предмет
      <input type="text" name="subject" required placeholder="Математика" />
    </label>
    <label>
      Формат
      <select name="format" defaultValue="Онлайн">
        <option value="Онлайн">Онлайн</option>
        <option value="Офлайн">Офлайн</option>
      </select>
    </label>
  </>
)}
```

- [ ] **Step 3: Reset modal state on close paths**

Whenever the slot modal closes, ensure these resets happen together:

```ts
setShowBookSlotModal(false);
setSelectedSlot(null);
setBookSlotMode('platform');
```

Apply the same reset pattern in:

- the `Escape` handler for slot modals;
- the modal backdrop/button cancel handler;
- the success branch after creating a lesson.

- [ ] **Step 4: Run final frontend verification**

Run from `frontend`:

```powershell
npm test -- tutor.test.ts tutor-calendar-helpers.test.ts
npm run typecheck
```

Expected: all targeted tests pass and the calendar page typechecks cleanly.

- [ ] **Step 5: Run final backend verification**

Run from `backend/crm-service`:

```powershell
mvn test "-Dtest=PageViewServiceTest" "-DfailIfNoTests=false"
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Manual smoke checklist**

Execute these manual checks after the code is in place:

```text
1. Open tutor calendar and click an empty cell.
2. Confirm the modal defaults to "Из принятых заявок" when candidates exist.
3. Create a lesson from a platform candidate and verify the candidate disappears after refresh/sync.
4. Re-open the modal and switch to "Вручную"; create a lesson for an off-platform student.
5. Open a free slot, repeat both flows, and confirm each one still creates exactly one lesson.
```

- [ ] **Step 7: Final checkpoint**

If git becomes available:

```powershell
git add src/pages/app/TutorCalendarPage.tsx
git commit -m "feat(frontend): support platform students in tutor calendar"
```

If git is still unavailable, capture the final Maven/Vitest/typecheck outputs and the smoke-test notes.

---

## Self-Review

**Spec coverage:** The plan covers the agreed endpoint, accepted-only filtering, duplicate exclusion, both modal flows, manual fallback, and the narrow verification commands required by the repo instructions.

**Placeholder scan:** No `TODO`, `TBD`, or “implement later” markers remain. Each code-changing step includes concrete snippets or exact commands.

**Type consistency:** The same names are used throughout: `TutorLessonCandidateDto`, `TutorLessonCandidate`, `getTutorLessonCandidates`, `buildCellLessonPayload`, and `buildSlotLessonPayload`.
