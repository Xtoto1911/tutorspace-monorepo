# Tutor name propagation across services

**Date:** 2026-06-15
**Status:** Approved
**Branch:** `feature/tutor-name-propagation`

## Problem

When a tutor changes their full name in `/app/tutor/profile` (the
`TutorWorkspaceProfilePage` editor), the new name does not propagate everywhere.

**Confirmed places that still show the old name after a rename:**

- The workspace header chip on every `/app/tutor/*` page — it reads
  `user.name` from the `AuthSession` in `localStorage`, which is populated from
  the `name` field of `JwtAuthenticationDto` at login/refresh and never updated
  after.
- The student-side applications list (`/app/client/applications`,
  `ClientApplicationsPage.tsx`) and details
  (`/app/client/applications/:id`, `ClientApplicationDetailsPage.tsx`).
- The student-side schedule and history
  (`/app/client/schedule`, `ClientSchedulePage.tsx`).
- The student-side messages
  (`/app/client/messages`, `ClientMessagesPage.tsx`).
- The tutor's own requests, calendar, students and messages views
  (`/app/tutor/requests`, `/app/tutor/calendar`, `/app/tutor/students`,
  `/app/tutor/messages`).

These surfaces read `tutorName` from denormalised snapshot fields in the
CRM database, not from the live tutor profile. The current editor flow
updates the CRM `TutorSettings.profileName` and the marketplace
`TutorProfile.fullName` (via `MarketplaceClient.syncTutorProfile`), but does
not touch the snapshots and does not write back to `auth-service.User.name`.

## Goal

When a tutor changes their name in `/app/tutor/profile`, the new name must
appear everywhere within seconds — workspace header, every existing
application, every existing lesson, every existing message thread, and the
public surfaces that already work (catalog, public profile, favorites).

## Non-goals

- Renaming students or admins through a dedicated endpoint (out of scope for
  this iteration; only the tutor editor is rewired).
