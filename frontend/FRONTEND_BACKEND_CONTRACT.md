# TutorSpace Frontend API Contract

Документ описывает, какие данные frontend ожидает от backend, какие ручки уже заложены в `src/shared/api/*`, какие страницы сейчас используют mock/static данные, и как backend лучше отдавать ответы, чтобы постепенно заменить заглушки на реальные сервисы.

## 1. Как frontend ходит в API

Все сетевые вызовы проходят через:

```text
src/shared/api/http.ts
```

Frontend работает в двух режимах:

```env
VITE_API_MODE=mock
VITE_AUTH_API_MODE=live
VITE_API_BASE_URL=/api
```

По умолчанию публичные/кабинетные данные остаются в `mock`, а auth уже ходит в live backend. Это позволяет подключить регистрацию и вход первыми, не ломая каталог и кабинеты, которые еще частично на заглушках.

Для полного подключения backend нужно запускать frontend с:

```env
VITE_API_MODE=live
VITE_AUTH_API_MODE=live
VITE_API_BASE_URL=http://localhost:<gateway-port>/api
```

Или оставить:

```env
VITE_API_BASE_URL=/api
```

если frontend и backend проксируются через один origin.

Локальный dev server теперь проксирует `/api/auth/**` в auth-service:

```env
AUTH_SERVICE_URL=http://localhost:8080
```

`AUTH_SERVICE_URL` читается серверным скриптом `scripts/serve.mjs`; после изменения этого скрипта нужно перезапустить `npm run dev`.

В live-режиме helper делает запрос так:

```ts
fetch(`${VITE_API_BASE_URL}${path}`, {
  headers: {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  },
  ...init,
})
```

Важно: в коде API path пишется без `/api`, например `/auth/login`. При `VITE_API_BASE_URL=/api` итоговая ручка будет `/api/auth/login`.

## 2. Авторизация и роли

Session хранится в `localStorage` по ключу:

```text
tutorspace.auth.session
```

Формат сессии:

```ts
type AuthSession = {
  token: string;
  user: {
    id: string;
    name: string;
    email: string;
    role: 'parent' | 'tutor' | 'admin' | 'moderator';
  };
};
```

Frontend ожидает JWT Bearer token. Все защищенные ручки должны принимать:

```http
Authorization: Bearer <jwt>
```

Роли в frontend:

| Frontend role | Backend role | Доступ |
| --- | --- | --- |
| `parent` | `ROLE_CLIENT` или `ROLE_PARENT` | `/app/client/**` |
| `tutor` | `ROLE_TUTOR` | `/app/tutor/**` |
| `admin` | `ROLE_ADMIN` | `/admin/**`, также может смотреть client/tutor зоны |
| `moderator` | `ROLE_MODERATOR` | `/moderator/**`, также может смотреть client/tutor зоны |

После входа `/app` редиректит:

| Role | Redirect |
| --- | --- |
| `parent` | `/app/client` |
| `tutor` | `/app/tutor` |
| `admin` | `/admin` |
| `moderator` | `/moderator` |

## 3. Общие правила ответов

Рекомендуемый успешный ответ:

```json
{
  "data": {}
}
```

Но текущий frontend ожидает сам объект напрямую, без обертки `data`. Поэтому на первом этапе проще отдавать объект напрямую:

```json
{
  "token": "jwt",
  "user": {
    "id": "user-1",
    "name": "Анна",
    "email": "anna@example.com",
    "role": "parent"
  }
}
```

Если backend будет отдавать `{ "data": ... }`, нужно будет слегка обновить `apiRequest`.

Рекомендуемый формат ошибки:

```json
{
  "message": "Описание ошибки",
  "code": "VALIDATION_ERROR",
  "details": {
    "field": "email"
  }
}
```

HTTP статусы:

| Status | Когда |
| --- | --- |
| `200` | GET/PATCH/POST с телом ответа |
| `201` | создание сущности |
| `204` | успешное действие без тела |
| `400` | ошибка валидации |
| `401` | нет/просрочен JWT |
| `403` | роль не имеет доступа |
| `404` | сущность не найдена |
| `409` | конфликт состояния, например заявка уже принята |
| `422` | бизнес-валидация |

## 4. Public/Auth API

Файл frontend:

```text
src/shared/api/public.ts
```

### `GET /` home content

Используется на главной странице `/`.

На текущем MVP-этапе эта секция остается локальным static/mock контентом внутри frontend и не требует backend endpoint.

Ответ:

```ts
type HomeContent = {
  metrics: Array<{
    label: string;
    value: string;
    tone: 'indigo' | 'emerald' | 'amber';
  }>;
  process: Array<{
    title: string;
    text: string;
  }>;
  testimonials: Array<{
    author: string;
    role: string;
    text: string;
  }>;
  faq: Array<{
    question: string;
    answer: string;
  }>;
};
```

### `GET /api/marketplace/subjects`

Используется на `/`, `/catalog`, `/catalog/subject/:subjectSlug`.

Ответ:

```ts
type Subject = {
  slug: string;
  title: string;
  description: string;
  tutorCount: number;
  accent: string;
};
```

`accent` сейчас используется как CSS background. Backend может отдавать OKLCH/HEX строку, например `oklch(0.86 0.06 240)` или `#dbeafe`.

### `GET /api/marketplace/tutors`

Используется на `/`, `/catalog`, `/catalog/subject/:subjectSlug`.

Query параметры:

```ts
type TutorFilters = {
  query?: string;
  city?: string;
  subject?: string;
  minRating?: number;
  maxPrice?: number;
  onlineOnly?: boolean;
};
```

Пример:

