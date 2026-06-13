# Создание окна вручную и запись ученика — Implementation Plan

**Goal:** Репетитор может в календаре одним кликом создать разовое окно (свободный слот) и затем записать в него ученика.

**Architecture:** 
- Добавляем кнопку «Создать окно» в тулбар календаря — открывает модал с датой, временем и длительностью, который создаёт `SlotOverride` типа `EXTRA`.
- Клик по свободному окно в сетке открывает модал «Записать ученика» — форма с именем, предметом и форматом, которая создаёт урок напрямую (без заявки).
- Backend: расширяем `CreateLessonRequest` и `LessonService`, чтобы урок можно было создавать без привязки к заявке.

---

## Task 1: Backend — разрешить создание урока без заявки

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/service/CreateLessonRequest.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/Lesson.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java`

### Step 1: Расширить `CreateLessonRequest`

```java
public record CreateLessonRequest(
        UUID applicationId,
        LocalDateTime startsAt,
        @Min(value = 30, message = "Lesson duration must be at least 30 minutes")
        Integer durationMinutes,
        SlotRequest slot,
        String studentName,
        String subject,
        String format
) {
    public record SlotRequest(LocalDate date, LocalTime startTime, Integer durationMinutes) {}
}
```

### Step 2: Сделать поля `Lesson` nullable

Убрать `nullable = false` у:
- `applicationId`
- `studentId`
- `clientUserId`

(Оставить `nullable = false` у `tutorUserId`, `tutorName`, `studentName`, `title`, `subjectName`, `startsAt`, `durationMinutes`, `format`, `status`)

### Step 3: Обновить `LessonService.createTutorLesson`

```java
@Transactional
public Lesson createTutorLesson(UUID tutorUserId, CreateLessonRequest request) {
    LocalDateTime startTime;
    int duration;
    String studentName;
    String subjectName;
    String format;
    UUID applicationId;
    UUID studentId;
    UUID clientUserId;
    String title;

    if (request.applicationId() != null) {
        // Старая логика через заявку
        CrmApplication application = resolveApplication(tutorUserId, request.applicationId());
        StudentCard studentCard = studentCardRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new ConflictException("Сначала примите заявку, чтобы создать карточку ученика."));
        
        if (request.slot() != null) {
            boolean available = availabilitySlotService.isSlotAvailable(
                    tutorUserId,
                    request.slot().date(),
                    request.slot().startTime(),
                    request.slot().durationMinutes()
            );
            if (!available) {
                throw new ConflictException("Выбранный слот недоступен или уже забронирован.");
            }
            startTime = request.slot().date().atTime(request.slot().startTime());
            duration = request.slot().durationMinutes();
        } else {
            startTime = request.startsAt() == null ? application.getDesiredStartTime() : request.startsAt();
            duration = request.durationMinutes() == null ? 60 : request.durationMinutes();
        }
        
        applicationId = application.getId();
        studentId = studentCard.getId();
        clientUserId = application.getClientUserId();
        studentName = studentCard.getName();
        subjectName = application.getSubjectName();
        format = application.getFormat();
        title = application.getSubjectName() + " · пробный урок";
    } else {
        // Новая логика — без заявки
        if (request.startsAt() == null) {
            throw new ConflictException("Укажите время начала урока.");
        }
        startTime = request.startsAt();
        duration = request.durationMinutes() == null ? 60 : request.durationMinutes();
        studentName = request.studentName() == null ? "Ученик" : request.studentName();
        subjectName = request.subject() == null ? "Предмет" : request.subject();
        format = request.format() == null ? "Онлайн" : request.format();
        title = subjectName + " · " + studentName;
        applicationId = null;
        studentId = null;
        clientUserId = null;
    }

    ensureNoOverlap(tutorUserId, startTime, duration);

    Lesson lesson = new Lesson();
    lesson.setApplicationId(applicationId);
    lesson.setStudentId(studentId);
    lesson.setClientUserId(clientUserId);
    lesson.setTutorUserId(tutorUserId);
    lesson.setTutorName("Репетитор"); // TODO: подставлять реальное имя
    lesson.setStudentName(studentName);
    lesson.setTitle(title);
    lesson.setSubjectName(subjectName);
    lesson.setStartsAt(startTime);
    lesson.setDurationMinutes(duration);
    lesson.setFormat(format);
    lesson.setStatus("Запланировано");
    lesson.setBookingSource(BookingSource.MANUAL);

    Lesson saved = lessonRepository.save(lesson);
    
    if (clientUserId != null) {
        notificationEventPublisher.publishLessonCreated(
                clientUserId,
                lesson.getTutorName(),
                subjectName,
                startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
    }
    return saved;
}
```

### Step 4: Добавить `BookingSource.MANUAL`

В `BookingSource.java` добавить `MANUAL`.

### Step 5: Тесты

Run: `mvn test -f backend/crm-service/pom.xml`
Expected: BUILD SUCCESS

---

## Task 2: Frontend — API helpers для создания окна и урока с учеником

**Files:**
- Modify: `frontend/src/shared/api/tutor.ts`

### Step 1: Обновить `createTutorSlotOverride`

Убедиться, что payload включает `type: 'EXTRA'`:
```typescript
export function createTutorSlotOverride(payload: Omit<SlotOverride, 'id'>) {
  return apiRequest<SlotOverride>('/tutor/slot-overrides', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
```

### Step 2: Обновить `createTutorLesson`

```typescript
export function createTutorLesson(payload?: {
  applicationId?: string | null;
  startsAt?: string;
  durationMinutes?: number;
  slot?: { date: string; startTime: string; durationMinutes: number } | null;
  studentName?: string;
  subject?: string;
  format?: string;
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

## Task 3: Frontend — UI для создания окна и записи ученика

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`
- Modify: `frontend/src/styles/global.css`

### Step 1: Добавить state

```typescript
const [showCreateSlotModal, setShowCreateSlotModal] = useState(false);
const [showBookSlotModal, setShowBookSlotModal] = useState(false);
const [selectedSlot, setSelectedSlot] = useState<TutorCalendarSlot | null>(null);
```

### Step 2: Добавить кнопку «Создать окно» в header

Рядом с «Добавить занятие»:
```tsx
<Button onClick={() => setShowCreateSlotModal(true)}>Создать окно</Button>
```

### Step 3: Модал «Создать окно»

```tsx
{showCreateSlotModal && (
  <div className="lesson-modal-overlay" onClick={() => setShowCreateSlotModal(false)}>
    <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
      <h2>Создать окно</h2>
      <form onSubmit={handleCreateSlot}>
        <label>
          Дата
          <input type="date" name="date" required min={formatDateISO(new Date())} 
            defaultValue={formatDateISO(new Date())} />
        </label>
        <label>
          Время начала
          <input type="time" name="startTime" required defaultValue="09:00" />
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
        <div className="modal-actions">
          <Button type="submit">Создать</Button>
          <Button type="button" variant="ghost" onClick={() => setShowCreateSlotModal(false)}>Отмена</Button>
        </div>
      </form>
    </dialog>
  </div>
)}
```

Handler:
```typescript
const handleCreateSlot = async (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();
  const form = e.currentTarget;
  const formData = new FormData(form);
  const date = String(formData.get('date'));
  const startTime = String(formData.get('startTime'));
  const duration = Number(formData.get('duration'));
  try {
    await createTutorSlotOverride({
      date,
      startTime,
      durationMinutes: duration,
      type: 'EXTRA',
    });
    setShowCreateSlotModal(false);
    await handleSync();
  } catch (err) {
    alert('Не удалось создать окно: ' + (err instanceof Error ? err.message : 'Unknown error'));
  }
};
```

### Step 4: Клик по окну в сетке → модал «Записать ученика»

Сейчас свободные слоты отображаются как `tutor-week-event--slate` с текстом "Свободно". Нужно сделать их кликабельными:

```tsx
{pageData.slots
  .filter((slot) => slot.available)
  .map((slot, index) => {
    // ... existing positioning logic ...
    return (
      <article
        key={`slot-${index}`}
        className="tutor-week-event tutor-week-event--slate"
        style={{...}}
        onClick={(e) => {
          e.stopPropagation();
          setSelectedSlot(slot);
          setShowBookSlotModal(true);
        }}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setSelectedSlot(slot); setShowBookSlotModal(true); } }}
      >
        <strong>Свободно</strong>
        <span>{slot.startTime} – {slot.endTime}</span>
      </article>
    );
  })}
```

### Step 5: Модал «Записать ученика»

```tsx
{showBookSlotModal && selectedSlot && (
  <div className="lesson-modal-overlay" onClick={() => setShowBookSlotModal(false)}>
    <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
      <h2>Записать ученика</h2>
      <p style={{ color: 'var(--muted)', fontSize: 14, marginBottom: 16 }}>
        {selectedSlot.date} · {selectedSlot.startTime} – {selectedSlot.endTime}
      </p>
      <form onSubmit={handleBookSlot}>
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
        <div className="modal-actions">
          <Button type="submit">Записать</Button>
          <Button type="button" variant="ghost" onClick={() => { setShowBookSlotModal(false); setSelectedSlot(null); }}>Отмена</Button>
        </div>
      </form>
    </dialog>
  </div>
)}
```

Handler:
```typescript
const handleBookSlot = async (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();
  if (!selectedSlot) return;
  const form = e.currentTarget;
  const formData = new FormData(form);
  const payload = {
    applicationId: null,
    startsAt: `${selectedSlot.date}T${selectedSlot.startTime}:00`,
    durationMinutes: selectedSlot.endTime ? 
      (Number(selectedSlot.endTime.split(':')[0]) * 60 + Number(selectedSlot.endTime.split(':')[1])) -
      (Number(selectedSlot.startTime.split(':')[0]) * 60 + Number(selectedSlot.startTime.split(':')[1]))
      : 60,
    studentName: String(formData.get('studentName')),
    subject: String(formData.get('subject')),
    format: String(formData.get('format')),
  };
  try {
    await createTutorLesson(payload);
    setShowBookSlotModal(false);
    setSelectedSlot(null);
    await handleSync();
  } catch (err) {
    alert('Не удалось записать ученика: ' + (err instanceof Error ? err.message : 'Unknown error'));
  }
};
```

### Step 6: Escape handlers

```typescript
useEffect(() => {
  const handleEsc = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      setShowCreateSlotModal(false);
      setShowBookSlotModal(false);
      setSelectedSlot(null);
    }
  };
  if (showCreateSlotModal || showBookSlotModal) {
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }
}, [showCreateSlotModal, showBookSlotModal]);
```

### Step 7: CSS

Убедиться, что `.tutor-week-event--slate` имеет `cursor: pointer` и hover-эффекты.

---

## Task 4: Сборка и верификация

- [ ] Frontend build: `cd frontend && npm run build` → success
- [ ] Docker rebuild: `docker compose up --build -d`
- [ ] DevTools MCP:
  - Клик «Создать окно» → модал → создать окно на завтра
  - Переключить на «Неделя занятий» → окно появилось в сетке
  - Клик по окну → модал «Записать ученика»
  - Заполнить имя/предмет → «Записать» → окно исчезло/превратилось в занятие
