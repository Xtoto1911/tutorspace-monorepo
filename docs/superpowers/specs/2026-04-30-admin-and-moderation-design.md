# Iteration E: Admin And Moderation — Design Spec

**Date:** 2026-04-30  
**Status:** Approved  
**Scope:** Create admin-service with moderation-first scope: complaints, review moderation, certificate moderation, audit log, user blocking.

## 1. Context

The TutorSpace platform has four microservices:
- `auth-service` (port 8080) — authentication, user management, JWT, admin user blocking API
- `marketplace-service` (port 8081) — tutor profiles, reviews, certificates
- `crm-service` (port 8082) — applications, lessons, messages
- `notification-service` (port 8083) — notification log

Reviews and certificates already exist in `marketplace-service` with `ReviewStatus` and `CertificateStatus` enums. Currently reviews are created as `PUBLISHED` and certificates as `PENDING_MODERATION`.

This iteration creates a new `admin-service` to centralize moderation capabilities.

## 2. Goals

1. Create `backend/admin-service` (Spring Boot 3, H2, port 8084).
2. Implement complaints system with categories and priorities.
3. Implement review moderation workflow.
4. Implement certificate moderation workflow.
5. Implement audit log for moderation actions.
6. Connect user blocking through `auth-service` API contract.

## 3. Non-Goals

- Frontend admin UI (separate iteration).
- Automatic moderation / ML-based filtering.
- Real-time notifications for moderation events.
- Complex RBAC beyond ROLE_ADMIN.

## 4. Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  admin-service  │────►│  auth-service    │     │  marketplace-s  │
│  (port 8084)    │     │  (block user)    │     │  (review/cert)  │
└─────────────────┘     └──────────────────┘     └─────────────────┘
         │                                              ▲
         │           ┌─────────────┐                    │
         └──────────►│   H2 (own)  │                    │
                     │ Complaints  │                    │
                     │ AuditLog    │                    │
                     └─────────────┘                    │
                                                        │
         PATCH /internal/reviews/{id}/status ──────────┘
         PATCH /internal/certificates/{id}/status ─────┘
         PATCH /api/admin/users/{id}/status ───► auth-service
```

## 5. Entities

### 5.1 Complaint

```java
@Entity
@Table(name = "complaints")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID reportedUserId;

    @Column(nullable = false)
    private UUID reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory category;

    @Column(nullable = false)
    private String reason;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant moderatedAt;

    private UUID moderatorId;

    @Column(length = 1000)
    private String resolutionNotes;
}
```

**Enums:**
- `ComplaintCategory`: SPAM, HARASSMENT, FAKE_PROFILE, INAPPROPRIATE_CONTENT, OTHER
- `ComplaintPriority`: LOW, MEDIUM, HIGH
- `ComplaintStatus`: PENDING, APPROVED, REJECTED

### 5.2 AuditLog

```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID adminUserId;

    @Column(nullable = false)
    private String actionType; // COMPLAINT_APPROVED, COMPLAINT_REJECTED, etc.

    @Column(nullable = false)
    private String targetType; // COMPLAINT, REVIEW, CERTIFICATE, USER

    @Column(nullable = false)
    private UUID targetId;

    @Column(length = 2000)
    private String details; // JSON with action details

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
```

## 6. API Specification

### 6.1 Complaints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/complaints` | List complaints (filter: status, priority, category) |
| GET | `/api/admin/complaints/{id}` | Get complaint details |
| POST | `/api/admin/complaints` | Create complaint (for clients/tutors) |
| PATCH | `/api/admin/complaints/{id}/moderate` | Moderate complaint (approve/reject) |

### 6.2 Moderation

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/moderation/reviews` | List reviews by status |
| POST | `/api/admin/moderation/reviews/{id}/approve` | Approve review |
| POST | `/api/admin/moderation/reviews/{id}/reject` | Reject review |
| GET | `/api/admin/moderation/certificates` | List certificates by status |
| POST | `/api/admin/moderation/certificates/{id}/approve` | Approve certificate |
| POST | `/api/admin/moderation/certificates/{id}/reject` | Reject certificate |

### 6.3 User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/users/{id}/block` | Block user via auth-service |
| POST | `/api/admin/users/{id}/unblock` | Unblock user via auth-service |

