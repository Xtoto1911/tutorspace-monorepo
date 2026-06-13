# Kafka Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace sync REST notification transport with async Kafka event-driven integration while keeping REST fallback.

**Architecture:** Spring Kafka with JSON serialization. Producers in marketplace-service and crm-service publish to Kafka topics (tutor-notifications, client-notifications). Notification-service consumes from both topics and persists notifications. REST ingest endpoint remains as fallback.

**Tech Stack:** Spring Boot 3.5.10, Spring Kafka, JSON, Maven, Docker Compose, EmbeddedKafka for tests.

---

## File Structure

**New Files:**
- `backend/notification-service/src/main/java/com/tutor/notificationservice/kafka/NotificationEventEnvelope.java` — Event envelope DTO
- `backend/notification-service/src/main/java/com/tutor/notificationservice/kafka/NotificationPayload.java` — Payload DTO
- `backend/notification-service/src/main/java/com/tutor/notificationservice/kafka/NotificationKafkaListener.java` — Kafka consumer
- `backend/notification-service/src/test/java/com/tutor/notificationservice/kafka/NotificationKafkaListenerTest.java` — Consumer integration test
- `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/kafka/NotificationEventEnvelope.java` — Event envelope DTO
- `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/kafka/NotificationPayload.java` — Payload DTO
- `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/kafka/NotificationEventPublisher.java` — Kafka producer
- `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/kafka/NotificationEventPublisherTest.java` — Publisher unit test
- `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/NotificationEventEnvelope.java` — Event envelope DTO
- `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/NotificationPayload.java` — Payload DTO
- `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/NotificationEventPublisher.java` — Kafka producer
- `backend/crm-service/src/test/java/com/tutor/crmservice/kafka/NotificationEventPublisherTest.java` — Publisher unit test

**Modified Files:**
- `docker-compose.yml` — Add kafka and kafka-ui services
- `backend/notification-service/pom.xml` — Add spring-kafka dependency
- `backend/notification-service/src/main/resources/application.yaml` — Add kafka config
- `backend/marketplace-service/pom.xml` — Add spring-kafka dependency
- `backend/marketplace-service/src/main/resources/application.yaml` — Add kafka config
- `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/application/TutorApplicationService.java` — Replace REST notification with Kafka publish
- `backend/crm-service/pom.xml` — Add spring-kafka dependency
- `backend/crm-service/src/main/resources/application.yaml` — Add kafka config
- `backend/crm-service/src/main/java/com/tutor/crmservice/application/ApplicationLifecycleService.java` — Replace REST notification with Kafka publish
- `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java` — Replace REST notification with Kafka publish

---

## Task 1: Add Kafka to docker-compose

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add kafka and kafka-ui services to docker-compose**

```yaml
  kafka:
    image: bitnami/kafka:3.7.0
    container_name: tutorspace-kafka
    environment:
      KAFKA_CFG_NODE_ID: 0
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 0@kafka:9093
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CFG_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      ALLOW_PLAINTEXT_LISTENER: yes
      KAFKA_KRAFT_CLUSTER_ID: tutorspace-kraft-cluster
    ports:
      - "9092:9092"

  kafka-ui:
    image: provectuslabs/kafka-ui:v0.7.2
    container_name: tutorspace-kafka-ui
    environment:
      KAFKA_CLUSTERS_0_NAME: tutorspace
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    ports:
      - "18084:8080"
    depends_on:
      - kafka
```

- [ ] **Step 2: Add KAFKA_BOOTSTRAP to all backend services**

Add environment variable to `auth-service`, `marketplace-service`, `crm-service`, `notification-service`:
```yaml
      KAFKA_BOOTSTRAP: kafka:9092
```

Also add `depends_on: kafka` to `marketplace-service`, `crm-service`, `notification-service`.

- [ ] **Step 3: Verify docker-compose syntax**

Run: `docker compose config`
Expected: No errors, services listed.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml
git commit -m "infra: add kafka and kafka-ui to docker-compose"
```

---

## Task 2: Add Kafka dependencies to notification-service

**Files:**
- Modify: `backend/notification-service/pom.xml`

- [ ] **Step 1: Add spring-kafka dependencies**

Add to `<dependencies>` section:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Add to `<dependencies>` section (test scope):
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Verify Maven can resolve dependencies**

Run: `cd backend/notification-service && mvn dependency:resolve`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/notification-service/pom.xml
git commit -m "deps(notification-service): add spring-kafka"
```

---

## Task 3: Configure Kafka in notification-service

**Files:**
- Modify: `backend/notification-service/src/main/resources/application.yaml`

