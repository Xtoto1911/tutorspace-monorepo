# Полное закрытие past-date байпаса в календаре репетитора — Design Specification

> **Date:** 2026-06-16
> **Status:** Approved (in-chat 2026-06-16)
> **Scope:** Frontend + backend защита от создания/переноса/бронирования уроков в прошлом во всех оставшихся векторах после предыдущего фикса (commits `3c0cb5f`, `52b6db9`, `42f181b`).
> **Branch:** работа ведётся на `fix/tutor-calendar-recurrence-past-date`, в `dev` не сливаем до явного указания пользователя.

---

## 1. Goal

Закрыть три оставшихся вектора обхода правила «нельзя создать/перенести урок в прошлое» в `app/tutor/calendar`:

1. **Single урок в прошлое** — `handleCreateLessonSubmit` без recurrence (создание одиночного урока на прошедшую дату)
2. **Reschedule в прошлое** — `handleRescheduleLesson` (перенос существующего урока на прошедшую дату)
3. **Past slot booking** — уже закрыт в `42f181b`, переделывать не нужно; покрытие остаётся как есть

Defense in depth на 2 уровнях: frontend UX-валидация перед submit + backend-проверка перед записью. Backend покрывает **все** пути получения `startTime` в `createTutorLesson` (slot, `request.startsAt()`, `application.getDesiredStartTime()`).

---

## 2. Current State & Problems

### Что есть сейчас (после предыдущих фиксов)

**Frontend (`TutorCalendarPage.tsx`):**
- `handleCreateLessonSubmit` (line 312): валидирует только `WEEKLY` recurrence через `getRecurrenceValidationError`. Одиночный урок (`recurrenceType === 'NONE'`) — без past-проверки.
- `handleBookSlot` (line 469): то же самое, валидирует только `WEEKLY`.
- `handleRescheduleLesson` (line 506): **без** past-проверки вообще.
- Click на слот: фильтр + click guard в render (commit `42f181b`) — past slot'ы не отображаются и не открывают модал.

**Backend (`LessonService.java`):**
- `createTutorLesson` slot-ветка (lines 57-72): проверка `slotStart.isBefore(now)` (commit `42f181b`).
- `createTutorLesson` recurrence-цикл (line 114): `if (occurrences > 1 && lessonStart.isBefore(now))` (commit `3c0cb5f`).
- `createTutorLesson` main-ветка: `startTime` берётся из `request.startsAt()` или `application.getDesiredStartTime()` — **без** past-проверки.
- `rescheduleTutorLesson` (lines 153-189): **без** past-проверки.

### Что нужно добавить

1. Обобщить `getRecurrenceValidationError` до `getLessonStartValidationError(date, time, kind, now)`, где `kind: 'create' | 'recurrence' | 'reschedule'`. Сообщения отличаются по контексту.
2. В `handleCreateLessonSubmit` и `handleBookSlot` — `kind = recurrenceType === 'WEEKLY' ? 'recurrence' : 'create'`.
3. В `handleRescheduleLesson` — извлечь date/time, вызвать helper с `kind = 'reschedule'`, показать alert и выйти при ошибке.
4. В `LessonService.createTutorLesson` — заменить recurrence-специфичную проверку на общую `if (startTime.isBefore(now))` после вычисления `startTime` (покрывает все 3 пути).
5. В `LessonService.rescheduleTutorLesson` — ранняя проверка `if (request.startsAt().isBefore(now))` перед `findByIdForUpdate`.

---

## 3. Design

### 3.1 Frontend: единый helper

`frontend/src/pages/app/tutor-calendar-helpers.ts`:

- Удалить `getRecurrenceValidationError` (текущий line 149-160).
- Добавить тип и функцию:

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

Поведение:
- `isCalendarCellInPast` уже реализован (line 145) — переиспользуем
- `kind` — discriminator, не enum-флаг; caller решает контекст
- Сообщения отличаются по `kind` для точности UX (не «урок в прошлое» когда речь о серии)
- Exhaustiveness: switch без `default` — TypeScript гарантирует полноту при добавлении нового `kind` (compile error если не обработан)

