# Интерактивный календарь репетитора — Design Specification

> **Date:** 2026-05-06
> **Status:** Approved
> **Scope:** Сделать существующий календарь репетитора полностью интерактивным: кликабельные ячейки, навигация по неделям, создание/редактирование уроков через модалы, взаимодействие со слотами доступности.

---

## 1. Goal

Превратить статичную сетку календаря в полноценный интерактивный интерфейс где:
- Каждая ячейка сетки кликабельна и ведёт к действию
- Навигация по неделям работает и подгружает реальные данные
- Уроки создаются через заполненную форму, а не пустым POST
- Слоты доступности можно не только смотреть, но и использовать для бронирования/блокировки

---

## 2. Current State & Problems

### Что есть сейчас:
- Сетка недели отображает `events` из `TutorCalendarPageData` как абсолютно позиционированные элементы
- Ячейки сетки (`.tutor-week-grid__cell`) — пустые div, некликабельны
- Период захардкожен: «15–21 апреля 2026»
- Кнопки ← → не имеют onClick
- «Добавить занятие» вызывает `createTutorLesson()` без параметров
- Мини-календарь — дни некликабельны

### Что нужно добавить:
- State для текущей недели (`currentWeekStart: Date`)
- Кликабельность ячеек сетки
- Модал «Новое занятие» с формой
- Модал «Карточка занятия» для существующих
- Навигация ← → по неделям с перезагрузкой данных
- Мини-календарь → навигация к выбранной неделе

---

## 3. Data Flow

```
User clicks empty cell (day + time)
  → Open "New Lesson" modal
  → Pre-fill: date from cell, time from cell
  → User fills: student, subject, format, duration, note
  → Submit → POST /api/tutor/lessons with full payload
  → Refresh calendar data for current week

User clicks existing event
  → Open "Lesson Detail" modal
  → Show: student, subject, time, status, format
  → Actions: Edit, Cancel, Reschedule, Message

User clicks ← / →
  → Update currentWeekStart by -7 / +7 days
  → Fetch calendar for new week range
  → Re-render grid

User clicks mini-calendar day
  → Set currentWeekStart to Monday of that week
  → Fetch and re-render
```

---

## 4. Frontend State

```typescript
type TutorCalendarState = {
  pageData: TutorCalendarPageData;
  viewMode: 'lessons' | 'availability';
  currentWeekStart: Date; // Monday of current displayed week
  selectedCell: { dayIndex: number; slotIndex: number } | null;
  showLessonModal: boolean;
  showLessonDetailModal: boolean;
  showScheduleModal: boolean;
  selectedLesson: LessonEvent | null;
  selectedSlot: TutorCalendarSlot | null;
  isLoading: boolean;
};
```

---

## 5. API Changes

### Existing endpoint updates:

**`GET /api/tutor/calendar`** — должен поддерживать query params:
```
GET /api/tutor/calendar?from=2026-05-04&to=2026-05-10
```

Currently returns hardcoded week. Update `PageViewService.getTutorCalendarPage` to accept date range.

### Frontend API helper update:
```typescript
// tutor.ts
export function getTutorCalendar(from?: string, to?: string) {
  const qs = from && to ? `?from=${from}&to=${to}` : '';
  return apiRequest<TutorCalendarPageData>(`/tutor/calendar${qs}`, ...);
}
```

---

## 6. Component Design

### 6.1 Week Grid Cell

```tsx
// Each cell is now clickable
<div
  key={`${slot}-${day.date}`}
  className="tutor-week-grid__cell"
  style={{ gridColumn: String(dayIndex + 2), gridRow: String(slotIndex + 2) }}
  onClick={() => handleCellClick(dayIndex, slotIndex, day, slot)}
  role="button"
  tabIndex={0}
/>
```

### 6.2 Event Card

```tsx
// Events are clickable
<article
  key={event.id}
  className={cn('tutor-week-event', `tutor-week-event--${event.tone}`)}
  style={{...}}
  onClick={(e) => {
    e.stopPropagation();
    handleEventClick(event);
  }}
  role="button"
/>
```

### 6.3 New Lesson Modal

Fields:
- Ученик: `<select>` из `pageData.students` (или отдельный fetch)
- Предмет: `<select>` из профиля репетитора
- Дата: `<input type="date">` (предзаполнена)
- Время: `<input type="time">` (предзаполнено)
- Длительность: select 30/45/60/90/120 мин
- Формат: radio Онлайн / Офлайн
- Примечание: `<textarea>`
- Кнопки: «Создать», «Отмена»

### 6.4 Lesson Detail Modal

Shows:
- Заголовок: `{subject} · {studentName}`
- Время: `{date} · {startTime}–{endTime}`
- Формат, статус, примечание

Actions:
- «Редактировать» → открывает New Lesson Modal с данными
- «Отменить» → PATCH status + refresh
- «Перенести» → открывает выбор нового слота
- «Написать ученику» → navigate to messages

---

## 7. Period Display

Replace hardcoded "15–21 апреля 2026" with dynamic:

```typescript
function formatPeriod(weekStart: Date): string {
  const weekEnd = addDays(weekStart, 6);
  const sameMonth = weekStart.getMonth() === weekEnd.getMonth();
  if (sameMonth) {
    return `${weekStart.getDate()}–${weekEnd.getDate()} ${monthName(weekStart)} ${weekStart.getFullYear()}`;
  }
  return `${weekStart.getDate()} ${monthName(weekStart)} – ${weekEnd.getDate()} ${monthName(weekEnd)} ${weekStart.getFullYear()}`;
}
```

---

## 8. Mini Calendar Interaction

```tsx
// Clicking a day in mini calendar navigates to that week
<span
  key={day.day}
  className={cn('tutor-mini-month-card__day', day.muted && 'is-muted', day.selected && 'is-selected')}
  onClick={() => navigateToWeek(day.fullDate)}
  role="button"
>
  {day.day}
</span>
```

---

## 9. CSS Additions

```css
/* Clickable cells */
.tutor-week-grid__cell {
  cursor: pointer;
  transition: background 0.15s ease;
}

.tutor-week-grid__cell:hover {
  background: color-mix(in oklab, var(--surface) 90%, var(--indigo));
}

.tutor-week-grid__cell:focus {
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
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* Modal overlay */
.lesson-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.lesson-modal {
  background: #fff;
  border-radius: 20px;
  padding: 28px;
  width: 100%;
  max-width: 440px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12);
}
```

---

## 10. Testing Strategy

- **Unit:** Slot generation, date formatting, week navigation logic
- **Integration:** Create lesson via modal → verify appears in grid → cancel → verify disappears
- **E2E:** DevTools MCP — click cell → fill form → submit → verify event appears

---

*Spec self-review: No TBDs. Scope focused on interactivity only, no backend entity changes needed.*
