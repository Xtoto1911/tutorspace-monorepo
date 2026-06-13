# Tutor Availability Slots — Design Specification

> **Date:** 2026-05-06
> **Status:** Approved
> **Scope:** Enable tutors to define recurring weekly availability, add one-off overrides (blocked days / extra slots), and let clients book lessons through generated concrete time slots.

---

## 1. Goal

Replace the current tutor-calendar "windows" mock with a real availability system:
- Tutor defines **recurring weekly schedules** (e.g. Mon–Fri 15:00–18:00, 60-min slots).
- Tutor can **override** any specific day (block it or add an extra slot).
- The system **generates concrete open slots on-the-fly** for any date range.
- After a tutor accepts a client application, either party can **pick an open slot** to create a `Lesson`.
- Already-booked slots automatically disappear from the open list.

---

## 2. Data Model

### 2.1 `AvailabilitySchedule` (recurring rules)

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | `UUID` | PK |
| `tutorUserId` | `UUID` | Indexed |
| `subjectId` | `UUID?` | Nullable = any subject |
| `dayOfWeek` | `int` | 1 (Mon) … 7 (Sun) |
| `startTime` | `LocalTime` | |
| `endTime` | `LocalTime` | > startTime |
| `slotDurationMinutes` | `int` | 30 / 45 / 60 / 90 / 120 |
| `active` | `boolean` | Default `true` |
| `validUntil` | `LocalDate?` | Nullable = indefinite |

**Semantics:** Each row represents one weekly recurring rule. To express "Mon–Fri 15:00–18:00" the tutor creates 5 rows.

### 2.2 `SlotOverride` (one-off exceptions)

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | `UUID` | PK |
| `tutorUserId` | `UUID` | Indexed |
| `date` | `LocalDate` | |
| `startTime` | `LocalTime` | |
| `durationMinutes` | `int` | |
| `type` | `SlotOverrideType` | `BLOCKED` or `EXTRA` |

**Semantics:**
- `BLOCKED` — removes from the generated slot list any slot that overlaps the override interval.
- `EXTRA` — adds a brand-new concrete slot that is NOT produced by recurring rules.

### 2.3 `Lesson` — minimal additions

| Addition | Type | Notes |
|----------|------|-------|
| `bookingSource` | `BookingSource` | `APPLICATION` (existing flow) or `DIRECT_SLOT` (future) |

The link between a `Lesson` and a concrete slot is **implicit** — same tutor, same `startsAt` + `durationMinutes`. No `slotId` FK is required because concrete slots are not materialised.

---

## 3. Slot Generation Algorithm (on-the-fly)

**Input:** `tutorUserId`, `fromDate`, `toDate`, optional `subjectId`
**Output:** Array of `{ date, startTime, endTime, available: boolean }`

```
for each date in [fromDate, toDate]:
  slots = empty list

  // 1. Generate from recurring rules
  rules = find active AvailabilitySchedule where dayOfWeek matches date
  for each rule in rules:
    t = rule.startTime
    while t + rule.slotDuration <= rule.endTime:
      slots.add({ date, startTime: t, endTime: t + duration })
      t += rule.slotDuration

  // 2. Apply overrides
  for each override on date:
    if override.type == BLOCKED:
      remove slots that overlap [override.startTime, override.endTime]
    if override.type == EXTRA:
      slots.add({ date, startTime: override.startTime, endTime: override.endTime })

  // 3. Subtract booked lessons
  lessons = find Lesson for tutor where startsAt between date 00:00 and 23:59
  for each lesson in lessons:
    mark any overlapping slot as available: false

return slots sorted by date, then startTime
```

**Performance note:** The algorithm is O(days × rules-per-day). For a 2-week view and a handful of rules this is negligible. If tutors accumulate thousands of overrides, add an index on `(tutorUserId, date)`.

---

## 4. Backend API

### 4.1 Tutor availability management (authenticated, `ROLE_TUTOR`)

```
GET    /api/tutor/availability-schedules
→ [{ id, dayOfWeek, startTime, endTime, slotDurationMinutes, subjectId?, active, validUntil }]

POST   /api/tutor/availability-schedules
→ Body: { dayOfWeek, startTime, endTime, slotDurationMinutes, subjectId?, validUntil? }
→ Returns created rule

PATCH  /api/tutor/availability-schedules/{id}
→ Body: partial update (active, startTime, endTime, slotDurationMinutes, validUntil)

DELETE /api/tutor/availability-schedules/{id}
→ 204 No Content

GET    /api/tutor/slot-overrides?from=YYYY-MM-DD&to=YYYY-MM-DD
→ [{ id, date, startTime, durationMinutes, type }]

POST   /api/tutor/slot-overrides
→ Body: { date, startTime, durationMinutes, type }
→ Returns created override

DELETE /api/tutor/slot-overrides/{id}
→ 204 No Content
```

