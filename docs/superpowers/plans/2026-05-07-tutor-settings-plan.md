# Tutor Settings Page Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Обновить страницу `/app/tutor/settings` — убрать секцию аккаунта (email/пароль), заменить readonly-предметы на редактируемый multiselect, переименовать поле в ФИО, добавить toggle скрытия профиля из каталога.

**Architecture:** Минимальные изменения в существующей странице `TutorSettingsPage.tsx` + обновление типа `TutorSettingsPageData` и mock-данных. Без разделения на вкладки, без новых компонентов.

**Tech Stack:** React + TypeScript, custom Rollup/Vite build, no external UI libraries.

---

## File Structure

| File | Responsibility |
|------|---------------|
| `frontend/src/shared/types.ts` | Тип `TutorSettingsPageData` — убрать `account`, `danger`, добавить `availableSubjects`, `isHidden` |
| `frontend/src/shared/api/mock-data.ts` | Объект `tutorSettingsPage` — синхронизировать с новым типом |
| `frontend/src/pages/app/TutorSettingsPage.tsx` | Основной компонент — обновить UI, логику, валидацию |

---

## Task 1: Обновить тип `TutorSettingsPageData`

**Files:**
- Modify: `frontend/src/shared/types.ts:680-713`

- [ ] **Step 1: Удалить поля `account` и `danger`, добавить `availableSubjects` и `isHidden`**

Заменить существующий тип на:

```ts
export type TutorSettingsPageData = {
  profile: {
    name: string;
    city: string;
    headline: string;
    about: string;
    initials: string;
    moderationStatus: string;
  };
  subjects: string[];
  availableSubjects: string[];
  isHidden: boolean;
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
};
```

- [ ] **Step 2: Проверить что нет других использований удалённых полей**

Run: `grep -r "\.account\." frontend/src/ --include="*.ts" --include="*.tsx"`
Expected: Только `TutorSettingsPage.tsx` использует `account` и `danger`.

---

## Task 2: Обновить mock-данные `tutorSettingsPage`

**Files:**
- Modify: `frontend/src/shared/api/mock-data.ts:963-1016`

- [ ] **Step 1: Удалить `account` и `danger`, добавить `availableSubjects` и `isHidden`**

Заменить объект `tutorSettingsPage` на:

```ts
export const tutorSettingsPage: TutorSettingsPageData = {
  profile: {
    name: 'Анна Смирнова',
    city: 'Москва',
    headline: 'Математика без страха: ЕГЭ, ОГЭ и школьная программа',
    about:
      'Помогаю школьникам увидеть логику в задачах, спокойно готовиться к экзаменам и регулярно отслеживать прогресс. После диагностики собираю понятный план занятий.',
    initials: 'АС',
    moderationStatus: 'Аватар и описание проверены',
  },
  subjects: ['Математика', 'ЕГЭ', 'ОГЭ'],
  availableSubjects: ['Математика', 'Английский язык', 'Русский язык', 'Физика', 'Химия', 'Информатика', 'История', 'Биология', 'Литература', 'Обществознание', 'ЕГЭ', 'ОГЭ'],
  isHidden: false,
  visibility: [
    {
      title: 'Разрешить быстрые заявки',
      description: 'Клиенты смогут отправить заявку без предварительного чата.',
      enabled: true,
    },
  ],
  notifications: [
    {
      title: 'Сообщения',
      description: 'Новые сообщения от родителей и учеников.',
      enabled: true,
    },
    {
      title: 'Заявки',
      description: 'Новые заявки и изменения статусов.',
      enabled: true,
    },
    {
      title: 'Календарь',
      description: 'Напоминания о занятиях и переносах.',
      enabled: false,
    },
  ],
  moderation: {
    status: 'Профиль опубликован',
    description: 'Изменения описания и документов могут попасть на повторную проверку модератором.',
  },
};
```

Примечание: убран `visibility[0]` "Показывать профиль в каталоге", так как его функция заменена на `isHidden`.

---

## Task 3: Обновить `TutorSettingsPage.tsx`

**Files:**
- Modify: `frontend/src/pages/app/TutorSettingsPage.tsx`

### Step 1: Обновить imports и state (строки 1-35)

- [ ] Убрать `validateEmail` и `validatePhone` (строки 10-16)
- [ ] Убрать `account` из деструктуризации в `validate`
- [ ] Убрать `saveError` и `saveSuccess` state если не используются

### Step 2: Обновить `validate()` (строки 37-72)

- [ ] Убрать валидацию email и phone
- [ ] Обновить сообщение об ошибке имени

```ts
const validate = (): boolean => {
  const next: FormErrors = {};
  const p = settings.profile;

  if (!p.name || p.name.trim().length < 2) {
    next.name = 'ФИО должно содержать минимум 2 символа';
  } else if (p.name.length > 100) {
    next.name = 'ФИО не должно превышать 100 символов';
  }

  if (!p.city || p.city.trim().length < 1) {
    next.city = 'Укажите город';
  } else if (p.city.length > 100) {
    next.city = 'Город не должен превышать 100 символов';
  }

  if (p.headline.length > 200) {
    next.headline = 'Заголовок не должен превышать 200 символов';
  }

  if (p.about.length > 600) {
    next.about = 'Описание не должно превышать 600 символов';
  }

  setErrors(next);
  return Object.keys(next).length === 0;
};
```