```http
GET /api/marketplace/tutors?query=егэ&city=Москва&subject=Математика&minRating=4.7&maxPrice=2000&onlineOnly=true
```

Ответ:

```ts
type TutorSummary = {
  id: string;
  slug: string;
  name: string;
  city: string;
  experienceYears: number;
  rating: number;
  reviewCount: number;
  hourlyRate: number;
  subjects: string[];
  tags: string[];
  verified: boolean;
  featured: boolean;
  nextAvailable: string;
  about: string;
  avatarTone: string;
  primarySubjectSlug: string;
};
```

Примечания для backend:

- `slug` нужен для URL `/tutors/:slug`.
- `primarySubjectSlug` нужен для хлебных крошек и похожих репетиторов.
- `subjects` используются для фильтров и карточек.
- `tags` используются для формата, уровня, онлайн/очно.
- `hourlyRate` число, frontend сам форматирует цену.

### `GET /api/marketplace/tutors/{slug}`

Используется на `/tutors/:slug`.

Ответ:

```ts
type TutorProfile = TutorSummary & {
  education: string[];
  certificates: Array<{
    title: string;
    issuer: string;
    status: 'verified' | 'pending';
  }>;
  highlights: string[];
  schedule: string[];
  reviews: Array<{
    id: string;
    author: string;
    date: string;
    rating: number;
    text: string;
    lesson: string;
  }>;
  stats: {
    responseTime: string;
    reviews: number;
    successRate: string;
    activeStudents: number;
    averageRating: number;
    lessons?: number;
    repeatRate?: string;
  };
};
```

### `POST /api/marketplace/tutors/{tutorId}/requests`

Используется на публичном профиле репетитора при отправке заявки после клиентского логина.

Request:

```ts
{
  clientUserId: string;
  subjectId: string;
  desiredStartTime: string;
  comment: string;
}
```

Будущий response:

```ts
{
  success: true;
  applicationId: string;
}
```

Backend-смысл: `marketplace-service` валидирует заявку, синхронно пересылает ее во временный internal CRM ingest и позже будет публиковать `RequestCreated` в Kafka.

### `GET /faq` content

Используется для FAQ страниц.

На текущем MVP-этапе FAQ остается локальным static/mock контентом внутри frontend и не требует backend endpoint.

### `POST /api/auth/login`

Страница: `/auth/sign-in`.

Request:

```ts
{
  email: string;
  password: string;
}
```

Response:

```ts
{
  token: string;
  refreshToken: string;
}
```

Frontend декодирует access token и берет из claims `userId`, `role`, `sub`.

### `POST /api/auth/register`

Страницы: `/auth/sign-up/parent`, `/auth/sign-up/tutor`.

Request:

```ts
{
  email: string;
  password: string;
  role: 'ROLE_CLIENT' | 'ROLE_TUTOR';
}
```

Response:

```ts
{
  token: string;
  refreshToken: string;
}
```

Frontend пока отправляет в auth-service только `email`, `password`, `role`. Дополнительные поля анкет (`name`, `city`, `subjects`, `goal`) позже должны сохраняться в профильном сервисе (`marketplace-service` для репетитора, `crm-service` или user-profile для клиента).

### `POST /api/auth/refresh`

Request:

```ts
{
  refreshToken: string;
}
```

Response:

```ts
{
  token: string;
  refreshToken: string;
}
```

### `POST /api/auth/logout`

Request:

```ts
{
  refreshToken: string;
}
```

Response:

```http
204 No Content
```

### Password reset

Сейчас в `auth-service` нет ручек восстановления пароля. Страницы `/auth/forgot-password` и `/auth/reset-password/:token` остаются на mock-заглушках до добавления backend flow.

Рекомендуемые будущие ручки:

```http
POST /api/auth/password-reset
POST /api/auth/password-reset/confirm
```

Request:

```ts
{
  email: string;
}
```

Response:

```ts
{
  success: true;
  message: string;
}
```

Будущий request для `POST /api/auth/password-reset/confirm`:


```ts
{
  token: string;
  password: string;
}
```

Будущий response:

```ts
{
  success: true;
  message: string;
}
```

## 5. Client API

Файл frontend:

```text
src/shared/api/client.ts
```

Защищенная зона:

```text
/app/client/**
```

Требуется роль:

```text
parent, admin, moderator
```

### `GET /api/client/applications`

Страница: `/app/client/applications`.

Сейчас страница частично статичная, но контракт нужен такой:

```ts
{
  metrics: Array<{
    value: string;
    label: string;
    tone: string;
  }>;
  tabs: string[];
  applications: Array<{
    id: string;
    tutorId: string;
    tutorSlug: string;
    initials: string;
    tutorName: string;
    subject: string;
    chips: string[];
    status: string;
    statusTone: 'green' | 'amber' | 'indigo' | 'red' | string;
    price: string;
    note: string;
    nextAction: string;
  }>;
  nearestLesson?: {
    id: string;
    title: string;
    startsAt: string;
    format: 'online' | 'offline';
    joinUrl?: string;
  };
  recommendations: string[];
}
```

### `POST /api/client/applications`

Создание заявки клиентом из каталога/профиля.

Request:

```ts
{
  tutorId?: string;
  tutorSlug?: string;
  subject: string;
  studentName?: string;
  gradeGoal?: string;
  preferredTime?: string;
  format: 'online' | 'offline' | 'any';
  message: string;
}
```

Response:

```ts
{
  id: string;
  status: 'created' | 'pending';
}
```

### `GET /api/client/applications/{applicationId}`

Страница: `/app/client/applications/:applicationId`.

Ожидаемые данные:

```ts
{
  id: string;
  status: string;
  tutor: TutorSummary;
  subject: string;
  student: {
    name: string;
    gradeGoal: string;
  };
  lesson?: {
    id: string;
    startsAt: string;
    durationMinutes: number;
    format: 'online' | 'offline';
    joinUrl?: string;
  };
  messagesPreview: Array<{
    author: string;
    text: string;
    createdAt: string;
  }>;
  timeline: Array<{
    title: string;
    createdAt: string;
    status: string;
    };
 }
 ```

### `PATCH /api/client/applications/{applicationId}/cancel`

Отмена заявки.

Response:

```ts
{
  id: string;
  status: 'cancelled';
}
```

### `GET /api/client/schedule?from=YYYY-MM-DD&to=YYYY-MM-DD`

Страница: `/app/client/schedule`.

Страница больше не должна считаться статичной: backend уже отдает агрегированный live read model из CRM, а frontend должен рендерить действия и empty state из ответа.

Параметры `from` и `to` необязательны, но передаются только вместе. Диапазон должен начинаться в понедельник и заканчиваться в воскресенье той же недели. Без параметров backend выбирает неделю автоматически.

Контракт:

```ts
{
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    caption: string;
    tone: string;
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
     applicationId: string;
     threadId?: string | null;
     title: string;
     meta: string;
     time: string;
     startsAt: string;
     endsAt: string;
     tone: string;
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
      startsAt: string;
      title: string;
      status: string;
      tone: string;
    }>;
   miniMonthDays: Array<{
     date: string;
     active: boolean;
   }>;
   upcomingLessons: Array<{
     id: string;
     lessonId: string;
     applicationId: string;
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
}
```

Примечания для backend:

- `threadId` может быть `null`, если диалог по заявке еще не создан.
- `applicationId` и `lessonId` нужны для CTA на сообщения и перенос.
- `emptyState.show=true` должен возвращаться при отсутствии уроков, вместо фейковых карточек расписания.
- `events[].column` - 1-based индекс дня в текущей сетке недели: понедельник = `1`, ..., воскресенье = `7`.
- `events[].row` - 0-based индекс строки в массиве `timeSlots`; событие должно рендериться в слот `timeSlots[row]`.
- `events[].startsAt` и `rescheduleRequests[].startsAt` - полная локальная дата/время в ISO-формате (`YYYY-MM-DDTHH:mm:ss`) для логики UI; поле `time` остается короткой подписью для отображения.

### `GET /api/client/messages`

Страница: `/app/client/messages`.

Контракт:

```ts
{
  metrics: Array<{
    value: string;
    label: string;
    tone: string;
  }>;
  conversations: Array<{
    id: string;
    name: string;
    initials: string;
    subject: string;
    preview: string;
    time: string;
    unread: number;
    active?: boolean;
  }>;
  activeDialog: {
    threadId: string;
    initials: string;
    name: string;
    subtitle: string;
    requestId?: string;
    studentId?: string;
  };
  messages: Array<{
    id: string;
    author: string;
    time: string;
    text: string;
    own?: boolean;
  }>;
  details: Array<{
    label: string;
    value: string;
  }>;
  quickActions: string[];
  materials: Array<{
    name: string;
    meta: string;
    url?: string;
  }>;
}
```

### `POST /api/client/messages/{threadId}`

Request:

```ts
{
  text: string;
}
```

Response:

```ts
{
  id: string;
  threadId: string;
  text: string;
  status: 'sent';
}
```

### `GET /api/client/favorites`

Страница: `/app/client/favorites`.

Контракт:

```ts
{
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: string;
  }>;
  filters: string[];
  tutors: Array<TutorSummary & {
    savedAt?: string;
    note?: string;
  }>;
  compareRows: Array<{
    tutorId: string;
    name: string;
    price: string;
    nearestSlot: string;
  }>;
  recommendedTutors: TutorSummary[];
}
```

### `POST /api/client/favorites/{tutorId}`

Переключение избранного. Лучше сделать явно:

```http
POST /api/client/favorites/{tutorId}
DELETE /api/client/favorites/{tutorId}
```

Response:

```ts
{
  tutorId: string;
  favorite: boolean;
}
```

### `GET /api/client/history`

Страница: `/app/client/history`.

Сейчас frontend ожидает минимум:

```ts
Array<{
  id: string;
  title: string;
  date: string;
  status: string;
}>
```

Рекомендуемый расширенный контракт:

```ts
{
  metrics: Array<{
    value: string;
    label: string;
    tone: string;
  }>;
  lessons: Array<{
    id: string;
    title: string;
    tutorName: string;
    subject: string;
    date: string;
    status: string;
    homeworkStatus?: string;
    materials?: Array<{
      title: string;
      url: string;
    }>;
  }>;
}
```

### `GET /api/client/settings`

Страница: `/app/client/settings`.

Текущий тип:

```ts
type ClientSettings = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'emerald' | 'indigo' | 'amber' | 'violet';
  }>;
  profile: {
    parentName: string;
    phone: string;
    email: string;
    city: string;
    timezone: string;
    language: string;
  };
  students: Array<{
    id: string;
    name: string;
    details: string;
    goal: string;
    tone: 'indigo' | 'violet' | 'emerald';
  }>;
  notifications: Array<{
    id: string;
    title: string;
    description: string;
    enabled: boolean;
  }>;
  privacy: Array<{
    id: string;
    title: string;
    description: string;
    enabled: boolean;
  }>;
  searchPreferences: string[];
};
```

### `PATCH /api/client/settings`

Request:

```ts
ClientSettings
```

Response:

```ts
ClientSettings & {
  savedAt: string;
}
```

## 6. Tutor API

Файл frontend:

```text
src/shared/api/tutor.ts
```

Защищенная зона:

```text
/app/tutor/**
```