- [ ] **Step 1: Add kafka configuration**

Add to `application.yaml`:
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.tutor.*"
```

- [ ] **Step 2: Commit**

```bash
git add backend/notification-service/src/main/resources/application.yaml
git commit -m "config(notification-service): add kafka consumer config"
```

---

## Task 4: Create Kafka DTOs in notification-service

**Files:**
- Create: `backend/notification-service/src/main/java/com/tutor/notificationservice/kafka/NotificationEventEnvelope.java`
- Create: `backend/notification-service/src/main/java/com/tutor/notificationservice/kafka/NotificationPayload.java`

- [ ] **Step 1: Create NotificationEventEnvelope**

```java
package com.tutor.notificationservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record NotificationEventEnvelope<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String producer,
    T payload
) {
    public NotificationEventEnvelope(String eventType, String producer, T payload) {
        this(UUID.randomUUID(), eventType, Instant.now(), producer, payload);
    }
}
```

- [ ] **Step 2: Create NotificationPayload**

```java
package com.tutor.notificationservice.kafka;

import java.util.Map;
import java.util.UUID;

public record NotificationPayload(
    UUID userId,
    String title,
    String message,
    Map<String, String> metadata
) {}
```

- [ ] **Step 3: Commit**

```bash
git add backend/notification-service/src/main/java/com/tutor/notificationservice/kafka/
git commit -m "feat(notification-service): add kafka event DTOs"
```

---

## Task 5: Create Kafka consumer in notification-service

**Files:**
- Create: `backend/notification-service/src/main/java/com/tutor/notificationservice/kafka/NotificationKafkaListener.java`

- [ ] **Step 1: Create listener**

```java
package com.tutor.notificationservice.kafka;

import com.tutor.notificationservice.notification.NotificationIngestDto;
import com.tutor.notificationservice.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = {"tutor-notifications", "client-notifications"})
    public void onNotificationEvent(NotificationEventEnvelope<NotificationPayload> envelope) {
        log.info("Received notification event: {} from producer: {}", envelope.eventType(), envelope.producer());

        var payload = envelope.payload();
        var dto = new NotificationIngestDto(
                payload.userId().toString(),
                envelope.eventType(),
                payload.title(),
                payload.message(),
                payload.metadata()
        );

        notificationService.ingest(dto);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/notification-service/src/main/java/com/tutor/notificationservice/kafka/NotificationKafkaListener.java
git commit -m "feat(notification-service): add kafka listener for notifications"
```

---

## Task 6: Add consumer integration test in notification-service

**Files:**
- Create: `backend/notification-service/src/test/java/com/tutor/notificationservice/kafka/NotificationKafkaListenerTest.java`

- [ ] **Step 1: Write integration test with EmbeddedKafka**

```java
package com.tutor.notificationservice.kafka;

import com.tutor.notificationservice.notification.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"tutor-notifications", "client-notifications"})
@DirtiesContext
class NotificationKafkaListenerTest {

    @Autowired
    private KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldConsumeTutorNotificationEvent() {
        var userId = UUID.randomUUID();
        var payload = new NotificationPayload(
                userId,
                "Новая заявка",
                "Клиент отправил заявку",
                Map.of("tutorName", "Анна", "subjectName", "Математика")
        );
        var envelope = new NotificationEventEnvelope<>("REQUEST_CREATED", "marketplace-service", payload);

        kafkaTemplate.send("tutor-notifications", userId.toString(), envelope);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId.toString());
            assertEquals(1, notifications.size());
            assertEquals("Новая заявка", notifications.get(0).getTitle());
            assertEquals("REQUEST_CREATED", notifications.get(0).getEventType());
        });
    }

    @Test
    void shouldConsumeClientNotificationEvent() {
        var userId = UUID.randomUUID();
        var payload = new NotificationPayload(
                userId,
                "Заявка принята",
                "Репетитор принял заявку",
                Map.of("tutorName", "Анна", "subjectName", "Математика")
        );
        var envelope = new NotificationEventEnvelope<>("APPLICATION_ACCEPTED", "crm-service", payload);

        kafkaTemplate.send("client-notifications", userId.toString(), envelope);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId.toString());
            assertEquals(1, notifications.size());
            assertEquals("Заявка принята", notifications.get(0).getTitle());
            assertEquals("APPLICATION_ACCEPTED", notifications.get(0).getEventType());
        });
    }
}
```

- [ ] **Step 2: Add awaitility test dependency**

Add to `backend/notification-service/pom.xml`:
```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Run the test**

