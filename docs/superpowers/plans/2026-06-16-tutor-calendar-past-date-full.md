# Full Past-Date Bypass Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Полностью закрыть past-date байпас в `app/tutor/calendar` на 2 уровнях: одиночный урок в прошлое, reschedule в прошлое, любой путь backend'а (slot/startsAt/application) — после предыдущих частичных фиксов.

**Architecture:** Заменить узкий `getRecurrenceValidationError` на общий `getLessonStartValidationError(date, time, kind, now)`, где `kind: 'create' | 'recurrence' | 'reschedule'`. Подключить helper в 3 handler'ах (`handleCreateLessonSubmit`, `handleBookSlot`, `handleRescheduleLesson`). В backend заменить две точечные проверки (recurrence + slot) на одну общую после вычисления `startTime` в `createTutorLesson`; добавить раннюю проверку в `rescheduleTutorLesson`.

**Tech Stack:** React 19 + TypeScript + Vitest (frontend), Spring Boot 3.5.10 + Java 21 + JUnit 5 + Mockito (backend).

**Branch:** работа ведётся на `fix/tutor-calendar-recurrence-past-date`, в `dev` не мерджим.

**Reference spec:** `docs/superpowers/specs/2026-06-16-tutor-calendar-past-date-full-design.md`

---

## File Map

- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java` — убрать recurrence-специфичную проверку в цикле, убрать slot-специфичную проверку, добавить общую `if (startTime.isBefore(now))` после вычисления startTime, добавить раннюю проверку в `rescheduleTutorLesson`.
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java` — переименовать single-в-прошлом тест, добавить 2 новых (application-past и reschedule-past), бампнуть fixture 2026→2027 в `createLesson_nonOverlapping_ok`.
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.ts` — добавить `LessonStartKind` type + `getLessonStartValidationError`; позже удалить старый `getRecurrenceValidationError`.
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.test.ts` — переименовать describe, обновить 5 кейсов, добавить 3.
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx` — обновить import, обновить 2 call site'а (handleCreateLessonSubmit, handleBookSlot), добавить валидацию в handleRescheduleLesson.

---

## Task 1: Backend — failing test «single lesson в прошлом бросает»

**Files:**
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java`

- [ ] **Step 1: Переименовать существующий тест и инвертировать ожидание**

В `LessonServiceTest.java` найти тест `createLesson_singleLessonInPast_doesNotThrow` (расположен после `createLesson_withWeeklyRecurrenceStartingInPast_throwsConflict`). Переименовать метод в `createLesson_singleLessonInPast_throwsConflict` и заменить `assertDoesNotThrow(...)` на `assertThrows(ConflictException.class, ...)`. Полный итоговый вид:

```java
@Test
void createLesson_singleLessonInPast_throwsConflict() {
    UUID tutorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    StudentCard card = new StudentCard();
    card.setId(studentId);
    card.setTutorUserId(tutorId);
    card.setName("Мария");
    card.setSubject("Математика");

    when(studentCardRepository.findByIdAndTutorUserId(studentId, tutorId)).thenReturn(Optional.of(card));

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
                    null
            )));
}
```

Обрати внимание: убрана строка `when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of());` и `when(lessonRepository.save(any(Lesson.class)))...` — после реализации общая проверка будет бросать ConflictException **до** `ensureNoOverlap` и до `lessonRepository.save`, эти стабы станут dead code и приведут к `UnnecessaryStubbingException` в strict mode Mockito.

- [ ] **Step 2: Запустить тест, убедиться что падает**

Run: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest#createLesson_singleLessonInPast_throwsConflict`
Expected: FAIL с `ConflictException` не выброшен (текущий код пропускает single урок в прошлом).

---

## Task 2: Backend — failing test «application с past desiredStartTime бросает»

