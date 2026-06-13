# TutorSpace Backlog

This backlog tracks the diploma-scale implementation plan. The goal is to keep the scope large enough for a strong diploma project, but small enough to implement and demonstrate.

## Legend

- `[x]` done
- `[~]` in progress
- `[ ]` planned

## Phase 0 - Local Foundation

- `[x]` Create frontend project.
- `[x]` Create `auth-service`.
- `[x]` Create `marketplace-service`.
- `[x]` Add Docker Compose for current services.
- `[x]` Integrate frontend registration/login with `auth-service`.
- `[x]` Add frontend route guards by role.
- `[x]` Add frontend logout flow.
- `[x]` Add root `.env.example`.
- `[x]` Add healthcheck endpoints for backend services.
- `[ ]` Add persistent PostgreSQL containers instead of H2.

## Phase 1 - Auth Stabilization

- `[x]` Support roles: client, tutor, admin, moderator.
- `[x]` Issue JWT with `userId` and `role`.
- `[x]` Store refresh sessions.
- `[x]` Revoke refresh session on logout.
- `[x]` Add frontend refresh-token flow.
- `[x]` Add admin seed user for local development.
- `[ ]` Add user list endpoint for admin panel if needed.
- `[ ]` Add integration tests for auth endpoints.

## Phase 2 - Marketplace Integration

- `[x]` Implement marketplace service skeleton.
- `[x]` Implement tutors, subjects, certificates, reviews API.
- `[x]` Implement request intake endpoint.
- `[x]` Seed marketplace with demo subjects and tutors.
- `[x]` Connect frontend catalog to `GET /api/marketplace/tutors`.
- `[x]` Connect frontend subject filters to `GET /api/marketplace/subjects`.
- `[x]` Connect public tutor profile to `GET /api/marketplace/tutors/{slug}`.
- `[x]` Connect application form to `POST /api/marketplace/tutors/{tutorId}/requests`.
- `[ ]` Add validation and user-friendly frontend errors.
- `[ ]` Add tests for filtering and tutor profile lookup.

## Phase 3 - CRM Service

- `[x]` Create `backend/crm-service`.
- `[ ]` Add separate CRM database.
- `[x]` Add JWT validation.
- `[x]` Implement applications aggregate.
- `[x]` Implement client application list.
- `[x]` Implement tutor request list.
- `[x]` Implement accept/reject application.
- `[x]` Implement student cards.
- `[x]` Implement lessons and calendar.
- `[x]` Implement simple CRM messages.
- `[ ]` Connect client workspace pages to CRM API.
- `[ ]` Connect tutor workspace pages to CRM API.
- `[ ]` Add tests for request status transitions.
- `[ ]` Add rule: tutor cannot create overlapping lessons.
- `[ ]` Add rule: one client cannot have duplicate active request for same tutor.

## Phase 4 - Kafka And Events

- `[ ]` Add Kafka to Docker Compose.
- `[ ]` Define event envelope format.
- `[ ]` Publish `RequestCreated` from `marketplace-service`.
- `[ ]` Consume `RequestCreated` in `crm-service`.
- `[ ]` Publish `ApplicationAccepted` from `crm-service`.
- `[ ]` Publish `ApplicationRejected` from `crm-service`.
- `[ ]` Publish `LessonCreated` from `crm-service`.
- `[ ]` Add retry/dead-letter notes or minimal handling.
- `[ ]` Add end-to-end test or documented demo flow.

## Phase 5 - Notification Service

- `[ ]` Create `backend/notification-service`.
- `[ ]` Add separate notification database.
- `[ ]` Consume `RequestCreated`.
- `[ ]` Consume `ApplicationAccepted`.
- `[ ]` Consume `ApplicationRejected`.
- `[ ]` Consume `LessonCreated`.
- `[ ]` Consume `SubscriptionActivated`.
- `[ ]` Store in-app notification log.
- `[ ]` Add email/telegram stub interface.
- `[ ]` Add admin diagnostics endpoint for notification log.

## Phase 6 - Admin And Moderation

- `[ ]` Create `backend/admin-service`.
- `[ ]` Add separate admin database.
- `[ ]` Add JWT validation.
- `[ ]` Implement admin dashboard summary.
- `[ ]` Implement audit log.
- `[ ]` Implement complaints.
- `[ ]` Implement review moderation.
- `[ ]` Implement certificate moderation.
- `[ ]` Implement warnings.
- `[ ]` Integrate user blocking with `auth-service`.
- `[ ]` Connect admin workspace pages.
- `[ ]` Connect moderator workspace pages.