### 3.2 Frontend: handler changes

**`handleCreateLessonSubmit` (TutorCalendarPage.tsx:312-348):**

Заменить:
```typescript
const recurrenceError = getRecurrenceValidationError(date, time, recurrenceType);
```
На:
```typescript
const lessonStartKind: LessonStartKind = recurrenceType === 'WEEKLY' ? 'recurrence' : 'create';
const recurrenceError = getLessonStartValidationError(date, time, lessonStartKind);
```

Имя переменной `recurrenceError` сохраняем (минимальная дифф-стоимость), даже когда `kind === 'create'`. Смысл: «ошибка начала урока».

**`handleBookSlot` (TutorCalendarPage.tsx:469-510):** то же изменение.

**`handleRescheduleLesson` (TutorCalendarPage.tsx:506-524):** добавить валидацию сразу после извлечения `formData`:

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
  // ... остальное без изменений
};
```

`getLessonStartValidationError` возвращает null для future-дат — submit проходит как раньше.

### 3.3 Backend: `createTutorLesson`

`backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java`:

1. **Удалить** recurrence-специфичную проверку в цикле (line 110-117 в текущем коде):

```java
int occurrences = recurrenceOccurrences(request.recurrence());
UUID seriesId = occurrences > 1 ? UUID.randomUUID() : null;
Lesson firstSaved = null;
LocalDateTime now = LocalDateTime.now();

for (int index = 0; index < occurrences; index++) {
    LocalDateTime lessonStart = startTime.plusWeeks(index);
    if (occurrences > 1 && lessonStart.isBefore(now)) {  // ← удалить
        throw new ConflictException("Нельзя создать серию уроков в прошлом.");  // ← удалить
    }
    ensureNoOverlap(...);
    // ...
}
```

Заменить на (одна общая проверка до цикла):

```java
int occurrences = recurrenceOccurrences(request.recurrence());
UUID seriesId = occurrences > 1 ? UUID.randomUUID() : null;
Lesson firstSaved = null;
LocalDateTime now = LocalDateTime.now();

if (startTime.isBefore(now)) {
    throw new ConflictException("Нельзя создать урок в прошлом.");
}

