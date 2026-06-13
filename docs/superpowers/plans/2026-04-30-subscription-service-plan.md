# Subscription Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `subscription-service` with seeded tariffs, mock checkout/payment confirmation, subscription renewal periods, and Kafka activation events consumed by `crm-service`.

**Architecture:** New Spring Boot service owns subscription domain state in H2 and publishes `SUBSCRIPTION_ACTIVATED` events to Kafka topic `crm-events`. `crm-service` consumes the event and maintains a local `crm_access` projection. YooKassa is mocked through a local test-confirm endpoint.

**Tech Stack:** Spring Boot 3.5.10, Java 21, Maven, H2, Spring Data JPA, Spring Kafka, Lombok, JUnit/Mockito, Docker Compose.

---

## File Structure

**New `subscription-service` files:**
- `backend/subscription-service/pom.xml` — Maven config
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/SubscriptionServiceApplication.java` — main class
- `backend/subscription-service/src/main/resources/application.yaml` — service config
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/controller/HealthController.java` — health endpoint
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/Tariff.java` — tariff entity
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/TariffRepository.java` — tariff repository
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/TariffDto.java` — tariff DTO
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/TariffSeedConfig.java` — seed tariffs
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/Payment.java` — payment entity
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/PaymentStatus.java` — payment enum
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/TutorSubscription.java` — subscription entity
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionStatus.java` — subscription enum
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionPeriod.java` — period entity
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/PaymentRepository.java` — payment repository
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/TutorSubscriptionRepository.java` — subscription repository
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionPeriodRepository.java` — period repository
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/CreateCheckoutRequest.java` — checkout request DTO
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/CheckoutResponse.java` — checkout response DTO
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/CurrentSubscriptionDto.java` — current subscription DTO
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionActivationResult.java` — activation result DTO
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionService.java` — domain service
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionController.java` — public API
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/InternalPaymentController.java` — mock confirm API
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/kafka/SubscriptionEventEnvelope.java` — Kafka envelope
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/kafka/SubscriptionActivatedPayload.java` — Kafka payload
- `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/kafka/SubscriptionEventPublisher.java` — Kafka publisher
- `backend/subscription-service/src/test/java/com/tutor/subscriptionservice/subscription/SubscriptionServiceTest.java` — unit tests

**Modified `crm-service` files:**
- `backend/crm-service/pom.xml` — already has Spring Kafka from previous iteration; verify no missing dependency
- `backend/crm-service/src/main/resources/application.yaml` — add Kafka consumer config if absent
- `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/CrmAccess.java` — new entity
- `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/CrmAccessRepository.java` — new repository
- `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/SubscriptionEventEnvelope.java` — new DTO
- `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/SubscriptionActivatedPayload.java` — new DTO
- `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/SubscriptionActivatedListener.java` — new Kafka consumer
- `backend/crm-service/src/test/java/com/tutor/crmservice/subscription/SubscriptionActivatedListenerTest.java` — unit test

**Modified root files:**
- `docker-compose.yml` — add subscription-service
- `doc/TutorSpace_E2E_MVP_Iterations.md` — mark Iteration F complete

---

## Task 1: Scaffold subscription-service

**Files:**
- Create: `backend/subscription-service/pom.xml`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/SubscriptionServiceApplication.java`
- Create: `backend/subscription-service/src/main/resources/application.yaml`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/controller/HealthController.java`

- [ ] **Step 1: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.10</version>
        <relativePath/>
    </parent>

    <groupId>com.tutor</groupId>
    <artifactId>subscription-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>subscription-service</name>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create main class**

```java
package com.tutor.subscriptionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SubscriptionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application config**

```yaml
server:
  port: ${SERVER_PORT:8085}

spring:
  application:
    name: subscription-service
  datasource:
    url: ${SUBSCRIPTION_DB_URL:jdbc:h2:mem:subscriptiondb}
    driver-class-name: ${SUBSCRIPTION_DB_DRIVER:org.h2.Driver}
    username: ${SUBSCRIPTION_DB_USERNAME:sa}
    password: ${SUBSCRIPTION_DB_PASSWORD:}
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

- [ ] **Step 4: Create health controller**

```java
package com.tutor.subscriptionservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "subscription-service");
    }
}
```

- [ ] **Step 5: Verify compile**

Run: `cd backend/subscription-service && mvn clean compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add backend/subscription-service
git commit -m "feat(subscription-service): scaffold project"
```

---

## Task 2: Add tariff domain and seed data

**Files:**
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/Tariff.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/TariffRepository.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/TariffDto.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/TariffSeedConfig.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff/TariffController.java`

