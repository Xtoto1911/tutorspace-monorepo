# Hidden Tutor Profile Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make new tutor public cards hidden by default and guide tutors through first profile setup before opening the card.

**Architecture:** CRM owns editable tutor settings and sends visibility to marketplace. Marketplace only exposes `ACTIVE` profiles publicly, so hidden-by-default is enforced by CRM defaults plus marketplace profile defaults.

**Tech Stack:** Java 21, Spring Boot 3.5.10, Maven, React, TypeScript, Vitest.

---

### Task 1: Backend Hidden Defaults

**Files:**
- Modify: `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/tutor/TutorProfileServiceTest.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileService.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfile.java`
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/settings/SettingsServiceTest.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/settings/TutorSettings.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/settings/SettingsService.java`

- [ ] Write a failing marketplace test asserting `TutorProfileService.create(...)` returns `HIDDEN`.
- [ ] Run `mvn test -Dtest=TutorProfileServiceTest` in `backend/marketplace-service` and confirm the new test fails because status is `ACTIVE`.
- [ ] Set new marketplace profiles to `HIDDEN` by default.
- [ ] Run `mvn test -Dtest=TutorProfileServiceTest` and confirm it passes.
- [ ] Write failing CRM tests asserting default tutor settings are hidden and workspace status says hidden.
- [ ] Run `mvn test -Dtest=SettingsServiceTest` in `backend/crm-service` and confirm the new tests fail because defaults are visible.
- [ ] Set `TutorSettings.isHidden` and default settings to hidden, and make moderation text reflect hidden status.
- [ ] Run `mvn test -Dtest=SettingsServiceTest` and confirm it passes.

### Task 2: Frontend Registration Payload

**Files:**
- Modify: `frontend/src/shared/api/public.test.ts`
- Modify: `frontend/src/shared/api/public.ts`
- Modify: `frontend/src/shared/auth/session.ts`
- Modify: `frontend/src/pages/public/AuthPages.tsx`

- [ ] Update the tutor registration test to expect `isHidden: true` in the initial `/tutor/profile` PATCH payload.
- [ ] Run `npm test -- src/shared/api/public.test.ts` in `frontend` and confirm the test fails.
- [ ] Add `isHidden: true` to the registration CRM payload.
- [ ] Add an optional redirect hint so tutor registration navigates to `/app/tutor/profile?welcome=1`.
- [ ] Run `npm test -- src/shared/api/public.test.ts` and confirm it passes.

### Task 3: Frontend Onboarding UI

**Files:**
- Modify: `frontend/src/pages/app/tutor-profile-settings-helpers.test.ts`
- Modify: `frontend/src/pages/app/tutor-profile-settings-helpers.ts`
- Modify: `frontend/src/pages/app/TutorWorkspaceProfilePage.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] Add a failing helper test for `shouldShowTutorProfileOnboarding(profile, settings, welcome)` returning true for hidden or incomplete profiles.
- [ ] Run `npm test -- src/pages/app/tutor-profile-settings-helpers.test.ts` in `frontend` and confirm the test fails.
- [ ] Implement the helper.
- [ ] Render the onboarding notice on the tutor profile page using the helper.
- [ ] Add CSS matching the current warm product UI, with no glassmorphism and no nested cards.
- [ ] Run `npm test -- src/pages/app/tutor-profile-settings-helpers.test.ts` and confirm it passes.

### Task 4: Verification

**Files:**
- No production file edits expected.

- [ ] Run `mvn test -Dtest=TutorProfileServiceTest` in `backend/marketplace-service`.
- [ ] Run `mvn test -Dtest=SettingsServiceTest` in `backend/crm-service`.
- [ ] Run `npm test -- src/shared/api/public.test.ts src/pages/app/tutor-profile-settings-helpers.test.ts` in `frontend`.
- [ ] Run `npm run typecheck` in `frontend`.
- [ ] Run `npm run build` in `frontend`.
- [ ] Start or rebuild relevant Docker services if needed and verify `/app/tutor/profile?welcome=1` in the browser.
