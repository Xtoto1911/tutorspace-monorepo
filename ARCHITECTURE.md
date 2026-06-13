# TutorSpace Architecture

## Goal

TutorSpace is a diploma-scale system that combines:

- public tutor marketplace;
- CRM workspace for tutors;
- client workspace for applications, lessons and communication;
- administration, moderation, notifications and subscriptions.

The architecture intentionally stays smaller than a production marketplace with dozens of services. The goal is to demonstrate clear bounded contexts, service ownership, REST APIs, JWT security, asynchronous integration and independent persistence without making the implementation impossible for a diploma project.

## Diploma Service Set

The target backend consists of seven services plus the frontend:

```text
frontend
  |
api-gateway
  |
  |-- auth-service
  |-- marketplace-service
  |-- crm-service
  |-- admin-service
  |-- subscription-service
  |-- notification-service

Kafka:
marketplace-service -> crm-service
crm-service -> notification-service
subscription-service -> notification-service
admin-service -> notification-service
```

## Current State

Implemented now:

- `frontend`
- `auth-service`
- `marketplace-service`
- `crm-service`
- local `docker-compose.yml`

Planned next:

- `notification-service`
- `admin-service`
- `subscription-service`
- `api-gateway`
- Kafka integration

## Service Responsibilities

### frontend

Owns the web UI.

Responsibilities:

- public pages;
- auth pages;
- client workspace;
- tutor workspace;
- admin workspace;
- moderator workspace;
- calling backend through `/api/**`.

Frontend must not call internal service hostnames directly in browser code. It should call one public API origin through the gateway or the local frontend proxy during development.

### api-gateway

Single backend entry point for frontend.

Responsibilities:

- route `/api/auth/**` to `auth-service`;
- route `/api/marketplace/**` to `marketplace-service`;
- route `/api/client/**` and `/api/tutor/**` to `crm-service`;
- route `/api/admin/**` and `/api/moderator/**` to `admin-service`;
- route `/api/subscription/**` to `subscription-service`;
- central CORS configuration;
- JWT forwarding;
- basic rate limiting;
- request logging.

For the current local Docker setup, the custom frontend dev server temporarily proxies `/api/auth/**` and `/api/marketplace/**`. After `api-gateway` is added, this proxy should point to the gateway.

### auth-service

Owns identity and access.

Responsibilities:

- registration;
- login;
- refresh token;
- logout;
- user roles;
- user access status;
- active sessions;
- password hashing;
- JWT issuing.

Data ownership:

- users;
- credentials;
- roles;
- statuses;
- refresh sessions.