Требуется роль:

```text
tutor, admin, moderator
```

### `GET /api/tutor/dashboard`

Страница: `/app/tutor`.

Минимальный текущий ответ:

```ts
{
  requests: number;
  upcomingLessons: number;
  monthlyRevenue: string;
  profileViews: number;
}
```

### `GET /api/tutor/requests`

Страница: `/app/tutor/requests`.

Ответ:

```ts
type TutorRequestsPageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'cyan' | 'emerald' | 'amber';
  }>;
  requests: Array<{
    id: string;
    initials: string;
    studentName: string;
    gradeGoal: string;
    subject: string;
    status: string;
    description: string;
    price: string;
    format: string;
    preferredTime: string;
    tone: 'indigo' | 'emerald' | 'amber';
    highlighted?: boolean;
  }>;
  today: {
    title: string;
    summary: string;
    events: Array<{
      time: string;
      text: string;
      tone: 'indigo' | 'emerald' | 'amber';
    }>;
  };
  quality: {
    label: string;
    value: string;
    text: string;
    percent: number;
  };
};
```

### `POST /api/tutor/requests/{requestId}/accept`

Response:

```ts
{
  requestId: string;
  status: 'accepted';
  nextStep: 'lesson-card' | string;
}
```

Backend-смысл: CRM меняет статус заявки, может создать карточку ученика и событие для уведомлений.

### `POST /api/tutor/requests/{requestId}/reject`

Request:

```ts
{
  reason: string;
}
```

Response:

```ts
{
  requestId: string;
  status: 'rejected';
  reason: string;
}
```

### `GET /api/tutor/calendar`

Страница: `/app/tutor/calendar`.

Ответ:

```ts
type TutorCalendarPageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'cyan' | 'emerald' | 'amber';
  }>;
  weekDays: Array<{
    day: string;
    date: string;
    active?: boolean;
  }>;
  timeSlots: string[];
  events: Array<{
    id: string;
    title: string;
    subtitle: string;
    tone: 'indigo' | 'emerald' | 'slate' | 'amber';
    column: number;
    row: number;
    rows?: number;
  }>;
  nextLesson: {
    time: string;
    title: string;
    description: string;
  };
  upcomingEvents: Array<{
    time: string;
    text: string;
    tone: 'indigo' | 'emerald' | 'amber';
  }>;
  monthDays: Array<{
    day: string;
    muted?: boolean;
    selected?: boolean;
  }>;
};
```

### `POST /api/tutor/calendar/lessons`

Создать занятие.

Request рекомендуется:

```ts
{
  studentId: string;
  applicationId?: string;
  startsAt: string;
  durationMinutes: number;
  format: 'online' | 'offline';
  topic?: string;
}
```

Текущий frontend готов принять:

```ts
{
  id: string;
  status: 'created';
}
```

### `POST /api/tutor/calendar/availability`

Открыть свободные слоты.

Request рекомендуется:

```ts
{
  slots: Array<{
    startsAt: string;
    endsAt: string;
    format: 'online' | 'offline' | 'any';
  }>;
}
```

Текущий response:

```ts
{
  status: 'opened';
  slots: number;
}
```

### `GET /api/tutor/students`

Страница: `/app/tutor/students`.

Ответ:

```ts
type TutorStudentsPageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'cyan';
  }>;
  students: Array<{
    id: string;
    initials: string;
    name: string;
    details: string;
    subject: string;
    status: string;
    note: string;
    progress: number;
    tone: 'indigo' | 'emerald' | 'amber';
    highlighted?: boolean;
  }>;
  selectedStudent: {
    initials: string;
    name: string;
    details: string;
    badges: string[];
    progress: number;
    tasks: Array<{
      title: string;
      meta: string;
      tone: 'indigo' | 'emerald' | 'amber';
    }>;
  };
  cards: {
    homework: { title: string; text: string };
    payments: { label: string; value: string; text: string };
    reports: { title: string; text: string };
  };
};
```

Примечание: блок `payments` в UI не должен означать гаранта оплаты уроков. TutorSpace берет деньги только за подписку CRM, поэтому backend должен трактовать это как заметки/историю договоренностей, а не платежный escrow.

### `POST /api/tutor/students`

Создать карточку ученика.

Response:

```ts
{
  id: string;
  status: 'created';
}
```

### `POST /api/tutor/students/{studentId}/reports`

Создать отчет для родителя/клиента.

Response:

```ts
{
  studentId: string;
  status: 'report-created';
}
```

### `GET /api/tutor/profile`

Страница: `/app/tutor/profile`.

Ответ:

```ts
type TutorWorkspaceProfilePageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'cyan';
  }>;
  identity: {
    name: string;
    role: string;
    city: string;
    avatarInitials: string;
    verified: boolean;
    status: string;
    completion: number;
  };
  publicCard: {
    title: string;
    subtitle: string;
    rating: string;
    price: string;
    response: string;
    views: string;
    chips: string[];
  };
  fields: Array<{
    id: string;
    label: string;
    value: string;
    type: 'input' | 'textarea';
  }>;
  subjects: Array<{
    title: string;
    meta: string;
    price: string;
    tone: 'indigo' | 'emerald' | 'amber';
  }>;
  documents: Array<{
    title: string;
    meta: string;
    status: string;
    tone: 'indigo' | 'emerald' | 'amber';
  }>;
  checklist: Array<{
    title: string;
    text: string;
    done: boolean;
  }>;
};
```

Важно: этот же path сейчас используется еще в `getTutorProfileSettings()`, где ожидается публичный `TutorProfile`. Лучше разделить:

```http
GET /api/tutor/profile/workspace
GET /api/tutor/profile/public-preview
```

