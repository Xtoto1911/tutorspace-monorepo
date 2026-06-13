# TutorSpace Live API and Responsive Completion Design

## Goal

Complete every existing TutorSpace route so mutable product data comes from real backend APIs and PostgreSQL. Remove runtime mock and fabricated fallback data from the frontend, implement missing backend capabilities, replace the moderator placeholder, and make every route usable at desktop, laptop, tablet, and mobile widths.

Static editorial content remains frontend-owned. This includes the home page copy, FAQ, about, contacts, privacy, terms, navigation labels, and explanatory product text.

## Delivery Strategy

Work in vertical product scenarios:

1. Public marketplace and authentication.
2. Client workspace.
3. Tutor workspace.
4. Admin and moderator workspaces.
5. Cross-route responsive and accessibility audit.
6. Full Docker regression.

Each scenario includes backend persistence and API changes, frontend integration, focused automated tests, and browser verification before moving to the next scenario.

## Repository and Branch Strategy

The workspace contains seven independent Git repositories:

- `frontend`
- `backend/auth-service`
- `backend/marketplace-service`
- `backend/crm-service`
- `backend/notification-service`
- `backend/admin-service`
- `backend/subscription-service`

Existing work is checkpointed before implementation. All repositories use the aligned implementation branch `feature/live-api-responsive`. Commits remain scoped to the owning repository and product scenario.

## Service Ownership

### Auth Service

Owns registration, login, refresh sessions, user roles, account status, and administrator user identity operations.

### Marketplace Service

Owns subjects, public tutor profiles, catalog filtering, reviews, public profile complaints, and request intake.

### CRM Service

Owns client applications, tutor requests, schedules, lessons, students, favorites, CRM messages, and client or tutor workspace settings.

### Notification Service

Owns persisted in-app notifications, unread counts, and read state.

### Subscription Service

Owns tariffs, tutor subscription state, checkout attempts, payment refresh, and YooKassa test-mode integration.

### Admin Service

Owns platform dashboard aggregation, site statistics, audit records, complaints, review or profile moderation, and administrator settings. It may aggregate snapshots from auth, marketplace, notification, and subscription services through internal APIs, but it does not access their databases.

### Frontend Gateway

Exposes one browser origin under `/api/**` and routes requests to the owning service. Browser code never calls Docker service hostnames directly.

## API Design Rules

- Backend endpoints return domain DTOs, not decorative UI concepts such as icon names, color tones, or fabricated metric captions.
- Frontend adapters derive presentational labels and visual tones from real domain fields.
- Empty collections and absent optional values are valid responses and render informative empty states.
- Authentication and authorization failures remain distinct from empty data.
- API errors use consistent HTTP statuses and a response containing at least `message`; stable error codes are added where the frontend needs specific handling.
- DTO changes are tested at controller or service boundaries.
- No service reads or writes another service database.

## Frontend Data Rules

- `VITE_API_MODE` and `VITE_AUTH_API_MODE` are live in the Docker environment.
- Runtime API functions no longer import `mock-data.ts` or accept mock response factories.
- Pages do not initialize state with invented users, applications, messages, metrics, lessons, profiles, or subscriptions.
- Loading, loaded-empty, loaded-data, and error are separate UI states.
- Optimistic updates are allowed only after a real request starts and must roll back or reload on failure.
- Static editorial arrays may remain local when they describe product navigation, guidance, or legal content rather than mutable system state.
- Test fixtures remain allowed inside test files.

## Public and Authentication Scenario

- Subjects, tutor cards, profiles, availability, reviews, favorites, review eligibility, complaints, and application submission use live APIs.
- Tutor registration subject options come from the marketplace API. Failure shows a retryable error instead of a hardcoded subject list.
- Login and registration use auth-service only.
- Forgot-password and reset-password routes either gain real auth-service endpoints or display an explicit unavailable state. They must not claim success without backend processing.
- Catalog and profile pages render honest empty states when no tutors, reviews, subjects, or slots exist.

## Client Workspace Scenario

