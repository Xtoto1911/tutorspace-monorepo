# Auth Pages Password Toggle And Registration Validation Design

## Goal

Make the TutorSpace authentication forms friendlier and safer without changing
their data contracts, route map, or backend integration:

1. Add a show/hide eye toggle to every password field on the login, parent
   registration, tutor registration, and reset-password pages.
2. Add field-level dynamic validation under every input on the two
   registration pages, with red or green border highlighting and a short
   message below the field.

## Scope

This is a frontend-only change inside `frontend/`. No backend service,
docker-compose, or contract document changes are required. The shared
`PasswordInput` and validation helpers stay inside the existing module
boundaries:

- `frontend/src/components/primitives.tsx`
- `frontend/src/pages/public/AuthPages.tsx`
- `frontend/src/pages/public/auth-helpers.ts` (new)
- `frontend/src/pages/public/auth-helpers.test.ts` (new)
- `frontend/src/styles/global.css`

The `ForgotPasswordPage` is intentionally excluded from validation work
because it only collects an email address and posts a request; the eye toggle
does not apply because the form has no password field.

## Page Behavior

### Password eye toggle (all five password fields)

- Every password input becomes a `PasswordInput` wrapper around the existing
  `TextInput` with a small inline button on the right edge.
- The button toggles the input `type` between `password` and `text` and swaps
  between an "open eye" and "eye with slash" SVG icon (24x24 viewBox, 18px
  rendered).
- The button has `type="button"`, `aria-label` of "Показать пароль" /
  "Скрыть пароль", and `aria-pressed` reflecting the current state.
- Toggling is local to each input. Switching the eye on one field does not
  affect the other fields. State lives inside the `PasswordInput` component
  via `useState`; no parent state or prop wiring is required.
- The existing `minLength` constraint, `name` attribute, and `value` /
  `onChange` wiring are preserved.
- Each affected page keeps its own `useState` for the password value as today;
  the eye only controls the local visibility flag.

Affected pages and fields:

- `SignInPage` — single `Пароль` field.
- `ParentRegistrationPage` — single `Пароль` field.
- `TutorRegistrationPage` — single `Пароль` field.
- `ResetPasswordPage` — `Новый пароль` and `Повторите пароль` fields.

### Registration validation (parent + tutor)

- Validation runs on field `onBlur` and on `submit`. It does not run on every
  keystroke to avoid noisy feedback while the user is still typing.
- Once a field is `touched`, the input border switches to:
  - Red (`var(--danger)`) plus a short error text under the field if invalid.
  - Green (`var(--success)`) for the email and password fields if valid (name
    does not get a green border to avoid visual noise on the simplest field).
- The submit handler:
  1. Runs all field validators locally.
  2. Marks every field as `touched` so all errors render at once.
  3. Aborts the API call if any local validation fails; the existing
     `success-banner` / `error-banner` server feedback is unchanged.
- Server errors thrown from `registerParent` / `registerTutor` continue to
  surface through the existing banner. If the server message mentions the
  field name (`email`, `password`), the helper maps it onto that field's
  `serverError` so the same red border + inline text pattern is used.

Rules:

- `name` — required, trimmed length at least 2 characters.
- `email` — required and matches the existing `emailRegex` used by the
  backend contract (case-insensitive, one `@`, non-empty local and domain
  parts).
- `password` — required, length at least 6 on registration pages and at
  least 8 on the reset-password page. The `ResetPasswordPage` keeps its own
  `minLength={8}` rule and gains the same eye toggle, but its dynamic
  validation feedback is out of scope for this change.
- `confirmPassword` — required and equal to `password` on the
  `ResetPasswordPage`. (The two registration pages do not collect a
  confirmation field today; adding one would change the data contract and
  is therefore out of scope.)
- `subjects` (tutor only) — at least one subject must be selected. The
  existing submit-time guard stays in place; the helper also reports the
  error inline next to the subject picker when the user attempts to submit
  with an empty selection.

## Components

### `PasswordInput` (`components/primitives.tsx`)