**Files:**
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java`

- [ ] **Step 1: Добавить тест `createLesson_applicationDesiredStartInPast_throwsConflict`**

Вставить в `LessonServiceTest.java` после теста из Task 1:

```java
@Test
void createLesson_applicationDesiredStartInPast_throwsConflict() {
    UUID tutorId = UUID.randomUUID();
    UUID appId = UUID.randomUUID();

    CrmApplication app = new CrmApplication();
    app.setId(appId);
    app.setClientUserId(UUID.randomUUID());
    app.setTutorUserId(tutorId);
    app.setTutorName("Иван");
    app.setSubjectName("Математика");
    app.setFormat("Онлайн");
    app.setDesiredStartTime(LocalDateTime.now().minusDays(1));

    StudentCard card = new StudentCard();
    card.setId(UUID.randomUUID());
    card.setName("Ученик");

    when(applicationRepository.findByIdAndTutorUserId(appId, tutorId)).thenReturn(Optional.of(app));
    when(studentCardRepository.findByApplicationId(appId)).thenReturn(Optional.of(card));

    assertThrows(ConflictException.class, () ->
            service.createTutorLesson(tutorId, new CreateLessonRequest(
                    appId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )));
}
```

Логика: `request.startsAt() == null`, значит `startTime = application.getDesiredStartTime()` (в прошлом). Общая проверка после вычисления startTime должна бросить. До реализации — текущий код пропустит, тест упадёт.

- [ ] **Step 2: Запустить тест, убедиться что падает**

Run: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest#createLesson_applicationDesiredStartInPast_throwsConflict`
Expected: FAIL.

---

## Task 3: Backend — failing test «reschedule в прошлое бросает»

**Files:**
- Modify: `backend/crm-service/src/test/java/com/tutor\crmservice\lesson\LessonServiceTest.java`

- [ ] **Step 1: Добавить тест `rescheduleLesson_toPast_throwsConflict`**

Вставить в `LessonServiceTest.java` после теста из Task 2:

```java
@Test
void rescheduleLesson_toPast_throwsConflict() {
    UUID tutorId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();
    Lesson lesson = createLesson(tutorId, LocalDateTime.of(2027, 5, 4, 18, 0), 60);
    lesson.setId(lessonId);

    LocalDateTime pastTarget = LocalDateTime.now().minusDays(1);

    assertThrows(ConflictException.class, () ->
            service.rescheduleTutorLesson(tutorId, lessonId, new RescheduleLessonRequest(
                    pastTarget,
                    RescheduleLessonRequest.Scope.SINGLE
            )));
}
```

Логика: новый `startsAt` в прошлом. Текущий код пропустит, тест упадёт. После реализации — ранняя проверка бросит ConflictException.

- [ ] **Step 2: Запустить тест, убедиться что падает**

Run: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest#rescheduleLesson_toPast_throwsConflict`
Expected: FAIL с `ConflictException` не выброшен.

---

