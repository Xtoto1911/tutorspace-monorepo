# Replace Vorontsova And Friendly Error Messages Design

## Goal

Two related changes inside the existing auth and marketplace services plus the shared frontend `http.ts`:

1. Remove tutor **Ирина Воронцова** from the seed data and add **Дарья Лазарева** in her place, keeping the public catalog at exactly 25 tutor profiles with 25 reviews.
2. Stop the user interface from ever showing raw HTTP status codes or internal request paths in error messages. All API errors should be rendered as short, friendly Russian sentences. This includes the concrete case where registering with an already-registered email currently surfaces `Request failed for /api/auth/register: 409 Conflict` instead of `Этот email уже зарегистрирован.`

## Scope

- Backend: `auth-service`, `marketplace-service`, `crm-service`, `admin-service` error handlers. The `notification-service` and `subscription-service` are out of scope unless their current handlers already leak codes — verify in the plan.
- Frontend: `frontend/src/shared/api/http.ts` plus the six `'Unknown error'` literals in `frontend/src/pages/app/TutorCalendarPage.tsx`.
- Tests for both layers.
- No change to any happy-path API response, no change to the AGENTS.md `VITE_API_MODE` defaults, no docker-compose changes, no new dependencies.

## Replace Vorontsova With Lazareva

### Files

- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/config/AdminSeedConfig.java` — drop the `SeedUser` for Ирина, add a `SeedUser` for Дарья.
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/config/MarketplaceSeedConfig.java` — drop the `TutorProfile irina = createTutor(...)` block and its `createReview(... irina ...)` call, add an equivalent block for Дарья plus one new review.

### New Tutor Record

- UUID: `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb10`
- Auth email: `darya@tutorspace.local`, password: `tutor12345`, role: `ROLE_TUTOR`
- Marketplace slug: `darya-lazareva`
- Full name: `Дарья Лазарева`
- Description: «Готовлю к ЕГЭ и олимпиадам по истории: учу работать с источниками, аргументировать позицию и видеть причинно-следственные связи».
- City: `Нижний Новгород`
- Education: `["ННГУ им. Лобачевского, исторический факультет"]`
- Highlights: `["Помогаю выстраивать таймлайн и связывать эпохи", "Готовлю к олимпиадам «Высшая проба» и «Ломоносов»"]`
- Tags: `["ЕГЭ", "История России", "Олимпиады"]`
- Response time: 25 minutes
- Experience years: 8
- Photo URL: `https://example.com/darya.jpg`
- Price per hour: `1500`
- Rating: `4.6`
- Reviews count: `35`
- Subjects: `Set.of(history)`
- One published review: rating 5, text «Хороший преподаватель, ребёнок стал понимать историю как единый процесс», client UUID `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa26`.

### Resulting Counts

- `AdminSeedConfig`: still 25 tutors (1 admin + 25 tutors) and 25 clients.
- `MarketplaceSeedConfig`: still 8 subjects, 25 tutor profiles, 25 published reviews (exactly one per tutor, since removing Irina's review and adding Lazareva's keeps the one-to-one mapping), all UUIDs in the `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaNN` range stay unique.

## Friendly Error Messages

### Background

The frontend's only place that builds a user-facing error string from a failed response is `frontend/src/shared/api/http.ts:43-57`. Two branches leak raw data:

- `readErrorMessage` line 46: `const raw = payload.message ?? payload.error ?? \`Request failed for ${path}\``. The path fallback fires whenever the backend returns a JSON body without `message` or `error` keys.
- `readErrorMessage` line 55: `return \`Request failed for ${path}: ${response.status} ${response.statusText}\``. The status-fallback fires whenever `response.json()` throws, which happens for nginx 502 pages, empty bodies, and the rare non-JSON backend error.

The user-visible bug is the duplicate-email case. `auth-service` returns `ResponseEntity<String>` for `EmailAlreadyExistsException`, which serialises to a JSON string literal (`"Этот email уже зарегистрирован."`). The frontend reads that as a `string`, so `payload.message` is undefined and the path-fallback wins, surfacing `Request failed for /api/auth/register`. When `response.json()` itself throws (which can happen if the body is empty or has a content-type mismatch), the status-fallback wins and the user sees the literal `409`.

### Frontend Changes (`frontend/src/shared/api/http.ts`)

1. In `readErrorMessage`, after `response.json()`, add a branch:
   ```ts
   if (typeof payload === 'string' && payload.length > 0) {
     return translateErrorMessage(payload);
   }
   ```
   This treats a JSON-encoded string body as the message itself. It fixes the duplicate-email case immediately and any other backend that returns a plain string.

