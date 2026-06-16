# Tutor Calendar: Recurrence Past-Date Bypass — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Закрыть бэкдор, при котором создание серии уроков (WEEKLY recurrence) в `app/tutor/calendar` позволяет разместить уроки в прошлом.

**Architecture:** Двухслойная защита по принципу defense-in-depth, но в минимальном объёме (только путь с recurrence, single-lesson не трогаем):
1. Backend (`LessonService.createTutorLesson`) — внутри recurrence-цикла проверяем, что `lessonStart` не раньше `LocalDateTime.now()`. Бросаем `ConflictException` иначе. Single-lesson путь (occurrences == 1) валидацию не проходит.
2. Frontend (`TutorCalendarPage` + `tutor-calendar-helpers`) — чистый helper `getRecurrenceValidationError`, который модалы «Новое занятие» и «Записать ученика» вызывают перед `createTutorLesson`. Показывает понятный `dialog.alert` и блокирует submit. Helper покрыт vitest-тестом.

**Tech Stack:** React 19 + TypeScript + Vitest (frontend), Spring Boot 3.5.10 + Java 21 + JUnit 5 + Mockito (backend).

**Branch:** работа ведётся на `fix/tutor-calendar-recurrence-past-date`, **не мерджим в `dev`** до явного указания пользователя.

---

## File Map

- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java` — добавить проверку `lessonStart.isBefore(LocalDateTime.now())` внутри recurrence-цикла (только при `occurrences > 1`).
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java` — добавить 3 теста: recurrence-в-прошлом бросает, single-в-прошлом НЕ бросает, recurrence-в-будущем НЕ бросает.
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.ts` — добавить экспортируемый helper `getRecurrenceValidationError`.
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.test.ts` — добавить тесты для нового helper.
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx` — вызвать helper в `handleCreateLessonSubmit` и `handleBookSlot` перед `createTutorLesson`.

---

## Task 1: Backend — failing test «recurrence в прошлом бросает ConflictException»

**Files:**
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java`

- [ ] **Step 1: Добавить тест `createLesson_withWeeklyRecurrenceStartingInPast_throwsConflict`**

Вставить в `LessonServiceTest.java` после `createLesson_withWeeklyRecurrenceForExistingStudent_createsSeries` (после строки 138). Использовать `LocalDateTime.now().minusDays(1)` как `startTime` (надёжно в прошлом, не зависит от таймзоны тестов):

```java
@Test
void createLesson_withWeeklyRecurrenceStartingInPast_throwsConflict() {
    UUID tutorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    StudentCard card = new StudentCard();
    card.setId(studentId);
    card.setTutorUserId(tutorId);
    card.setName("Мария");
    card.setSubject("Математика");

    when(studentCardRepository.findByIdAndTutorUserId(studentId, tutorId)).thenReturn(Optional.of(card));
    when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of());

    LocalDateTime pastStart = LocalDateTime.now().minusDays(1);

    assertThrows(ConflictException.class, () ->
            service.createTutorLesson(tutorId, new CreateLessonRequest(
                    null,
                    studentId,
                    pastStart,
                    60,
                    null,
                    null,
                    "Математика",
                    "Онлайн",
                    new CreateLessonRequest.RecurrenceRequest(CreateLessonRequest.RecurrenceType.WEEKLY, 4)
            )));
}
```

- [ ] **Step 2: Запустить тест, убедиться что падает**

Run: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest#createLesson_withWeeklyRecurrenceStartingInPast_throwsConflict`
Expected: FAIL с `ConflictException` НЕ выброшен (текущая реализация пропускает).

---

## Task 2: Backend — failing test «single lesson в прошлом НЕ бросает»