### 6.4 Audit Log

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/audit-log` | List audit log entries |

## 7. Changes in Existing Services

### 7.1 marketplace-service

1. **Review status change:** Create review with `PENDING` status (instead of `PUBLISHED`).
2. **Add ReviewModerationController:**
   - `PATCH /internal/reviews/{id}/status` — update review status (for admin-service)
   - `GET /internal/reviews?status=PENDING` — list reviews by status
3. **Add CertificateModerationController:**
   - `PATCH /internal/certificates/{id}/status` — update certificate status
   - `GET /internal/certificates?status=PENDING_MODERATION` — list certificates by status
4. **Filter published:** `listPublished` returns only `PUBLISHED` reviews.

### 7.2 auth-service

No changes required. The existing `AdminUserController` already provides:
- `PATCH /api/admin/users/{id}/status` — update user status (ACTIVE/BLOCKED)

## 8. Service Integration

### 8.1 admin-service → auth-service
- Uses `RestClient` to call `PATCH /api/admin/users/{id}/status`
- Requires `AUTH_SERVICE_URL` environment variable
- Admin JWT token forwarded via `Authorization` header

### 8.2 admin-service → marketplace-service
- Uses `RestClient` to call moderation endpoints
- Requires `MARKETPLACE_SERVICE_URL` environment variable
- Internal endpoints use simple service-to-service auth (no JWT)

## 9. Security

- **admin-service:** Protected by JWT filter, requires `ROLE_ADMIN`
- **marketplace-service internal endpoints:** Permit all (service-to-service, no JWT)
- **auth-service admin endpoints:** Already require `ROLE_ADMIN`

## 10. Configuration

### application.yaml (admin-service)
```yaml
server:
  port: ${SERVER_PORT:8084}

spring:
  application:
    name: admin-service
  datasource:
    url: ${ADMIN_DB_URL:jdbc:h2:mem:admindb}
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: ${JWT_SECRET}

app:
  auth-service-url: ${AUTH_SERVICE_URL:http://auth-service:8080}
  marketplace-service-url: ${MARKETPLACE_SERVICE_URL:http://marketplace-service:8081}
```

## 11. Testing Strategy

### 11.1 Unit Tests
- ComplaintService: create, moderate, list with filters
- AuditLogService: log creation, retrieval
- UserBlockClient: mock RestClient, verify auth-service call

### 11.2 Integration Tests
- Full flow: create complaint → moderate → verify audit log entry

## 12. Docker Compose

Add to `docker-compose.yml`:
```yaml
  admin-service:
    build:
      context: ./backend/admin-service
    container_name: tutorspace-admin-service
    environment:
      SERVER_PORT: 8084
      JWT_SECRET: ${JWT_SECRET}
      AUTH_SERVICE_URL: http://auth-service:8080
      MARKETPLACE_SERVICE_URL: http://marketplace-service:8081
      KAFKA_BOOTSTRAP: kafka:9092
    ports:
      - "18084:8084"
    depends_on:
      - auth-service
      - marketplace-service
```

## 13. Migration Plan

1. Scaffold `admin-service` project (pom.xml, main class, config).
2. Create `Complaint` entity + repository + service + controller + DTOs.
3. Create `AuditLog` entity + repository + service + controller.
4. Create `UserBlockClient` for auth-service integration.
5. Create `MarketplaceModerationClient` for marketplace-service integration.
6. Add JWT security config (ROLE_ADMIN).
7. Modify `marketplace-service`: change review creation to PENDING, add moderation controllers.
8. Add tests.
9. Update `docker-compose.yml`.
10. Run all tests.

## 14. Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| auth-service unavailable | Log error, don't fail moderation action |
| marketplace-service unavailable | Log error, allow complaint moderation anyway |
| Circular dependency | admin-service depends on auth/marketplace, not vice versa |

## 15. Open Questions

None. Design approved.
