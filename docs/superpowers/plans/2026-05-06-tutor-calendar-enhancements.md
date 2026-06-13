# Дополнительные фичи календаря репетитора — Implementation Plan

**Goal:**
1. Запретить создание уроков в прошлом
2. Добавить редактирование/удаление правил доступности
3. Отображать свободные слоты как "Окна" в сетке календаря занятий

---

## Task A: Запретить уроки в прошлом

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`

- [ ] **Step 1: Добавить проверку в handleCellClick**

```typescript
const handleCellClick = (day: typeof pageData.weekDays[0], time: string, dayIndex: number) => {
  const date = formatDateISO(addDays(currentWeekStart, dayIndex));
  const today = formatDateISO(new Date());
  if (date < today) {
    return; // Нельзя ставить уроки в прошлое
  }
  setSelectedCell({ day, time, date });
  setShowLessonModal(true);
};
```

- [ ] **Step 2: Добавить min в date input модала**

```tsx
<input 
  type="date" 
  name="date" 
  required 
  min={formatDateISO(new Date())}
  defaultValue={selectedCell ? selectedCell.date : formatDateISO(new Date())} 
/>
```

---

## Task B: Редактирование и удаление правил доступности

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Добавить импорты API**

```typescript
import {
  createTutorAvailabilitySchedule,
  createTutorLesson,
  getTutorCalendar,
  getTutorCalendarSlots,
  getTutorAvailabilitySchedules,
  updateTutorAvailabilitySchedule,
  deleteTutorAvailabilitySchedule,
} from '../../shared/api/tutor';
```

- [ ] **Step 2: Добавить state для schedules и редактирования**

```typescript
const [schedules, setSchedules] = useState<AvailabilitySchedule[]>([]);
const [editingSchedule, setEditingSchedule] = useState<AvailabilitySchedule | null>(null);
```

- [ ] **Step 3: Обновить loadAvailabilityData**

```typescript
const loadAvailabilityData = async () => {
  const from = new Date().toISOString().slice(0, 10);
  const to = new Date(Date.now() + 13 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  const [slots, schedulesData] = await Promise.all([
    getTutorCalendarSlots(from, to),
    getTutorAvailabilitySchedules(),
  ]);
  setPageData((prev) => ({ ...prev, slots }));
  setSchedules(schedulesData);
};
```

- [ ] **Step 4: Добавить обработчики редактирования и удаления**

```typescript
const handleDeleteSchedule = async (id: string) => {
  if (!confirm('Удалить это правило?')) return;
  try {
    await deleteTutorAvailabilitySchedule(id);
    await loadAvailabilityData();
  } catch (err) {
    alert('Не удалось удалить: ' + (err instanceof Error ? err.message : 'Unknown error'));
  }
};

const handleUpdateSchedule = async (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();
  if (!editingSchedule) return;
  const form = e.currentTarget;
  const formData = new FormData(form);
  const payload: Partial<AvailabilitySchedule> = {
    startTime: String(formData.get('startTime')),
    endTime: String(formData.get('endTime')),
    slotDurationMinutes: Number(formData.get('duration')),
    validUntil: formData.get('validUntil') as string | null,
    active: formData.get('active') === 'on',
  };
  try {
    await updateTutorAvailabilitySchedule(editingSchedule.id, payload);
    setEditingSchedule(null);
    await loadAvailabilityData();
  } catch (err) {
    alert('Не удалось обновить: ' + (err instanceof Error ? err.message : 'Unknown error'));
  }
};
```

- [ ] **Step 5: Добавить список правил в availability view**

После кнопки "Добавить правило" добавить:

```tsx
{schedules.length > 0 && (
  <div className="availability-schedules-list" style={{ marginBottom: 24 }}>
    <h3 style={{ fontSize: 15, marginBottom: 12, color: 'var(--ink)' }}>Правила</h3>
    {schedules.map((schedule) => (
      <div key={schedule.id} className="availability-schedule-row">
        <div>
          <strong>{['Пн','Вт','Ср','Чт','Пт','Сб','Вс'][schedule.dayOfWeek - 1]}</strong>
          <span>{schedule.startTime}–{schedule.endTime} · {schedule.slotDurationMinutes} мин</span>
          {schedule.validUntil && <span>до {schedule.validUntil}</span>}
        </div>
        <div className="availability-schedule-actions">
          <Button variant="ghost" onClick={() => setEditingSchedule(schedule)}>Редактировать</Button>
          <Button variant="ghost" onClick={() => void handleDeleteSchedule(schedule.id)}>Удалить</Button>
        </div>
      </div>
    ))}
  </div>
)}
```

- [ ] **Step 6: Добавить модал редактирования**

После существующего `showScheduleModal` блока:

```tsx
{editingSchedule && (
  <div className="lesson-modal-overlay" onClick={() => setEditingSchedule(null)}>
    <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
      <h2>Редактировать правило</h2>
      <form onSubmit={handleUpdateSchedule}>
        <label>
          Время начала
          <input type="time" name="startTime" required defaultValue={editingSchedule.startTime} />
        </label>
        <label>
          Время окончания
          <input type="time" name="endTime" required defaultValue={editingSchedule.endTime} />
        </label>
        <label>
          Длительность слота
          <select name="duration" defaultValue={String(editingSchedule.slotDurationMinutes)}>
            <option value="30">30 мин</option>
            <option value="45">45 мин</option>
            <option value="60">60 мин</option>
            <option value="90">90 мин</option>
            <option value="120">120 мин</option>
          </select>
        </label>
        <label>
          Действует до
          <input type="date" name="validUntil" defaultValue={editingSchedule.validUntil || ''} />
        </label>
        <label className="checkbox-row">
          <input type="checkbox" name="active" defaultChecked={editingSchedule.active} /> Активно
        </label>
        <div className="modal-actions">
          <Button type="submit">Сохранить</Button>
          <Button type="button" variant="ghost" onClick={() => setEditingSchedule(null)}>Отмена</Button>
        </div>
      </form>
    </dialog>
  </div>
)}
```

- [ ] **Step 7: Добавить CSS для списка правил**

```css
.availability-schedules-list {
  margin-top: 16px;
}

.availability-schedule-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--surface);
  border-radius: 12px;
  margin-bottom: 8px;
  border: 1px solid var(--border);
}