или оставить `/api/tutor/profile`, но вернуть единый объект с секциями.

### `PATCH /api/tutor/profile`

Request:

```ts
Partial<TutorWorkspaceProfilePageData>
```

Response:

```ts
{
  status: 'saved';
  updatedAt: string;
}
```

### `POST /api/tutor/profile/documents`

Загрузка/создание документа профиля. Сейчас frontend отправляет JSON title, но для реального backend лучше `multipart/form-data`.

Текущий request:

```ts
{
  title: string;
}
```

Response:

```ts
{
  id: string;
  title: string;
  status: 'pending';
}
```

### `GET /api/tutor/messages`

Страница: `/app/tutor/messages`.

Ответ:

```ts
type TutorMessagesPageData = {
  metrics: Array<{
    value: string;
    label: string;
    tone: 'indigo' | 'emerald' | 'amber';
  }>;
  conversations: Array<{
    id: string;
    name: string;
    initials: string;
    subject: string;
    preview: string;
    time: string;
    unread: number;
    active?: boolean;
  }>;
  activeDialog: {
    initials: string;
    name: string;
    subtitle: string;
    requestId: string;
    studentId: string;
  };
  messages: Array<{
    author: string;
    time: string;
    text: string;
    own?: boolean;
  }>;
  details: Array<{
    label: string;
    value: string;
  }>;
  quickActions: string[];
  materials: Array<{
    name: string;
    meta: string;
  }>;
};
```

### `POST /api/tutor/messages/{threadId}`

Request:

```ts
{
  text: string;
}
```

Response:

```ts
{
  id: string;
  threadId: string;
  text: string;
  status: 'sent';
}
```

### `GET /api/tutor/subscription`

Страница: `/app/tutor/subscription`.

Ответ:

```ts
type TutorSubscriptionPageData = {
  plan: {
    name: string;
    status: string;
    price: string;
    period: string;
    expiresAt: string;
    nextPayment: string;
    usagePercent: number;
  };
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'cyan';
  }>;
  features: Array<{
    title: string;
    text: string;
    enabled: boolean;
  }>;
  tariffs: Array<{
    id: string;
    name: string;
    price: string;
    period: string;
    description: string;
    badge?: string;
    current?: boolean;
    features: string[];
  }>;
  history: Array<{
    date: string;
    title: string;
    amount: string;
    status: string;
  }>;
  recommendation: {
    title: string;
    text: string;
    action: string;
  };
};
```

### `POST /api/tutor/subscription/checkout`

Request:

```ts
{
  tariffId: string;
}
```

Response:

```ts
{
  checkoutId: string;
  tariffId: string;
  status: 'created';
  paymentUrl?: string;
}
```

### `GET /api/tutor/subscription/checkout`

Страница: `/app/tutor/subscription/checkout`.

Ответ:

```ts
type TutorTariffCheckoutPageData = {
  status: string;
  summary: Array<{
    icon: string;
    value: string;
    label: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'cyan';
  }>;
  paymentMethods: Array<{
    id: string;
    title: string;
    description: string;
    active?: boolean;
  }>;
  qr: {
    title: string;
    hint: string;
    timer: string;
    amount: string;
    footnote: string;
  };
  order: Array<{
    label: string;
    value: string;
  }>;
  timeline: Array<{
    title: string;
    hint: string;
    done?: boolean;
  }>;
  webhook: {
    title: string;
    subtitle: string;
    code: string[];
  };
  documents: Array<{
    label: string;
    status: string;
  }>;
  activation: Array<{
    title: string;
    text: string;
  }>;
};
```

### `POST /api/tutor/subscription/checkout/{checkoutId}/confirm`

Response:

```ts
{
  checkoutId: string;
  status: 'payment-confirmation-requested';
}
```

Важно: это только подписка за CRM-возможности. Не оплата уроков и не гарантийная сделка.

### `GET /api/tutor/settings`

Страница: `/app/tutor/settings`.

Ответ:

```ts
type TutorSettingsPageData = {
  profile: {
    name: string;
    city: string;
    headline: string;
    about: string;
    initials: string;
    moderationStatus: string;
  };
  subjects: string[];
  account: {
    email: string;
    phone: string;
    twoFactor: string;
  };
  visibility: Array<{
    title: string;
    description: string;
    enabled: boolean;
  }>;
  notifications: Array<{
    title: string;
    description: string;
    enabled: boolean;
  }>;
  moderation: {
    status: string;
    description: string;
  };
  danger: {
    title: string;
    description: string;
  };
};
```

### `PATCH /api/tutor/settings`

Request:

```ts
Partial<TutorSettingsPageData>
```

Response:

```ts
{
  status: 'saved';
  updatedAt: string;
}
```

## 7. Admin API

Файл frontend:

```text
src/shared/api/admin.ts
```

Защищенная зона:

```text
/admin/**
```

Требуется роль:

```text
admin
```

### `GET /api/admin/dashboard`

Страница: `/admin`.

Ответ:

```ts
type AdminDashboardPageData = {
  metrics: Array<{
    icon: string;
    label: string;
    value: string;
    delta: string;
    tone: 'teal' | 'indigo' | 'amber' | 'rose';
  }>;
  services: Array<{
    name: string;
    status: string;
    uptime: string;
    latency: string;
    tone: 'green' | 'amber' | 'red';
  }>;
  alerts: Array<{
    id: string;
    title: string;
    description: string;
    severity: 'high' | 'medium' | 'low';
    time: string;
  }>;
  moderationQueue: Array<{
    id: string;
    type: string;
    target: string;
    status: string;
    owner: string;
  }>;
  quickActions: Array<{
    title: string;
    text: string;
    to: string;
  }>;
  audit: Array<{
    id: string;
    actor: string;
    action: string;
    date: string;
  }>;
};
```

