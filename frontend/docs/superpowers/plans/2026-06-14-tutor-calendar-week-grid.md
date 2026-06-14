# Tutor Calendar Week Grid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the tutor calendar display a complete seven-day week at desktop width and position lessons correctly from 08:00 through 22:00.

**Architecture:** CRM produces a canonical hourly scale and event grid coordinates based on real lesson times. Frontend helpers provide the same coordinate model for availability slots, while the page layout gives the calendar full width and moves context cards below it.

**Tech Stack:** React, TypeScript, CSS Grid, Vitest, Spring Boot, JUnit 5, Docker Compose

---

### Task 1: Define Calendar Grid Math

**Files:**
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.test.ts`
- Modify: `frontend/src/pages/app/tutor-calendar-helpers.ts`

- [ ] **Step 1: Write failing helper tests**

Add tests asserting:

```ts
expect(buildTutorCalendarTimeSlots()).toEqual([
  '08:00', '09:00', '10:00', '11:00', '12:00',
  '13:00', '14:00', '15:00', '16:00', '17:00',
  '18:00', '19:00', '20:00', '21:00', '22:00',
]);
expect(getTutorCalendarGridPosition('08:00', '09:30')).toEqual({
  row: 0,
  rowSpan: 2,
});
expect(getTutorCalendarGridPosition('07:30', '08:30')).toEqual({
  row: 0,
  rowSpan: 1,
});
```

- [ ] **Step 2: Run the focused frontend test**

Run: `npm test -- tutor-calendar-helpers.test.ts`

Expected: FAIL because the new helpers are not exported.

- [ ] **Step 3: Implement the grid helpers**

Add `buildTutorCalendarTimeSlots()` for 08:00-22:00 and
`getTutorCalendarGridPosition(startTime, endTime)` that converts minutes from
08:00 into a zero-based row and duration into a minimum one-row span.

- [ ] **Step 4: Run the focused frontend test**

Run: `npm test -- tutor-calendar-helpers.test.ts`

Expected: PASS.

### Task 2: Correct CRM Calendar Coordinates

**Files:**
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`

- [ ] **Step 1: Write a failing CRM regression test**

Create a lesson at `2026-05-06T08:00` lasting 90 minutes and assert:

```java
assertEquals("08:00", page.timeSlots().get(0));
assertEquals("22:00", page.timeSlots().get(page.timeSlots().size() - 1));
assertEquals(15, page.timeSlots().size());
assertEquals(0, page.events().get(0).row());
assertEquals(2, page.events().get(0).rows());
```

- [ ] **Step 2: Run the focused CRM test**

Run: `mvn -Dtest=PageViewServiceTest#getTutorCalendarPage_positionsEarlyLessonOnFullTimeScale test`

Expected: FAIL with the current noon-based row and five-slot response.

- [ ] **Step 3: Implement canonical backend positioning**

Replace the fixed five-label list with 08:00-22:00 hourly labels. Calculate
event row from minutes after 08:00 and calculate row span from lesson duration,
rounding partial hours up and clamping to the visible grid.

- [ ] **Step 4: Run the focused CRM test**

Run: `mvn -Dtest=PageViewServiceTest#getTutorCalendarPage_positionsEarlyLessonOnFullTimeScale test`

Expected: PASS.

### Task 3: Rebuild The Tutor Calendar Layout

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Use shared slot positioning**

Import `getTutorCalendarGridPosition` and replace nearest-label slot placement
with exact start/end positioning.

- [ ] **Step 2: Move calendar context below the grid**

Keep `.tutor-week-card` as the first full-width item and render
`.tutor-calendar-side` after it as a responsive context row.

- [ ] **Step 3: Make the grid responsive**

Use a desktop grid of:

```css
grid-template-columns: 56px repeat(7, minmax(0, 1fr));
grid-template-rows: 58px repeat(15, 64px);
```

Remove the desktop `min-width: 820px`, constrain vertical height with internal
overflow, and apply a minimum width only below the desktop breakpoint.

- [ ] **Step 4: Run frontend tests and build**

Run: `npm test`

Expected: all tests pass.

Run: `npm run build`

Expected: exit code 0.

### Task 4: Verify And Integrate

**Files:**
- Modify: `UI_FIX_CHECKLIST.md`

- [ ] **Step 1: Run CRM tests**

Run: `mvn test`

Expected: all CRM tests pass.

- [ ] **Step 2: Rebuild with Docker Compose**

Run: `docker compose up --build -d`

Expected: exit code 0 and required containers running.

- [ ] **Step 3: Verify desktop and mobile in Browser**

At 1440 px, assert the calendar scroll container has
`scrollWidth === clientWidth`, seven day headers are visible, and the document
has no horizontal overflow.

At 390 px, assert the document has no horizontal overflow and the calendar
scroll container has `scrollWidth > clientWidth`.

- [ ] **Step 4: Mark the checklist**

Change checklist item 11 to `[x]` only after automated and browser verification
succeeds.

- [ ] **Step 5: Commit and merge**

Commit the implementation on `codex/fix-tutor-calendar-week-grid`, switch to
`dev`, merge with a merge commit, and delete the feature branch.
