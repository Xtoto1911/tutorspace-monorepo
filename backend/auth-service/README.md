# Auth Service

Authentication service for TutorSpace.

## Runtime configuration

The service requires a Base64-encoded JWT secret:

```powershell
$env:JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
```

Optional token lifetimes:

```powershell
$env:JWT_ACCESS_TOKEN_TTL = "15m"
$env:JWT_REFRESH_TOKEN_TTL = "24h"
```

`ROLE_CLIENT` and `ROLE_TUTOR` can be created through public registration.
`ROLE_ADMIN` and `ROLE_MODERATOR` are reserved for administrative assignment.

Refresh tokens are stored as active sessions by SHA-256 hash. `POST /api/auth/logout`
revokes the session bound to the submitted refresh token.

Admin-only user management endpoints:

- `GET /api/admin/users/{userId}`
- `PATCH /api/admin/users/{userId}/role`
- `PATCH /api/admin/users/{userId}/status`

Blocking a user revokes all active refresh sessions for that user.