- [ ] **Step 1: Create `Tariff.java`**

```java
package com.tutor.subscriptionservice.tariff;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tariffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
```

- [ ] **Step 2: Create repository**

```java
package com.tutor.subscriptionservice.tariff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TariffRepository extends JpaRepository<Tariff, UUID> {
    Optional<Tariff> findByCode(String code);
    boolean existsByCode(String code);
    List<Tariff> findByActiveTrueOrderByPriceRubAsc();
}
```

- [ ] **Step 3: Create DTO**

```java
package com.tutor.subscriptionservice.tariff;

import java.util.UUID;

public record TariffDto(
        UUID id,
        String code,
        String name,
        int priceRub,
        int durationDays,
        boolean active
) {
    public static TariffDto from(Tariff tariff) {
        return new TariffDto(
                tariff.getId(),
                tariff.getCode(),
                tariff.getName(),
                tariff.getPriceRub(),
                tariff.getDurationDays(),
                tariff.isActive()
        );
    }
}
```

- [ ] **Step 4: Create seed config**

```java
package com.tutor.subscriptionservice.tariff;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TariffSeedConfig {

    @Bean
    CommandLineRunner seedTariffs(TariffRepository tariffRepository) {
        return args -> {
            seed(tariffRepository, "CRM_BASIC", "CRM Basic", 990, 30);
            seed(tariffRepository, "CRM_PRO", "CRM Pro", 1990, 30);
            seed(tariffRepository, "CRM_PREMIUM", "CRM Premium", 2990, 30);
        };
    }

    private void seed(TariffRepository repository, String code, String name, int priceRub, int durationDays) {
        if (repository.existsByCode(code)) {
            return;
        }
        repository.save(Tariff.builder()
                .code(code)
                .name(name)
                .priceRub(priceRub)
                .durationDays(durationDays)
                .active(true)
                .build());
    }
}
```

- [ ] **Step 5: Create tariff controller**

```java
package com.tutor.subscriptionservice.tariff;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions/tariffs")
public class TariffController {

    private final TariffRepository tariffRepository;

    @GetMapping
    public List<TariffDto> listActive() {
        return tariffRepository.findByActiveTrueOrderByPriceRubAsc()
                .stream()
                .map(TariffDto::from)
                .toList();
    }
}
```

- [ ] **Step 6: Verify compile**

Run: `cd backend/subscription-service && mvn clean compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add backend/subscription-service/src/main/java/com/tutor/subscriptionservice/tariff
git commit -m "feat(subscription-service): add tariff domain and seed data"
```

---

## Task 3: Add subscription entities and repositories

**Files:**
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/PaymentStatus.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionStatus.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/Payment.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/TutorSubscription.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionPeriod.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/PaymentRepository.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/TutorSubscriptionRepository.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionPeriodRepository.java`

- [ ] **Step 1: Create enums**

```java
package com.tutor.subscriptionservice.subscription;

public enum PaymentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
```

```java
package com.tutor.subscriptionservice.subscription;

public enum SubscriptionStatus {
    ACTIVE,
    EXPIRED
}
```

- [ ] **Step 2: Create `Payment.java`**

```java
package com.tutor.subscriptionservice.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private String checkoutUrl;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant confirmedAt;
}
```

- [ ] **Step 3: Create `TutorSubscription.java`**

```java
package com.tutor.subscriptionservice.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tutor_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tutorUserId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(nullable = false)
    private Instant activeUntil;

    @Column(nullable = false)
    private String currentTariffCode;

    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