Implement and connect real APIs for:

- overview aggregation;
- applications and application details;
- cancellation;
- schedule and reschedule requests;
- message threads and sending;
- favorites;
- lesson history;
- client profile, communication, privacy, and notification settings.

Client pages derive counts, tabs, and summaries from returned entities rather than fixed arrays. Empty accounts remain fully usable and explain the next action.

## Tutor Workspace Scenario

Implement and connect real APIs for:

- overview aggregation;
- request queue and stage changes;
- request acceptance or rejection;
- calendar, availability schedules, overrides, lessons, and rescheduling;
- students and student progress;
- message threads and sending;
- public workspace profile editing and marketplace synchronization;
- tutor settings;
- tariffs, subscription state, checkout, and payment refresh.

YooKassa remains test-only. TutorSpace language describes CRM subscription charges only and never implies that lesson payments are guaranteed by the platform.

## Admin and Moderator Scenario

- Replace `/moderator` placeholder content with a real moderator overview.
- Admin dashboard, users, site statistics, subscriptions, audit, settings, alerts, and notifications use live data.
- Moderator complaints and review or profile moderation use persisted queues and real decisions.
- User role and status changes go through auth-service APIs.
- Cross-service dashboard data is aggregated through explicit internal clients with partial-failure handling. One unavailable dependency must not fabricate healthy values.
- Empty moderation queues show a calm completion state.

## Responsive Design

Audit every route at approximately `1440`, `1024`, `768`, and `390` CSS pixels.

### Public Shell

- Header actions and navigation remain reachable without overlap.
- Catalog filters collapse into a usable narrow layout.
- Tutor cards, profile facts, forms, and footer columns avoid horizontal overflow.

### Workspace Shell

- Sidebar navigation collapses into an accessible mobile control.
- Topbar search, notifications, identity, and logout remain reachable.
- Main content uses the full available width without nested-card clutter.

### Dense Content

- Tables use a deliberate mobile representation or labeled horizontal scrolling.
- Multi-column dashboards reduce columns at explicit breakpoints.
- Calendars retain readable dates and usable actions.
- Split message views become a thread list followed by a focused conversation view.
- Primary actions remain visible and do not depend on hover.

### Accessibility

- Preserve WCAG AA contrast targets.
- Keep visible keyboard focus and logical tab order.
- Associate form errors with fields.
- Status is communicated through text or icons as well as color.
- Respect reduced-motion preferences.

## Error and Empty States

Each data surface defines:

- loading skeleton;
- empty state with a relevant next action;
- retryable service or network error;
- authorization failure;
- validation or conflict feedback for mutations.

The UI must not replace failures with plausible-looking content. Partial aggregate responses identify unavailable sections rather than inventing values.

## Testing Strategy

### Backend

- Run `mvn test` in every changed service.
- Add focused unit, controller, repository, or integration tests according to the changed boundary.
- Keep H2 restricted to tests and PostgreSQL as the runtime database.

### Frontend

- Run `npm test`, `npm run typecheck`, and `npm run build`.
- Update API tests to assert live paths and response adaptation without runtime fallback factories.
- Add component or helper coverage for empty, error, and responsive state transformations.
- Add Playwright coverage for the principal role journeys where stable seed data is available.

### Docker and Browser

- Rebuild with `docker compose up --build`.
- Verify service health and gateway routing.
- Exercise public, client, tutor, moderator, and admin routes in the in-app browser.
- Verify representative routes at all target widths.
- Recheck authentication, application submission, tutor request handling, lesson management, notifications, moderation, and subscription test flows.

## Completion Criteria

- No production frontend code imports mutable runtime content from `mock-data.ts`.
- No page silently replaces API failures with invented data.
- Every existing route renders a complete page or an explicit truthful unavailable state backed by real capability status.
- `/moderator` is a real overview page.
- All changed-service tests and frontend checks pass.
- The Docker stack builds and principal role journeys pass browser regression.
- All seven repositories are clean on `feature/live-api-responsive` after final commits.
