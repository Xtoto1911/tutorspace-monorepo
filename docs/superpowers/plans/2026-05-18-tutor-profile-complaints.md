# Tutor Profile Complaints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tutor profile complaints and moderator actions to hide reviews/profiles or prevent a user from publishing reviews.

**Architecture:** Marketplace owns tutor profiles, reviews, and review-publication restrictions. Admin-service owns complaint workflow and calls marketplace internal endpoints when a moderator approves concrete actions. Frontend uses existing `/api/**` helpers and keeps the UI inline with the current tutor profile and moderator complaint pages.

**Tech Stack:** Java 21, Spring Boot 3.5.10, Maven, React, TypeScript, custom Rollup frontend build, Vitest.

---

### Task 1: Marketplace Review Restriction

**Files:**
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewPublicationRestriction.java`
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewPublicationRestrictionRepository.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewService.java`
- Test: `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/review/ReviewServiceTest.java`

- [ ] Write a failing test proving `ReviewService.create` rejects a client with an active review restriction.
- [ ] Run `mvn test -Dtest=ReviewServiceTest` in `backend/marketplace-service` and confirm the new test fails because restrictions are not checked.
- [ ] Add the restriction entity/repository and inject it into `ReviewService`.
- [ ] Check `existsByUserIdAndActiveTrue(clientUserId)` before lesson eligibility and throw `ReviewNotAllowedException`.
- [ ] Add a service method to create or reactivate a restriction for a user.
- [ ] Run `mvn test -Dtest=ReviewServiceTest` and confirm it passes.

### Task 2: Marketplace Tutor Profile Complaint

**Files:**
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/CreateTutorProfileComplaintRequest.java`
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileComplaintDto.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/AdminComplaintClient.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileService.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileController.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/security/SecurityConfig.java`
- Test: `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/tutor/TutorProfileServiceTest.java`

- [ ] Write a failing test proving profile complaint forwards `targetType=TUTOR_PROFILE`, reported tutor user id, reporter user id, and a snapshot.
- [ ] Run `mvn test -Dtest=TutorProfileServiceTest` and confirm the new test fails.
- [ ] Generalize `AdminComplaintClient` so it can create complaints for both reviews and tutor profiles.
- [ ] Add `TutorProfileService.createComplaint(idOrSlug, reporterUserId, request)`.
- [ ] Add `POST /api/marketplace/tutors/{idOrSlug}/complaints`.
- [ ] Permit the endpoint for `ROLE_CLIENT` and `ROLE_TUTOR`.
- [ ] Run `mvn test -Dtest=TutorProfileServiceTest,ReviewServiceTest`.

### Task 3: Marketplace Internal Moderation Actions

**Files:**
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileService.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/ReviewModerationController.java` or create a focused internal controller under tutor/review packages.
- Test: `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/review/ReviewServiceTest.java`

- [ ] Write a failing test proving a restriction can be created for a reported review author.
- [ ] Add internal endpoint to restrict review publication for a user.
- [ ] Add internal endpoint to hide a tutor profile by id or slug.
- [ ] Run targeted marketplace tests.

### Task 4: Admin Complaint Action Routing

**Files:**
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintTargetType.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ModerateComplaintRequest.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintService.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/client/MarketplaceServiceClient.java`
- Test: `backend/admin-service/src/test/java/com/tutor/adminservice/complaint/ComplaintServiceTest.java`

- [ ] Write failing tests for `HIDE_REVIEW`, `RESTRICT_REVIEW_PUBLICATION`, and `HIDE_TUTOR_PROFILE`.
- [ ] Add `TUTOR_PROFILE` target type.
- [ ] Add `action` to moderation request.
- [ ] Route approved actions to marketplace client methods.
- [ ] Run `mvn test -Dtest=ComplaintServiceTest` in `backend/admin-service`.

### Task 5: Frontend API and Tutor Profile UI

**Files:**
- Modify: `frontend/src/shared/api/public.ts`
- Modify: `frontend/src/shared/api/public.test.ts`
- Modify: `frontend/src/shared/types.ts`
- Modify: `frontend/src/pages/public/TutorProfilePage.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] Write failing Vitest coverage for `reportTutorProfile`.
- [ ] Add API helper for profile complaints.
- [ ] Add a profile complaint action/status in the booking card.
- [ ] Keep anonymous users routed to sign-in.
- [ ] Run `npm test -- src/shared/api/public.test.ts`.

### Task 6: Frontend Moderator Actions

**Files:**
- Modify: `frontend/src/shared/api/admin.ts`
- Modify: `frontend/src/shared/api/admin.test.ts`
- Modify: `frontend/src/pages/app/ModeratorComplaintsPage.tsx`

- [ ] Write failing Vitest coverage for exact moderator action payloads.
- [ ] Map backend target type `TUTOR_PROFILE`.
- [ ] Send concrete actions instead of display labels.
- [ ] Let the moderator select rows and refresh page data after action.
- [ ] Run `npm test -- src/shared/api/admin.test.ts`.

### Task 7: Full Verification

**Files:**
- No new files.

- [ ] Run `mvn test` in `backend/marketplace-service`.
- [ ] Run `mvn test` in `backend/admin-service`.
- [ ] Run `npm run typecheck` in `frontend`.
- [ ] Run `npm run build` in `frontend`.
- [ ] Rebuild affected Docker services if requested or if runtime verification is needed.
