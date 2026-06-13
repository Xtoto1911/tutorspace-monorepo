# Client Schedule Week Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a seven-day client calendar with real CRM data and previous/current/next week navigation.

**Architecture:** Extend the CRM read-model endpoint with a validated Monday-to-Sunday range, while retaining global upcoming-lesson data. Add pure frontend date helpers, pass the selected range through the API helper, and render a seven-column responsive calendar.

**Tech Stack:** Spring Boot 3.5, Java 21, React 19, TypeScript, CSS, JUnit 5, Vitest, Docker Compose

---

### Task 1: Add a selected-week CRM read model

**Files:**
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`

- [ ] **Step 1: Write failing service tests**

Add tests that call:

```java
service.getClientSchedulePage(
        clientId,
        LocalDate.of(2026, 6, 15),
        LocalDate.of(2026, 6, 21)
);
```

Assert that the response has seven `weekDays`, includes a Sunday lesson in
`events`, reports `period.to()` as `2026-06-21`, and still returns the next
global actionable lesson when the selected week has no events.

- [ ] **Step 2: Run the focused CRM test**

Run:

```powershell
mvn -Dtest=PageViewServiceTest test
```

Expected: compilation or assertion failure because the range overload and
seven-day mapping do not exist.

- [ ] **Step 3: Implement the selected-week overload**

Add:

```java
public ClientSchedulePageDto getClientSchedulePage(
        UUID clientUserId,
        LocalDate requestedFrom,
        LocalDate requestedTo
)
```

Resolve the displayed range from the requested dates when present, otherwise
use the existing default selection. Use `weekStart.plusDays(6)`, map event
columns from Monday `1` through Sunday `7`, and build seven week-day DTOs.

- [ ] **Step 4: Verify the CRM service tests**

Run:

```powershell
mvn -Dtest=PageViewServiceTest test
```

Expected: PASS.

### Task 2: Expose and validate schedule range parameters

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/ClientCrmController.java`
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/controller/ClientCrmControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Cover:

```http
GET /client/schedule?from=2026-06-15&to=2026-06-21
```

and invalid cases where only one parameter is present or the range is not a
Monday-to-Sunday week.

- [ ] **Step 2: Run the focused controller test**

Run:

```powershell
mvn -Dtest=ClientCrmControllerTest test
```

Expected: FAIL because the controller ignores range parameters.

- [ ] **Step 3: Implement controller validation**

Accept optional `LocalDate from` and `LocalDate to` request parameters. Reject
partial or non-seven-day ranges with `ResponseStatusException(BAD_REQUEST, ...)`
and call the selected-week service overload for valid input.

- [ ] **Step 4: Verify CRM tests**

Run:

```powershell
mvn test
```

Expected: PASS.

### Task 3: Add frontend week-range helpers and API query

**Files:**
- Modify: `frontend/src/pages/app/client-schedule-helpers.test.ts`
- Modify: `frontend/src/pages/app/client-schedule-helpers.ts`
- Modify: `frontend/src/shared/api/client.test.ts`
- Modify: `frontend/src/shared/api/client.ts`

- [ ] **Step 1: Write failing helper and API tests**

Test that:

```ts
getClientScheduleWeekRange(new Date('2026-06-17T12:00:00'))
```

returns `{ from: '2026-06-15', to: '2026-06-21' }`, week shifts move by exactly
seven days, and:

```ts
getClientSchedulePage('2026-06-15', '2026-06-21')
```

requests `/client/schedule?from=2026-06-15&to=2026-06-21`.

- [ ] **Step 2: Run focused frontend tests**

Run:

```powershell
npm test -- src/pages/app/client-schedule-helpers.test.ts src/shared/api/client.test.ts
```

Expected: FAIL because the helpers and range arguments do not exist.

- [ ] **Step 3: Implement pure date helpers and API query**

Add helpers for local ISO date formatting, Monday normalization, and week
offsets. Update `getClientSchedulePage` to append encoded `from` and `to`
parameters only when both are provided.

- [ ] **Step 4: Verify focused frontend tests**

Run:

```powershell
npm test -- src/pages/app/client-schedule-helpers.test.ts src/shared/api/client.test.ts
```

Expected: PASS.

### Task 4: Implement week navigation and seven-day UI

**Files:**
- Modify: `frontend/src/pages/app/ClientSchedulePage.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Add selected-week state**

Initialize state from the current Monday, request its range in `loadPage`, and
reload whenever the selected Monday changes.

- [ ] **Step 2: Add navigation controls**

Render previous, current, and next buttons in `.schedule-period`. Disable them
while loading and use accessible labels:

```text
Предыдущая неделя
Текущая неделя
Следующая неделя
```

- [ ] **Step 3: Render seven columns**

Use all seven `pageData.weekDays` values for day buttons, header cells, and
calendar cells. Keep the selected response period visible in the toolbar.

- [ ] **Step 4: Update responsive CSS**

Use seven `minmax(0, 1fr)` day columns at desktop widths and a horizontally
scrollable calendar grid with seven stable day columns below the desktop
breakpoint. Ensure event text cannot expand a grid column.

- [ ] **Step 5: Run frontend verification**

Run:

```powershell
npm test
npm run build
```

Expected: PASS.

### Task 5: Document, rebuild, and verify

**Files:**
- Modify: `frontend/FRONTEND_BACKEND_CONTRACT.md`
- Modify: `UI_FIX_CHECKLIST.md`

- [ ] **Step 1: Update the API contract**

Document optional paired `from` and `to` parameters and the Monday-to-Sunday
range rule for `GET /api/client/schedule`.

- [ ] **Step 2: Rebuild the local stack**

Run:

```powershell
docker compose up --build -d
docker compose ps
```

Expected: all TutorSpace containers are running.

- [ ] **Step 3: Verify in the browser**

At `http://localhost:4173/app/client/schedule`, confirm seven columns fit at
desktop width, previous and next buttons load different period labels, current
week returns to today's week, and events match the selected CRM range.

- [ ] **Step 4: Mark the checklist and merge**

Change the `/app/client/schedule` checklist parent and its three child items to
`[x]`, commit the scoped changes, switch to `dev`, merge
`codex/fix-client-schedule-weeks`, and rerun frontend and CRM tests.