```

- [ ] **Step 4: Create `SubscriptionPeriod.java`**

```java
package com.tutor.subscriptionservice.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_periods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false)
    private String tariffCode;

    @Column(nullable = false, unique = true)
    private UUID paymentId;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant activeUntil;
}
```

- [ ] **Step 5: Create repositories**

```java
package com.tutor.subscriptionservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
```

```java
package com.tutor.subscriptionservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TutorSubscriptionRepository extends JpaRepository<TutorSubscription, UUID> {
    Optional<TutorSubscription> findByTutorUserId(UUID tutorUserId);
}
```

```java
package com.tutor.subscriptionservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPeriodRepository extends JpaRepository<SubscriptionPeriod, UUID> {
    Optional<SubscriptionPeriod> findByPaymentId(UUID paymentId);
}
```

- [ ] **Step 6: Verify compile**

Run: `cd backend/subscription-service && mvn clean compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription
git commit -m "feat(subscription-service): add subscription entities and repositories"
```

---

## Task 4: Add Kafka publisher in subscription-service

**Files:**
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/kafka/SubscriptionEventEnvelope.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/kafka/SubscriptionActivatedPayload.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/kafka/SubscriptionEventPublisher.java`

- [ ] **Step 1: Create envelope**

```java
package com.tutor.subscriptionservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionEventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        T payload
) {
    public SubscriptionEventEnvelope(String eventType, String producer, T payload) {
        this(UUID.randomUUID(), eventType, Instant.now(), producer, payload);
    }
}
```

- [ ] **Step 2: Create payload**

```java
package com.tutor.subscriptionservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionActivatedPayload(
        UUID tutorUserId,
        String tariffCode,
        Instant activeUntil
) {}
```

- [ ] **Step 3: Create publisher**

```java
package com.tutor.subscriptionservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventPublisher {

    private static final String TOPIC = "crm-events";

    private final KafkaTemplate<String, SubscriptionEventEnvelope<SubscriptionActivatedPayload>> kafkaTemplate;

    public void publishSubscriptionActivated(UUID tutorUserId, String tariffCode, Instant activeUntil) {
        var payload = new SubscriptionActivatedPayload(tutorUserId, tariffCode, activeUntil);
        var envelope = new SubscriptionEventEnvelope<>("SUBSCRIPTION_ACTIVATED", "subscription-service", payload);

        try {
            kafkaTemplate.send(TOPIC, tutorUserId.toString(), envelope)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish SUBSCRIPTION_ACTIVATED for tutor {}: {}", tutorUserId, exception.getMessage());
                        } else {
                            log.info("Published SUBSCRIPTION_ACTIVATED for tutor {}", tutorUserId);
                        }
                    });
        } catch (Exception exception) {
            log.error("Exception publishing SUBSCRIPTION_ACTIVATED for tutor {}: {}", tutorUserId, exception.getMessage());
        }
    }
}
```

- [ ] **Step 4: Verify compile**

Run: `cd backend/subscription-service && mvn clean compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add backend/subscription-service/src/main/java/com/tutor/subscriptionservice/kafka
git commit -m "feat(subscription-service): add subscription activation Kafka publisher"
```

---

## Task 5: Implement checkout and activation service

**Files:**
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/CreateCheckoutRequest.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/CheckoutResponse.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/CurrentSubscriptionDto.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionActivationResult.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionService.java`

- [ ] **Step 1: Create request/response DTOs**

```java
package com.tutor.subscriptionservice.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCheckoutRequest(
        @NotNull UUID tutorUserId,
        @NotBlank String tariffCode
) {}
```

```java
package com.tutor.subscriptionservice.subscription;

import java.util.UUID;

public record CheckoutResponse(
        UUID paymentId,
        String checkoutUrl,
        PaymentStatus status
) {}
```

```java
package com.tutor.subscriptionservice.subscription;

import java.time.Instant;
import java.util.UUID;