**Files:**
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java`

- [ ] **Step 1: Добавить тест `createLesson_singleLessonInPast_doesNotThrow`**

Вставить в `LessonServiceTest.java` после теста из Task 1:

```java
@Test
void createLesson_singleLessonInPast_doesNotThrow() {
    UUID tutorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    StudentCard card = new StudentCard();
    card.setId(studentId);
    card.setTutorUserId(tutorId);
    card.setName("Мария");
    card.setSubject("Математика");

    when(studentCardRepository.findByIdAndTutorUserId(studentId, tutorId)).thenReturn(Optional.of(card));
    when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of());
    when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

    LocalDateTime pastStart = LocalDateTime.now().minusDays(1);

    assertDoesNotThrow(() ->
            service.createTutorLesson(tutorId, new CreateLessonRequest(
                    null,
                    studentId,
                    pastStart,
                    60,
                    null,
                    null,
                    "Математика",
                    "Онлайн",
                    null
            )));
}
```

- [ ] **Step 2: Запустить тест, убедиться что проходит (sanity check)**

Run: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest#createLesson_singleLessonInPast_doesNotThrow`
Expected: PASS (recurrence null → occurrences == 1 → валидация не применяется).

---

## Task 3: Backend — failing test «recurrence в будущем НЕ бросает»

**Files:**
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java`

- [ ] **Step 1: Добавить тест `createLesson_weeklyRecurrenceStartingInFuture_doesNotThrow`**

Вставить в `LessonServiceTest.java` после теста из Task 2:

```java
@Test
void createLesson_weeklyRecurrenceStartingInFuture_doesNotThrow() {
    UUID tutorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    StudentCard card = new StudentCard();
    card.setId(studentId);
    card.setTutorUserId(tutorId);
    card.setName("Мария");
    card.setSubject("Математика");

    when(studentCardRepository.findByIdAndTutorUserId(studentId, tutorId)).thenReturn(Optional.of(card));
    when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of());
    when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

    LocalDateTime futureStart = LocalDateTime.now().plusSeconds(1);

    assertDoesNotThrow(() ->
            service.createTutorLesson(tutorId, new CreateLessonRequest(
                    null,
                    studentId,
                    futureStart,
                    60,
                    null,
                    null,
                    "Математика",
                    "Онлайн",
                    new CreateLessonRequest.RecurrenceRequest(CreateLessonRequest.RecurrenceType.WEEKLY, 4)
            )));
}
```

- [ ] **Step 2: Запустить тест, убедиться что проходит (sanity check)**

Run: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest#createLesson_weeklyRecurrenceStartingInFuture_doesNotThrow`
Expected: PASS.

---

## Task 4: Backend — реализовать валидацию в `LessonService.createTutorLesson`

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java`

- [ ] **Step 1: Заменить recurrence-цикл (строки 107–131) на версию с проверкой**

Найти в `LessonService.java`:

```java
        int occurrences = recurrenceOccurrences(request.recurrence());
        UUID seriesId = occurrences > 1 ? UUID.randomUUID() : null;
        Lesson firstSaved = null;

        for (int index = 0; index < occurrences; index++) {
            LocalDateTime lessonStart = startTime.plusWeeks(index);
            ensureNoOverlap(tutorUserId, lessonStart, duration, Set.of());
            Lesson saved = lessonRepository.save(buildLesson(
                    applicationId,
                    studentId,
                    clientUserId,
                    tutorUserId,
                    studentName,
                    tutorName,
                    title,
                    subjectName,
                    lessonStart,
                    duration,
                    format,
                    seriesId
            ));
            if (firstSaved == null) {
                firstSaved = saved;
            }
        }