### 4.2 Concrete slot views

```
GET /api/tutor/calendar/slots?from=YYYY-MM-DD&to=YYYY-MM-DD
→ Authenticated tutor sees their own generated slots.

GET /api/marketplace/tutors/{tutorId}/slots?from=YYYY-MM-DD&to=YYYY-MM-DD
→ Public endpoint. Clients see only `available: true` slots.
```

### 4.3 Lesson creation via slot (update existing endpoint)

```
POST /api/tutor/lessons
→ Body (extended, backward-compatible):
{
  "applicationId": "uuid",
  "startsAt": "2026-05-10T15:00:00",   // legacy field, still supported
  "durationMinutes": 60,               // legacy field, still supported
  "slot": {                            // NEW: preferred when booking from generated slots
    "date": "2026-05-10",
    "startTime": "15:00",
    "durationMinutes": 60
  }
}
```

**Backward compatibility:** If `slot` is absent, server falls back to legacy `startsAt` + `durationMinutes` validation (overlap check only).

**Server validation (when `slot` present):**
1. Application exists and is accepted.
2. Generated slot list for that date contains the requested slot.
3. Slot is not overlapped by an existing Lesson.
4. Slot is not blocked by a `BLOCKED` override.
5. If `subjectId` is set on the matching rule, it must match the application subject.

**On success:** Creates `Lesson` with `status = "Запланировано"`, `bookingSource = APPLICATION`.

---

## 5. Frontend UI

### 5.1 Tutor Calendar Page — Two modes

| Mode | Purpose | Actions |
|------|---------|---------|
| **Неделя занятий** | Existing view: shows `Lesson` events on grid | «Добавить занятие» — manual assignment |
| **Моя доступность** | NEW: shows generated slots | «Добавить правило», click slot → block/extra |

**Recurring rule creation modal**
- Days of week: checkboxes Mon–Sun
- Time range: `startTime` → `endTime`
- Slot duration: radio 30 / 45 / 60 / 90 / 120 min
- Subject: dropdown (optional)
- Valid until: date picker or «Бессрочно»

**Inline override actions**
- Click a day cell → context menu: «Заблокировать день» / «Добавить extra-слот»
- Visual badges: 🔴 blocked / 🟢 extra

### 5.2 Client slot picker

- After tutor accepts an application, the client application details page shows a **mini calendar** (2 weeks forward) with open slots.
- Client clicks a slot → confirmation modal → server creates Lesson.
- Alternatively, tutor can pick a slot from their calendar and assign it to an accepted application.

---

## 6. Integration Points

| Existing system | Change |
|-----------------|--------|
| `TutorCalendarPageDto` | Add `slots: SlotDto[]` alongside existing `events`. `SlotDto = { date, startTime, endTime, available: boolean, source: 'RULE' | 'EXTRA' }` |
| `TutorCalendarPage.tsx` | Add mode switcher + slot generation UI |
| `CreateLessonRequest` | Add optional `slot` field |
| `LessonService.createTutorLesson()` | Accept slot-based creation, run slot validation |
| `PageViewService.getTutorCalendarPage()` | Inject generated slots into DTO |
| Marketplace tutor profile | Optionally show next 7 days of open slots *(future — out of current scope)* |
| Kafka notification | Existing lesson-created event fires automatically |

---

## 7. Out of Scope (future iterations)

- Drag-to-resize slots on calendar
- Client self-service booking without application (direct slot booking)
- Slot wait-lists
- Calendar sharing / export (ICS)
- Recurring complex patterns (e.g. "every second Tuesday")

---

## 8. Testing Strategy

- **Unit:** Slot generation algorithm with edge cases (overrides overlapping multiple generated slots, BLOCKED after EXTRA, etc.)
- **Integration:** Full flow: create rule → generate slots → block day → create lesson → verify slot disappears
- **Frontend:** Modal validation, mode switcher, slot picker visibility

---

*Spec self-review: No TBDs. Internal consistency verified. Scope is focused on rule+override+generation flow. Booking is slot-based but still tied to existing application lifecycle.*
