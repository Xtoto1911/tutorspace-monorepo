# Iteration F: Subscription Service — Design Spec

**Date:** 2026-04-30  
**Status:** Approved  
**Scope:** Create `subscription-service` as a bounded context for tutor CRM subscriptions, mock checkout/payment confirmation, subscription periods, and CRM access activation via Kafka.

## 1. Context

TutorSpace currently has `auth-service`, `marketplace-service`, `crm-service`, `notification-service`, and `admin-service`. Kafka is already available in `docker-compose.yml`.

Iteration F adds a separate `subscription-service` for tutor CRM subscription management. The service is the source of truth for tariffs, payments, subscriptions, and renewal periods. CRM receives activation events and stores a local access projection.

## 2. Goals

1. Create `backend/subscription-service` (Spring Boot 3, Java 21, H2, JPA, Kafka producer).
2. Implement tariffs as seeded database data.
3. Implement current subscription lookup.
4. Implement checkout with mock YooKassa payment URL.
5. Implement mock YooKassa test-confirm endpoint.
6. Activate subscription only after confirmed payment.
7. Store subscription periods and renewal history.
8. Publish `SubscriptionActivated` to Kafka topic `crm-events`.
9. Add `crm-service` consumer that stores CRM access in `crm_access`.

## 3. Non-Goals

- Real YooKassa API integration.
- Real webhook signature verification.
- Autopayment/autorenewal charging.
- Frontend subscription UI.
- Enforcing CRM access in existing CRM endpoints.

## 4. Architecture

```text
frontend / tutor
   │
   ▼
subscription-service (8085)
   ├─ tariffs seed data
   ├─ checkout
   ├─ mock YooKassa confirmation
   ├─ payments
   ├─ tutor subscriptions
   ├─ subscription periods
   └─ Kafka publish: crm-events / SUBSCRIPTION_ACTIVATED
                         │
                         ▼
                    crm-service
                    └─ crm_access projection
```

## 5. subscription-service Domain

### 5.1 Tariff

```java
@Entity
@Table(name = "tariffs")
public class Tariff {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int priceRub;

    @Column(nullable = false)
    private int durationDays;

    @Column(nullable = false)
    private boolean active = true;
}
```

Seed tariffs in DB on startup. Initial data:

| code | name | priceRub | durationDays |
|------|------|----------|--------------|
| CRM_BASIC | CRM Basic | 990 | 30 |
| CRM_PRO | CRM Pro | 1990 | 30 |
| CRM_PREMIUM | CRM Premium | 2990 | 30 |

### 5.2 Payment

```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tutorUserId;

    @Column(nullable = false)
    private String tariffCode;

    @Column(nullable = false)
    private int amountRub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private String checkoutUrl;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant confirmedAt;
}
```

`PaymentStatus`: `PENDING`, `CONFIRMED`, `CANCELLED`.

### 5.3 TutorSubscription

```java
@Entity
@Table(name = "tutor_subscriptions")
public class TutorSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tutorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(nullable = false)
    private Instant activeUntil;

    @Column(nullable = false)
    private String currentTariffCode;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
```

`SubscriptionStatus`: `ACTIVE`, `EXPIRED`.

For MVP, active access is determined by `activeUntil > now`. No scheduled expiry job is required.

### 5.4 SubscriptionPeriod

```java
@Entity
@Table(name = "subscription_periods")
public class SubscriptionPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false)
    private String tariffCode;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant activeUntil;
}
```

## 6. subscription-service API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health endpoint |
| GET | `/api/subscriptions/tariffs` | List active tariffs |
| GET | `/api/subscriptions/current?tutorUserId=...` | Get current subscription/access state |
| POST | `/api/subscriptions/checkout` | Create pending mock payment and return checkout URL |
| POST | `/internal/payments/yookassa/test-confirm/{paymentId}` | Confirm mock payment and activate subscription |

### Checkout request

```java
public record CreateCheckoutRequest(
    UUID tutorUserId,
    String tariffCode
) {}
```

### Checkout response

```java
public record CheckoutResponse(
    UUID paymentId,
    String checkoutUrl,
    PaymentStatus status
) {}
```

## 7. Activation Flow

1. Tutor calls checkout for a tariff.
2. `subscription-service` validates active tariff.
3. Creates `Payment(PENDING)`.
4. Returns fake checkout URL: `/mock-yookassa/payments/{paymentId}`.
5. Test-confirm endpoint receives `paymentId`.
6. If payment is `PENDING`, mark as `CONFIRMED`.
7. Find existing subscription for tutor.
8. If active, extend from current `activeUntil`; otherwise start from now.
9. Create `SubscriptionPeriod`.
10. Update `TutorSubscription`.
11. Publish `SUBSCRIPTION_ACTIVATED` to Kafka topic `crm-events`.

The confirmation endpoint must be idempotent: confirming an already confirmed payment returns existing subscription data and does not create a duplicate period or duplicate activation event.

## 8. Kafka Event

Topic: `crm-events`.

Envelope:

```java
public record SubscriptionEventEnvelope<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String producer,
    T payload
) {}
```

Payload:

```java
public record SubscriptionActivatedPayload(
    UUID tutorUserId,
    String tariffCode,
    Instant activeUntil
) {}
```

Event type: `SUBSCRIPTION_ACTIVATED`.  
Producer: `subscription-service`.

## 9. crm-service Projection

### CrmAccess

```java
@Entity
@Table(name = "crm_access")
public class CrmAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tutorUserId;

    @Column(nullable = false)
    private String tariffCode;

    @Column(nullable = false)
    private Instant activeUntil;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
```

Consumer:
- listens to `crm-events`
- handles only `SUBSCRIPTION_ACTIVATED`
- upserts `crm_access` by `tutorUserId`

No enforcement in CRM endpoints in this iteration.

## 10. Security

- Public read: `GET /api/subscriptions/tariffs`
- Tutor endpoints: protected later by gateway; in MVP accept `tutorUserId` in request/query.
- Internal test-confirm endpoint is open in MVP for local demo.

## 11. Docker Compose

Add `subscription-service`:

```yaml
  subscription-service:
    build:
      context: ./backend/subscription-service
    container_name: tutorspace-subscription-service
    environment:
      SERVER_PORT: 8085
      JWT_SECRET: ${JWT_SECRET:-MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}
      KAFKA_BOOTSTRAP: kafka:9092
    ports:
      - "18086:8085"
    depends_on:
      - kafka
```

Frontend should depend on `subscription-service` when added.

## 12. Testing Strategy

### subscription-service

- Tariff seed/list returns active tariffs.
- Checkout creates `PENDING` payment with fake checkout URL.
- Test-confirm activates subscription, creates period, marks payment confirmed.
- Confirming same payment twice is idempotent.
- Activation publishes Kafka event.

### crm-service

- Kafka consumer handles `SUBSCRIPTION_ACTIVATED` and upserts `crm_access`.

## 13. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Duplicate webhook/test-confirm | Confirmation is idempotent by payment status and existing period lookup. |
| Kafka unavailable | Log publishing error; payment/subscription state remains persisted. |
| Subscription status stale | Active access computed by `activeUntil > now`; no scheduled job required for MVP. |

## 14. Open Questions

None. Design approved.