## Task 4: Backend — реализация, бамп fixture, прогон, коммит

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor\crmservice\lesson\LessonService.java`
- Modify: `backend/crm-service/src/test/java/com\tutor\crmservice\lesson\LessonServiceTest.java`

- [ ] **Step 1: В `LessonService.createTutorLesson` удалить recurrence-специфичную проверку в цикле**

В `LessonService.java` найти (после commit `3c0cb5f`):

```java
        for (int index = 0; index < occurrences; index++) {
            LocalDateTime lessonStart = startTime.plusWeeks(index);
            if (occurrences > 1 && lessonStart.isBefore(now)) {
                throw new ConflictException("Нельзя создать серию уроков в прошлом.");
            }
            ensureNoOverlap(tutorUserId, lessonStart, duration, Set.of());
```

Заменить на (убираем if внутри цикла):

```java
        for (int index = 0; index < occurrences; index++) {
            LocalDateTime lessonStart = startTime.plusWeeks(index);
            ensureNoOverlap(tutorUserId, lessonStart, duration, Set.of());
```

- [ ] **Step 2: В `LessonService.createTutorLesson` удалить slot-специфичную past-проверку**

В `LessonService.java` найти (после commit `42f181b`):

```java
                startTime = request.slot().date().atTime(request.slot().startTime());
                duration = request.slot().durationMinutes();
            } else {
```

Прямо перед строкой `startTime = ...` находится блок:

```java
                LocalDateTime slotStart = request.slot().date().atTime(request.slot().startTime());
                if (slotStart.isBefore(LocalDateTime.now())) {
                    throw new ConflictException("Нельзя записать ученика на прошедшее время.");
                }
```

Удалить эти 4 строки. Итоговый код slot-ветки:

```java
                startTime = request.slot().date().atTime(request.slot().startTime());
                duration = request.slot().durationMinutes();
            } else {
```

- [ ] **Step 3: В `LessonService.createTutorLesson` добавить общую past-проверку перед циклом**

В `LessonService.java` найти:

```java
        int occurrences = recurrenceOccurrences(request.recurrence());
        UUID seriesId = occurrences > 1 ? UUID.randomUUID() : null;
        Lesson firstSaved = null;
        LocalDateTime now = LocalDateTime.now();

        for (int index = 0; index < occurrences; index++) {
```

Заменить на (добавляем if между `now = ...` и циклом):

```java
        int occurrences = recurrenceOccurrences(request.recurrence());
        UUID seriesId = occurrences > 1 ? UUID.randomUUID() : null;
        Lesson firstSaved = null;
        LocalDateTime now = LocalDateTime.now();

        if (startTime.isBefore(now)) {
            throw new ConflictException("Нельзя создать урок в прошлом.");
        }

        for (int index = 0; index < occurrences; index++) {
```

- [ ] **Step 4: В `LessonService.rescheduleTutorLesson` добавить раннюю past-проверку**

В `LessonService.java` найти начало метода:

```java
    @Transactional
    public Lesson rescheduleTutorLesson(UUID tutorUserId, UUID lessonId, RescheduleLessonRequest request) {
        if (request == null || request.startsAt() == null) {
            throw new IllegalArgumentException("Новое время урока обязательно.");
        }
        Lesson lesson = lessonRepository.findByIdForUpdate(lessonId)
```

Заменить на (добавляем if после проверки null):

```java
    @Transactional
    public Lesson rescheduleTutorLesson(UUID tutorUserId, UUID lessonId, RescheduleLessonRequest request) {
        if (request == null || request.startsAt() == null) {
            throw new IllegalArgumentException("Новое время урока обязательно.");
        }
        if (request.startsAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Нельзя перенести урок в прошлое.");
        }
        Lesson lesson = lessonRepository.findByIdForUpdate(lessonId)
```

- [ ] **Step 5: Бампнуть все 2026 fixture-даты в `LessonServiceTest` на 2027**

Новая общая проверка `if (startTime.isBefore(now)) throw ...` ловит прошлые даты. В `LessonServiceTest.java` 23 строки используют `LocalDateTime.of(2026, ...)` для fixture-дат (старт-тайм урока, desiredStartTime приложения, новый startTime в reschedule). Все эти даты сегодня (2026-06-16) в прошлом — тесты, которые их используют, начнут падать с `ConflictException` от общей проверки вместо ожидаемого исхода.

Решение: **bulk replace `LocalDateTime.of(2026` → `LocalDateTime.of(2027` в файле** `LessonServiceTest.java` через `edit` с `replaceAll: true`. Это безопасно, потому что:
- Все 23 совпадения — fixture-даты для тестовых сценариев
- `createLesson_withWeeklyRecurrenceForExistingStudent_createsSeries` (line 119) уже был бампнут на 2027 в коммите `3c0cb5f` (предыдущий фикс), совпадений `2026` там нет
- `createLesson_slotInPast_throwsConflict` (commit `42f181b`) и новые тесты из Tasks 1-3 используют `LocalDateTime.now().minusDays(1)`, не 2026
- `LessonRescheduleServiceTest.java` и `LessonRepositoryTest.java` — другие файлы, не задеты

Затронутые тесты (все должны проходить с бампнутыми 2027-фикстурами):
- `createLesson_overlappingTime_throwsConflict` (lines 57, 66, 70) — бамп fixture сохраняет intent (overlap, не past)
- `createLesson_nonOverlapping_ok` (lines 85, 94, 99) — happy path
- `createLesson_forManualStudent_createsStudentCardAndLinksLesson` (line 284) — manual student path
- `rescheduleLesson_singleKeepsOtherSeriesLessonsInPlace` (lines 303, 306, 313, 317, 318) — single scope reschedule
- `rescheduleLesson_closesPendingClientRescheduleRequest` (lines 325, 336) — pending request closure
- `createLesson_fromApplicationKeepsTutorNameForClientSchedule` (lines 356, 370) — tutor name propagation
- `rescheduleLesson_seriesMovesSelectedAndFutureSeriesLessons` (lines 387, 390, 392, 401, 405, 406, 407) — series reschedule

Все assertions (например, `assertEquals(LocalDateTime.of(2026, 5, 5, 19, 0), lesson.getStartsAt())`) тоже бампнутся автоматически — replaceAll покрывает все вхождения подстроки.

Run после бампа: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest`
Expected: все 14 тестов PASS (12 pre-existing + 1 изменённый single + 2 новых, все на 2027-фикстурах или `now().minusDays(1)` для негативных).

- [ ] **Step 6: Запустить все тесты LessonServiceTest**

Run: `mvn test -f backend/crm-service/pom.xml -Dtest=LessonServiceTest`
Expected: все 14 тестов PASS, BUILD SUCCESS (8 pre-existing + 3 новых из Task 1-3 + 1 обновлённый single-в-прошлом + 2 обновлённых fixture 2026→2027 в существующих).

Если что-то падает — диагностируй: проверь, что mock'и в тестах соответствуют новому коду (общая проверка бросает до `ensureNoOverlap` и до `lessonRepository.save`, эти стабы нужно убрать в любых новых тестах, которые должны бросать).

- [ ] **Step 7: Запустить полный test suite crm-service (регрессия)**

Run: `mvn test -f backend/crm-service/pom.xml`
Expected: BUILD SUCCESS. Никаких регрессий в других test-классах.

- [ ] **Step 8: Закоммитить backend-часть**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java
git commit -m "fix(crm-service): reject any lesson creation or reschedule to past"
```

---

## Task 5: Frontend — failing test для нового helper (test rewrite)

**Files:**
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.test.ts`

- [ ] **Step 1: Заменить импорт helper'а в тестовом файле**

В `frontend/src/pages/app/tutor-calendar-helpers.test.ts` найти блок импортов (lines 3-14). Текущий:

```typescript
import {
  buildCalendarWeekDays,
  buildCellLessonPayload,
  buildLessonReschedulePayload,
  buildSlotLessonPayload,
  buildTutorCalendarTimeSlots,
  formatLessonRequestStartTime,
  getLessonModalDefaults,
  getRecurrenceValidationError,
  getTutorCalendarGridPosition,
  getTutorCalendarDisplayStats,
  isCalendarCellInPast,
} from './tutor-calendar-helpers';
```

Заменить `getRecurrenceValidationError,` на `getLessonStartValidationError,` (в алфавитном порядке между `getLessonModalDefaults,` и `getTutorCalendarGridPosition,`):

```typescript
import {
  buildCalendarWeekDays,
  buildCellLessonPayload,
  buildLessonReschedulePayload,
  buildSlotLessonPayload,
  buildTutorCalendarTimeSlots,
  formatLessonRequestStartTime,
  getLessonModalDefaults,
  getLessonStartValidationError,
  getTutorCalendarGridPosition,
  getTutorCalendarDisplayStats,
  isCalendarCellInPast,
} from './tutor-calendar-helpers';
```

- [ ] **Step 2: Заменить describe-блок `getRecurrenceValidationError` на новый**

В `frontend/src/pages/app/tutor-calendar-helpers.test.ts` найти describe-блок (lines ~242-267) и заменить его полностью на:

```typescript
  describe('getLessonStartValidationError', () => {
    const now = new Date('2026-05-14T12:30:00');

    it('returns null for create kind when start is in the future', () => {
      expect(getLessonStartValidationError('2026-05-15', '09:00', 'create', now)).toBeNull();
    });

    it('returns null for create kind when start is today in the future', () => {
      expect(getLessonStartValidationError('2026-05-14', '18:00', 'create', now)).toBeNull();
    });

    it('returns null for recurrence kind when start is in the future', () => {
      expect(getLessonStartValidationError('2026-05-15', '09:00', 'recurrence', now)).toBeNull();
    });

    it('returns null for reschedule kind when start is in the future', () => {
      expect(getLessonStartValidationError('2026-05-15', '09:00', 'reschedule', now)).toBeNull();
    });

    it('returns create-specific error message for create kind in the past', () => {
      const error = getLessonStartValidationError('2026-05-13', '20:00', 'create', now);
      expect(error).not.toBeNull();
      expect(error).toMatch(/назначить урок/);
    });

    it('returns recurrence-specific error message for recurrence kind in the past', () => {
      const error = getLessonStartValidationError('2026-05-13', '20:00', 'recurrence', now);
      expect(error).not.toBeNull();
      expect(error).toMatch(/серию уроков/);
    });

    it('returns reschedule-specific error message for reschedule kind in the past', () => {
      const error = getLessonStartValidationError('2026-05-13', '20:00', 'reschedule', now);
      expect(error).not.toBeNull();
      expect(error).toMatch(/перенести урок/);
    });

    it('returns create error for create kind with today + past time', () => {
      const error = getLessonStartValidationError('2026-05-14', '09:00', 'create', now);
      expect(error).not.toBeNull();
      expect(error).toMatch(/назначить урок/);
    });
  });
```

Итого: 8 кейсов в новом describe (3 null-cases по future + 3 kind-specific errors + 1 сегодня+прошедшее + 1 reschedule в прошлом).

- [ ] **Step 3: Запустить тесты, убедиться что падают**

Run: `cd frontend && npm test -- --run tutor-calendar-helpers.test.ts`
Expected: FAIL с `getLessonStartValidationError is not a function` (helper ещё не добавлен в helper-файл).

---

## Task 6: Frontend — добавить новый helper, прогнать, закоммитить

**Files:**
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.ts`

- [ ] **Step 1: Добавить `LessonStartKind` type + `getLessonStartValidationError` в helper-файл**

В `frontend/src/pages/app/tutor-calendar-helpers.ts` найти конец файла (после последнего export). Добавить:

```typescript

export type LessonStartKind = 'create' | 'recurrence' | 'reschedule';

export function getLessonStartValidationError(
  date: string,
  time: string,
  kind: LessonStartKind,
  now: Date = new Date(),
): string | null {
  if (!isCalendarCellInPast(date, time, now)) {
    return null;
  }
  switch (kind) {
    case 'recurrence':
      return 'Нельзя назначить серию уроков в прошлое. Выберите будущую дату и время.';
    case 'reschedule':
      return 'Нельзя перенести урок в прошлое. Выберите будущую дату и время.';
    case 'create':
      return 'Нельзя назначить урок в прошлое. Выберите будущую дату и время.';
  }
}
```

Не удаляем старый `getRecurrenceValidationError` пока — он ещё используется в `TutorCalendarPage.tsx`. Удалим в Task 7 после обновления call-сайтов.

- [ ] **Step 2: Запустить тесты, убедиться что проходят**

Run: `cd frontend && npm test -- --run tutor-calendar-helpers.test.ts`
Expected: PASS. Все 8 новых кейсов + 19 существующих = 27 тестов. (19 — это 14 pre-existing + 5 старого `getRecurrenceValidationError`. Старый describe остаётся, потому что helper не удалён.)

- [ ] **Step 3: Закоммитить helper**

```bash
git add frontend/src/pages/app/tutor-calendar-helpers.ts frontend/src/pages/app/tutor-calendar-helpers.test.ts
git commit -m "feat(tutor-calendar): generalize helper to getLessonStartValidationError with kind"
```

---

## Task 7: Frontend — обновить call-сайты + удалить старый helper

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.ts`
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.test.ts`

- [ ] **Step 1: В `TutorCalendarPage.tsx` обновить import**

В `frontend/src/pages/app/TutorCalendarPage.tsx` найти блок импорта из `./tutor-calendar-helpers` (lines 20-31). Заменить `getRecurrenceValidationError,` на `getLessonStartValidationError,` + добавить `type LessonStartKind,`:

```typescript
import {
  buildCalendarWeekDays,
  buildCellLessonPayload,
  buildLessonReschedulePayload,
  buildSlotLessonPayload,
  formatLessonRequestStartTime,
  getLessonModalDefaults,
  getLessonStartValidationError,
  getTutorCalendarGridPosition,
  getTutorCalendarDisplayStats,
  isCalendarCellInPast,
  type LessonRescheduleScope,
  type LessonStartKind,
} from './tutor-calendar-helpers';
```

- [ ] **Step 2: В `handleCreateLessonSubmit` заменить вызов helper'а**

В `frontend/src/pages/app/TutorCalendarPage.tsx` найти:

```typescript
    const recurrenceType = String(formData.get('recurrenceType'));
    const date = String(formData.get('date'));
    const time = String(formData.get('time'));
    const recurrenceError = getRecurrenceValidationError(date, time, recurrenceType);
    if (recurrenceError) {
```

Заменить на:

```typescript
    const recurrenceType = String(formData.get('recurrenceType'));
    const date = String(formData.get('date'));
    const time = String(formData.get('time'));
    const lessonStartKind: LessonStartKind = recurrenceType === 'WEEKLY' ? 'recurrence' : 'create';
    const recurrenceError = getLessonStartValidationError(date, time, lessonStartKind);
    if (recurrenceError) {
```

Имя переменной `recurrenceError` сохраняем (минимальная дифф-стоимость), даже когда `kind === 'create'`.

- [ ] **Step 3: В `handleBookSlot` заменить вызов helper'а**

В `frontend/src/pages/app/TutorCalendarPage.tsx` найти:

```typescript
    const recurrenceType = String(formData.get('recurrenceType'));
    const slotDate = selectedSlot.date;
    const slotTime = selectedSlot.startTime.slice(0, 5);
    const recurrenceError = getRecurrenceValidationError(slotDate, slotTime, recurrenceType);
    if (recurrenceError) {
```

Заменить на:

```typescript
    const recurrenceType = String(formData.get('recurrenceType'));
    const slotDate = selectedSlot.date;
    const slotTime = selectedSlot.startTime.slice(0, 5);
    const lessonStartKind: LessonStartKind = recurrenceType === 'WEEKLY' ? 'recurrence' : 'create';
    const recurrenceError = getLessonStartValidationError(slotDate, slotTime, lessonStartKind);
    if (recurrenceError) {
```

- [ ] **Step 4: В `handleRescheduleLesson` добавить валидацию**

В `frontend/src/pages/app/TutorCalendarPage.tsx` найти:

```typescript
  const handleRescheduleLesson = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedLesson) return;
    const formData = new FormData(e.currentTarget);
    const scope = String(formData.get('scope') ?? 'SINGLE') as LessonRescheduleScope;
    const payload = buildLessonReschedulePayload(
      String(formData.get('date')),
      String(formData.get('time')),
      scope,
    );
```

Заменить на:

```typescript
  const handleRescheduleLesson = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedLesson) return;
    const formData = new FormData(e.currentTarget);
    const rescheduleError = getLessonStartValidationError(
      String(formData.get('date')),
      String(formData.get('time')),
      'reschedule',
    );
    if (rescheduleError) {
      await dialog.alert(rescheduleError);
      return;
    }
    const scope = String(formData.get('scope') ?? 'SINGLE') as LessonRescheduleScope;
    const payload = buildLessonReschedulePayload(
      String(formData.get('date')),
      String(formData.get('time')),
      scope,
    );
```

- [ ] **Step 5: Удалить старый `getRecurrenceValidationError` из helper-файла**

В `frontend/src/pages/app/tutor-calendar-helpers.ts` найти весь блок:

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

Удалить его (вместе с завершающим переводом строки). Проверь что перед удалением нет других пользователей `getRecurrenceValidationError` — `TutorCalendarPage.tsx` уже обновлён в шагах 1-4 этого таска, других вызовов быть не должно.

- [ ] **Step 6: В тестовом файле удалить старый describe-блок `getRecurrenceValidationError`**

В `frontend/src/pages/app/tutor-calendar-helpers.test.ts` найти describe `getRecurrenceValidationError` (старый блок с 5 кейсами, который остался после Task 5). Удалить его полностью. Оставить только новый describe `getLessonStartValidationError` (добавлен в Task 5).

- [ ] **Step 7: Прогнать typecheck и тесты**

Run: `cd frontend && npm run typecheck && npm test`
Expected: typecheck без ошибок, vitest PASS. Всего 22 теста в `tutor-calendar-helpers.test.ts` (19 pre-existing — 5 старого describe + 8 нового = 22).

Если `npm test` падает с упоминанием `getRecurrenceValidationError` — где-то остался caller, проверь импорты в `TutorCalendarPage.tsx` (должен быть `getLessonStartValidationError`, не `getRecurrenceValidationError`).

- [ ] **Step 8: Закоммитить frontend-часть**

```bash
git add frontend/src/pages/app/TutorCalendarPage.tsx frontend/src/pages/app/tutor-calendar-helpers.ts frontend/src/pages/app/tutor-calendar-helpers.test.ts
git commit -m "fix(tutor-calendar): validate lesson start time on create and reschedule"
```

---

## Task 8: Финальная проверка

- [ ] **Step 1: Прогнать backend tests целиком**

Run: `mvn test -f backend/crm-service/pom.xml`
Expected: BUILD SUCCESS. Все тесты (14 в `LessonServiceTest` + другие классы) зелёные.

- [ ] **Step 2: Прогнать frontend typecheck и тесты**

Run: `cd frontend && npm run typecheck && npm test`
Expected: typecheck OK, vitest PASS.

- [ ] **Step 3: Прогнать frontend build**

Run: `cd frontend && npm run build`
Expected: успешный build без ошибок. (`npm run build` = `tsc --noEmit -p tsconfig.json && node scripts/build.mjs` per AGENTS.md.)

- [ ] **Step 4: Просмотреть `git log` на текущей ветке**

Run: `git log --oneline dev..HEAD`
Expected: 3 новых коммита поверх предыдущих 6:
1. `fix(crm-service): reject any lesson creation or reschedule to past`
2. `feat(tutor-calendar): generalize helper to getLessonStartValidationError with kind`
3. `fix(tutor-calendar): validate lesson start time on create and reschedule`

(Порядок коммитов может отличаться — backend был в Task 4, helper в Task 6, integration в Task 7.)

- [ ] **Step 5: НЕ мерджить в dev**

Ждать явного указания пользователя. Ветка остаётся `fix/tutor-calendar-recurrence-past-date` локально.

---

## Notes

- В Task 4 Step 1-2 удаляются две старые точечные проверки. Это намеренное упрощение: общая `if (startTime.isBefore(now))` покрывает оба случая. Трейд-офф в UX: раньше slot-флоу говорил «записать ученика», теперь общее «создать урок» — приемлемо.
- Сообщение в backend общее (`"Нельзя создать урок в прошлом."`), а не специфичное по сценарию. Контекст-специфичные сообщения остаются на frontend через `kind`.
- `handleRescheduleLesson` не имеет своего `dialog.alert` в catch (в отличие от других handler'ов) — это особенность текущего кода. В рамках этого плана не правим, но если backend бросит `ConflictException`, текущий catch отработает (`'Не удалось перенести урок: ' + err.message`). Наша валидация срабатывает раньше и не пускает запрос — catch не задействован.
- Тест `createLesson_nonOverlapping_ok` использует `LocalDateTime.of(2026, 5, 1, 14, 0)` и `LocalDateTime.of(2026, 5, 1, 10, 0)` для существующего урока. Оба в прошлом, общая проверка бросит. Бампим 2026→2027 в обоих местах.
- `ConflictException` уже импортирован в `LessonService.java`, никаких новых import'ов в backend не нужно.
- `LessonRescheduleRequest` уже импортирован в `LessonService.java` (он используется в `rescheduleTutorLesson`), не нужно добавлять.
