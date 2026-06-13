# Design Spec: Настройки профиля репетитора (/app/tutor/settings)

## Date

2026-05-07

## Context

Страница `/app/tutor/settings` существует, но содержит лишние поля (email, телефон, смена пароля), которые пользователь пока не должен редактировать. Нужно привести страницу в соответствие с реальной задачей: редактирование профильных данных (ФИО, город, заголовок, о себе), управление предметами, и возможность скрыть карточку из каталога.

## Goals

1. Убрать из UI секцию «Аккаунт» (email, телефон, смена пароля, 2FA) — не редактируем пока.
2. Переименовать «Имя в профиле» → «ФИО» (одно текстовое поле).
3. Заменить readonly-список предметов на редактируемый multiselect (выбор из заранее заданного списка).
4. Добавить toggle «Скрыть профиль из каталога» в секцию «Видимость».
5. Убрать секцию «Опасная зона» (кнопку «Скрыть профиль»), так как функционал переносится в toggle.
6. Обновить типы и mock-данные.

## Non-Goals

- Не добавляем загрузку фото аватара (пользователь сказал «пока не будем»).
- Не добавляем редактирование email/пароля.
- Не меняем общую структуру страницы (без вкладок).
- Не создаём отдельный компонент multiselect (inline-реализация в странице).

## Architecture

### Data Flow

```
TutorSettingsPage.tsx
  ├── useEffect: getTutorSettings() → setSettings()
  ├── handleChange('profile', field, value) → setSettings()
  ├── handleSubjectsToggle(subject) → setSettings()
  ├── handleToggle('visibility', index) → setSettings()
  ├── validate() → формирует errors
  └── handleSave() → updateTutorSettings(settings) → setSettings(updated)
```

### State Shape (TutorSettingsPageData)

```ts
type TutorSettingsPageData = {
  profile: {
    name: string;        // ФИО
    city: string;
    headline: string;
    about: string;
    initials: string;    // derived from name
    moderationStatus: string;
  };
  subjects: string[];    // выбранные предметы
  availableSubjects: string[]; // список всех доступных предметов для выбора
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

### Files to Change

| File | Change |
|------|--------|
| `frontend/src/pages/app/TutorSettingsPage.tsx` | Основные изменения UI и логики |
| `frontend/src/shared/types.ts` | Обновить `TutorSettingsPageData` |
| `frontend/src/shared/api/mock-data.ts` | Обновить `tutorSettingsPage` |
| `frontend/src/shared/api/tutor.ts` | Обновить типы если нужно |

## UI Changes

### Секция «Публичная информация»
- Поле **«ФИО»** (было «Имя в профиле»)
- Поле **«Город»**
- Поле **«Заголовок»**
- Аватар (readonly, кнопка «Загрузить фото» остаётся disabled или убирается — оставляем как есть, пользователь сказал «пока не будем реализовывать»)

### Секция «О себе»
- Без изменений (textarea, max 600 символов)

### Секция «Предметы и форматы»
- Убрать readonly-теги
- Добавить список чекбоксов (или chips с toggle) из `availableSubjects`
- Выбранные предметы — `subjects`
- Предупреждение о модерации оставляем

### Секция «Видимость»
- Добавить новый toggle:
  - Title: «Скрыть профиль из каталога»
  - Description: «Ваша карточка не будет отображаться в поиске и каталоге»
- Оставить существующие visibility-toggles если есть

### Секция «Уведомления»
- Без изменений

### Секция «Модерация»
- Без изменений

### Убрать
- **Секция «Аккаунт»** полностью
- **Секция «Опасная зона»** полностью

## Validation

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

## API Contract (существующий)

`GET /api/tutor/settings` → `TutorSettingsPageData`
`PATCH /api/tutor/settings` → `Partial<TutorSettingsPageData>`

Backend пока работает с mock, но тип должен быть синхронизирован.

## Edge Cases

1. **Пустой список предметов** — показывать плейсхолдер «Выберите хотя бы один предмет» или оставить пустым? Оставляем пустым, но валидация может потребовать минимум один.
2. **Все предметы выбраны** — чекбоксы все отмечены, disabled state не нужен.
3. **Initials** — должны автоматически обновляться при изменении ФИО (frontend-derivation: первые буквы слов).

## Accessibility

- Чекбоксы предметов — native `<input type="checkbox">` с `<label>`
- Toggle «Скрыть профиль» — существующий компонент `settings-switch` с `aria-pressed`
- Сохраняем WCAG AA контраст

## Implementation Order

1. Обновить тип `TutorSettingsPageData` в `types.ts`
2. Обновить `tutorSettingsPage` в `mock-data.ts`
3. Изменить `TutorSettingsPage.tsx`:
   a. Убрать account-валидацию и account-секцию
   b. Переименовать label
   c. Заменить subjects на multiselect
   d. Добавить toggle visibility
   e. Убрать danger-секцию
4. Проверить типы (`npm run typecheck`)
5. Запустить dev-сервер и проверить вручную
