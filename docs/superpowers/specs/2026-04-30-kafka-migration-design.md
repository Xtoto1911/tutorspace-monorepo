# Iteration D: Kafka Migration — Design Spec

**Date:** 2026-04-30  
**Status:** Approved  
**Scope:** Replace sync REST notification transport with async Kafka event-driven integration.

## 1. Context

Iteration C created `notification-service` as a notification log with sync REST ingest (`POST /internal/notifications`). Producers (`marketplace-service`, `crm-service`) call this endpoint directly after completing their business operations. CRM sync between `marketplace-service` and `crm-service` remains REST.

This iteration migrates notification publishing to Kafka while keeping REST as fallback.

## 2. Goals

1. Add Kafka to docker-compose.
2. Introduce a standard event envelope for all Kafka messages.
3. Migrate `marketplace-service` notification publishing from REST to Kafka.
4. Migrate `crm-service` notification publishing from REST to Kafka.
5. Add Kafka consumer to `notification-service` alongside existing REST ingest.
6. Keep CRM sync REST unchanged (hybrid approach).

## 3. Non-Goals

- Replace CRM sync REST with Kafka (stays REST).
- Remove REST ingest endpoint from `notification-service`.
- Add DLT/RetryTopic in this iteration (deferred).
- Use Schema Registry or Avro.

## 4. Architecture

```
┌──────────────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
│ marketplace-service  │────►│ Kafka:               │────►│ notification-service│
│ - publish RequestCreated   │   tutor-notifications│     │ - Kafka consumer    │
│   (after CRM REST sync)    │                      │     │ - REST ingest (kept)│
└──────────────────────┘     └──────────────────────┘     └─────────────────────┘
┌──────────────────────┐     ┌──────────────────────┐
│ crm-service          │────►│ Kafka:               │
│ - publish ApplicationAccepted    client-notifications│
│ - publish ApplicationRejected  │
│ - publish LessonCreated        │
└──────────────────────┘     └──────────────────────┘
```

## 5. Event Envelope

All Kafka messages use a single envelope:

```java
public record NotificationEventEnvelope<T>(
    UUID eventId,         // Unique event identifier
    String eventType,     // REQUEST_CREATED, APPLICATION_ACCEPTED, etc.
    Instant occurredAt,   // Event timestamp
    String producer,      // Service name (marketplace-service, crm-service)
    T payload             // Event-specific data
) {}
```

## 6. Payload

```java
public record NotificationPayload(
    UUID userId,                  // Recipient user ID
    String title,                 // Notification title
    String message,               // Notification body
    Map<String, String> metadata  // Additional context (tutorName, subjectName, etc.)
) {}
```

## 7. Topics

| Topic               | Events Produced                                      | Producer(s)         | Consumer(s)          |
|---------------------|------------------------------------------------------|---------------------|----------------------|
| `tutor-notifications`   | `REQUEST_CREATED`                                    | marketplace-service | notification-service |
| `client-notifications`  | `APPLICATION_ACCEPTED`, `APPLICATION_REJECTED`, `LESSON_CREATED` | crm-service         | notification-service |

## 8. Producer Strategy

### 8.1 marketplace-service
- After successful `crmSyncClient.syncRequest(...)` and DB save.
- Build `NotificationEventEnvelope<NotificationPayload>`.
- `kafkaTemplate.send("tutor-notifications", tutorUserId.toString(), envelope)`.
- Key = `tutorUserId` (for partition affinity).
- Exception handling: log.error, do not fail the request.

### 8.2 crm-service
- After successful DB save in `ApplicationLifecycleService` / `LessonService`.
- Build `NotificationEventEnvelope<NotificationPayload>`.
- `kafkaTemplate.send("client-notifications", clientUserId.toString(), envelope)`.
- Key = `clientUserId` (for partition affinity).
- Exception handling: log.error, do not fail the request.

## 9. Consumer Strategy (notification-service)

```java
@KafkaListener(topics = {"tutor-notifications", "client-notifications"})
public void onNotificationEvent(ConsumerRecord<String, NotificationEventEnvelope<NotificationPayload>> record) {
    // Deserialize envelope
    // Map payload to NotificationIngestDto
    // Call notificationService.ingest(dto)
}
```

- Use `StringDeserializer` for keys.
- Use `JsonDeserializer` for values (with `NotificationEventEnvelope.class` and type info).
- Manual ACK not required in MVP (auto-commit).
- Keep existing `POST /internal/notifications` for fallback/direct calls.

## 10. Dependencies

Add to each service `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Test dependency:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 11. Configuration

### application.yaml (all services)
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.tutor.*"
```

### docker-compose.yml
Add:
- `kafka` (Bitnami Kafka image, port 9092)
- `kafka-ui` (optional, port 18084)

Environment variables:
- `KAFKA_BOOTSTRAP: kafka:9092` for all backend services.

## 12. Testing Strategy

### 12.1 Unit Tests
- Mock `KafkaTemplate`, verify `send()` called with correct envelope.
- Services: `marketplace-service`, `crm-service`.

### 12.2 Integration Tests
- Use `EmbeddedKafka` or `Testcontainers`.
- Test: producer sends → consumer receives → notification persisted.
- Service: `notification-service`.

## 13. Migration Plan

1. Add Kafka to docker-compose.
2. Add `spring-kafka` dependency to `marketplace-service`, `crm-service`, `notification-service`.
3. Create shared event DTOs (`NotificationEventEnvelope`, `NotificationPayload`) in each service (no shared lib yet).
4. Configure Kafka producers in `marketplace-service` and `crm-service`.
5. Replace REST notification calls with Kafka publish.
6. Add Kafka listener to `notification-service`.
7. Run tests.
8. Verify with `docker compose up`.

## 14. Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Kafka unavailable | Producer logs error, does not fail business operation. REST fallback kept. |
| Message loss | Auto-commit consumer. In MVP acceptable. DLT added later. |
| Serialization mismatch | Use same package/class name across services. Trusted packages configured. |

## 15. Open Questions

None. Design approved.
