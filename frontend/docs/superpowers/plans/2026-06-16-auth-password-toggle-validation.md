# Auth Password Toggle And Registration Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a show/hide eye toggle to every password input and field-level dynamic validation to both registration pages without changing backend contracts, route map, or design tokens.

**Architecture:** Reusable `PasswordInput` component (wraps `TextInput` with a right-side SVG button toggling `type` between `password` and `text`) + pure `auth-helpers.ts` validation functions + a touched/error state machine inside each form + small additions to `FormField` and `global.css`.

**Tech Stack:** React 19, TypeScript 5, Vitest 3, existing OKLCH design tokens.

**Branch:** `feature/auth-password-toggle-validation`. Do not push to `dev` until the user confirms.

---

## File Structure

- `frontend/src/components/primitives.tsx` — modify: add `PasswordInput` (with `EyeIcon`), extend `FormField` with `error` and `valid` props.
- `frontend/src/pages/public/auth-helpers.ts` — create: pure validators returning `{ valid, message }`.
- `frontend/src/pages/public/auth-helpers.test.ts` — create: Vitest coverage for validators.
- `frontend/src/pages/public/AuthPages.tsx` — modify: use `PasswordInput`, add touched/error state, add inline validation in `ParentRegistrationPage` and `TutorRegistrationPage`, add eye toggle in `SignInPage` and `ResetPasswordPage`.
- `frontend/src/styles/global.css` — modify: add `.password-input`, `.password-input__toggle`, `.control--error`, `.control--success`, `.form-error` styles; introduce `--success` token if missing.

---

## Task 1: Add `validateName` and `validateEmail` (TDD)

**Files:**
- Create: `frontend/src/pages/public/auth-helpers.ts`
- Create: `frontend/src/pages/public/auth-helpers.test.ts`

- [ ] **Step 1: Write the failing test for `validateName` and `validateEmail`**

Create `frontend/src/pages/public/auth-helpers.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { validateEmail, validateName } from './auth-helpers';

describe('validateName', () => {
  it('rejects an empty name', () => {
    expect(validateName('')).toEqual({ valid: false, message: 'Введите имя' });
  });

  it('rejects a whitespace-only name', () => {
    expect(validateName('   ')).toEqual({ valid: false, message: 'Введите имя' });
  });

  it('rejects a name with one character', () => {
    expect(validateName('A')).toEqual({ valid: false, message: 'Имя должно содержать минимум 2 символа' });
  });

  it('accepts a two-character name', () => {
    expect(validateName('Ан')).toEqual({ valid: true, message: null });
  });

  it('accepts a long name with surrounding spaces after trim', () => {
    expect(validateName('  Иван Петров  ')).toEqual({ valid: true, message: null });
  });
});

describe('validateEmail', () => {
  it('rejects an empty email', () => {
    expect(validateEmail('')).toEqual({ valid: false, message: 'Введите email' });
  });

  it('rejects a malformed email', () => {
    expect(validateEmail('user@')).toEqual({ valid: false, message: 'Некорректный формат email' });
    expect(validateEmail('user@host')).toEqual({ valid: false, message: 'Некорректный формат email' });
    expect(validateEmail('plainstring')).toEqual({ valid: false, message: 'Некорректный формат email' });
  });

  it('accepts a standard email', () => {
    expect(validateEmail('user@example.com')).toEqual({ valid: true, message: null });
  });

  it('accepts a plus-suffix email', () => {
    expect(validateEmail('user+tag@example.co.uk')).toEqual({ valid: true, message: null });
  });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run from `frontend/`:
```bash
npm test -- auth-helpers.test.ts
```
Expected: FAIL — module `./auth-helpers` not found.

- [ ] **Step 3: Implement the minimal code**

Create `frontend/src/pages/public/auth-helpers.ts`:

```ts
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const NAME_MIN_LENGTH = 2;

export type FieldValidation = { valid: boolean; message: string | null };

export function validateName(name: string): FieldValidation {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    return { valid: false, message: 'Введите имя' };
  }
  if (trimmed.length < NAME_MIN_LENGTH) {
    return { valid: false, message: `Имя должно содержать минимум ${NAME_MIN_LENGTH} символа` };
  }
  return { valid: true, message: null };
}