.availability-schedule-row strong {
  display: block;
  font-size: 15px;
  color: var(--ink);
}

.availability-schedule-row span {
  font-size: 13px;
  color: var(--muted);
}

.availability-schedule-actions {
  display: flex;
  gap: 8px;
}
```

---

## Task C: Отображать слоты в сетке календаря занятий

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Добавить helper для определения row слота**

```typescript
function timeToRow(time: string, timeSlots: string[]): number {
  const [h, m] = time.split(':').map(Number);
  const slotMinutes = h * 60 + m;
  let bestRow = 0;
  let bestDiff = Infinity;
  timeSlots.forEach((slot, i) => {
    const [sh, sm] = slot.split(':').map(Number);
    const diff = Math.abs(slotMinutes - (sh * 60 + sm));
    if (diff < bestDiff) {
      bestDiff = diff;
      bestRow = i;
    }
  });
  return bestRow;
}
```

- [ ] **Step 2: Отображать свободные слоты в сетке lessons view**

В `viewMode === 'lessons'` блоке, после `pageData.events.map(...)` добавить:

```tsx
{pageData.slots
  .filter((slot) => slot.available)
  .map((slot, index) => {
    const slotDate = new Date(slot.date);
    const dayDiff = Math.round((slotDate.getTime() - currentWeekStart.getTime()) / (24 * 60 * 60 * 1000));
    const column = dayDiff + 1;
    const row = timeToRow(slot.startTime, pageData.timeSlots);
    if (column < 1 || column > 5 || row < 0 || row >= pageData.timeSlots.length) return null;
    return (
      <article
        key={`slot-${index}`}
        className="tutor-week-event tutor-week-event--slate"
        style={{
          gridColumn: String(column + 1),
          gridRow: `${row + 2} / span 1`,
        }}
      >
        <strong>Свободно</strong>
        <span>{slot.startTime} – {slot.endTime}</span>
      </article>
    );
  })}
```

- [ ] **Step 3: Обновить легенду**

Легенда уже содержит "Окно" с tone slate — это корректно.

---

## Task D: Сборка и верификация

- [ ] **Step 1: TypeScript check**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 2: Docker rebuild**

Run: `docker compose up --build -d`
Expected: All containers Up

- [ ] **Step 3: DevTools MCP verification**

Navigate: `http://127.0.0.1:4173/app/tutor/calendar`
Verify:
- Клик на ячейку прошлой недели → модал НЕ открывается
- Модал создания урока → date input не позволяет выбрать прошлое
- Режим "Моя доступность" → показывает список правил с кнопками Редактировать/Удалить
- Редактирование правила работает
- Удаление правила работает
- В режиме "Неделя занятий" свободные слоты отображаются как "Свободно" в сетке