### Step 3: Добавить обработчик предметов

- [ ] Добавить `handleSubjectToggle` после `handleToggle`:

```ts
const handleSubjectToggle = (subject: string) => {
  setSettings((prev) => {
    const current = prev.subjects;
    const next = current.includes(subject)
      ? current.filter((s) => s !== subject)
      : [...current, subject];
    return { ...prev, subjects: next };
  });
  setSaveSuccess(false);
  setSaveError(null);
};
```

### Step 4: Добавить обработчик `isHidden`

- [ ] Добавить `handleHiddenToggle`:

```ts
const handleHiddenToggle = () => {
  setSettings((prev) => ({ ...prev, isHidden: !prev.isHidden }));
  setSaveSuccess(false);
  setSaveError(null);
};
```

### Step 5: Обновить label имени (строка 154)

- [ ] Заменить `<span>Имя в профиле</span>` на `<span>ФИО</span>`

### Step 6: Заменить секцию предметов (строки 206-219)

- [ ] Заменить readonly-список на multiselect:

```tsx
<section className="tutor-settings-card tutor-settings-subjects">
  <div className="tutor-settings-card__heading tutor-settings-card__heading--row">
    <h2>Предметы и форматы</h2>
  </div>
  <div className="tutor-settings-subject-list tutor-settings-subject-list--editable">
    {settings.availableSubjects.map((subject) => (
      <label key={subject} className="tutor-settings-subject-checkbox">
        <input
          type="checkbox"
          checked={settings.subjects.includes(subject)}
          onChange={() => handleSubjectToggle(subject)}
        />
        <span>{subject}</span>
      </label>
    ))}
  </div>
  <p className="tutor-settings-notice">Изменение предметов может отправить профиль на повторную модерацию.</p>
</section>
```

### Step 7: Убрать секцию "Опасная зона" (строки 221-230)

- [ ] Удалить `<section className="tutor-settings-card tutor-settings-danger">...</section>`

### Step 8: Убрать секцию "Аккаунт" (строки 234-262)

- [ ] Удалить `<section className="tutor-settings-card">` с аккаунтом полностью

### Step 9: Добавить toggle скрытия профиля в секцию "Видимость"

- [ ] Перед `settings.visibility.map(...)` добавить статичный toggle:

```tsx
<article className="tutor-settings-toggle">
  <div>
    <strong>Скрыть профиль из каталога</strong>
    <p>Ваша карточка не будет отображаться в поиске и каталоге</p>
  </div>
  <button
    className={cn('settings-switch', settings.isHidden && 'is-on')}
    type="button"
    aria-pressed={settings.isHidden}
    onClick={handleHiddenToggle}
  />
</article>
```

### Step 10: Обновить derivation initials при изменении имени

- [ ] В `handleChange` для поля `name` добавить авто-обновление `initials`:

```ts
const handleChange = (section: 'profile', field: string, value: string) => {
  setSettings((prev) => {
    const nextProfile = { ...prev.profile, [field]: value };
    if (field === 'name') {
      nextProfile.initials = value
        .split(' ')
        .map((w) => w[0])
        .filter(Boolean)
        .slice(0, 2)
        .join('')
        .toUpperCase();
    }
    return { ...prev, profile: nextProfile };
  });
  setSaveSuccess(false);
  setSaveError(null);
};
```

Примечание: `handleChange` больше не принимает `'account'`, только `'profile'`.

---

## Task 4: Проверка типов и dev-сервер

**Files:**
- N/A (verification only)

- [ ] **Step 1: Проверить типы**

Run: `cd frontend && npm run typecheck`
Expected: No errors

- [ ] **Step 2: Запустить dev-сервер и проверить страницу**

Run: `cd frontend && npm run dev`
Open: `http://localhost:4173/app/tutor/settings`
Verify:
- [ ] Поле "ФИО" вместо "Имя в профиле"
- [ ] Нет секции "Аккаунт"
- [ ] Предметы — чекбоксы, можно выбирать/убирать
- [ ] Есть toggle "Скрыть профиль из каталога"
- [ ] Нет секции "Опасная зона"
- [ ] Сохранение работает без ошибок

---

## Self-Review

**Spec coverage:**
- ✅ Убрать секцию «Аккаунт» — Task 3 Step 8
- ✅ Переименовать «Имя в профиле» → «ФИО» — Task 3 Step 5
- ✅ Заменить readonly-предметы на multiselect — Task 3 Step 6
- ✅ Добавить toggle «Скрыть профиль из каталога» — Task 3 Step 9
- ✅ Убрать секцию «Опасная зона» — Task 3 Step 7
- ✅ Обновить типы — Task 1
- ✅ Обновить mock — Task 2

**Placeholder scan:** ✅ Нет TBD/TODO.

**Type consistency:** ✅ `availableSubjects` и `isHidden` добавлены в тип и mock.
