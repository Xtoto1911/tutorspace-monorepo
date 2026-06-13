# Design: Backend and Frontend Thesis Defense Presentations

## Goal

Create two independent, editable Google Slides presentations for two graduation
theses that implement different parts of the same TutorSpace project:

- backend thesis by Smirnov K. A.;
- frontend thesis by Skryabin N. Yu.

The backend presentation is shown first. The frontend presentation follows it
but remains a separate thesis defense with its own goal, decisions, and results.

## Source Boundary

- Backend content is based only on the backend thesis and approved backend
  speech.
- Frontend content is based only on the frontend thesis and approved frontend
  speech.
- The thesis documents and speech documents are not edited.
- No factual claim is added from external sources.

## Delivery

- Create two native, editable Google Slides presentations.
- Place both presentations in the provided Google Drive folder.
- Each presentation contains exactly 12 slides.
- Slides support a spoken defense of no more than seven minutes.
- There is no live application demonstration.

## Visual Direction

- Strict academic style.
- Light warm-white background.
- Dark neutral text.
- Backend accent: restrained dark green.
- Frontend accent: restrained dark blue.
- Related typography, margins, numbering, and footer treatment across both
  decks.
- Minimal decoration and no glass effects, gradients, or dashboard-like card
  grids.
- One main thesis and one main visual proof per slide.
- Titles remain large and single-line where possible.
- Body copy is concise and presentation-readable.

## Visual Evidence

Diagrams are redrawn as editable presentation graphics from the logic described
in the theses. They are not screenshots of thesis pages.

Frontend application screenshots are not available. The frontend deck uses
three clearly labelled, editable placeholders:

1. tutor catalogue with filters and tutor cards;
2. client cabinet with requests and their statuses;
3. tutor cabinet with students, calendar, or lesson creation.

Each placeholder states what must remain visible in the future screenshot.

## Backend Slide Plan

1. Title: thesis title, author, supervisor, university and year.
2. Relevance: fragmented tutor tools flowing into one platform.
3. Goal and tasks: concise chain from analysis to implementation and checks.
4. Requirements and business rules: request lifecycle and timetable
   restrictions.
5. Backend architecture: client, entry point, services, Kafka, and databases.
6. Six microservices: responsibilities and ownership boundaries.
7. Service interaction: REST for immediate responses and Kafka for events.
8. Security: JWT, role checks, ownership checks, and audit.
9. Data: database-per-service and cross-service references by identifiers.
10. Subscriptions: tutor CRM subscription and test YooKassa payment flow.
11. Implementation and verification: technology stack and verified scenarios.
12. Results: the author's backend contribution and transition to the frontend
    defense.

## Frontend Slide Plan

1. Title: thesis title, author, supervisor, university and year.
2. Goal and tasks: scope of the independent frontend thesis.
3. Interface requirements: functional states and quality criteria.
4. Roles and scenarios: client-to-tutor request and lesson workflow.
5. Interface design: use cases, activities, screen data, and view models.
6. Technology stack: React, TypeScript, React Router, Vitest, and Playwright.
7. Frontend architecture: pages, reusable components, session, and API layer.
8. Routing and access: public routes, protected routes, and role routing.
9. API and session: Bearer token, refresh attempt, logout, and error display.
10. Implemented interfaces: three labelled screenshot placeholders.
11. Testing and quality: unit tests, E2E tests, and UI states.
12. Results: the author's frontend contribution and practical significance.

## Diagram Rules

- Connectors are placed behind nodes.
- Arrows do not cross labels.
- Service and state names use the terminology from the corresponding thesis.
- Diagrams contain only information needed for the spoken point of the slide.
- Legends are used only when color has semantic meaning.
- No diagram is copied from an external source.

## Acceptance Criteria

- Two separate presentations exist in the supplied Drive folder.
- Both presentations contain exactly 12 slides.
- Backend includes the only general relevance explanation.
- Frontend begins with its own thesis scope and does not repeat the general
  relevance section.
- Every technical claim is traceable to the corresponding thesis.
- YooKassa is described as a test integration for tutor CRM subscriptions, not
  lesson payments.
- Frontend slide 10 contains three labelled screenshot placeholders.
- No unresolved generic template text remains.
- All objects stay inside slide bounds and no text overlaps.
- Every slide is structurally verified and visually reviewed before handoff.