```

Заменить на:

```java
        int occurrences = recurrenceOccurrences(request.recurrence());
        UUID seriesId = occurrences > 1 ? UUID.randomUUID() : null;
        Lesson firstSaved = null;
        LocalDateTime now = LocalDateTime.now();

        for (int index = 0; index < occurrences; index++) {
            LocalDateTime lessonStart = startTime.plusWeeks(index);
            if (occurrences > 1 && lessonStart.isBefore(now)) {
                throw new ConflictException("Нельзя создать серию уроков в прошлом.");
            }
            ensureNoOverlap(tutorUserId, lessonStart, duration, Set.of());
            Lesson saved = lessonRepository.save(buildLesson(
                    applicationId,
                    studentId,
                    clientUserId,
                    tutorUserId,
                    studentName,
                    tutorName,
                    title,
                    subjectName,
                    lessonStart,
                    duration,
                    format,
                    seriesId
            ));
            if (firstSaved == null) {
                firstSaved = saved;
            }
        }
```

Единственные изменения относительно оригинала:
- Добавлена строка `LocalDateTime now = LocalDateTime.now();` перед циклом.
- Добавлена проверка `if (occurrences > 1 && lessonStart.isBefore(now)) { throw ... }` первой строкой внутри цикла.

- [ ] **Step 2: Запустить все три теста из Tasks 1–3, убедиться что 1 и 3 проходят, 2 проходит**

Run: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest`
Expected: Все 3 новых теста + существующие тесты в `LessonServiceTest` PASS. BUILD SUCCESS.

- [ ] **Step 3: Запустить полный набор тестов crm-service (регрессия)**

Run: `mvn test -f backend/crm-service/pom.xml`
Expected: BUILD SUCCESS. Ни один из существующих тестов не сломан (single-lesson путь не затронут, проверка включается только при `occurrences > 1`).

- [ ] **Step 4: Закоммитить backend-часть**

Run:
```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java
git commit -m "fix(crm-service): reject weekly recurrence starting in the past"
```

---

## Task 5: Frontend — failing test для helper `getRecurrenceValidationError`

**Files:**
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.test.ts`

- [ ] **Step 1: Добавить тесты для нового helper**

В `frontend/src/pages/app/tutor-calendar-helpers.test.ts` в начало импортов (строка 3–14) добавить `getRecurrenceValidationError` в список импортов из `./tutor-calendar-helpers`. Вставить после строки 14:

```typescript
  getRecurrenceValidationError,
```

В конец `describe('tutor calendar helpers', ...)` (после строки 239, перед закрывающей `});`) добавить новый блок:

```typescript
  describe('getRecurrenceValidationError', () => {
    const now = new Date('2026-05-14T12:30:00');

    it('returns null for non-weekly recurrence even when start is in the past', () => {
      expect(getRecurrenceValidationError('2026-05-13', '20:00', 'NONE', now)).toBeNull();
    });

    it('returns null for weekly recurrence starting in the future', () => {
      expect(getRecurrenceValidationError('2026-05-15', '09:00', 'WEEKLY', now)).toBeNull();
    });

    it('returns null for weekly recurrence starting today in the future', () => {
      expect(getRecurrenceValidationError('2026-05-14', '18:00', 'WEEKLY', now)).toBeNull();
    });

    it('returns an error message for weekly recurrence starting in the past', () => {
      const error = getRecurrenceValidationError('2026-05-13', '20:00', 'WEEKLY', now);
      expect(error).not.toBeNull();
      expect(error).toMatch(/прошлом/);
    });

    it('returns an error message for weekly recurrence with today + past time', () => {
      const error = getRecurrenceValidationError('2026-05-14', '09:00', 'WEEKLY', now);
      expect(error).not.toBeNull();
      expect(error).toMatch(/прошлом/);
    });
  });
```

- [ ] **Step 2: Запустить тест, убедиться что падает**

Run: `cd frontend && npm test -- --run tutor-calendar-helpers.test.ts`
Expected: FAIL — `getRecurrenceValidationError is not a function` или «Cannot find module».

---

## Task 6: Frontend — реализовать helper `getRecurrenceValidationError`

**Files:**
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.ts`

- [ ] **Step 1: Добавить helper после `isCalendarCellInPast` (после строки 147)**

В `frontend/src/pages/app/tutor-calendar-helpers.ts` найти:

```typescript
export function isCalendarCellInPast(date: string, time: string, now = new Date()): boolean {
  return new Date(`${date}T${time}:00`).getTime() < now.getTime();
}
```

Сразу после этого блока добавить:

```typescript
export function getRecurrenceValidationError(
  date: string,
  time: string,
  recurrenceType: string,
  now: Date = new Date(),
): string | null {
  if (recurrenceType !== 'WEEKLY') {
    return null;
  }
  if (isCalendarCellInPast(date, time, now)) {
    return 'Нельзя назначить серию уроков в прошлое. Выберите будущую дату и время.';
  }
  return null;
}
```

- [ ] **Step 2: Запустить тесты, убедиться что проходят**

Run: `cd frontend && npm test -- --run tutor-calendar-helpers.test.ts`
Expected: PASS. Все 5 новых кейсов зелёные, существующие тесты не сломаны.

- [ ] **Step 3: Закоммитить helper**

Run:
```bash
git add frontend/src/pages/app/tutor-calendar-helpers.ts frontend/src/pages/app/tutor-calendar-helpers.test.ts
git commit -m "feat(tutor-calendar): add getRecurrenceValidationError helper"
```

---

## Task 7: Frontend — вызвать helper в `handleCreateLessonSubmit`

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Добавить import `getRecurrenceValidationError`**

В `frontend/src/pages/app/TutorCalendarPage.tsx` в блоке импортов из `./tutor-calendar-helpers` (строки 20–31) найти:

```typescript
import {
  buildCalendarWeekDays,
  buildCellLessonPayload,
  buildLessonReschedulePayload,
  buildSlotLessonPayload,
  formatLessonRequestStartTime,
  getLessonModalDefaults,
  getTutorCalendarGridPosition,
  getTutorCalendarDisplayStats,
  isCalendarCellInPast,
  type LessonRescheduleScope,
} from './tutor-calendar-helpers';
```

Заменить на:

```typescript
import {
  buildCalendarWeekDays,
  buildCellLessonPayload,
  buildLessonReschedulePayload,
  buildSlotLessonPayload,
  formatLessonRequestStartTime,
  getLessonModalDefaults,
  getRecurrenceValidationError,
  getTutorCalendarGridPosition,
  getTutorCalendarDisplayStats,
  isCalendarCellInPast,
  type LessonRescheduleScope,
} from './tutor-calendar-helpers';
```

- [ ] **Step 2: Добавить проверку в `handleCreateLessonSubmit` (строки 311–340)**

Найти начало `handleCreateLessonSubmit`:

```typescript
  const handleCreateLessonSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const recurrenceType = String(formData.get('recurrenceType'));
    const studentId = lessonMode === 'manual' && studentInputMode === 'existing'
      ? String(formData.get('studentId') ?? '')
      : '';
    const payload = buildCellLessonPayload({
```

Заменить на:

```typescript
  const handleCreateLessonSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const recurrenceType = String(formData.get('recurrenceType'));
    const date = String(formData.get('date'));
    const time = String(formData.get('time'));
    const recurrenceError = getRecurrenceValidationError(date, time, recurrenceType);
    if (recurrenceError) {
      await dialog.alert(recurrenceError);
      return;
    }
    const studentId = lessonMode === 'manual' && studentInputMode === 'existing'
      ? String(formData.get('studentId') ?? '')
      : '';
    const payload = buildCellLessonPayload({
```

И ниже в этом же обработчике заменить две строки:

```typescript
      date: String(formData.get('date')),
      time: String(formData.get('time')),
```

на:

```typescript
      date,
      time,
```

- [ ] **Step 3: Запустить typecheck и тесты**

Run: `cd frontend && npm run typecheck && npm test -- --run tutor-calendar-helpers.test.ts`
Expected: typecheck без ошибок, vitest PASS.

---

