# Design: Defense Scripts for Backend and Frontend Theses

## Goal

Rewrite two existing defense scripts for two independent graduation theses that
implement different parts of the same TutorSpace project.

The backend thesis is presented first. The frontend thesis follows immediately.
Each author must defend their own subject, decisions, implementation, and
results.

## Source Boundary

- Backend script content is based only on the backend thesis.
- Frontend script content is based only on the frontend thesis.
- The thesis documents must not be edited.
- Repository files may be used only to detect an obvious contradiction, not as
  a source of new claims for the speeches.
- The two existing speech documents are the only documents to be edited.

## Shared Constraints

- 12 slides per speech, within the required range of 11-13.
- No live demonstration.
- Each speech must remain under 7 minutes.
- Target spoken length: approximately 750-850 Russian words.
- Use formal but natural spoken Russian suitable for a graduation defense.
- Avoid long enumerations, repeated conclusions, and implementation details
  that are hard to explain from a slide.

## Backend Speech

The backend speaker provides the only general introduction to the shared
project. The speech then focuses on the backend thesis:

1. Thesis title and author.
2. Relevance of the project and division of responsibilities.
3. Backend thesis goal and tasks.
4. Requirements and key business rules.
5. Server architecture.
6. Responsibilities of the six services.
7. REST and Kafka interaction.
8. Authentication, JWT, and role-based access.
9. Data model and database-per-service.
10. Tutor subscriptions and test YooKassa integration.
11. Implementation and verification.
12. Personal backend results and transition to the frontend speaker.

The conclusion must distinguish the author's backend contribution from the
shared product.

## Frontend Speech

The frontend speaker does not repeat the relevance, competitor analysis, or
general description already given by the backend speaker. A short transition
establishes that this is a separate thesis covering the client side:

1. Thesis title, author, and connection to the presented project.
2. Frontend thesis goal and tasks.
3. User-interface requirements.
4. Roles and key user scenarios.
5. Screen and view-model design.
6. Frontend technology stack.
7. Client application architecture and structure.
8. Routing and role-based access.
9. API layer, session handling, and errors.
10. Implemented client and tutor interfaces.
11. Testing and quality verification.
12. Personal frontend results and conclusion.

The frontend conclusion must describe the author's own interface contribution
without presenting backend work as part of that contribution.

## Acceptance Criteria

- Both scripts contain exactly 12 clearly numbered slide sections.
- The backend script contains the common introduction; the frontend script
  contains only a short transition.
- Every technical claim is traceable to the corresponding thesis.
- YooKassa is described as a test integration for tutor CRM subscriptions, not
  payment for lessons.
- Each script fits the seven-minute limit at a normal defense pace.
- Both Google Docs are re-read after editing to verify their content.
