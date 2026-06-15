# Simple UI Card Removals Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove four low-value UI blocks in separate feature branches and merge each verified change into `dev`.

**Architecture:** Each task changes only one React page plus a focused source-structure regression test and its checklist line. Backend payloads and shared types remain unchanged. User-owned uncommitted checklist content is preserved outside task commits.

**Tech Stack:** React, TypeScript, Vitest, custom Rollup build, Docker Compose, Git.

---

### Task 1: Remove Subscription Statistics

**Files:**
- Modify: `frontend/src/pages/app/TutorSubscriptionPage.tsx`
- Create: `frontend/src/pages/app/simple-ui-card-removals.test.mjs`
- Modify: `UI_FIX_CHECKLIST.md`

- [ ] **Step 1: Create branch `codex/remove-tutor-subscription-stats` from the planning branch.**
- [ ] **Step 2: Add a source test asserting `TutorSubscriptionPage.tsx` does not contain `tutor-subscription-stats`.**
- [ ] **Step 3: Run the focused test and confirm it fails because the class is present.**
- [ ] **Step 4: Remove only the `tutor-subscription-stats` section and any now-unused local values.**
- [ ] **Step 5: Run the focused test, `npm test`, and `npm run build`.**
- [ ] **Step 6: Run `docker compose up --build -d` and inspect `/app/tutor/subscription`.**
- [ ] **Step 7: Mark the matching checklist line, commit, merge into `dev`, verify, and delete the branch.**

### Task 2: Remove Checkout Summary

**Files:**
- Modify: `frontend/src/pages/app/TutorTariffCheckoutPage.tsx`
- Modify: `frontend/src/pages/app/simple-ui-card-removals.test.mjs`
- Modify: `UI_FIX_CHECKLIST.md`

- [ ] **Step 1: Create branch `codex/remove-tutor-checkout-summary` from updated `dev`.**
- [ ] **Step 2: Add a source test asserting `TutorTariffCheckoutPage.tsx` does not contain `tutor-checkout-summary`.**
- [ ] **Step 3: Run the focused test and confirm it fails because the class is present.**
- [ ] **Step 4: Remove only the checkout summary section and any now-unused local values.**
- [ ] **Step 5: Run the focused test, `npm test`, and `npm run build`.**
- [ ] **Step 6: Run `docker compose up --build -d` and inspect `/app/tutor/subscription/checkout/result`.**
- [ ] **Step 7: Mark the matching checklist line, commit, merge into `dev`, verify, and delete the branch.**

### Task 3: Remove Calendar Linked Statistic

**Files:**
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`
- Modify: `frontend/src/pages/app/simple-ui-card-removals.test.mjs`
- Modify: `UI_FIX_CHECKLIST.md`

- [ ] **Step 1: Create branch `codex/remove-tutor-calendar-linked-stat` from updated `dev`.**
- [ ] **Step 2: Add a source test asserting `TutorCalendarPage.tsx` does not contain `tutor-calendar-stat--amber`.**
- [ ] **Step 3: Run the focused test and confirm it fails because the class is present.**
- [ ] **Step 4: Remove only the amber `Связано` statistic card.**
- [ ] **Step 5: Run the focused test, `npm test`, and `npm run build`.**
- [ ] **Step 6: Run `docker compose up --build -d` and inspect `/app/tutor/calendar`.**
- [ ] **Step 7: Mark the matching checklist line, commit, merge into `dev`, verify, and delete the branch.**

### Task 4: Remove Tutor Profile Information Cards

**Files:**
- Modify: `frontend/src/pages/app/TutorWorkspaceProfilePage.tsx`
- Modify: `frontend/src/pages/app/simple-ui-card-removals.test.mjs`
- Modify: `UI_FIX_CHECKLIST.md`

- [ ] **Step 1: Create branch `codex/remove-tutor-profile-info-cards` from updated `dev`.**
- [ ] **Step 2: Add a source test asserting the profile page does not contain headings `Уведомления`, `Модерация`, or `Что улучшить`.**
- [ ] **Step 3: Run the focused test and confirm it fails because the cards are present.**
- [ ] **Step 4: Remove the three named cards and any now-unused rendering values.**
- [ ] **Step 5: Run the focused test, `npm test`, and `npm run build`.**
- [ ] **Step 6: Run `docker compose up --build -d` and inspect `/app/tutor/profile`.**
- [ ] **Step 7: Mark the matching checklist line, commit, merge into `dev`, verify, and delete the branch.**

### Task 5: Final Integrated Verification

- [ ] **Step 1: Run `npm test` and `npm run build` on `dev`.**
- [ ] **Step 2: Run `docker compose ps --format json` and confirm all services are running.**
- [ ] **Step 3: Confirm the four checklist lines are checked and the user's unrelated checklist edit remains uncommitted.**
