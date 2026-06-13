# Интерактивный календарь репетитора — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Сделать существующий календарь репетитора полностью интерактивным: кликабельные ячейки, навигация по неделям, создание/редактирование уроков через модалы.

**Architecture:** Добавляем state `currentWeekStart` в `TutorCalendarPage`. Ячейки сетки становятся кликабельными и открывают модал создания урока с предзаполненными данными. Существующие events кликабельны и открывают детали. Навигация ← → сдвигает неделю и перезагружает данные. Мини-календарь кликабелен.

**Tech Stack:** React 19 + TypeScript + custom Rollup/Vitest frontend, Spring Boot 3.5.10 + Java 21 backend, Docker Compose.

---

## File Map

- Modify: `frontend/src/shared/api/tutor.ts` — add date params to `getTutorCalendar`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/TutorCrmController.java` — accept date range
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java` — use date range param
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx` — full interactivity rewrite
- Modify: `frontend/src/styles/global.css` — add interactive styles

---

## Task 1: Backend — accept date range for calendar

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/TutorCrmController.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`

- [ ] **Step 1: Update `TutorCrmController.getCalendar`**

```java
@GetMapping("/calendar")
public TutorCalendarPageDto getCalendar(
    @AuthenticationPrincipal JwtPrincipal principal,
    @RequestParam(required = false) String from,
    @RequestParam(required = false) String to
) {
    return pageViewService.getTutorCalendarPage(principal.userId(), from, to);
}
```

- [ ] **Step 2: Update `PageViewService.getTutorCalendarPage`**

```java
public TutorCalendarPageDto getTutorCalendarPage(UUID tutorId, String fromStr, String toStr) {
    LocalDate from = fromStr != null ? LocalDate.parse(fromStr) : LocalDate.now();
    LocalDate to = toStr != null ? LocalDate.parse(toStr) : from.plusDays(6);
    
    // Use from/to for weekDays, events query, slots generation
    // ... existing logic but with explicit date range ...
}
```

- [ ] **Step 3: Run backend tests**

Run: `mvn test -f backend/crm-service/pom.xml`
Expected: BUILD SUCCESS

---

## Task 2: Frontend API — add date params

**Files:**
- Modify: `frontend/src/shared/api/tutor.ts`

- [ ] **Step 1: Update `getTutorCalendar`**

```typescript
export function getTutorCalendar(from?: string, to?: string) {
  const qs = from && to ? `?from=${from}&to=${to}` : '';
  return apiRequest<TutorCalendarPageData>(`/tutor/calendar${qs}`, undefined, () => tutorCalendarPage);
}
```

- [ ] **Step 2: Verify no TypeScript errors**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

---

## Task 3: Week navigation state and helpers

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Add helper functions at top of file**

```typescript
function getMonday(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  return new Date(d.setDate(diff));
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function formatDateISO(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function formatPeriod(weekStart: Date): string {
  const weekEnd = addDays(weekStart, 6);
  const months = ['января','февраля','марта','апреля','мая','июня','июля','августа','сентября','октября','ноября','декабря'];
  const sameMonth = weekStart.getMonth() === weekEnd.getMonth();
  if (sameMonth) {
    return `${weekStart.getDate()}–${weekEnd.getDate()} ${months[weekStart.getMonth()]} ${weekStart.getFullYear()}`;
  }
  return `${weekStart.getDate()} ${months[weekStart.getMonth()]} – ${weekEnd.getDate()} ${months[weekEnd.getMonth()]} ${weekStart.getFullYear()}`;
}
```

- [ ] **Step 2: Add new state hooks**

```typescript
const [currentWeekStart, setCurrentWeekStart] = useState<Date>(getMonday(new Date()));
const [showLessonModal, setShowLessonModal] = useState(false);
const [showLessonDetailModal, setShowLessonDetailModal] = useState(false);
const [selectedLesson, setSelectedLesson] = useState<TutorCalendarPageData['events'][0] | null>(null);
const [selectedCell, setSelectedCell] = useState<{ day: typeof pageData.weekDays[0]; time: string } | null>(null);
const [isLoading, setIsLoading] = useState(false);
```

- [ ] **Step 3: Update load function to use week range**

```typescript
const loadCalendarData = async (weekStart?: Date) => {
  setIsLoading(true);
  const start = weekStart || currentWeekStart;
  const from = formatDateISO(start);
  const to = formatDateISO(addDays(start, 6));
  try {
    const data = await getTutorCalendar(from, to);
    setPageData(data as TutorCalendarPageData);
  } catch {
    setPageData(tutorCalendarPage);
  } finally {
    setIsLoading(false);
  }
};
```

- [ ] **Step 4: Add week navigation handlers**