export function validateEmail(email: string): FieldValidation {
  const trimmed = email.trim();
  if (trimmed.length === 0) {
    return { valid: false, message: 'Введите email' };
  }
  if (!EMAIL_REGEX.test(trimmed)) {
    return { valid: false, message: 'Некорректный формат email' };
  }
  return { valid: true, message: null };
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run from `frontend/`:
```bash
npm test -- auth-helpers.test.ts
```
Expected: PASS — all 9 cases.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/public/auth-helpers.ts frontend/src/pages/public/auth-helpers.test.ts
git commit -m "feat(auth): add validateName and validateEmail helpers"
```

---

## Task 2: Add `validatePassword`, `validatePasswordMatch`, `validateSubjects` and `runRegistrationValidations`

**Files:**
- Modify: `frontend/src/pages/public/auth-helpers.ts`
- Modify: `frontend/src/pages/public/auth-helpers.test.ts`

- [ ] **Step 1: Add the failing tests**

Append to `frontend/src/pages/public/auth-helpers.test.ts`:

```ts
import { runRegistrationValidations, validatePassword, validatePasswordMatch, validateSubjects } from './auth-helpers';

describe('validatePassword', () => {
  it('rejects an empty password', () => {
    expect(validatePassword('', 6)).toEqual({ valid: false, message: 'Введите пароль' });
  });

  it('rejects a password shorter than minLength', () => {
    expect(validatePassword('12345', 6)).toEqual({ valid: false, message: 'Минимум 6 символов' });
    expect(validatePassword('1234567', 8)).toEqual({ valid: false, message: 'Минимум 8 символов' });
  });

  it('accepts a password at exactly minLength', () => {
    expect(validatePassword('123456', 6)).toEqual({ valid: true, message: null });
    expect(validatePassword('12345678', 8)).toEqual({ valid: true, message: null });
  });
});

describe('validatePasswordMatch', () => {
  it('rejects when confirm is empty', () => {
    expect(validatePasswordMatch('123456', '')).toEqual({ valid: false, message: 'Повторите пароль' });
  });

  it('rejects when values differ', () => {
    expect(validatePasswordMatch('123456', '123457')).toEqual({ valid: false, message: 'Пароли не совпадают' });
  });

  it('accepts identical values', () => {
    expect(validatePasswordMatch('123456', '123456')).toEqual({ valid: true, message: null });
  });
});

describe('validateSubjects', () => {
  it('rejects an empty selection', () => {
    expect(validateSubjects([])).toEqual({ valid: false, message: 'Выберите хотя бы один предмет' });
  });

  it('accepts a non-empty selection', () => {
    expect(validateSubjects(['Математика'])).toEqual({ valid: true, message: null });
  });
});

describe('runRegistrationValidations', () => {
  it('reports errors for all invalid fields and leaves valid fields empty', () => {
    const result = runRegistrationValidations({
      name: 'A',
      email: 'broken',
      password: '12345',
      subjects: [],
    });
    expect(result.name).toBeTruthy();
    expect(result.email).toBeTruthy();
    expect(result.password).toBeTruthy();
    expect(result.subjects).toBeTruthy();
  });

  it('returns nulls for every field when the values are all valid', () => {
    const result = runRegistrationValidations({
      name: 'Иван',
      email: 'i@example.com',
      password: '123456',
      subjects: ['Математика'],
    });
    expect(result).toEqual({ name: null, email: null, password: null, subjects: null });
  });
});
```

Update the top-level import to:

```ts
import { runRegistrationValidations, validateEmail, validateName, validatePassword, validatePasswordMatch, validateSubjects } from './auth-helpers';
```

- [ ] **Step 2: Run the test to verify the new cases fail**

Run from `frontend/`:
```bash
npm test -- auth-helpers.test.ts
```
Expected: FAIL — `validatePassword`, `validatePasswordMatch`, `validateSubjects`, `runRegistrationValidations` are not exported.

- [ ] **Step 3: Implement the missing functions**

Append to `frontend/src/pages/public/auth-helpers.ts`:

```ts
export function validatePassword(password: string, minLength: number): FieldValidation {
  if (password.length === 0) {
    return { valid: false, message: 'Введите пароль' };
  }
  if (password.length < minLength) {
    return { valid: false, message: `Минимум ${minLength} символов` };
  }
  return { valid: true, message: null };
}

export function validatePasswordMatch(password: string, confirmPassword: string): FieldValidation {
  if (confirmPassword.length === 0) {
    return { valid: false, message: 'Повторите пароль' };
  }
  if (password !== confirmPassword) {
    return { valid: false, message: 'Пароли не совпадают' };
  }
  return { valid: true, message: null };
}

export function validateSubjects(subjects: readonly string[]): FieldValidation {
  if (subjects.length === 0) {
    return { valid: false, message: 'Выберите хотя бы один предмет' };
  }
  return { valid: true, message: null };
}

export type RegistrationValues = {
  name: string;
  email: string;
  password: string;
  subjects: readonly string[];
};

export type RegistrationErrors = Record<keyof RegistrationValues, string | null>;

export function runRegistrationValidations(values: RegistrationValues): RegistrationErrors {
  return {
    name: validateName(values.name).message,
    email: validateEmail(values.email).message,
    password: validatePassword(values.password, 6).message,
    subjects: validateSubjects(values.subjects).message,
  };
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run from `frontend/`:
```bash
npm test -- auth-helpers.test.ts
```
Expected: PASS — all cases across the four `describe` blocks.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/public/auth-helpers.ts frontend/src/pages/public/auth-helpers.test.ts
git commit -m "feat(auth): add password, match, and subjects validators"
```

---

## Task 3: Add `PasswordInput` and extend `FormField` in primitives

**Files:**
- Modify: `frontend/src/components/primitives.tsx`

- [ ] **Step 1: Extend `FormField` to accept `error` and `valid` props**

In `frontend/src/components/primitives.tsx`, replace the `FormField` definition with:

```tsx
export function FormField({
  label,
  hint,
  error,
  valid,
  children,
}: {
  label: string;
  hint?: string;
  error?: string;
  valid?: boolean;
  children: ReactNode;
}) {
  const modifiers: string[] = [];
  if (error) modifiers.push('is-invalid');
  if (valid && !error) modifiers.push('is-valid');
  return (
    <label className={cn('form-field', modifiers.length > 0 ? modifiers.join(' ') : undefined)}>
      <span className="form-label">{label}</span>
      {children}
      {error ? <span className="form-error">{error}</span> : null}
      {!error && hint ? <span className="form-hint">{hint}</span> : null}
    </label>
  );
}
```

- [ ] **Step 2: Add the `EyeIcon` and `PasswordInput` components**

At the bottom of `frontend/src/components/primitives.tsx`, append:

```tsx
import { forwardRef, useState, type InputHTMLAttributes, type SVGProps } from 'react';

function EyeIcon({ open, ...props }: { open: boolean } & SVGProps<SVGSVGElement>) {
  if (open) {
    return (
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
      <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-6.5 0-10-7-10-7a18.45 18.45 0 0 1 4.22-5.39" />
      <path d="M9.9 4.24A10.94 10.94 0 0 1 12 4c6.5 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19" />
      <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
      <line x1="2" y1="2" x2="22" y2="22" />
    </svg>
  );
}

export const PasswordInput = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function PasswordInput({ className, ...props }, ref) {
    const [visible, setVisible] = useState(false);
    return (
      <span className={cn('password-input', className)}>
        <input
          {...props}
          ref={ref}
          type={visible ? 'text' : 'password'}
          className={cn('control', (props as { invalid?: boolean }).invalid ? 'control--error' : undefined)}
        />
        <button
          type="button"
          className="password-input__toggle"
          aria-label={visible ? 'Скрыть пароль' : 'Показать пароль'}
          aria-pressed={visible}
          onClick={() => setVisible((value) => !value)}
        >
          <EyeIcon open={visible} />
        </button>
      </span>
    );
  }
);
```

Also remove the duplicate `InputHTMLAttributes` import at the top of the file because the new `import` block at the bottom of the file re-imports it. To keep the diff minimal, leave the top `InputHTMLAttributes` import in place and remove the duplicate from the bottom block — adjust the bottom import to:

```ts
import { forwardRef, useState, type SVGProps } from 'react';
```

(`InputHTMLAttributes` is already imported at the top of the file.)

- [ ] **Step 3: Run typecheck**

Run from `frontend/`:
```bash
npm run typecheck
```
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/primitives.tsx
git commit -m "feat(primitives): add PasswordInput and FormField error/valid props"
```

---

## Task 4: Add CSS for password input, control error/success, and form-error

**Files:**
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Add the `--success` token near the other tokens**

Search for `--primary-soft:` and add a sibling line:

```css
--success: oklch(0.62 0.12 155);
```

- [ ] **Step 2: Add the new component styles**

Find the existing `.control:focus` block (around line 300) and append immediately after it:

```css
.password-input {
  position: relative;
  display: block;
}

.password-input .control {
  padding-right: 40px;
}

.password-input__toggle {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: var(--muted);
  border-radius: 6px;
  cursor: pointer;
}

.password-input__toggle:hover {
  color: var(--text);
  background: var(--surface-alt);
}

.password-input__toggle:focus-visible {
  outline: 2px solid var(--primary-soft);
}

.control--error,
.form-field.is-invalid .control {
  border-color: var(--danger);
}

.control--success,
.form-field.is-valid .control {
  border-color: var(--success);
}

.form-field.is-invalid .form-label {
  color: var(--danger);
}

.form-error {
  color: var(--danger);
  font-size: 12px;
}
```

- [ ] **Step 3: Build to confirm the CSS does not break the bundle**

Run from `frontend/`:
```bash
npm run build
```
Expected: build succeeds.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/global.css
git commit -m "style(auth): password input, control error/success, form-error"
```

---

## Task 5: Wire `PasswordInput` into `SignInPage` and `ResetPasswordPage`

**Files:**
- Modify: `frontend/src/pages/public/AuthPages.tsx`

- [ ] **Step 1: Add `PasswordInput` to the imports**

Replace the existing import line:

```ts
import { Badge, Button, EmptyState, FormField, LinkButton, TextArea, TextInput } from '../../components/primitives';
```

with:

```ts
import { Badge, Button, EmptyState, FormField, LinkButton, PasswordInput, TextArea, TextInput } from '../../components/primitives';
```

- [ ] **Step 2: Replace the password `TextInput` in `SignInPage` with `PasswordInput`**

In `SignInPage`, replace the existing пароль `FormField` block:

```tsx
<FormField label="Пароль">
  <TextInput
    name="password"
    type="password"
    value={form.password}
    onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
    placeholder="Введите пароль"
    minLength={6}
    required
  />
</FormField>
```

with:

```tsx
<FormField label="Пароль">
  <PasswordInput
    name="password"
    value={form.password}
    onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
    placeholder="Введите пароль"
    minLength={6}
    required
  />
</FormField>
```

- [ ] **Step 3: Replace both password `TextInput`s in `ResetPasswordPage` with `PasswordInput`**

In `ResetPasswordPage`, replace the existing two `FormField` password blocks with:

```tsx
<FormField label="Новый пароль">
  <PasswordInput
    name="password"
    value={form.password}
    onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
    minLength={8}
    required
  />
</FormField>
<FormField label="Повторите пароль">
  <PasswordInput
    name="confirmPassword"
    value={form.confirmPassword}
    onChange={(event) => setForm((current) => ({ ...current, confirmPassword: event.target.value }))}
    minLength={8}
    required
  />
</FormField>
```

- [ ] **Step 4: Run typecheck and tests**

Run from `frontend/`:
```bash
npm run typecheck
npm test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/public/AuthPages.tsx
git commit -m "feat(auth): eye toggle on sign-in and reset password pages"
```

---

## Task 6: Wire `PasswordInput` and dynamic validation into `ParentRegistrationPage`

**Files:**
- Modify: `frontend/src/pages/public/AuthPages.tsx`

- [ ] **Step 1: Add touched/error state and validation handler**

In `ParentRegistrationPage`, after the existing `useState` block (which currently holds `form` and `status`), add:

```tsx
const [touched, setTouched] = useState<Record<keyof typeof form, boolean>>({
  name: false,
  email: false,
  password: false,
});
```

Update the existing `useState` for `form` so it is typed (the type is needed for the touched key). The existing form already has shape `{ name, email, password }`; no change required.

Replace the `handleSubmit` function with:

```tsx
const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
  event.preventDefault();
  setTouched({ name: true, email: true, password: true });

  const errors = runRegistrationValidations({
    name: form.name,
    email: form.email,
    password: form.password,
    subjects: [],
  });
  if (errors.name || errors.email || errors.password) {
    setStatus('Проверьте правильность заполнения полей.');
    return;
  }

  try {
    const response = await registerParent(form);
    writeAuthSession(response);
    setStatus('Аккаунт создан. Теперь можно выбрать репетитора и отправить заявку.');
    navigate(getAppRouteForRole(response.user.role));
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Не удалось создать аккаунт.');
  }
};
```

Update the imports at the top of the file to also import the helpers:

```ts
import { runRegistrationValidations, validateEmail, validateName, validatePassword } from './auth-helpers';
```

- [ ] **Step 2: Add a small field-state helper inside the component**

Right above the `return` of `ParentRegistrationPage`, add:

```tsx
const nameError = touched.name ? validateName(form.name).message : null;
const emailError = touched.email ? validateEmail(form.email).message : null;
const passwordError = touched.password ? validatePassword(form.password, 6).message : null;
const emailValid = touched.email && !emailError;
const passwordValid = touched.password && !passwordError;
```

- [ ] **Step 3: Wire the field props**

Replace the three `FormField`s inside the parent form with:

```tsx
<FormField label="Имя" error={nameError ?? undefined}>
  <TextInput
    name="name"
    value={form.name}
    onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
    onBlur={() => setTouched((current) => ({ ...current, name: true }))}
    invalid={Boolean(nameError)}
    required
  />
</FormField>
<FormField label="Email" error={emailError ?? undefined} valid={emailValid}>
  <TextInput
    name="email"
    type="email"
    value={form.email}
    onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
    onBlur={() => setTouched((current) => ({ ...current, email: true }))}
    className={emailError ? 'control--error' : emailValid ? 'control--success' : undefined}
    required
  />
</FormField>
<FormField label="Пароль" error={passwordError ?? undefined} valid={passwordValid}>
  <PasswordInput
    name="password"
    value={form.password}
    onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
    onBlur={() => setTouched((current) => ({ ...current, password: true }))}
    className={passwordError ? 'is-invalid' : passwordValid ? 'is-valid' : undefined}
    minLength={6}
    required
  />
</FormField>
```

- [ ] **Step 4: Typecheck and test**

Run from `frontend/`:
```bash
npm run typecheck
npm test
```
Expected: PASS. If `TextInput` complains about an `invalid` prop, drop it (the `control--error` class on the input itself is enough; the `FormField` `is-invalid` wrapper already styles the field).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/public/AuthPages.tsx
git commit -m "feat(auth): dynamic validation in parent registration form"
```

---

## Task 7: Wire `PasswordInput` and dynamic validation into `TutorRegistrationPage`

**Files:**
- Modify: `frontend/src/pages/public/AuthPages.tsx`

- [ ] **Step 1: Add touched state and update `handleSubmit`**

In `TutorRegistrationPage`, after the existing `useState` for `form`, add:

```tsx
const [touched, setTouched] = useState<Record<'name' | 'email' | 'password' | 'subjects', boolean>>({
  name: false,
  email: false,
  password: false,
  subjects: false,
});
```

Replace the existing `handleSubmit` with:

```tsx
const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
  event.preventDefault();
  setTouched({ name: true, email: true, password: true, subjects: true });

  const errors = runRegistrationValidations({
    name: form.name,
    email: form.email,
    password: form.password,
    subjects: form.subjects,
  });
  if (errors.name || errors.email || errors.password || errors.subjects) {
    setStatus('Проверьте правильность заполнения полей.');
    return;
  }

  try {
    const response = await registerTutor(form);
    writeAuthSession(response);
    setStatus('Профиль создан. Дальше можно перейти к заполнению кабинета и модерации.');
    navigate('/app/tutor/profile?welcome=1');
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Не удалось создать профиль.');
  }
};
```

- [ ] **Step 2: Add field-state helpers**

Just before the `return` of `TutorRegistrationPage`, add:

```tsx
const nameError = touched.name ? validateName(form.name).message : null;
const emailError = touched.email ? validateEmail(form.email).message : null;
const passwordError = touched.password ? validatePassword(form.password, 6).message : null;
const subjectsError = touched.subjects ? validateSubjects(form.subjects).message : null;
const emailValid = touched.email && !emailError;
const passwordValid = touched.password && !passwordError;
```

- [ ] **Step 3: Wire the fields**

Replace the `Имя`, `Email`, `Пароль` `FormField`s and the subjects wrapper with:

```tsx
<FormField label="ФИО" error={nameError ?? undefined}>
  <TextInput
    name="name"
    value={form.name}
    onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
    onBlur={() => setTouched((current) => ({ ...current, name: true }))}
    className={nameError ? 'control--error' : undefined}
    required
  />
</FormField>
<FormField label="Email" error={emailError ?? undefined} valid={emailValid}>
  <TextInput
    name="email"
    type="email"
    value={form.email}
    onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
    onBlur={() => setTouched((current) => ({ ...current, email: true }))}
    className={emailError ? 'control--error' : emailValid ? 'control--success' : undefined}
    required
  />
</FormField>
<FormField label="Пароль" error={passwordError ?? undefined} valid={passwordValid}>
  <PasswordInput
    name="password"
    value={form.password}
    onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
    onBlur={() => setTouched((current) => ({ ...current, password: true }))}
    className={passwordError ? 'is-invalid' : passwordValid ? 'is-valid' : undefined}
    minLength={6}
    required
  />
</FormField>
<div className="form-field form-field--wide">
  <span className="form-label">Преподаваемые предметы</span>
  {subjectsLoading ? <div className="loading-panel">Загружаем предметы...</div> : null}
  {!subjectsLoading && subjectsError ? (
    <EmptyState
      title="Предметы временно недоступны"
      text={subjectsError}
      action={<Button type="button" variant="secondary" onClick={() => setSubjectsLoadKey((value) => value + 1)}>Повторить</Button>}
    />
  ) : null}
  {!subjectsLoading && !subjectsError && availableSubjects.length === 0 ? (
    <EmptyState
      title="Список предметов пока пуст"
      text="Регистрация репетитора станет доступна после добавления предметов в каталог."
    />
  ) : null}
  {!subjectsLoading && !subjectsError && availableSubjects.length > 0 ? (
    <SubjectPicker
      availableSubjects={availableSubjects}
      selectedSubjects={form.subjects}
      onToggleSubject={handleSubjectToggle}
    />
  ) : null}
  {subjectsError ? <span className="form-error">{subjectsError}</span> : (
    <span className="form-hint">Нажмите на предмет, чтобы добавить или убрать его из итогового списка.</span>
  )}
</div>
```

- [ ] **Step 4: Typecheck and test**

Run from `frontend/`:
```bash
npm run typecheck
npm test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/public/AuthPages.tsx
git commit -m "feat(auth): dynamic validation in tutor registration form"
```

---

## Task 8: Final verification

**Files:** none.

- [ ] **Step 1: Run typecheck, tests, and build**

Run from `frontend/`:
```bash
npm run typecheck
npm test
npm run build
```
Expected: all three commands succeed.

- [ ] **Step 2: Smoke-test the dev server**

Start the stack from repo root:
```bash
docker compose up --build -d
```

Then open `http://localhost:4173/auth/sign-in`, `/auth/sign-up/parent`, `/auth/sign-up/tutor`, `/auth/reset-password/test-token` and verify:
- The eye toggle on every password field switches between masked and visible text.
- The toggle button has `aria-label` and `aria-pressed` reflecting the state.
- On the registration pages, blurring an empty field shows the red border and error text; entering a valid value shows the green border (email, password) and clears the error.
- Submitting the parent registration with an empty form marks every field invalid and shows the status banner.
- The tutor registration blocks submission when no subject is selected.

- [ ] **Step 3: Stop the stack and do not push**

```bash
docker compose down
```

Do **not** run `git push` — the user will decide when to push and merge to `dev`.

---

## Self-Review Notes

- Spec coverage: Tasks 1, 2, 6, 7 implement the validation rules. Tasks 3, 4, 5, 6, 7 wire the eye toggle. Task 8 verifies.
- Placeholder scan: every code step includes full content; no `TBD`/`TODO`.
- Type consistency: `RegistrationValues`, `RegistrationErrors`, `FieldValidation` are defined in Task 2 and reused in Tasks 6 and 7. `PasswordInput` is defined in Task 3 and reused in Tasks 5, 6, 7.