Run: `cd backend/notification-service && mvn test -Dtest=NotificationKafkaListenerTest`
Expected: Both tests PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/notification-service/src/test/java/com/tutor/notificationservice/kafka/NotificationKafkaListenerTest.java backend/notification-service/pom.xml
git commit -m "test(notification-service): add kafka listener integration tests"
```

---

## Task 7: Add Kafka dependencies to marketplace-service

**Files:**
- Modify: `backend/marketplace-service/pom.xml`

- [ ] **Step 1: Add spring-kafka dependencies**

Add to `<dependencies>` section:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Add test dependency:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Verify Maven can resolve**

Run: `cd backend/marketplace-service && mvn dependency:resolve`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/marketplace-service/pom.xml
git commit -m "deps(marketplace-service): add spring-kafka"
```

---

## Task 8: Configure Kafka in marketplace-service

**Files:**
- Modify: `backend/marketplace-service/src/main/resources/application.yaml`

- [ ] **Step 1: Add kafka producer configuration**

Add to `application.yaml`:
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

- [ ] **Step 2: Commit**

```bash
git add backend/marketplace-service/src/main/resources/application.yaml
git commit -m "config(marketplace-service): add kafka producer config"
```

---

## Task 9: Create Kafka DTOs and publisher in marketplace-service

**Files:**
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/kafka/NotificationEventEnvelope.java`
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/kafka/NotificationPayload.java`
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/kafka/NotificationEventPublisher.java`

- [ ] **Step 1: Create NotificationEventEnvelope**

```java
package com.tutor.marketplaceservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record NotificationEventEnvelope<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String producer,
    T payload
) {
    public NotificationEventEnvelope(String eventType, String producer, T payload) {
        this(UUID.randomUUID(), eventType, Instant.now(), producer, payload);
    }
}
```

- [ ] **Step 2: Create NotificationPayload**

```java
package com.tutor.marketplaceservice.kafka;

import java.util.Map;
import java.util.UUID;