Main API:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/admin/users/{userId}`
- `PATCH /api/admin/users/{userId}/role`
- `PATCH /api/admin/users/{userId}/status`

Notes:

- `ROLE_CLIENT` and `ROLE_TUTOR` can register publicly.
- `ROLE_ADMIN` and `ROLE_MODERATOR` are assigned administratively.
- Blocking a user should revoke active refresh sessions.

### marketplace-service

Owns public marketplace data.

Responsibilities:

- tutor catalog;
- public tutor profiles;
- subjects;
- filters;
- public certificates metadata;
- public reviews and ratings;
- request intake from client to tutor;
- publishing `RequestCreated`.

Data ownership:

- tutor public profiles;
- subjects;
- certificate metadata;
- reviews;
- rating aggregates;
- marketplace request intake records.

Main API:

- `GET /api/marketplace/tutors`
- `GET /api/marketplace/tutors/{idOrSlug}`
- `POST /api/marketplace/tutors`
- `POST /api/marketplace/tutors/{tutorId}/requests`
- `GET /api/marketplace/subjects`
- `POST /api/marketplace/subjects`
- `GET /api/marketplace/reviews`
- `POST /api/marketplace/reviews`
- `GET /api/marketplace/certificates`
- `POST /api/marketplace/certificates`

Events:

- `TutorProfileCreated`
- `TutorProfileUpdated`
- `ReviewCreated`
- `RequestCreated`

### crm-service

Owns the working process after a request is created.

Responsibilities:

- client applications;
- tutor requests;
- request statuses;
- student cards;
- lesson calendar;
- lessons;
- lesson notes;
- basic tutor analytics;
- basic client history;
- simple CRM messaging for diploma scope.

Data ownership:

- applications;
- application statuses;
- lessons;
- lesson calendar;
- student cards;
- CRM messages;
- lesson notes.

Main API:

- `GET /api/client/applications`
- `GET /api/client/applications/{id}`
- `PATCH /api/client/applications/{id}/cancel`
- `GET /api/client/schedule`
- `GET /api/client/history`
- `GET /api/client/messages`
- `GET /api/tutor/requests`
- `PATCH /api/tutor/requests/{id}/accept`
- `PATCH /api/tutor/requests/{id}/reject`
- `GET /api/tutor/students`
- `GET /api/tutor/calendar`
- `POST /api/tutor/lessons`
- `GET /api/tutor/messages`

Consumes:

- `RequestCreated`

Publishes:

- `ApplicationAccepted`
- `ApplicationRejected`
- `LessonCreated`
- `LessonCancelled`
- `MessageCreated`

Diploma simplification:

- Do not create a separate `messaging-service`.
- Store CRM messages in `crm-service`.

### notification-service

Owns delivery of notifications.

Responsibilities:

- email notification stub;
- in-app notification log;
- future Telegram integration;
- notification templates;
- delivery history.

Data ownership:

- notification templates;
- notification delivery log;
- user notification inbox for in-app messages.

Consumes:

- `RequestCreated`
- `ApplicationAccepted`
- `ApplicationRejected`
- `LessonCreated`
- `LessonCancelled`
- `MessageCreated`
- `SubscriptionActivated`
- `SubscriptionExpired`
- `UserBlocked`

Main API:

- no public frontend API in MVP;
- optional admin diagnostics endpoint: `GET /api/admin/notifications`.

Diploma simplification:

- Real email sending is optional.
- It is enough to store notification records and show that events are consumed.

### admin-service

Owns administration and moderation workflow.

Responsibilities:

- admin dashboard;
- moderation queue;
- complaints;
- review moderation;
- certificate moderation;
- warnings;
- audit log;
- platform statistics;
- user management orchestration through `auth-service`.

Data ownership:

- complaints;
- moderation decisions;
- warnings;
- audit records;
- admin dashboard snapshots.

Main API:

- `GET /api/admin/dashboard`
- `GET /api/admin/users`
- `PATCH /api/admin/users/{id}/status`
- `PATCH /api/admin/users/{id}/role`
- `GET /api/admin/audit`
- `GET /api/moderator/complaints`
- `POST /api/moderator/complaints/{id}/resolve`
- `GET /api/moderator/reviews`
- `POST /api/moderator/reviews/{id}/approve`
- `POST /api/moderator/reviews/{id}/reject`
- `GET /api/moderator/certificates`
- `POST /api/moderator/certificates/{id}/approve`
- `POST /api/moderator/certificates/{id}/reject`

Complaint scope:

- client can complain about tutor review/comment;
- client can complain about tutor public profile;
- client can complain about tutor message;
- tutor can complain about client;
- complaints are not tied to payment for lessons.

Publishes:

- `ComplaintCreated`
- `ModerationDecisionCreated`
- `UserBlocked`
- `WarningSent`

Diploma simplification:

- Do not create a separate `moderation-service`.
- Keep moderation inside `admin-service`.
- Do not create a separate `audit-service`.
- Keep audit inside `admin-service`.

### subscription-service

Owns tutor subscription for CRM capabilities.

Responsibilities:

- tariffs;
- tutor subscription status;
- YooKassa test checkout integration;
- payment attempt records;
- YooKassa test webhook handling;
- subscription activation;
- subscription expiration.

Data ownership:

- tariffs;
- subscriptions;
- payment attempts;
- payment provider webhook log.

Main API:

- `GET /api/tutor/subscription`
- `GET /api/tutor/subscription/tariffs`
- `POST /api/tutor/subscription/checkout`
- `POST /api/tutor/subscription/webhook`
- `GET /api/admin/subscriptions`

Events:

- `SubscriptionCheckoutCreated`
- `SubscriptionActivated`
- `SubscriptionExpired`
- `SubscriptionPaymentFailed`

Business rule:

- TutorSpace does not act as a guarantor for lesson payments.
- The platform charges only for tutor subscriptions that unlock CRM capabilities.

Payment provider plan:

- Integrate YooKassa only for tutor subscription payments.
- Use YooKassa test mode for the diploma implementation.
- Test mode must use test shop credentials from environment variables.
- No real money should be charged in the diploma environment.
- Payment objects from the test shop are expected to have `test=true`.
- Store YooKassa payment id, status, amount, currency, confirmation URL and webhook payload.
- Activate subscription only after a successful test payment/webhook.
- Official test documentation: https://yookassa.ru/developers/payment-acceptance/testing-and-going-live/testing

SDK candidate:

- `DeelTer/YooKassaSDK` can be evaluated as a Java SDK candidate for `subscription-service`.
- Repository: https://github.com/DeelTer/YooKassaSDK
- The SDK README shows Maven/JitPack usage and examples for payment creation, payment status lookup, receipts and refunds.
- Before adding it to the project, verify the latest release, license, maintenance status and compatibility with Spring Boot 3 / Java 21.
- Official YooKassa API documentation remains the source of truth. If SDK behavior differs from official docs, follow the official docs.
- If the SDK is not suitable, implement YooKassa calls with a small internal adapter over HTTP client.

Suggested environment variables:

- `YOOKASSA_SHOP_ID`
- `YOOKASSA_SECRET_KEY`
- `YOOKASSA_RETURN_URL`
- `YOOKASSA_WEBHOOK_SECRET`
- `YOOKASSA_TEST_MODE=true`

## Services Intentionally Not Created For Diploma

These can be mentioned as future evolution, but should not be implemented as separate services in the diploma scope:

- `profile-service`: client details can live in `crm-service`, public tutor data in `marketplace-service`, identity in `auth-service`.
- `messaging-service`: messages can live in `crm-service`.
- `media-service`: files and photos can be represented as URLs/metadata.
- `search-service`: filtering can stay inside `marketplace-service`.
- `analytics-service`: analytics can be computed in `crm-service` and `admin-service`.
- `audit-service`: audit can stay inside `admin-service`.
- `content-service`: static pages can stay in frontend.

## Data Ownership

| Data | Owner |
| --- | --- |
| Users, credentials, roles, sessions | `auth-service` |
| Public tutor profile, subjects, reviews, certificates | `marketplace-service` |
| Applications, lessons, students, CRM messages | `crm-service` |
| Complaints, moderation decisions, audit records | `admin-service` |
| Tariffs, subscriptions, payment attempts | `subscription-service` |
| Notification templates and delivery log | `notification-service` |

Services must not read or write another service's database directly.

## Communication

Synchronous REST:

- frontend -> `api-gateway`;
- `api-gateway` -> backend services;
- `admin-service` -> `auth-service` for user role/status changes if needed.

Asynchronous Kafka:

- `marketplace-service` publishes request events;
- `crm-service` consumes request events and publishes application/lesson events;
- `notification-service` consumes business events;
- `subscription-service` publishes subscription events;
- `admin-service` publishes moderation and blocking events.

Current pre-Kafka MVP integration:

- `marketplace-service` synchronously forwards request intake to `crm-service` through an internal REST endpoint;
- this transport is temporary and should be replaced by Kafka after the REST happy path is stable.

## Main Diploma Scenario

This scenario should be demonstrable during defense:

1. Client registers.
2. Client logs in.
3. Client opens tutor catalog.
4. Client opens tutor profile.
5. Client sends application.
6. `marketplace-service` validates input and forwards request intake to `crm-service`.
7. `crm-service` creates application and keeps the same payload event-ready for future `RequestCreated` publication.
8. `notification-service` stores notification for tutor.
9. Tutor logs in.
10. Tutor accepts application.
11. `crm-service` creates student card and lesson.
12. `notification-service` stores notification for client.
13. Tutor activates subscription.
14. `subscription-service` publishes `SubscriptionActivated`.
15. Admin/moderator processes a complaint or review moderation item.

## Implementation Order

1. Stabilize `auth-service`, frontend session refresh and shared JWT contract.
2. Finish live public marketplace integration.
3. Stabilize `crm-service` as the owner of applications, students, lessons and CRM messages.
4. Create `notification-service` with a notification log over temporary internal REST integration.
5. Add Kafka and migrate request/application/lesson events.
6. Create `admin-service` with moderation and audit.
7. Create `subscription-service` with checkout stub.
8. Add `api-gateway`.
9. Replace H2 with PostgreSQL per service.
10. Update Docker Compose to run all services.

## Diploma Acceptance Criteria

- Each service has its own database.
- JWT is issued by `auth-service` and accepted by other services.
- Frontend works through a single API entry point.
- At least one Kafka scenario works end-to-end.
- Client request flow works from public tutor profile to tutor CRM.
- Tutor can accept or reject application.
- Tutor can create lesson.
- Notifications are recorded from events.
- Moderator/admin can process complaint or review moderation.
- Tutor subscription can be activated through checkout stub.
- Docker Compose can run the whole system locally.