### `GET /api/admin/alerts`

Response:

```ts
AdminDashboardPageData['alerts']
```

### `GET /api/admin/users`

Страница: `/admin/users`.

Ответ:

```ts
type AdminUsersPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    title: string;
    meta: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
  }>;
  tabs: string[];
  users: Array<{
    id: string;
    initials: string;
    name: string;
    email: string;
    role: string;
    status: string;
    risk: 'Low' | 'Med' | 'High';
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
    active?: boolean;
  }>;
  selected: {
    initials: string;
    name: string;
    email: string;
    role: string;
    status: string;
    riskTitle: string;
    signals: Array<{
      title: string;
      meta: string;
    }>;
    actions: string[];
  };
  cards: {
    audit: { title: string; text: string; action: string };
    roles: { label: string; value: string; text: string; action: string };
    moderation: { title: string; text: string; action: string };
    note: { title: string; text: string; action: string };
  };
};
```

### `PATCH /api/admin/users/{userId}`

В коде сейчас стоит placeholder `/admin/users/:id`, backend должен реализовать реальный путь с id.

Request:

```ts
{
  action: string;
}
```

Рекомендуемые action:

```text
block
unblock
make_admin
make_moderator
remove_role
request_verification
```

Response:

```ts
{
  userId: string;
  action: string;
  updatedAt: string;
}
```

### `GET /api/admin/site-stats`

Страница: `/admin/site-stats`.

Ответ:

```ts
type AdminSiteStatsPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    title: string;
    meta: string;
    tone: 'indigo' | 'emerald' | 'cyan' | 'amber';
  }>;
  tabs: string[];
  growth: {
    title: string;
    caption: string;
    labels: string[];
    users: number[];
    requests: number[];
    footer: string;
  };
  conversion: {
    label: string;
    value: string;
    text: string;
  };
  quality: Array<{
    label: string;
    value: string;
    percent: number;
  }>;
  funnel: Array<{
    label: string;
    value: string;
    percent: number;
  }>;
  finance: {
    title: string;
    value: string;
    caption: string;
    trend: string;
  };
  geo: Array<{
    city: string;
    value: string;
    percent: number;
  }>;
  events: Array<{
    time: string;
    title: string;
    meta: string;
    tone: 'emerald' | 'amber' | 'indigo';
  }>;
  note: {
    title: string;
    text: string;
    action: string;
  };
};
```

### `GET /api/admin/subscriptions`

Страница: `/admin/subscriptions`.

Ответ:

```ts
type AdminSubscriptionsPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    tone: 'emerald' | 'violet' | 'indigo' | 'rose';
  }>;
  subscriptions: Array<{
    id: string;
    tutor: string;
    plan: 'Free' | 'Pro' | 'Premium';
    status: string;
    expiresAt: string;
    payment: string;
    tone: 'emerald' | 'slate' | 'amber' | 'rose';
  }>;
  revenueByPlan: Array<{
    plan: 'Free' | 'Pro' | 'Premium';
    value: string;
    percent: number;
    tone: 'slate' | 'indigo' | 'violet';
  }>;
  webhookQueue: Array<{
    id: string;
    event: string;
    action: string;
    tone: 'indigo' | 'amber' | 'rose';
  }>;
  planSettings: Array<{
    name: 'Free' | 'Pro' | 'Premium';
    price: string;
    body: string;
  }>;
};
```

### `PATCH /api/admin/subscriptions/{subscriptionId}`

Рекомендуемый request:

```ts
{
  action: 'activate' | 'pause' | 'cancel' | 'change_plan';
  plan?: 'Free' | 'Pro' | 'Premium';
}
```

### `GET /api/admin/audit`

Страница: `/admin/audit`.

Ответ:

```ts
type AdminAuditPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
  }>;
  tabs: string[];
  events: Array<{
    id: string;
    time: string;
    actor: string;
    role: string;
    action: string;
    target: string;
    service: string;
    severity: 'low' | 'medium' | 'high';
    status: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
    active?: boolean;
  }>;
  selected: {
    id: string;
    title: string;
    actor: string;
    description: string;
    meta: Array<{
      label: string;
      value: string;
    }>;
    payload: string[];
  };
  policies: Array<{
    title: string;
    text: string;
    percent: number;
  }>;
  exports: Array<{
    title: string;
    text: string;
    action: string;
  }>;
};
```

### `GET /api/admin/settings`

Страница: `/admin/settings`.

Ответ:

```ts
type AdminSettingsPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
  }>;
  security: Array<{
    title: string;
    description: string;
    enabled: boolean;
  }>;
  roles: Array<{
    title: string;
    description: string;
    users: string;
  }>;
  limits: Array<{
    label: string;
    value: string;
    description: string;
  }>;
  services: Array<{
    name: string;
    status: string;
    description: string;
    tone: 'emerald' | 'amber' | 'rose';
  }>;
  policies: Array<{
    title: string;
    text: string;
    tags: string[];
  }>;
};
```

### `PATCH /api/admin/settings`

Рекомендуемый request:

```ts
Partial<AdminSettingsPageData>
```

## 8. Moderator API

Файл frontend:

```text
src/shared/api/admin.ts
```

Защищенная зона:

```text
/moderator/**
```

Требуется роль:

```text
moderator, admin
```

### `GET /api/moderator/complaints`

Страница: `/moderator/complaints`.

Ответ:

```ts
type ModeratorComplaintsPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    trend: string;
    tone: 'rose' | 'amber' | 'emerald' | 'indigo';
  }>;
  tabs: string[];
  complaints: Array<{
    id: string;
    initials: string;
    direction: string;
    category: string;
    status: string;
    subject: string;
    summary: string;
    time: string;
    evidence: string;
    owner: string;
    tone: 'rose' | 'amber' | 'indigo' | 'slate';
    active?: boolean;
  }>;
  selected: {
    id: string;
    status: string;
    subject: string;
    copy: string;
    participants: Array<{
      initials: string;
      name: string;
      role: string;
    }>;
    evidence: Array<{
      label: string;
      ready: boolean;
    }>;
    impact: {
      label: string;
      hint: string;
    };
    actions: string[];
  };
  sla: Array<{
    label: string;
    value: string;
    percent: number;
  }>;
  weekly: {
    bars: number[];
    labels: string[];
    hint: string;
  };
  rules: {
    title: string;
    body: string;
    tags: string[];
  };
};
```

Бизнес-правило по жалобам:

- клиент может пожаловаться на отзыв/комментарий к преподавателю;
- клиент может пожаловаться на профиль преподавателя;
- клиент может пожаловаться на сообщение преподавателя;
- преподаватель может пожаловаться на клиента;
- TutorSpace не модерирует споры по оплате уроков как гарант, потому что платформа берет оплату только за подписку CRM.

### `POST /api/moderator/complaints/{complaintId}/resolve`

Request:

```ts
{
  action: string;
}
```

Рекомендуемые action:

```text
dismiss
warn_user
hide_review
hide_profile
restrict_messages
block_user
request_more_info
```

Response:

```ts
{
  complaintId: string;
  action: string;
  status: 'queued' | 'resolved';
  resolvedAt: string;
}
```

### `GET /api/moderator/review-moderation`

Страница: `/moderator/review-moderation`.

Название страницы осталось историческим, но по факту экран покрывает очередь модерации профилей/отзывов.

Ответ:

```ts
type ModeratorReviewModerationPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    trend: string;
    tone: 'rose' | 'amber' | 'emerald' | 'indigo';
  }>;
  tabs: string[];
  profiles: Array<{
    id: string;
    initials: string;
    name: string;
    subject: string;
    type: string;
    status: string;
    summary: string;
    score: string;
    documents: string;
    owner: string;
    tone: 'rose' | 'amber' | 'indigo' | 'emerald';
    active?: boolean;
  }>;
  selected: {
    id: string;
    status: string;
    initials: string;
    name: string;
    subject: string;
    experience: string;
    risk: string;
    documents: Array<{
      title: string;
      subtitle: string;
      status: string;
    }>;
    checklist: Array<{
      label: string;
      done: boolean;
    }>;
    score: string;
    note: string;
  };
  pipeline: Array<{
    label: string;
    value: string;
    percent: number;
  }>;
  rubric: string[];
  decision: {
    title: string;
    body: string;
    tags: string[];
  };
};
```

### `POST /api/moderator/review-moderation/{id}/approve`

Request:

```ts
{
  decision: 'approve' | 'changes';
}
```

Response:

```ts
{
  profileId: string;
  decision: 'approve' | 'changes';
  status: 'approved' | 'changes_requested';
  decidedAt: string;
}
```

### `POST /api/moderator/review-moderation/{id}/reject`

Рекомендуемый request:

```ts
{
  reason: string;
}
```

## 9. Роуты frontend и нужные API

| Frontend route | Основной API |
| --- | --- |
| `/` | static home content, `GET /api/marketplace/subjects`, `GET /api/marketplace/tutors` |
| `/catalog` | `GET /api/marketplace/subjects`, `GET /api/marketplace/tutors` |
| `/catalog/subject/:subjectSlug` | `GET /api/marketplace/subjects`, `GET /api/marketplace/tutors?subject=...` |
| `/tutors/:slug` | `GET /api/marketplace/tutors/{slug}`, `POST /api/marketplace/tutors/{tutorId}/requests` |
| `/faq` | static faq content |
| `/auth/sign-in` | `POST /api/auth/login` |
| `/auth/sign-up/parent` | `POST /api/auth/register` with `ROLE_CLIENT` |
| `/auth/sign-up/tutor` | `POST /api/auth/register` with `ROLE_TUTOR` |
| `/auth/forgot-password` | mock пока нет backend ручки |
| `/auth/reset-password/:token` | mock пока нет backend ручки |
| `/app/client/applications` | `GET /api/client/applications` |
| `/app/client/applications/:id` | `GET /api/client/applications/{id}`, `PATCH /api/client/applications/{id}/cancel` |
| `/app/client/schedule` | `GET /api/client/schedule` |
| `/app/client/messages` | `GET /api/client/messages`, `POST /api/client/messages/{threadId}` |
| `/app/client/favorites` | `GET /api/client/favorites`, `POST/DELETE /api/client/favorites/{tutorId}` |
| `/app/client/history` | `GET /api/client/history` |
| `/app/client/settings` | `GET /api/client/settings`, `PATCH /api/client/settings` |
| `/app/tutor/requests` | `GET /api/tutor/requests`, `PATCH /api/tutor/requests/{id}/accept`, `PATCH /api/tutor/requests/{id}/reject` |
| `/app/tutor/calendar` | `GET /api/tutor/calendar`, `POST /api/tutor/lessons` |
| `/app/tutor/students` | `GET /api/tutor/students`, `POST /api/tutor/students`, `POST /api/tutor/students/{id}/reports` |
| `/app/tutor/profile` | `GET /api/tutor/profile`, `PATCH /api/tutor/profile`, `POST /api/tutor/profile/documents` |
| `/app/tutor/messages` | `GET /api/tutor/messages`, `POST /api/tutor/messages/{threadId}` |
| `/app/tutor/subscription` | `GET /api/tutor/subscription`, `POST /api/tutor/subscription/checkout` |
| `/app/tutor/subscription/checkout` | `GET /api/tutor/subscription/checkout`, `POST /api/tutor/subscription/checkout/{checkoutId}/confirm` |
| `/app/tutor/settings` | `GET /api/tutor/settings`, `PATCH /api/tutor/settings` |
| `/admin` | `GET /api/admin/dashboard`, `GET /api/admin/alerts` |
| `/admin/users` | `GET /api/admin/users`, `PATCH /api/admin/users/{userId}` |
| `/admin/site-stats` | `GET /api/admin/site-stats` |
| `/admin/subscriptions` | `GET /api/admin/subscriptions`, `PATCH /api/admin/subscriptions/{subscriptionId}` |
| `/admin/audit` | `GET /api/admin/audit` |
| `/admin/settings` | `GET /api/admin/settings`, `PATCH /api/admin/settings` |
| `/moderator/complaints` | `GET /api/moderator/complaints`, `POST /api/moderator/complaints/{id}/resolve` |
| `/moderator/review-moderation` | `GET /api/moderator/review-moderation`, `POST /api/moderator/review-moderation/{id}/approve`, `POST /api/moderator/review-moderation/{id}/reject` |