Complaint scope:

- `[ ]` Client can complain about tutor review/comment.
- `[ ]` Client can complain about tutor public profile.
- `[ ]` Client can complain about tutor message.
- `[ ]` Tutor can complain about client.

## Phase 7 - Subscription Service

- `[ ]` Create `backend/subscription-service`.
- `[ ]` Add separate subscription database.
- `[ ]` Add JWT validation.
- `[ ]` Implement tariff list.
- `[ ]` Implement tutor current subscription.
- `[ ]` Add YooKassa test mode configuration.
- `[ ]` Add YooKassa test shop credentials through env variables.
- `[ ]` Evaluate `DeelTer/YooKassaSDK` as Java SDK candidate: https://github.com/DeelTer/YooKassaSDK
- `[ ]` Check YooKassa SDK license, latest release and Java 21 / Spring Boot 3 compatibility.
- `[ ]` Decide between third-party SDK and internal YooKassa HTTP adapter.
- `[ ]` Implement YooKassa test checkout creation.
- `[ ]` Store YooKassa payment id, status, amount and confirmation URL.
- `[ ]` Redirect tutor to YooKassa confirmation URL from checkout page.
- `[ ]` Implement YooKassa test webhook endpoint.
- `[ ]` Verify that processed payments are test payments before activating subscription.
- `[ ]` Publish `SubscriptionActivated`.
- `[ ]` Connect tutor subscription page.
- `[ ]` Connect tutor checkout page.
- `[ ]` Connect admin subscriptions page.

Business scope:

- `[ ]` Platform charges only for tutor CRM subscription.
- `[ ]` Platform does not process or guarantee lesson payments.
- `[ ]` Diploma environment uses only YooKassa test payments.
- `[ ]` No real money is charged during diploma demo.
- `[ ]` Document official YooKassa test flow: https://yookassa.ru/developers/payment-acceptance/testing-and-going-live/testing

## Phase 8 - API Gateway

- `[ ]` Create `backend/api-gateway`.
- `[ ]` Route `/api/auth/**`.
- `[ ]` Route `/api/marketplace/**`.
- `[ ]` Route `/api/client/**`.
- `[ ]` Route `/api/tutor/**`.
- `[ ]` Route `/api/admin/**`.
- `[ ]` Route `/api/moderator/**`.
- `[ ]` Route `/api/tutor/subscription/**`.
- `[ ]` Centralize CORS.
- `[ ]` Forward JWT.
- `[ ]` Add request logging.
- `[ ]` Update frontend proxy to point to gateway.
- `[ ]` Update Docker Compose to expose only gateway and frontend.

## Phase 9 - Docker And Persistence

- `[ ]` Add PostgreSQL for `auth-service`.
- `[ ]` Add PostgreSQL for `marketplace-service`.
- `[ ]` Add PostgreSQL for `crm-service`.
- `[ ]` Add PostgreSQL for `admin-service`.
- `[ ]` Add PostgreSQL for `subscription-service`.
- `[ ]` Add PostgreSQL for `notification-service`.
- `[ ]` Add Kafka container.
- `[ ]` Add service healthchecks.
- `[ ]` Add startup dependency notes.
- `[ ]` Add complete local run instructions.

## Phase 10 - Diploma Demo Script

- `[ ]` Prepare demo data.
- `[ ]` Client registers.
- `[ ]` Client searches tutor.
- `[ ]` Client sends application.
- `[ ]` Marketplace publishes `RequestCreated`.
- `[ ]` CRM creates application.
- `[ ]` Notification service records tutor notification.
- `[ ]` Tutor logs in.
- `[ ]` Tutor accepts application.
- `[ ]` CRM creates student card.
- `[ ]` Tutor creates lesson.
- `[ ]` Notification service records client notification.
- `[ ]` Tutor activates subscription.
- `[ ]` Admin/moderator processes complaint or review.
- `[ ]` Show Docker Compose running all services.

## Future Ideas Not In Diploma Scope

- Separate `profile-service`.
- Separate `messaging-service`.
- Separate `media-service`.
- Separate `search-service`.
- Separate `analytics-service`.
- Separate `audit-service`.
- Separate `content-service`.
- Real payment provider integration.
- Real email/Telegram delivery.
- Full-text search engine.
- Object storage for files.