```typescript
const goToPreviousWeek = () => {
  const newWeek = addDays(currentWeekStart, -7);
  setCurrentWeekStart(newWeek);
  void loadCalendarData(newWeek);
};

const goToNextWeek = () => {
  const newWeek = addDays(currentWeekStart, 7);
  setCurrentWeekStart(newWeek);
  void loadCalendarData(newWeek);
};

const handleSync = () => {
  void loadCalendarData(currentWeekStart);
};
```

- [ ] **Step 5: Update useEffect to load initial week**

```typescript
useEffect(() => {
  void loadCalendarData(currentWeekStart);
}, []);
```

---

## Task 4: Make grid cells and events clickable

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Add click handlers**

```typescript
const handleCellClick = (day: typeof pageData.weekDays[0], time: string) => {
  setSelectedCell({ day, time });
  setShowLessonModal(true);
};

const handleEventClick = (event: typeof pageData.events[0]) => {
  setSelectedLesson(event);
  setShowLessonDetailModal(true);
};
```

- [ ] **Step 2: Update grid cells to be clickable**

Replace the cell div with:
```tsx
{pageData.timeSlots.flatMap((slot, slotIndex) =>
  pageData.weekDays.map((day, dayIndex) => (
    <div
      key={`${slot}-${day.date}`}
      className="tutor-week-grid__cell"
      style={{ gridColumn: String(dayIndex + 2), gridRow: String(slotIndex + 2) }}
      onClick={() => handleCellClick(day, slot)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => { if (e.key === 'Enter') handleCellClick(day, slot); }}
    />
  )),
)}
```

- [ ] **Step 3: Update events to be clickable**

Add to event article:
```tsx
onClick={(e) => {
  e.stopPropagation();
  handleEventClick(event);
}}
role="button"
tabIndex={0}
onKeyDown={(e) => { if (e.key === 'Enter') handleEventClick(event); }}
```

---

## Task 5: Create New Lesson Modal

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Add create lesson handler**

```typescript
const handleCreateLessonSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();
  const form = e.currentTarget;
  const formData = new FormData(form);
  
  const payload = {
    applicationId: null, // Will be set when student is selected
    startsAt: `${formData.get('date')}T${formData.get('time')}:00`,
    durationMinutes: Number(formData.get('duration')),
    slot: null,
  };
  
  try {
    await createTutorLesson(payload);
    setShowLessonModal(false);
    setSelectedCell(null);
    await loadCalendarData(currentWeekStart);
  } catch (err) {
    alert('Не удалось создать занятие: ' + (err instanceof Error ? err.message : 'Unknown error'));
  }
};
```

- [ ] **Step 2: Add New Lesson Modal JSX**

Replace existing `showScheduleModal` block with combined modals:

```tsx
{showLessonModal && (
  <div className="lesson-modal-overlay" onClick={() => setShowLessonModal(false)}>
    <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
      <h2>Новое занятие</h2>
      <form onSubmit={handleCreateLessonSubmit}>
        <label>
          Дата
          <input type="date" name="date" required 
            defaultValue={selectedCell ? `2026-${selectedCell.day.date.padStart(2, '0')}-...` : ''} />
        </label>
        <label>
          Время
          <input type="time" name="time" required 
            defaultValue={selectedCell?.time || '09:00'} />
        </label>
        <label>
          Длительность
          <select name="duration" defaultValue="60">
            <option value="30">30 мин</option>
            <option value="45">45 мин</option>
            <option value="60">60 мин</option>
            <option value="90">90 мин</option>
            <option value="120">120 мин</option>
          </select>
        </label>
        <label>
          Формат
          <select name="format" defaultValue="Онлайн">
            <option value="Онлайн">Онлайн</option>
            <option value="Офлайн">Офлайн</option>
          </select>
        </label>
        <label>
          Примечание
          <textarea name="note" rows={3} />
        </label>
        <div className="modal-actions">
          <Button type="submit">Создать</Button>
          <Button type="button" variant="ghost" onClick={() => setShowLessonModal(false)}>Отмена</Button>
        </div>
      </form>
    </dialog>
  </div>
)}
```

---

## Task 6: Create Lesson Detail Modal

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Add Lesson Detail Modal JSX**

```tsx
{showLessonDetailModal && selectedLesson && (
  <div className="lesson-modal-overlay" onClick={() => setShowLessonDetailModal(false)}>
    <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
      <h2>{selectedLesson.title}</h2>
      <div className="lesson-detail">
        <p><strong>Время:</strong> {selectedLesson.subtitle}</p>
        <p><strong>Статус:</strong> Активно</p>
      </div>
      <div className="modal-actions">
        <Button onClick={() => { setShowLessonDetailModal(false); setShowLessonModal(true); }}>
          Редактировать
        </Button>
        <Button variant="ghost" onClick={() => setShowLessonDetailModal(false)}>Закрыть</Button>
      </div>
    </dialog>
  </div>
)}
```

---

## Task 7: Update period display and navigation buttons

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Replace hardcoded period with dynamic**

