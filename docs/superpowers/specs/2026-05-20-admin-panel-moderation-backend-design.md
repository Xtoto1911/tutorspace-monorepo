# Admin Panel And Moderation Backend Completion - Design Spec

**Date:** 2026-05-20
**Status:** Approved for implementation planning
**Scope:** Complete live backend data for all current admin and moderator frontend sections by using `admin-service` as a backend-for-frontend aggregator over existing services.

## Context

TutorSpace already has frontend pages for:

- `/admin`
- `/admin/users`
- `/admin/site-stats`
- `/admin/subscriptions`
- `/admin/audit`
- `/admin/settings`
- `/moderator/complaints`
- `/moderator/review-moderation`

The frontend contract expects these routes to use `/api/admin/**` and `/api/moderator/**`. The local frontend proxy already forwards those prefixes to `admin-service`.

The current backend has partial support:

- `admin-service` stores complaints and audit logs, and has basic moderation/user action endpoints.
- `marketplace-service` has review and certificate moderation internals, but review creation still publishes immediately.
- `auth-service` supports getting one user, changing role, and changing status, but does not expose a user list.
- `subscription-service` supports tariffs, checkout, payment refresh, and current subscription lookup, but does not expose admin read models or admin write actions.
- Several frontend API helpers still contain placeholder `:id` paths.

The target is not backend demo data. Admin pages must be backed by real aggregates from current services. Where data does not exist yet, the backend should return honest empty states or zero metrics.

## Goals

1. Make `admin-service` the single live API surface for admin and moderator pages.
2. Return page DTOs that match the current frontend types in `frontend/src/shared/types.ts`.
3. Aggregate real data from `auth-service`, `marketplace-service`, `subscription-service`, and `admin-service` local tables.
4. Support only real actions that already have backend meaning:
   - block and unblock users;
   - change user roles;
   - resolve complaints;
   - approve or reject moderated tutor/review/profile items where marketplace supports it.
5. Keep subscription management read-only in this iteration.
6. Preserve frontend's single public `/api/**` origin and avoid hardcoded service hostnames in browser code.

## Non-Goals

- Creating a new API gateway.
- Adding fake backend-seeded analytics for missing metrics.
- Adding admin pause/cancel/change-plan subscription actions before `subscription-service` has real business rules for them.
- Building automatic moderation or ML moderation.
- Replacing the existing frontend visual design.
- Implementing a new event-driven admin read model.

## Recommended Architecture

`admin-service` acts as a backend-for-frontend aggregator for the admin and moderator UI.

```text
frontend
  -> /api/admin/**, /api/moderator/**
      -> admin-service
          -> auth-service
          -> marketplace-service
          -> subscription-service
          -> admin-service H2 complaints/audit tables
```

Frontend continues to call one public origin. `admin-service` owns shaping data into frontend page DTOs.

## Public Admin And Moderator Endpoints

`admin-service` should expose:

```text
GET /api/admin/dashboard
GET /api/admin/alerts
GET /api/admin/users
PATCH /api/admin/users/{userId}
GET /api/admin/site-stats
GET /api/admin/subscriptions
GET /api/admin/audit
GET /api/admin/settings
GET /api/moderator/complaints
PATCH /api/moderator/complaints/{complaintId}/moderate
GET /api/moderator/review-moderation
POST /api/moderator/review-moderation/{id}/approve
POST /api/moderator/review-moderation/{id}/reject
```

These endpoint responses should match the existing frontend page types rather than requiring broad page rewrites.

## Internal Service Additions

### auth-service

Add:

```text
GET /api/admin/users
```

This returns all users as `UserAdminDto` records. Existing endpoints remain the write source:

```text
PATCH /api/admin/users/{userId}/status
PATCH /api/admin/users/{userId}/role
```

### subscription-service

Add:

```text
GET /internal/admin/subscriptions
```

This returns a compact snapshot containing:

- active tariffs;
- tutor subscriptions;
- payments;
- subscription periods.

The snapshot is read-only. Admin subscription page actions should be disabled or omitted unless a real backend action exists.

### marketplace-service

Use or complete:

```text
GET /internal/reviews?status=PENDING_MODERATION
GET /internal/certificates?status=PENDING_MODERATION
GET /internal/marketplace/tutors?status=PENDING_MODERATION
PATCH /internal/reviews/{id}/status
PATCH /internal/certificates/{id}/status
PATCH /internal/marketplace/tutors/{idOrSlug}/hide
POST /internal/reviews/publication-restrictions
```

