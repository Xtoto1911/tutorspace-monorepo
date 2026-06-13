# Marketplace Service

Public marketplace service for TutorSpace.

Initial ownership:

- tutor public profiles
- subjects
- certificates metadata
- public reviews and ratings
- request intake for future `RequestCreated` Kafka integration

Main endpoints:

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

Kafka is intentionally not wired yet. `RequestCreatedEvent` is returned from the
request endpoint now and can be published when the broker is added.

## Security

The service validates Bearer JWT tokens issued by `auth-service` with the same
Base64-encoded `JWT_SECRET`.

Public read endpoints stay open:

- `GET /api/marketplace/**`

Role-protected write endpoints:

- `ROLE_ADMIN`: `POST /api/marketplace/subjects`
- `ROLE_TUTOR`: `POST /api/marketplace/tutors`, `POST /api/marketplace/certificates`
- `ROLE_CLIENT`: `POST /api/marketplace/tutors/{tutorId}/requests`, `POST /api/marketplace/reviews`