public record CurrentSubscriptionDto(
        UUID tutorUserId,
        boolean active,
        String tariffCode,
        Instant activeUntil
) {
    public static CurrentSubscriptionDto inactive(UUID tutorUserId) {
        return new CurrentSubscriptionDto(tutorUserId, false, null, null);
    }
}
```

```java
package com.tutor.subscriptionservice.subscription;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionActivationResult(
        UUID subscriptionId,
        UUID paymentId,
        UUID periodId,
        UUID tutorUserId,
        String tariffCode,
        Instant activeUntil
) {}
```

- [ ] **Step 2: Create `SubscriptionService.java`**

```java
package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.kafka.SubscriptionEventPublisher;
import com.tutor.subscriptionservice.tariff.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final TariffRepository tariffRepository;
    private final PaymentRepository paymentRepository;
    private final TutorSubscriptionRepository subscriptionRepository;
    private final SubscriptionPeriodRepository periodRepository;
    private final SubscriptionEventPublisher eventPublisher;

    @Transactional
    public CheckoutResponse createCheckout(CreateCheckoutRequest request) {
        var tariff = tariffRepository.findByCode(request.tariffCode())
                .filter(t -> t.isActive())
                .orElseThrow(() -> new RuntimeException("Active tariff not found: " + request.tariffCode()));

        var payment = Payment.builder()
                .tutorUserId(request.tutorUserId())
                .tariffCode(tariff.getCode())
                .amountRub(tariff.getPriceRub())
                .status(PaymentStatus.PENDING)
                .checkoutUrl("/mock-yookassa/payments/pending")
                .build();

        var saved = paymentRepository.save(payment);
        saved.setCheckoutUrl("/mock-yookassa/payments/" + saved.getId());
        return toCheckoutResponse(paymentRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public CurrentSubscriptionDto getCurrent(UUID tutorUserId) {
        var now = Instant.now();
        return subscriptionRepository.findByTutorUserId(tutorUserId)
                .map(subscription -> new CurrentSubscriptionDto(
                        tutorUserId,
                        subscription.getActiveUntil().isAfter(now),
                        subscription.getCurrentTariffCode(),
                        subscription.getActiveUntil()))
                .orElseGet(() -> CurrentSubscriptionDto.inactive(tutorUserId));
    }

    @Transactional
    public SubscriptionActivationResult confirmPayment(UUID paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            var subscription = subscriptionRepository.findByTutorUserId(payment.getTutorUserId())
                    .orElseThrow(() -> new RuntimeException("Subscription not found for confirmed payment: " + paymentId));
            var period = periodRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new RuntimeException("Subscription period not found for confirmed payment: " + paymentId));
            return toActivationResult(subscription, payment, period);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not pending: " + paymentId);
        }

        var tariff = tariffRepository.findByCode(payment.getTariffCode())
                .orElseThrow(() -> new RuntimeException("Tariff not found: " + payment.getTariffCode()));

        var now = Instant.now();
        var subscription = subscriptionRepository.findByTutorUserId(payment.getTutorUserId())
                .orElseGet(() -> TutorSubscription.builder()
                        .tutorUserId(payment.getTutorUserId())
                        .status(SubscriptionStatus.ACTIVE)
                        .activeUntil(now)
                        .currentTariffCode(payment.getTariffCode())
                        .build());

        var start = subscription.getActiveUntil() != null && subscription.getActiveUntil().isAfter(now)
                ? subscription.getActiveUntil()
                : now;
        var activeUntil = start.plus(tariff.getDurationDays(), ChronoUnit.DAYS);

        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setConfirmedAt(now);
        paymentRepository.save(payment);

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentTariffCode(tariff.getCode());
        subscription.setActiveUntil(activeUntil);
        subscription.setUpdatedAt(now);
        var savedSubscription = subscriptionRepository.save(subscription);

        var period = periodRepository.save(SubscriptionPeriod.builder()
                .subscriptionId(savedSubscription.getId())
                .paymentId(payment.getId())
                .tariffCode(tariff.getCode())
                .startedAt(start)
                .activeUntil(activeUntil)
                .build());

        eventPublisher.publishSubscriptionActivated(payment.getTutorUserId(), tariff.getCode(), activeUntil);
        return toActivationResult(savedSubscription, payment, period);
    }

    private CheckoutResponse toCheckoutResponse(Payment payment) {
        return new CheckoutResponse(payment.getId(), payment.getCheckoutUrl(), payment.getStatus());
    }

    private SubscriptionActivationResult toActivationResult(TutorSubscription subscription, Payment payment, SubscriptionPeriod period) {
        return new SubscriptionActivationResult(
                subscription.getId(),
                payment.getId(),
                period.getId(),
                payment.getTutorUserId(),
                subscription.getCurrentTariffCode(),
                subscription.getActiveUntil()
        );
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `cd backend/subscription-service && mvn clean compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription
git commit -m "feat(subscription-service): implement checkout and activation service"
```

---

## Task 6: Add subscription API controllers

**Files:**
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionController.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/InternalPaymentController.java`

- [ ] **Step 1: Create public controller**

```java
package com.tutor.subscriptionservice.subscription;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    public CurrentSubscriptionDto current(@RequestParam UUID tutorUserId) {
        return subscriptionService.getCurrent(tutorUserId);
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CreateCheckoutRequest request) {
        return subscriptionService.createCheckout(request);
    }
}
```

- [ ] **Step 2: Create internal payment controller**

```java
package com.tutor.subscriptionservice.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payments/yookassa")
public class InternalPaymentController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/test-confirm/{paymentId}")
    public SubscriptionActivationResult testConfirm(@PathVariable UUID paymentId) {
        return subscriptionService.confirmPayment(paymentId);
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `cd backend/subscription-service && mvn clean compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionController.java backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/InternalPaymentController.java
git commit -m "feat(subscription-service): add subscription checkout and confirm APIs"
```

---

## Task 7: Add subscription-service unit tests

**Files:**
- Create: `backend/subscription-service/src/test/java/com/tutor/subscriptionservice/subscription/SubscriptionServiceTest.java`

- [ ] **Step 1: Create test class**

```java
package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.kafka.SubscriptionEventPublisher;
import com.tutor.subscriptionservice.tariff.Tariff;
import com.tutor.subscriptionservice.tariff.TariffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private TariffRepository tariffRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TutorSubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPeriodRepository periodRepository;
    @Mock
    private SubscriptionEventPublisher eventPublisher;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void shouldCreatePendingCheckoutPayment() {
        var tutorUserId = UUID.randomUUID();
        var tariff = Tariff.builder().code("CRM_BASIC").name("CRM Basic").priceRub(990).durationDays(30).active(true).build();
        when(tariffRepository.findByCode("CRM_BASIC")).thenReturn(Optional.of(tariff));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(UUID.randomUUID());
            }
            return payment;
        });

        var response = subscriptionService.createCheckout(new CreateCheckoutRequest(tutorUserId, "CRM_BASIC"));

        assertNotNull(response.paymentId());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertTrue(response.checkoutUrl().contains(response.paymentId().toString()));
    }

    @Test
    void shouldConfirmPaymentAndActivateSubscription() {
        var tutorUserId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var tariff = Tariff.builder().code("CRM_BASIC").name("CRM Basic").priceRub(990).durationDays(30).active(true).build();
        var payment = Payment.builder()
                .id(paymentId)
                .tutorUserId(tutorUserId)
                .tariffCode("CRM_BASIC")
                .amountRub(990)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("/mock-yookassa/payments/" + paymentId)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(tariffRepository.findByCode("CRM_BASIC")).thenReturn(Optional.of(tariff));
        when(subscriptionRepository.findByTutorUserId(tutorUserId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.save(any(TutorSubscription.class))).thenAnswer(invocation -> {
            TutorSubscription subscription = invocation.getArgument(0);
            subscription.setId(UUID.randomUUID());
            return subscription;
        });
        when(periodRepository.save(any(SubscriptionPeriod.class))).thenAnswer(invocation -> {
            SubscriptionPeriod period = invocation.getArgument(0);
            period.setId(UUID.randomUUID());
            return period;
        });

        var result = subscriptionService.confirmPayment(paymentId);

        assertEquals(paymentId, result.paymentId());
        assertEquals(tutorUserId, result.tutorUserId());
        assertEquals("CRM_BASIC", result.tariffCode());
        assertNotNull(result.activeUntil());
        verify(eventPublisher).publishSubscriptionActivated(eq(tutorUserId), eq("CRM_BASIC"), any());
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd backend/subscription-service && mvn test`

Expected: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add backend/subscription-service/src/test/java/com/tutor/subscriptionservice/subscription/SubscriptionServiceTest.java
git commit -m "test(subscription-service): add checkout and activation tests"
```

---

## Task 8: Add CRM access projection in crm-service

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/CrmAccess.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/CrmAccessRepository.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/SubscriptionEventEnvelope.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/SubscriptionActivatedPayload.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/subscription/SubscriptionActivatedListener.java`
- Modify: `backend/crm-service/src/main/resources/application.yaml`

- [ ] **Step 1: Create CRM access entity**

```java
package com.tutor.crmservice.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
```

- [ ] **Step 2: Create repository**

```java
package com.tutor.crmservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CrmAccessRepository extends JpaRepository<CrmAccess, UUID> {
    Optional<CrmAccess> findByTutorUserId(UUID tutorUserId);
}
```

- [ ] **Step 3: Create event DTOs**

```java
package com.tutor.crmservice.subscription;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionEventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        T payload
) {}
```

```java
package com.tutor.crmservice.subscription;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionActivatedPayload(
        UUID tutorUserId,
        String tariffCode,
        Instant activeUntil
) {}
```

- [ ] **Step 4: Create listener**

```java
package com.tutor.crmservice.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionActivatedListener {

    private final CrmAccessRepository crmAccessRepository;

    @Transactional
    @KafkaListener(topics = "crm-events")
    public void onSubscriptionEvent(SubscriptionEventEnvelope<SubscriptionActivatedPayload> envelope) {
        if (!"SUBSCRIPTION_ACTIVATED".equals(envelope.eventType())) {
            log.debug("Ignoring subscription event type {}", envelope.eventType());
            return;
        }

        var payload = envelope.payload();
        var access = crmAccessRepository.findByTutorUserId(payload.tutorUserId())
                .orElseGet(() -> CrmAccess.builder()
                        .tutorUserId(payload.tutorUserId())
                        .build());

        access.setTariffCode(payload.tariffCode());
        access.setActiveUntil(payload.activeUntil());
        access.setUpdatedAt(Instant.now());
        crmAccessRepository.save(access);
        log.info("Updated CRM access for tutor {} until {}", payload.tutorUserId(), payload.activeUntil());
    }
}
```

- [ ] **Step 5: Add/verify Kafka consumer config in CRM**

In `backend/crm-service/src/main/resources/application.yaml`, ensure `spring.kafka.consumer` exists:

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

- [ ] **Step 6: Verify compile**

Run: `cd backend/crm-service && mvn clean compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add backend/crm-service/src/main/java/com/tutor/crmservice/subscription backend/crm-service/src/main/resources/application.yaml
git commit -m "feat(crm-service): consume subscription activation events"
```

---

## Task 9: Add CRM listener unit test

**Files:**
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/subscription/SubscriptionActivatedListenerTest.java`

- [ ] **Step 1: Create listener test**

```java
package com.tutor.crmservice.subscription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionActivatedListenerTest {

    @Mock
    private CrmAccessRepository crmAccessRepository;

    @InjectMocks
    private SubscriptionActivatedListener listener;

    @Test
    void shouldCreateCrmAccessForNewTutor() {
        var tutorUserId = UUID.randomUUID();
        var activeUntil = Instant.now().plusSeconds(3600);
        var envelope = new SubscriptionEventEnvelope<>(
                UUID.randomUUID(),
                "SUBSCRIPTION_ACTIVATED",
                Instant.now(),
                "subscription-service",
                new SubscriptionActivatedPayload(tutorUserId, "CRM_BASIC", activeUntil)
        );
        when(crmAccessRepository.findByTutorUserId(tutorUserId)).thenReturn(Optional.empty());
        when(crmAccessRepository.save(any(CrmAccess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onSubscriptionEvent(envelope);

        verify(crmAccessRepository).save(argThat(access ->
                access.getTutorUserId().equals(tutorUserId)
                        && access.getTariffCode().equals("CRM_BASIC")
                        && access.getActiveUntil().equals(activeUntil)
        ));
    }

    @Test
    void shouldUpdateExistingCrmAccess() {
        var tutorUserId = UUID.randomUUID();
        var activeUntil = Instant.now().plusSeconds(7200);
        var existing = CrmAccess.builder()
                .id(UUID.randomUUID())
                .tutorUserId(tutorUserId)
                .tariffCode("CRM_BASIC")
                .activeUntil(Instant.now())
                .build();
        var envelope = new SubscriptionEventEnvelope<>(
                UUID.randomUUID(),
                "SUBSCRIPTION_ACTIVATED",
                Instant.now(),
                "subscription-service",
                new SubscriptionActivatedPayload(tutorUserId, "CRM_PRO", activeUntil)
        );
        when(crmAccessRepository.findByTutorUserId(tutorUserId)).thenReturn(Optional.of(existing));
        when(crmAccessRepository.save(any(CrmAccess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onSubscriptionEvent(envelope);

        assertEquals("CRM_PRO", existing.getTariffCode());
        assertEquals(activeUntil, existing.getActiveUntil());
        verify(crmAccessRepository).save(existing);
    }

    @Test
    void shouldIgnoreUnknownEventType() {
        var envelope = new SubscriptionEventEnvelope<SubscriptionActivatedPayload>(
                UUID.randomUUID(),
                "OTHER_EVENT",
                Instant.now(),
                "subscription-service",
                null
        );

        listener.onSubscriptionEvent(envelope);

        verifyNoInteractions(crmAccessRepository);
    }
}
```

- [ ] **Step 2: Run CRM tests**

Run: `cd backend/crm-service && mvn test`

Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add backend/crm-service/src/test/java/com/tutor/crmservice/subscription/SubscriptionActivatedListenerTest.java
git commit -m "test(crm-service): add subscription activation listener tests"
```

---

## Task 10: Add subscription-service to docker-compose

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add service**

Add after `admin-service`:

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

- [ ] **Step 2: Add frontend dependency**

Add `subscription-service` to `frontend.depends_on`.

- [ ] **Step 3: Validate compose**

Run: `docker compose config`

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml
git commit -m "infra: add subscription-service to docker-compose"
```

---

## Task 11: Final verification and docs

**Files:**
- Modify: `doc/TutorSpace_E2E_MVP_Iterations.md`

- [ ] **Step 1: Run tests**

Run:

```bash
cd backend/subscription-service && mvn test
cd backend/crm-service && mvn test
```

Expected: `BUILD SUCCESS` for both.

- [ ] **Step 2: Validate docker compose**

Run: `docker compose config`

Expected: no errors.

- [ ] **Step 3: Update Iteration F docs**

In `doc/TutorSpace_E2E_MVP_Iterations.md`:
- Change Iteration F `Status: [ ]` to `Status: [x]`
- Mark all Iteration F checklist items `[x]`
- Add a short Russian `Выполнено:` section mentioning:
  - created `backend/subscription-service`
  - seeded tariffs
  - checkout and mock YooKassa test-confirm
  - activation only after confirmed payment
  - subscription periods/renewal history
  - `SUBSCRIPTION_ACTIVATED` published to `crm-events`
  - `crm-service` consumes event and updates `crm_access`
  - tests pass

- [ ] **Step 4: Commit**

```bash
git add doc/TutorSpace_E2E_MVP_Iterations.md
git commit -m "docs: mark Iteration F as complete"
```

---

## Self-Review

### Spec coverage check
- ✅ Create `subscription-service` → Task 1
- ✅ Tariffs as seeded DB data → Task 2
- ✅ Current subscription lookup → Tasks 5–6
- ✅ Checkout → Tasks 5–6
- ✅ Mock YooKassa test-confirm → Tasks 5–6
- ✅ Activation only after confirmed payment → Task 5
- ✅ Subscription periods/renewal history → Tasks 3, 5
- ✅ Publish `SubscriptionActivated` to `crm-events` → Task 4, Task 5
- ✅ `crm-service` consume and update `crm_access` → Tasks 8–9
- ✅ Docker Compose → Task 10
- ✅ Final verification/docs → Task 11

### Placeholder scan
- No TBD/TODO placeholders.
- All code snippets are complete.
- File paths are exact.
- Commands and expected outcomes are specified.

### Type consistency
- Event type consistently `SUBSCRIPTION_ACTIVATED`.
- Kafka topic consistently `crm-events`.
- Tariff fields consistently `code`, `name`, `priceRub`, `durationDays`, `active`.
- CRM projection consistently `CrmAccess` with `tutorUserId`, `tariffCode`, `activeUntil`, `updatedAt`.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-30-subscription-service-plan.md`.

Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