## Task 8: Frontend — вызвать helper в `handleBookSlot`

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Добавить проверку в `handleBookSlot` (строки 461–489)**

Найти начало `handleBookSlot`:

```typescript
  const handleBookSlot = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedSlot) return;
    const form = e.currentTarget;
    const formData = new FormData(form);
    const recurrenceType = String(formData.get('recurrenceType'));
    const studentId = studentInputMode === 'existing' ? String(formData.get('studentId') ?? '') : '';
    const payload = buildSlotLessonPayload({
```

Заменить на:

```typescript
  const handleBookSlot = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedSlot) return;
    const form = e.currentTarget;
    const formData = new FormData(form);
    const recurrenceType = String(formData.get('recurrenceType'));
    const slotDate = selectedSlot.date;
    const slotTime = selectedSlot.startTime.slice(0, 5);
    const recurrenceError = getRecurrenceValidationError(slotDate, slotTime, recurrenceType);
    if (recurrenceError) {
      await dialog.alert(recurrenceError);
      return;
    }
    const studentId = studentInputMode === 'existing' ? String(formData.get('studentId') ?? '') : '';
    const payload = buildSlotLessonPayload({
```

И ниже в этом же обработчике заменить:

```typescript
      slotDate: selectedSlot.date,
      slotStartTime: selectedSlot.startTime.slice(0, 5),
```

на:

```typescript
      slotDate,
      slotStartTime: slotTime,
```

- [ ] **Step 2: Запустить typecheck и тесты**

Run: `cd frontend && npm run typecheck && npm test`
Expected: typecheck без ошибок, vitest PASS (все существующие тесты + новые 5 кейсов для `getRecurrenceValidationError`).

- [ ] **Step 3: Закоммитить frontend-часть**

Run:
```bash
git add frontend/src/pages/app/TutorCalendarPage.tsx
git commit -m "fix(tutor-calendar): block weekly recurrence submissions with past start time"
```

---

## Task 9: Финальная проверка

- [ ] **Step 1: Прогнать backend тесты целиком**

Run: `mvn test -f backend/crm-service/pom.xml`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Прогнать frontend typecheck и тесты**

Run: `cd frontend && npm run typecheck && npm test`
Expected: typecheck OK, vitest PASS.

- [ ] **Step 3: Прогнать frontend build (если есть `npm run build`)**

Run: `cd frontend && npm run build`
Expected: успешный build без ошибок. (По AGENTS.md: `npm run build` = `tsc --noEmit -p tsconfig.json && node scripts/build.mjs`.)

- [ ] **Step 4: Просмотреть `git log --oneline` на текущей ветке**

Run: `git log --oneline dev..HEAD`
Expected: 3 новых коммита:
1. `fix(crm-service): reject weekly recurrence starting in the past`
2. `feat(tutor-calendar): add getRecurrenceValidationError helper`
3. `fix(tutor-calendar): block weekly recurrence submissions with past start time`

- [ ] **Step 5: НЕ мерджить в dev**

Ждать явного указания пользователя. Ветка остаётся `fix/tutor-calendar-recurrence-past-date` локально.

---

## Notes

- `isCalendarCellInPast` переиспользуется внутри `getRecurrenceValidationError` — поведение «дата + время в прошлом» уже покрыто существующим тестом `marks only past calendar cells as unavailable for booking`.
- Backend-проверка стоит внутри цикла (а не только для `index == 0`) сознательно: защита от clock skew между моментом вычисления `now` и длинными сериями (до 52 недель) при медленных запросах. Практически для индекса 0 проверка достаточна, но затраты минимальны.
- Single-lesson путь и путь переноса урока (`rescheduleTutorLesson`) намеренно не затронуты — соответствует выбору пользователя «Только recurrence».
- Stash с изменениями `.opencode/agens/*` (коммит `opencode agens config rewrite`) лежит отдельно и не относится к этой задаче. Восстанавливать через `git stash pop` на `dev` по решению пользователя.