- Auditing the historical name of a tutor inside applications ("this
  application was originally sent to name X"). Out of scope.
- Removing the `name` field from `auth-service.User` or from the JWT claim.
  The claim is unused by every backend service (verified — there is no
  `getNameFromToken` in `JwtService`; only `userId`, `role`, `email` are
  consumed downstream), so it is dead weight, but cleaning it up is a
  separate refactor that does not fix this bug.
- Changing the data model of `CrmApplication`, `Lesson`, or `MessageThread`.
  The snapshot columns stay; they just become write-propagated.
- Changing `TutorProfile.slug` or any FK (`tutorUserId`, `tutorProfileId`,
  `subjectId`, etc.).

## Decisions taken during brainstorming

1. **Behaviour for historical records:** everywhere shows the current name.
   Snapshots are write-propagated; we do not preserve "name at the time
   the application was submitted".
2. **Source of truth:** `auth-service.User.name`. The frontend reuses the
   existing `TutorWorkspaceProfilePage` editor, but the name now flows
   through `auth-service` first.
3. **Propagation mechanism:** Kafka event `UserProfileChanged` published by
   `auth-service` and consumed by `crm-service`. Chosen over a synchronous
   REST fan-out (cleaner separation, fits the existing event-driven
   infrastructure) and over removing the snapshots (avoids a schema
   migration and a per-page-view N+1 lookup).

## Architecture

### Ownership of the tutor name

| Service | Role |
|---|---|
| `auth-service` | Single source of truth. Exposes `PATCH /api/auth/me` for name changes. Publishes `UserProfileChanged` to Kafka. |
| `crm-service` | Subscribes to `UserProfileChanged`. Updates the snapshots in `crm_applications`, `lessons`, `message_threads` for every row whose `tutor_user_id` matches the event. Marketplace sync (`MarketplaceClient.syncTutorProfile`) is already triggered from the profile save path and continues to handle the public surface. |
| `marketplace-service` | No change. Receives the new name via the existing CRM→marketplace sync. |
| `notification-service` | No change. The `tutorName` it puts in `RequestCreatedEvent` already comes from the CRM/UI path; the event payload will use the new name once the new save flow is in place. |
| `admin-service` / moderator | No change. They read names from marketplace / auth, both of which converge. |

### Data flow on a name change

1. **Frontend** (`TutorWorkspaceProfilePage.handleSave`):
   - First call: `PATCH /api/auth/me` with `{ name }`. Auth is the source of
     truth, so it must succeed before anything else happens.
   - On success: update the `name` field of the locally cached
     `AuthSession.user` in `localStorage` and dispatch a new
     `tutorspace:profile-changed` window event so the workspace header
     refreshes immediately.
   - Then call the existing `updateTutorWorkspaceProfile(profile)` and
     `updateTutorSettings(payload)` endpoints. These continue to update
     `TutorSettings.profileName` and trigger the CRM→marketplace sync.
   - On `PATCH /api/auth/me` failure: do not call the CRM endpoints; show
     the API error and keep the form open.

2. **Auth-service** (`PATCH /api/auth/me`):
   - Validates `name`: trim, length 2–100, non-blank.
   - Persists the new value on `User.name`.
   - Publishes a `UserProfileChanged` event to Kafka topic
     `user-profile-events`. Payload: `{ userId, name, changedAt }`.
   - Returns the updated public user DTO (`id`, `email`, `name`, `role`,
     `status`).

3. **CRM-service** (`UserProfileChangedListener`, new):
   - `@KafkaListener(topics = "user-profile-events")`.
   - In a single transaction, runs three pre-checked updates:
     - `UPDATE crm_applications SET tutor_name = :newName WHERE tutor_user_id = :userId AND tutor_name <> :newName`
     - `UPDATE lessons SET tutor_name = :newName WHERE tutor_user_id = :userId AND tutor_name <> :newName`
     - `UPDATE message_threads SET participant_name = :newName WHERE tutor_user_id = :userId AND participant_name <> :newName`
   - The `<> :newName` guard makes the consumer idempotent: a replayed or
     out-of-order event is a no-op.
   - Errors are handled by Spring Kafka's `DefaultErrorHandler`: 5 retries
     with exponential backoff, then publish to dead-letter topic
     `user-profile-events.DLT`. A future ops runbook can replay the DLT.

4. **Frontend header refresh**:
   - `Layout` listens for `tutorspace:profile-changed` and re-derives
     `workspaceUser.name` from the updated session. This is what makes the
     chip in the sidebar update without a page reload.

### JWT and stale tokens

`auth-service` puts `name` into the JWT claim and into
`JwtAuthenticationDto.name`, but no service reads the `name` claim from the
token (`JwtService` has `getUserIdFromToken`, `getRoleFromToken`,
`getEmailFromToken` — no `getNameFromToken`). The frontend already uses
the local session for display, so the only thing that lags after a rename
is the local session, which we fix in step 1. The next refresh produces a
JWT that contains the new `name`, in case anything ever starts reading it.

## Components

### Backend — `auth-service`

- `application.yaml`: add `spring.kafka.bootstrap-servers`,
  producer serializer config (string key, JSON value), and
  `spring.kafka.consumer.group-id` defaults if not already present.
  The dependency is already declared in `pom.xml`.
- `dto/UpdateProfileNameRequest.java` (new): record with
  `@NotBlank @Size(min = 2, max = 100) String name`.
- `dto/UserPublicDto.java` (new): `{ id, email, name, role, status }`.
- `kafka/UserProfileChangedEvent.java` (new): record
  `{ UUID userId, String name, Instant changedAt }`.
- `kafka/UserProfileEventPublisher.java` (new): wraps
  `KafkaTemplate<String, UserProfileChangedEvent>`. Sends to topic
  `user-profile-events` keyed by `userId.toString()`.
- `service/UserService.java`: add `updateProfileName(UUID userId, String name)`.
  Returns the updated `User`. Throws `UserNotFoundException` if missing.
- `controller/AuthController.java`: add
  `@PatchMapping("/me") public UserPublicDto updateMe(@AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody UpdateProfileNameRequest req)`.
- Tests: `UserServiceTest` — success, blank name, length boundaries, no-op
  when name unchanged, event published exactly once. `AuthControllerTest` —
  401 without token, 200 with token, 400 on invalid length.

### Backend — `crm-service`

- `application.yaml`: configure consumer factory for the
  `user-profile-events` topic (separate group id, JSON value
  deserializer). The existing `kafka` block already has both serializer
  and deserializer settings; the new topic just needs a group id.
- `kafka/UserProfileChangedListener.java` (new): `@Component` with
  `@KafkaListener` method. Receives `UserProfileChangedEvent` and calls a
  new service method.
- `service/TutorNamePropagationService.java` (new): transactional method
  `propagateTutorName(UUID userId, String newName)`. Calls the three
  repositories with the `<>` guard. Marked `@Transactional`.
- `request/CrmApplicationRepository.java`: add
  `@Modifying @Query("UPDATE CrmApplication a SET a.tutorName = :newName WHERE a.tutorUserId = :userId AND a.tutorName <> :newName") int updateTutorNameByTutorUserId(...)`.
- `lesson/LessonRepository.java`: same shape on `Lesson.tutorName`.
- `message/MessageThreadRepository.java`: same shape on
  `MessageThread.participantName`.
- `kafka/KafkaErrorHandlingConfig.java` (new if it does not exist): bean
  for `DefaultErrorHandler` with 5 retries and `DeadLetterPublishingRecoverer`
  pointing at `user-profile-events.DLT`. Verify against
  `subscription/KafkaErrorHandlingConfig` if it already exists.
- Tests: `UserProfileChangedListenerTest` — happy path updates all three
  tables for the right `userId`, ignores other `userId`s, no-op when name
  already matches. Verify idempotency by sending the same event twice.

### Frontend

- `shared/api/auth.ts` (new): `updateAuthProfile({ name })` →
  `PATCH /api/auth/me`. Uses the existing `apiRequest` helper.
- `shared/auth/session.ts`: add `updateSessionUserName(name)` that mutates
  the `AuthSession` in `localStorage`. Re-uses `readAuthSession` and
  `writeAuthSession`.
- `components/layout-helpers.ts`: export
  `profileChangedEvent = 'tutorspace:profile-changed'`.
- `pages/app/TutorWorkspaceProfilePage.tsx`: in `handleSave`:
  1. Call `updateAuthProfile({ name: profile.identity.name })`.
  2. On success, call `updateSessionUserName(profile.identity.name)` and
     `window.dispatchEvent(new Event(profileChangedEvent))`.
  3. Then run the existing `updateTutorWorkspaceProfile` and
     `updateTutorSettings` calls. Move them after the new auth call.
  4. On auth failure, set `status` to the error message and stop.
- `components/layout.tsx`: in the existing `useEffect` for the session,
  add a `window.addEventListener(profileChangedEvent, ...)` that re-reads
  the session and recomputes `workspaceUser`. Clean up on unmount.
- Tests:
  - `tutor-profile-settings-helpers.test.ts` — no changes expected (pure
    local helpers).
  - Optional Connekt E2E script in `scripts/connekt/` that:
    1. Registers a tutor and creates an application as a client.
    2. Patches `/api/auth/me` with a new name.
    3. Polls `GET /api/client/applications` until the new name appears
       (with a 5 second timeout).
    4. Asserts the lesson and message endpoints also reflect the new name.

## Error handling and edge cases

| Failure | Behaviour |
|---|---|
| `PATCH /api/auth/me` returns 4xx/5xx | Frontend shows the API error message, does not call CRM endpoints, keeps the form values. |
| `PATCH /api/auth/me` returns 200 but Kafka publish fails | Outbox pattern is out of scope; the public surface (marketplace) was already updated by the prior CRM→marketplace sync on the profile save, so the worst case is that snapshots in CRM lag. Mitigated by the frontend re-dispatching the event on next save. Logged at ERROR in `UserProfileEventPublisher`. |
| CRM consumer throws on a poisoned event | `DefaultErrorHandler` retries 5 times with backoff, then sends to `user-profile-events.DLT`. |
| Republisher runs DLT events | Out of scope for this iteration; the topic name and recoverer are wired so a future ops tool can replay. |
| Stale JWT in browser | The chip in the header is read from `localStorage`, which is updated synchronously after the auth call. No backend relies on the JWT `name` claim. |
| Student views an application immediately after the tutor renames | Eventually consistent within ~1 second (Kafka). Acceptable per the brainstorming decision. |
| `name` is the same as before | Backend's `updateProfileName` still persists (idempotent on the column) and still publishes the event. The CRM consumer's `<>` guard turns the updates into no-ops. |

## Validation rules for the new name

Reuse the same rules already in
`TutorWorkspaceProfilePage.validate` (lines 52–56) and propagate them to
`UpdateProfileNameRequest`:

- Trim leading/trailing whitespace.
- Collapse internal whitespace runs to a single space.
- Reject blank and length-out-of-range values.
- Length 2–100 characters after normalisation.

Error messages match the existing `GlobalExceptionHandler` shape in both
`auth-service` and `crm-service`.

## Testing strategy

- `auth-service`:
  - `UserServiceTest.updateProfileName` — happy path, blank rejected,
    length boundaries (1, 2, 100, 101), missing user → exception, event
    published exactly once.
  - `AuthControllerTest.patchMe` — 401 without token, 200 returns the
    new DTO, 400 on bad length.
  - Integration test using `@EmbeddedKafka` (the dependency
    `spring-kafka-test` is already in `pom.xml`) — assert the event is
    produced with the expected payload.
- `crm-service`:
  - `UserProfileChangedListenerTest` — assert the three `UPDATE` calls
    run for the matching `userId` only and are no-ops on a replay.
  - Regression — run the existing `PageViewServiceTest`,
    `LessonServiceTest`, `MessageServiceTest`, and
    `ApplicationLifecycleServiceTest`. They read `tutorName` from the
    snapshots, so as long as we do not change the read paths, they stay
    green.
- Frontend:
  - Smoke in `layout-helpers.test.ts` — the new `profileChangedEvent`
    constant has a stable name.
  - Manual: edit the name in `/app/tutor/profile`, save, observe the
    header chip update without reload, navigate to a client account that
    has an existing application, observe the new name in
    `ClientApplicationsPage` and `ClientApplicationDetailsPage`.
- Optional Connekt E2E: see the script sketch in "Components → Frontend".

## Rollout and risks

- The CRM `UPDATE` queries touch three tables. They are scoped by
  `tutor_user_id` and `tutor_name <> :newName`, so they are cheap even on
  large tables. The transaction is short.
- Adding Kafka producer config to `auth-service` is a new responsibility
  for that service. Verified that the dependency is already in `pom.xml`
  and that `crm-service` already has a producer/consumer block in
  `application.yaml` to copy from. The new `spring.kafka.*` block is
  minimal: `bootstrap-servers`, `producer.key-serializer`,
  `producer.value-serializer`. No new infra in `docker-compose.yml` (Kafka
  is already provisioned at `kafka:9092`, exposed on UI port 18085).
- The frontend ordering — auth first, then CRM — is the only behaviour
  change visible to users. The error path is preserved (a failed save
  shows a clear message), and the existing successful path is preserved
  once the auth call returns 200.

## What this does NOT do

- It does not rewrite the existing snapshot rows in a one-off backfill.
  Renamed data becomes visible only for renames that happen after this
  change ships. Old rows keep the name they had when they were written.
  The brainstorming decision is "everywhere shows the current name", and
  current renames are the only ones the product currently allows, so this
  is acceptable. If a backfill is later required, it can be a one-line
  script that joins `auth-service.User` and bulk-updates
  `crm_applications`, `lessons`, `message_threads` — out of scope here.
- It does not add an "edit name" affordance to the client or moderator
  profile pages. Clients and moderators do not currently have one, and
  adding it is a separate product change.

## References

- `frontend/src/pages/app/TutorWorkspaceProfilePage.tsx` (the editor)
- `frontend/src/components/layout.tsx` (lines 320–330, 657, 677 — header)
- `frontend/src/shared/auth/session.ts`
- `backend/auth-service/src/main/java/com/tutor/authservice/domain/User.java`
- `backend/auth-service/src/main/java/com/tutor/authservice/security/jwt/JwtService.java`
- `backend/auth-service/src/main/java/com/tutor/authservice/service/UserService.java`
- `backend/auth-service/src/main/java/com/tutor/authservice/controller/AuthController.java`
- `backend/crm-service/src/main/java/com/tutor/crmservice/request/CrmApplication.java`
- `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/Lesson.java`
- `backend/crm-service/src/main/java/com/tutor/crmservice/message/MessageThread.java`
- `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`
- `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/SubscriptionActivatedListener.java` (Kafka listener pattern to copy)
- `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/NotificationEventPublisher.java` (Kafka publisher pattern to copy)
- `backend/crm-service/src/main/java/com/tutor/crmservice/settings/SettingsService.java` (CRM→marketplace sync, left as-is)
- `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfile.java`