Review creation should create `PENDING_MODERATION` reviews. Public review lists must continue to show only `PUBLISHED` reviews.

## admin-service Components

Add read-model services:

- `AdminDashboardService`: user counts, moderation queue size, dependency service statuses, recent audit events, alerts.
- `AdminUsersService`: user table, role/status metrics, selected user card, and action mapping.
- `AdminSiteStatsService`: real platform metrics available from current services, with zero or empty states for unavailable metrics.
- `AdminSubscriptionReadService`: tariff, subscription, payment, and revenue summary from `subscription-service`.
- `AdminSettingsReadService`: platform configuration summary and dependency service status.
- `ModeratorQueueService`: complaints plus marketplace moderation queues.

Add service clients:

- `AuthAdminClient`
- `MarketplaceModerationClient`
- `SubscriptionAdminClient`

Clients should keep downstream DTOs separate from frontend page DTOs.

## Action Rules

`PATCH /api/admin/users/{userId}` accepts an action body:

```json
{
  "action": "block"
}
```

Supported actions:

- `block` -> `auth-service` status `BLOCKED`
- `unblock` -> `auth-service` status `ACTIVE`
- `make_admin` -> `auth-service` role `ROLE_ADMIN`
- `make_moderator` -> `auth-service` role `ROLE_MODERATOR`

Unsupported actions return `422` with a clear message.

Complaint moderation keeps the existing `PATCH /api/moderator/complaints/{id}/moderate` backend behavior. Frontend helpers still pass `/moderator/complaints/{id}/moderate` because `VITE_API_BASE_URL=/api` supplies the public prefix. The endpoint may apply marketplace effects when the complaint target supports them:

- hide review;
- restrict review publication;
- hide tutor profile;
- dismiss.

Profile/review moderation actions call marketplace internal endpoints and write audit records only after the downstream call succeeds.

## Error Handling

Read endpoints should be resilient:

- If a downstream service is unavailable, return the rest of the page.
- Mark the affected service or section as unavailable.
- Add a dashboard/settings alert where appropriate.
- Do not substitute demo data.

Write endpoints should be strict:

- Return an error when the downstream action fails.
- Do not write a successful audit entry for failed downstream actions.
- Use `404` for missing entities.
- Use `409` or `422` for invalid state or unsupported action.

## Security

- `/api/admin/**` requires `ROLE_ADMIN`.
- `/api/moderator/**` requires `ROLE_ADMIN` or `ROLE_MODERATOR`.
- Admin JWT is forwarded to `auth-service` admin endpoints.
- Internal service endpoints remain internal implementation details and are not exposed in the frontend contract.

## Frontend Changes

Keep the current page structure and visual design. Update only the API integration layer and any small state handling required by real data.

Required frontend fixes:

- Replace placeholder paths such as `/admin/users/:id` with template strings.
- Replace `/moderator/review-moderation/:id/approve` with real profile/review ids.
- Map backend action labels to supported backend actions.
- Keep mock fallback only as an emergency rendering fallback when live API is unavailable.
- Run `npm run typecheck`.

## Testing Strategy

Backend:

- `admin-service`: unit tests for page aggregators and action mapping with mocked clients.
- `auth-service`: test admin user list endpoint.
- `subscription-service`: test internal admin subscription snapshot.
- `marketplace-service`: test review creation goes to `PENDING_MODERATION`, public lists show only `PUBLISHED`, and internal moderation list works.

Frontend:

- Focused tests for `admin.ts` path/action mapping.
- `npm run typecheck`.

Local verification:

```text
cd backend/admin-service && mvn test
cd backend/auth-service && mvn test
cd backend/marketplace-service && mvn test
cd backend/subscription-service && mvn test
cd frontend && npm run typecheck
```

## Implementation Boundaries

This design is one implementation slice. It is complete when:

- every current admin/moderator frontend page can load live data through `admin-service`;
- unsupported metrics are represented as empty or zero values;
- supported admin/moderator actions work against real services;
- subscription admin remains read-only;
- tests pass for changed services and frontend typecheck passes.

## Risks

Downstream services may not expose enough fields for rich UI copy. The mitigation is to build truthful but modest page DTOs, not fake context.

The current project has no API gateway. The mitigation is to keep the existing frontend proxy behavior and route admin/moderator traffic to `admin-service`.

Internal endpoints are not strongly authenticated today. This design keeps the current local service-to-service style and does not expand the public frontend surface.