## 10. Распределение по backend-сервисам

Рекомендуемая привязка endpoint’ов к сервисам:

| Service | Endpoint prefix |
| --- | --- |
| `auth-service` | `/api/auth/**` |
| `marketplace-service` | `/api/marketplace/**` |
| `crm-service` | `/api/client/**`, `/api/tutor/requests/**`, `/api/tutor/calendar/**`, `/api/tutor/students/**`, `/api/tutor/messages/**` |
| `subscription-service` | `/api/tutor/subscription/**`, `/api/admin/subscriptions/**` |
| `admin-service` | `/api/admin/**`, `/api/moderator/**` |
| `notification-service` | напрямую frontend не дергает |
| `api-gateway` | единая точка `/api/**`, CORS, JWT forwarding |

## 11. События, которые backend должен заложить

Frontend напрямую Kafka не дергает, но по сценариям UI нужны события:

| Event | Кто публикует | Кто слушает |
| --- | --- | --- |
| `UserRegistered` | `auth-service` | `notification-service`, `crm-service` |
| `TutorProfileUpdated` | `marketplace-service` или `crm-service` | `admin-service`, `notification-service` |
| `TutorDocumentUploaded` | `marketplace-service` | `admin-service` |
| `RequestCreated` | `marketplace-service` или `crm-service` | `crm-service`, `notification-service` |
| `ApplicationAccepted` | `crm-service` | `notification-service` |
| `ApplicationRejected` | `crm-service` | `notification-service` |
| `LessonCreated` | `crm-service` | `notification-service` |
| `MessageCreated` | `crm-service` | `notification-service`, `admin-service` для жалоб |
| `ComplaintCreated` | `admin-service` | `notification-service` |
| `ComplaintResolved` | `admin-service` | `notification-service` |
| `SubscriptionCheckoutCreated` | `subscription-service` | `notification-service` |
| `SubscriptionActivated` | `subscription-service` | `notification-service`, `admin-service` |

## 12. Что сейчас нужно поправить во frontend при подключении real backend

1. Включить `VITE_API_MODE=live`.
2. Проверить, что `VITE_API_BASE_URL` указывает на gateway или backend.
3. Если backend отдает `{ data: ... }`, обновить `apiRequest`, чтобы возвращать `json.data`.
4. В `src/shared/api/admin.ts` заменить placeholder paths:

```ts
'/admin/users/:id'
'/moderator/complaints/:id/resolve'
'/moderator/review-moderation/:id/approve'
```

на реальные template strings:

```ts
`/admin/users/${userId}`
`/moderator/complaints/${complaintId}/resolve`
`/moderator/review-moderation/${profileId}/approve`
```

5. Подключить статичные client pages к API:

```text
ClientApplicationsPage
ClientApplicationDetailsPage
ClientSchedulePage
ClientMessagesPage
ClientFavoritesPage
ClientHistoryPage
```

6. Уточнить единый контракт для `/api/tutor/profile`, потому что сейчас frontend использует его и как workspace profile, и как public profile settings.
7. Refresh-token flow уже реализован во frontend через автоматическую попытку `POST /api/auth/refresh` на `401`.

## 13. MVP-приоритет для backend

Чтобы frontend быстрее перейти с mock на live, достаточно реализовать в таком порядке:

1. `POST /api/auth/login`
2. `POST /api/auth/register`
3. `POST /api/auth/refresh`
4. `GET /api/marketplace/subjects`
5. `GET /api/marketplace/tutors`
6. `GET /api/marketplace/tutors/{slug}`
7. `POST /api/marketplace/tutors/{tutorId}/requests`
8. `GET /api/client/applications`
9. `GET /api/tutor/requests`
10. `PATCH /api/tutor/requests/{id}/accept`
11. `GET /api/tutor/profile`
12. `PATCH /api/tutor/profile`
13. `GET /api/tutor/subscription`
14. `POST /api/tutor/subscription/checkout`
15. `GET /api/moderator/complaints`
16. `POST /api/moderator/complaints/{id}/resolve`

## 14. Источники в frontend

Основные файлы, по которым составлен контракт:

```text
src/shared/api/http.ts
src/shared/api/public.ts
src/shared/api/client.ts
src/shared/api/tutor.ts
src/shared/api/admin.ts
src/shared/types.ts
src/shared/config/roadmap.ts
src/App.tsx
src/components/route-guards.tsx
src/shared/auth/session.ts
```