for (int index = 0; index < occurrences; index++) {
    LocalDateTime lessonStart = startTime.plusWeeks(index);
    ensureNoOverlap(...);
    // ...
}
```

Логика:
- `startTime` уже вычислен к этому моменту (lines 67-72): из slot, `request.startsAt()`, или `application.getDesiredStartTime()` (line 70)
- Все 3 пути покрываются одной проверкой
- Recurrence-specificity больше не нужна — общая проверка строже
- Сообщение нейтральное («урок», не «серия»), т.к. применимо к обоим случаям

2. **Slot-специфичная проверка** в `if (request.slot() != null)` блоке (lines 67-70 в текущем коде) — **удалить**. Она redundant с общей проверкой после вычисления `startTime`. Один источник истины, одно сообщение.

Финальный код slot-ветки:
```java
if (request.slot() != null) {
    boolean available = availabilitySlotService.isSlotAvailable(...);
    if (!available) {
        throw new ConflictException("Выбранный слот недоступен или уже забронирован.");
    }
    startTime = request.slot().date().atTime(request.slot().startTime());
    duration = request.slot().durationMinutes();
} else { ... }
```

Удаляется блок:
```java
LocalDateTime slotStart = request.slot().date().atTime(request.slot().startTime());
if (slotStart.isBefore(LocalDateTime.now())) {
    throw new ConflictException("Нельзя записать ученика на прошедшее время.");
}
```

Трейд-офф: slot-флоу больше не говорит «записать ученика», общее сообщение — «создать урок». Это приемлемо: сообщение точное по факту, а не по UI-сценарию.

### 3.4 Backend: `rescheduleTutorLesson`

`LessonService.rescheduleTutorLesson` (line 153):

Добавить раннюю проверку в самое начало метода, до `findByIdForUpdate`:

```java
@Transactional
public Lesson rescheduleTutorLesson(UUID tutorUserId, UUID lessonId, RescheduleLessonRequest request) {
    if (request == null || request.startsAt() == null) {
        throw new IllegalArgumentException("Новое время урока обязательно.");
    }
    if (request.startsAt().isBefore(LocalDateTime.now())) {
        throw new ConflictException("Нельзя перенести урок в прошлое.");
    }
    // ... остальной код без изменений
}
```

Логика:
- Ранняя проверка — нет смысла блокировать ряд в БД через `findByIdForUpdate`, если запрос заведомо невалиден
- Сообщение соответствует frontend-сообщению для `kind='reschedule'`

### 3.5 Error handling

**Frontend:** при ошибке валидации — `await dialog.alert(message); return;`. Не отправляем запрос на бэкенд. Существующая `try/catch` в каждом handler'е остаётся для обработки прочих ошибок (network, server 500).

**Backend:** `ConflictException` бросается в обоих методах. `crm-service` имеет `@RestControllerAdvice GlobalExceptionHandler` (verified в `shared/GlobalExceptionHandler.java`), который обрабатывает `ConflictException → 409 Conflict` с `{"message": "..."}`. Не нужно ничего менять в контроллерах.

### 3.6 Test plan

#### Frontend: `tutor-calendar-helpers.test.ts`

- **Переименовать** `describe('getRecurrenceValidationError', ...)` → `describe('getLessonStartValidationError', ...)`
- **Обновить** 5 существующих кейсов:
  - `'returns null for non-weekly recurrence even when start is in the past'` → теперь `kind='create'` с past-датой → должен вернуть ошибку. **Изменить** на: `kind='reschedule'`, прошлая дата → null (т.к. reschedule не зависит от recurrence, теряет смысл в текущей формулировке). **Переписать** на: `'returns null for any kind when start is in the future'`
  - `'returns null for weekly recurrence starting in the future'` → `'returns null for create kind when start is in the future'`
  - `'returns null for weekly recurrence starting today in the future'` → `'returns null for create kind starting today in the future'`
  - `'returns an error message for weekly recurrence starting in the past'` → переделать на 3 кейса по `kind`: create/reschedule/recurrence в прошлом → проверить, что сообщение содержит соответствующий фрагмент
  - `'returns an error message for weekly recurrence with today + past time'` → аналогично

- **Добавить** 2 новых кейса:
  - `reschedule` kind в прошлом → сообщение содержит «перенести»
  - `create` kind в прошлом → сообщение содержит «назначить урок» (не «серию»)

Итого: 5 → 7 кейсов в новом describe.

#### Backend: `LessonServiceTest.java`

- **Оставить без изменений**:
  - `createLesson_withWeeklyRecurrenceStartingInPast_throwsConflict` — по-прежнему валиден (общая проверка его покрывает)
  - `createLesson_weeklyRecurrenceStartingInFuture_doesNotThrow`
  - `createLesson_slotInPast_throwsConflict` (commit `42f181b`) — по-прежнему валиден (slot-ветка теперь не имеет своей проверки, но общая после вычисления startTime ловит)
- **Изменить ожидание**:
  - `createLesson_singleLessonInPast_doesNotThrow` → `createLesson_singleLessonInPast_throwsConflict` (новое поведение)
- **Добавить**:
  - `createLesson_applicationDesiredStartInPast_throwsConflict` — путь (c), приложение с просроченным `desiredStartTime`
  - `rescheduleLesson_toPast_throwsConflict` — путь (2)
- **Бампнуть** fixture: `createLesson_nonOverlapping_ok` использует `2026, 5, 1, 14, 0` — может потребоваться перевод на 2027 (если today > 2026-05-01). Сегодня 2026-06-16 — да, бамп нужен.

Итого по тестам `LessonServiceTest`:
- 12 текущих тестов
- +1 новый: `createLesson_applicationDesiredStartInPast_throwsConflict`
- +1 новый: `rescheduleLesson_toPast_throwsConflict`
- Переименование `createLesson_singleLessonInPast_doesNotThrow` → `createLesson_singleLessonInPast_throwsConflict` (count не меняется, только ожидание)
- Бамп fixture `createLesson_nonOverlapping_ok`: 2026→2027
- **Итого: 14 тестов**

---

## 4. Files Affected

| File | Change |
|---|---|
| `frontend/src/pages/app/tutor-calendar-helpers.ts` | Удалить `getRecurrenceValidationError`. Добавить `LessonStartKind` type + `getLessonStartValidationError`. |
| `frontend/src/pages/app/tutor-calendar-helpers.test.ts` | Переименовать describe, обновить 5 кейсов, добавить 2. |
| `frontend/src/pages/app/TutorCalendarPage.tsx` | Импортировать `getLessonStartValidationError` и `LessonStartKind`. Обновить 2 call site'а (handleCreateLessonSubmit, handleBookSlot). Добавить валидацию в handleRescheduleLesson. |
| `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java` | Удалить recurrence-специфичную проверку в цикле. Удалить slot-специфичную проверку. Добавить общую `if (startTime.isBefore(now))` после вычисления startTime. Добавить раннюю проверку в rescheduleTutorLesson. |
| `backend/crm-service/src/test/java/com/tutor/crmservice/lesson/LessonServiceTest.java` | Изменить single-в-прошлом кейс. Добавить 2 новых теста. Бампнуть fixture 2026→2027 в `createLesson_nonOverlapping_ok`. |

---

## 5. Out of Scope

- `LessonRescheduleService` (другой сервис, не трогаем)
- `AvailabilitySlotService` (slot-генерация, уже покрыта в `42f181b`)
- Изменение UI `<input type="date" min={...}>` — это browser-attribute, наш UX-check на submit страхует
- Авто-предложение ближайшего свободного времени при попытке выбрать прошедшую дату
- Локализация сообщений (i18n)
- Логирование брокed past-date попыток
- Изменение UI/UX модальных окон (только текст сообщений)
- `RescheduleLessonRequest` валидация на уровне контроллера (`@Valid`)

---

## 6. Verification

После имплементации:

- `mvn test -f backend/crm-service/pom.xml` — все тесты LessonServiceTest зелёные, BUILD SUCCESS
- `cd frontend && npm run typecheck` — без ошибок
- `cd frontend && npm test` — все 189+ тестов зелёные (с учётом обновлённого helper'а)
- `cd frontend && npm run build` — успех

---

## 7. Commits Plan

1. `feat(tutor-calendar): generalize helper to getLessonStartValidationError with kind`
2. `fix(tutor-calendar): validate lesson start time on create and reschedule`
3. `fix(crm-service): reject any lesson creation or reschedule to past`

Ветка остаётся `fix/tutor-calendar-recurrence-past-date`, в `dev` не сливаем.

---

## 8. Risks & Open Questions

- **Поведение `createLesson_applicationDesiredStartInPast`:** репетитор принял заявку неделю назад с `desiredStartTime = "сегодня 14:00"`, но не назначил урок. Через неделю репетитор заходит и хочет создать урок на ту дату. Раньше — срабатывало (lesson в прошлом создаётся). Теперь — `ConflictException`. Это блокирующее изменение поведения, но соответствует правилу «нельзя в прошлое». **Принятое решение:** блокировать. Если пользователь хочет «форcе» — отдельная задача.
- **Сообщение «Нельзя создать урок в прошлое» vs «Нельзя назначить серию уроков в прошлое»:** в backend общее сообщение нейтральное, не упоминает серию. UX может быть менее точным, но фактически корректным. **Принятое решение:** единое сообщение, согласованность важнее нюансировки.
- **Тест `createLesson_nonOverlapping_ok` и хардкод дат:** в `LessonServiceTest.java:71` используется `LocalDateTime.of(2026, 5, 1, 14, 0)`. Сегодня 2026-06-16, дата в прошлом, новый общий check будет throw. **Бампнуть на 2027 при имплементации.**