2. Replace the two `Request failed for ${path}` fallbacks with a single `friendlyFallbackForStatus(response.status)` helper that returns a Russian sentence keyed by status:
   - `400` → `Проверьте правильность введённых данных.`
   - `401` → `Сессия истекла. Войдите снова.`
   - `403` → `Доступ запрещён. Обратитесь в поддержку.`
   - `404` → `Запрошенный ресурс не найден.`
   - `409` → `Конфликт данных. Проверьте введённые значения.`
   - `5xx` → `Сервер временно недоступен. Попробуйте позже.`
   - Anything else → `Не удалось выполнить запрос. Попробуйте позже.`

3. Loosen the `translateErrorMessage` matcher from `message.toLowerCase().startsWith(english.toLowerCase())` to `message.toLowerCase().includes(english.toLowerCase())`. The current `startsWith` misses the case where the backend message is the friendly Russian itself but happens to start with a Russian word that is not a prefix of any English key — and conversely, `includes` is more forgiving without false positives because the keys are domain-specific.

### Frontend Changes (`frontend/src/pages/app/TutorCalendarPage.tsx`)

Replace the six `'Unknown error'` literals at lines 348, 403, 421, 446, 467, 505, 534 with the Russian sentence `'Не удалось выполнить действие. Попробуйте ещё раз.'`. The first argument of every `dialog.alert('Не удалось X: ' + ...)` call already provides the action context; the fallback is only for the case where the caught error has no message at all.

### Backend Changes

Goal: every `GlobalExceptionHandler` returns a JSON object with a `message` key, so `response.json()` on the frontend always yields a `{ message: ... }` shape.

1. Add a small shared record `ApiError(String message, int status)` in each service's `exceptionHandler` (or `shared`) package. No new dependency.
2. Replace `ResponseEntity<String>` with `ResponseEntity<ApiError>` in:
   - `auth-service/exceptionHandler/GlobalExceptionHandler.java` — nine handlers: `handleBadCredentials`, `handleUserNotFound`, `handleEmailExists`, `handleInvalidRegistrationRole`, `handleUserNotFoundById`, `handleBlockedUser`, `handleAuthorizationDenied`, `handleIllegalArgument`, `handleGenericException`.
   - `marketplace-service/shared/GlobalExceptionHandler.java` — the equivalent handlers there.
   - `crm-service/...` and `admin-service/...` — same change for any handler that returns a string body.
3. The existing `handleValidationExceptions` in auth-service already returns `Map<String, String>` with a `message` key, so it stays as-is; if the plan finds any other handler that returns a raw string body, convert it to the same map shape so the frontend contract is uniform.
4. Verify `subscription-service` and `notification-service` during the plan; if they do not use `ResponseEntity<String>` for errors, leave them alone.

The success responses, request DTOs, and any controller signatures stay unchanged. Only the error response body changes from a JSON string to a JSON object.

## Verification

- Run `mvn test` in every backend service that was touched; ensure all unit tests pass and the new ApiError body assertions are green.
- Run `npm run typecheck` and `npm test` in `frontend/`. The existing `frontend/src/shared/api/http.test.ts` mocks will need a small update so that the 401/refresh test still passes after the fallback change.
- Run `docker compose up --build -d` and exercise the two specific UI paths:
  - Register with a duplicate email → banner shows `Этот email уже зарегистрирован.` (no `Request failed for /api/auth/register`, no `409`).
  - Stop the `auth-service` container and reload the sign-in page → banner shows `Сервер временно недоступен. Попробуйте позже.` (no `Request failed for /api/auth/login: 502 Bad Gateway`).
- Confirm in the catalog page that the count is still 25 tutors, the new slug `darya-lazareva` is present, and `irina-vorontsova` is gone.

## Out Of Scope

- Translating the existing `englishErrorMap` entries to more domain-specific messages — current Russian strings stay.
- Adding error codes like `EMAIL_TAKEN` to responses. The point of this change is the opposite: never show codes.
- Switching to Spring 6 `ProblemDetail`. That would change success-path semantics and ripple through every consumer; the minimal `ApiError` record is enough.
- Updating the `frontend/src/shared/api/mock-data.ts` mock array. It is unused in the live Docker flow.
- Touching `notification-service` and `subscription-service` beyond the exception-handler audit.

## Risks And Mitigations

- The frontend `http.test.ts` mocks assume specific body shapes for refresh. Update the mocks to match the new uniform `{ message: ... }` body so the test still passes.
- The `readErrorMessage` `catch` block was the only place surfacing HTTP status. Removing it changes how 502 (nginx gateway errors) render. The replacement `friendlyFallbackForStatus` covers 5xx generically, which is what we want.
- Some legacy backend may return a non-string, non-object JSON body. Treat anything that is not a plain object as "no message" and fall back to status-based text. This is the existing `payload.message ?? payload.error` check; do not change it beyond adding the string-body branch.