- `forwardRef` over a normal `TextInput`, accepts all `InputHTMLAttributes`.
- Renders:
  ```tsx
  <span className={cn('password-input', props.className)}>
    <input className="control" ... />
    <button type="button" className="password-input__toggle"
            aria-label={visible ? 'Скрыть пароль' : 'Показать пароль'}
            aria-pressed={visible}
            onClick={...}>
      <EyeIcon variant={visible ? 'open' : 'closed'} />
    </button>
  </span>
  ```
- `EyeIcon` is a small inline SVG component inside the same file (no new
  dependency, no asset file). 18x18 stroke-based icon, currentColor stroke.
- State defaults to `type="password"` (visible=false).

### `FormField` extension

The existing `FormField` keeps its current `label` / `hint` / `children`
contract and gains two optional props:

- `error?: string` — when present, renders a `<span class="form-error">`
  below the children and adds `is-invalid` modifier to the field wrapper.
- `valid?: boolean` — when true and `error` is not present, adds
  `is-valid` modifier for the optional green border accent (the input
  itself gets the green border via a `control--success` class the caller
  applies).

The eye toggle does not change `FormField`.

## Validation Helpers

`pages/public/auth-helpers.ts` exports pure functions, each returning
`{ valid: boolean, message: string | null }` and accepting a typed
context object:

- `validateName({ name })`
- `validateEmail({ email })`
- `validatePassword({ password, minLength })`
- `validatePasswordMatch({ password, confirmPassword })`
- `validateSubjects({ subjects })`

A small `runRegistrationValidations(values)` helper returns a
`Record<keyof RegistrationValues, string | null>` so forms can render
errors in one pass.

`auth-helpers.test.ts` covers:

- empty, whitespace-only, and 1-character names
- 2-character and longer valid names
- valid and invalid email shapes
- empty and short passwords
- exact-minimum-length passwords
- matching and mismatching confirmation values
- empty and non-empty subject arrays

Tests use Vitest, the runner already configured by `npm test` in
`frontend/`.

## Styles

All new styles live in `styles/global.css`, reusing the existing OKLCH
tokens (`--danger`, `--success`, `--muted`, `--text`, `--border`):

- `.password-input { position: relative; display: block; }`
- `.password-input .control { padding-right: 40px; }`
- `.password-input__toggle { position: absolute; top: 4px; right: 4px; width: 32px; height: 32px; display: inline-flex; align-items: center; justify-content: center; border: 0; background: transparent; color: var(--muted); border-radius: 6px; cursor: pointer; }`
- `.password-input__toggle:hover { color: var(--text); background: var(--surface-alt); }`
- `.password-input__toggle:focus-visible { outline: 2px solid var(--primary-soft); }`
- `.control--error { border-color: var(--danger); }`
- `.control--success { border-color: var(--success); }`
- `.form-error { color: var(--danger); font-size: 12px; }`

`--danger` and `--success` are introduced if not present. Both are
defined as OKLCH tokens alongside the existing palette in the same file
to keep the warm, calm style required by `frontend/DESIGN.md`.

## Verification

Run from `frontend/`:

1. `npm run typecheck` — must pass.
2. `npm test` — must pass, including the new `auth-helpers.test.ts`.
3. `npm run build` — must succeed.
4. `docker compose up --build -d` — start the stack and visually verify
   each affected page in the in-app browser at `http://localhost:4173`:
   - `/auth/sign-in` — toggle the eye, confirm password masking toggles.
   - `/auth/sign-up/parent` — eye works, blur each field to see the
     validation behavior, submit empty to see all errors at once.
   - `/auth/sign-up/tutor` — same, plus the subject picker error.
   - `/auth/reset-password/:token` — both password fields have an eye
     toggle.

## Out of Scope

- Adding a password confirmation field to the registration pages
  (changes the request body and the backend contract).
- Password strength meter or rules list.
- Showing validation feedback on the login page or the forgot-password
  page.
- Backend-side validation changes; the backend already validates and
  returns 400/422 with messages.
- Adding new npm dependencies; the eye is an inline SVG, validation
  helpers are hand-written, and tests use the existing Vitest setup.