public record NotificationPayload(
    UUID userId,
    String title,
    String message,
    Map<String, String> metadata
) {}
```

- [ ] **Step 3: Create NotificationEventPublisher**

```java
package com.tutor.marketplaceservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    public void publishRequestCreated(UUID tutorUserId, String tutorName, String subjectName) {
        var payload = new NotificationPayload(
                tutorUserId,
                "Новая заявка",
                "Клиент отправил заявку на занятие по предмету \"" + subjectName + "\".",
                Map.of("tutorName", tutorName, "subjectName", subjectName)
        );
        var envelope = new NotificationEventEnvelope<>("REQUEST_CREATED", "marketplace-service", payload);

        try {
            kafkaTemplate.send("tutor-notifications", tutorUserId.toString(), envelope)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish RequestCreated event for tutor {}: {}", tutorUserId, ex.getMessage());
                        } else {
                            log.info("Published RequestCreated event for tutor {}", tutorUserId);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing RequestCreated event for tutor {}: {}", tutorUserId, e.getMessage());
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/kafka/
git commit -m "feat(marketplace-service): add kafka event DTOs and publisher"
```

---

## Task 10: Wire Kafka publisher in marketplace-service

**Files:**
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/application/TutorApplicationService.java`

- [ ] **Step 1: Replace NotificationClient with NotificationEventPublisher**

Current file uses `NotificationClient`. Replace the injection and usage:

```java
// Remove:
// private final NotificationClient notificationClient;

// Add:
private final com.tutor.marketplaceservice.kafka.NotificationEventPublisher notificationEventPublisher;
```

In `submitApplication` method (after successful CRM sync):
```java
// Replace:
// notificationClient.notifyRequestCreated(tutor.getUserId(), tutor.getFirstName(), subject.getName());

// With:
notificationEventPublisher.publishRequestCreated(tutor.getUserId(), tutor.getFirstName(), subject.getName());
```

- [ ] **Step 2: Add publisher unit test**

Create: `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/kafka/NotificationEventPublisherTest.java`

```java
package com.tutor.marketplaceservice.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    @InjectMocks
    private NotificationEventPublisher publisher;

    @Test
    void shouldPublishRequestCreatedEvent() {
        var tutorUserId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishRequestCreated(tutorUserId, "Анна", "Математика");

        verify(kafkaTemplate).send(eq("tutor-notifications"), eq(tutorUserId.toString()), any(NotificationEventEnvelope.class));
    }
}
```

- [ ] **Step 3: Run marketplace-service tests**

Run: `cd backend/marketplace-service && mvn test`
Expected: All tests PASS (existing + new).

- [ ] **Step 4: Commit**

```bash
git add backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/application/TutorApplicationService.java backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/kafka/
git commit -m "feat(marketplace-service): wire kafka publisher, replace REST notification client"
```

---

## Task 11: Add Kafka dependencies to crm-service

**Files:**
- Modify: `backend/crm-service/pom.xml`

- [ ] **Step 1: Add spring-kafka dependencies**

Add to `<dependencies>` section:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Add test dependency:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Verify Maven can resolve**

Run: `cd backend/crm-service && mvn dependency:resolve`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/crm-service/pom.xml
git commit -m "deps(crm-service): add spring-kafka"
```

---

## Task 12: Configure Kafka in crm-service

**Files:**
- Modify: `backend/crm-service/src/main/resources/application.yaml`

- [ ] **Step 1: Add kafka producer configuration**

Add to `application.yaml`:
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

- [ ] **Step 2: Commit**

```bash
git add backend/crm-service/src/main/resources/application.yaml
git commit -m "config(crm-service): add kafka producer config"
```

---

## Task 13: Create Kafka DTOs and publisher in crm-service

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/NotificationEventEnvelope.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/NotificationPayload.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/kafka/NotificationEventPublisher.java`

- [ ] **Step 1: Create NotificationEventEnvelope**

```java
package com.tutor.crmservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record NotificationEventEnvelope<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String producer,
    T payload
) {
    public NotificationEventEnvelope(String eventType, String producer, T payload) {
        this(UUID.randomUUID(), eventType, Instant.now(), producer, payload);
    }
}
```

- [ ] **Step 2: Create NotificationPayload**

```java
package com.tutor.crmservice.kafka;

import java.util.Map;
import java.util.UUID;

public record NotificationPayload(
    UUID userId,
    String title,
    String message,
    Map<String, String> metadata
) {}
```

- [ ] **Step 3: Create NotificationEventPublisher**

```java
package com.tutor.crmservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    public void publishApplicationAccepted(UUID clientUserId, String tutorName, String subjectName) {
        send(clientUserId, "APPLICATION_ACCEPTED", "Заявка принята",
                "Репетитор " + tutorName + " принял вашу заявку на занятие по предмету \"" + subjectName + "\".",
                Map.of("tutorName", tutorName, "subjectName", subjectName));
    }

    public void publishApplicationRejected(UUID clientUserId, String tutorName, String subjectName, String reason) {
        send(clientUserId, "APPLICATION_REJECTED", "Заявка отклонена",
                "Репетитор " + tutorName + " отклонил вашу заявку. Причина: " + reason,
                Map.of("tutorName", tutorName, "subjectName", subjectName, "reason", reason));
    }

    public void publishLessonCreated(UUID clientUserId, String tutorName, String subjectName, String startTime) {
        send(clientUserId, "LESSON_CREATED", "Занятие запланировано",
                "Занятие по предмету \"" + subjectName + "\" с репетитором " + tutorName + " запланировано на " + startTime + ".",
                Map.of("tutorName", tutorName, "subjectName", subjectName, "startTime", startTime));
    }

    private void send(UUID userId, String eventType, String title, String message, Map<String, String> metadata) {
        var payload = new NotificationPayload(userId, title, message, metadata);
        var envelope = new NotificationEventEnvelope<>(eventType, "crm-service", payload);

        try {
            kafkaTemplate.send("client-notifications", userId.toString(), envelope)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event for user {}: {}", eventType, userId, ex.getMessage());
                        } else {
                            log.info("Published {} event for user {}", eventType, userId);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing {} event for user {}: {}", eventType, userId, e.getMessage());
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/kafka/
git commit -m "feat(crm-service): add kafka event DTOs and publisher"
```

---

## Task 14: Wire Kafka publisher in crm-service

**Files:**
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/application/ApplicationLifecycleService.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java`

- [ ] **Step 1: Update ApplicationLifecycleService**

Replace `NotificationClient` injection with `NotificationEventPublisher`:

```java
// Remove:
// private final NotificationClient notificationClient;

// Add:
private final com.tutor.crmservice.kafka.NotificationEventPublisher notificationEventPublisher;
```

In `accept` method:
```java
// Replace:
// notificationClient.notifyApplicationAccepted(application.getClientUserId(), ...);

// With:
notificationEventPublisher.publishApplicationAccepted(
        application.getClientUserId(),
        tutor.getFirstName(),
        subject.getName());
```

In `reject` method:
```java
// Replace:
// notificationClient.notifyApplicationRejected(application.getClientUserId(), ...);

// With:
notificationEventPublisher.publishApplicationRejected(
        application.getClientUserId(),
        tutor.getFirstName(),
        subject.getName(),
        dto.reason());
```

- [ ] **Step 2: Update LessonService**

Replace `NotificationClient` injection with `NotificationEventPublisher`:

```java
// Remove:
// private final NotificationClient notificationClient;

// Add:
private final com.tutor.crmservice.kafka.NotificationEventPublisher notificationEventPublisher;
```

In `createTutorLesson` method (after saving lesson):
```java
// Replace:
// notificationClient.notifyLessonCreated(request.getClientUserId(), ...);

// With:
notificationEventPublisher.publishLessonCreated(
        request.getClientUserId(),
        tutor.getFirstName(),
        subject.getName(),
        lesson.getStartTime().format(formatter));
```

- [ ] **Step 3: Add publisher unit test**

Create: `backend/crm-service/src/test/java/com/tutor/crmservice/kafka/NotificationEventPublisherTest.java`

```java
package com.tutor.crmservice.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    @InjectMocks
    private NotificationEventPublisher publisher;

    @Test
    void shouldPublishApplicationAcceptedEvent() {
        var clientUserId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishApplicationAccepted(clientUserId, "Анна", "Математика");

        verify(kafkaTemplate).send(eq("client-notifications"), eq(clientUserId.toString()), any(NotificationEventEnvelope.class));
    }

    @Test
    void shouldPublishApplicationRejectedEvent() {
        var clientUserId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishApplicationRejected(clientUserId, "Анна", "Математика", "Нет времени");

        verify(kafkaTemplate).send(eq("client-notifications"), eq(clientUserId.toString()), any(NotificationEventEnvelope.class));
    }

    @Test
    void shouldPublishLessonCreatedEvent() {
        var clientUserId = UUID.randomUUID();
        when(kafkaTemplate.send(any(), any(), any(NotificationEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishLessonCreated(clientUserId, "Анна", "Математика", "2026-05-01 10:00");

        verify(kafkaTemplate).send(eq("client-notifications"), eq(clientUserId.toString()), any(NotificationEventEnvelope.class));
    }
}
```

- [ ] **Step 4: Run crm-service tests**

Run: `cd backend/crm-service && mvn test`
Expected: All tests PASS (existing + new).

- [ ] **Step 5: Commit**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/application/ApplicationLifecycleService.java backend/crm-service/src/main/java/com/tutor/crmservice/lesson/LessonService.java backend/crm-service/src/test/java/com/tutor/crmservice/kafka/
git commit -m "feat(crm-service): wire kafka publisher, replace REST notification client"
```

---

## Task 15: Final verification

- [ ] **Step 1: Run all service tests**

Run each:
```bash
cd backend/notification-service && mvn test
cd backend/marketplace-service && mvn test
cd backend/crm-service && mvn test
```
Expected: All BUILD SUCCESS.

- [ ] **Step 2: Verify docker-compose builds**

Run: `docker compose build`
Expected: All services build successfully (or at least no code errors; network issues are environment-specific).

- [ ] **Step 3: Update Iteration D status in doc**

Modify: `doc/TutorSpace_E2E_MVP_Iterations.md`
- Change `Status: [ ]` to `Status: [x]` for Iteration D.
- Mark all plan items as `[x]`.
- Add git commit hashes.

- [ ] **Step 4: Final commit**

```bash
git add doc/TutorSpace_E2E_MVP_Iterations.md
git commit -m "docs: mark Iteration D as complete"
```

---

## Self-Review

### Spec coverage check
- ✅ Add Kafka to docker-compose → Task 1
- ✅ Event envelope (`NotificationEventEnvelope`, `NotificationPayload`) → Tasks 4, 9, 13
- ✅ Migrate marketplace-service publish → Tasks 8-10
- ✅ Migrate crm-service publish → Tasks 11-14
- ✅ notification-service Kafka consumer → Tasks 2-6
- ✅ Keep REST fallback → REST endpoint not removed (Task 6 listener added alongside existing controller)
- ✅ CRM sync REST unchanged → No tasks modify CRM sync

### Placeholder scan
- No TBD/TODO/fill in details found.
- All code snippets are complete and compilable.
- All file paths are exact.
- All commands have expected outputs.

### Type consistency
- `NotificationEventEnvelope<T>` used consistently across all services.
- `NotificationPayload` fields consistent.
- Topic names consistent: `tutor-notifications`, `client-notifications`.
- Event types consistent: `REQUEST_CREATED`, `APPLICATION_ACCEPTED`, `APPLICATION_REJECTED`, `LESSON_CREATED`.

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-04-30-kafka-migration-plan.md`.**

Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