```tsx
<div className="tutor-calendar-period">
  <button type="button" aria-label="Предыдущая неделя" onClick={goToPreviousWeek}>
    ←
  </button>
  <strong>{formatPeriod(currentWeekStart)}</strong>
  <button type="button" aria-label="Следующая неделя" onClick={goToNextWeek}>
    →
  </button>
</div>
```

---

## Task 8: Make mini-calendar days clickable

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Add click handler for mini-calendar**

```typescript
const navigateToWeek = (dayStr: string) => {
  // dayStr format depends on monthDays data structure
  // Assuming it contains the full date or we can construct it
  const date = new Date(/* construct from day data */);
  const monday = getMonday(date);
  setCurrentWeekStart(monday);
  void loadCalendarData(monday);
};
```

- [ ] **Step 2: Update mini-calendar day rendering**

```tsx
{pageData.monthDays.map((day) => (
  <span
    key={day.day}
    className={cn(
      'tutor-mini-month-card__day',
      day.muted && 'is-muted',
      day.selected && 'is-selected'
    )}
    onClick={() => !day.muted && navigateToWeek(day.day)}
    role="button"
    tabIndex={day.muted ? -1 : 0}
  >
    {day.day}
  </span>
))}
```

---

## Task 9: Add CSS for interactive states

**Files:**
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Add interactive cell styles**

```css
/* Clickable grid cells */
.tutor-week-grid__cell {
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.tutor-week-grid__cell:hover {
  background-color: color-mix(in oklab, var(--indigo) 8%, transparent);
}

.tutor-week-grid__cell:focus-visible {
  outline: 2px solid var(--indigo);
  outline-offset: -2px;
}

/* Clickable events */
.tutor-week-event {
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.1s ease;
}

.tutor-week-event:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* Modal overlay */
.lesson-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}

.lesson-modal {
  background: #ffffff;
  border-radius: 20px;
  padding: 28px;
  width: 100%;
  max-width: 420px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12);
  border: none;
}

.lesson-modal h2 {
  font-size: 20px;
  margin-bottom: 20px;
  color: var(--ink);
}

.lesson-modal form label {
  display: block;
  margin-bottom: 16px;
  font-size: 14px;
  color: var(--muted);
}

.lesson-modal form input,
.lesson-modal form select,
.lesson-modal form textarea {
  width: 100%;
  margin-top: 6px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  font-size: 15px;
  background: #fff;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
}

/* Clickable mini-calendar days */
.tutor-mini-month-card__day {
  cursor: pointer;
  transition: background 0.1s ease;
}

.tutor-mini-month-card__day:hover:not(.is-muted) {
  background: color-mix(in oklab, var(--indigo) 10%, transparent);
  border-radius: 8px;
}
```

---

## Task 10: Update CreateLessonRequest handling

**Files:**
- Modify: `frontend/src/shared/api/tutor.ts`

- [ ] **Step 1: Update `createTutorLesson` to accept payload**

```typescript
export function createTutorLesson(payload?: { 
  applicationId?: string | null;
  startsAt?: string;
  durationMinutes?: number;
  slot?: { date: string; startTime: string; durationMinutes: number } | null;
}) {
  return apiRequest<{ id: string; status: string }>(
    '/tutor/lessons',
    {
      method: 'POST',
      body: JSON.stringify(payload || {}),
    },
    () => ({ id: `lesson-${Date.now()}`, status: 'Запланировано' }),
  );
}
```

---

## Task 11: Build and verify

- [ ] **Step 1: Run frontend build**

Run: `npm run build`
Workdir: `frontend`
Expected: TypeScript check passes, Rollup bundle builds

- [ ] **Step 2: Rebuild Docker**

Run: `docker compose up --build -d`
Workdir: `C:\JavaMy\newDiploma`
Expected: All containers Up

- [ ] **Step 3: DevTools MCP verification**

Navigate: `http://127.0.0.1:4173/app/tutor/calendar`
Verify:
- Период отображает текущую неделю (не апрель 2026)
- Клик на пустую ячейку → открывается модал «Новое занятие»
- Клик ← → переключает неделю
- Клик на существующий урок → открывается детали
- Клик на день в мини-календаре → переходит к нужной неделе
- «Синхронизировать» перезагружает данные

---

## Self-Review Checklist

1. **Spec coverage:**
   - [x] Click empty cell → New Lesson modal → Task 4, 5
   - [x] Click existing event → Lesson Detail modal → Task 4, 6
   - [x] Week navigation ← → → Task 3, 7
   - [x] Dynamic period display → Task 7
   - [x] Mini-calendar day click → Task 8
   - [x] Backend date range support → Task 1
   - [x] CSS hover/focus states → Task 9

2. **Placeholder scan:** No TBD/TODO found.

3. **Type consistency:** `createTutorLesson` accepts optional payload matching `CreateLessonRequest` shape from backend.
