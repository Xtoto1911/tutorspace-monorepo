# Admin Panel And Moderation Backend Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every current admin and moderator page load live backend aggregates through `admin-service`, with real supported actions and no backend demo data.

**Architecture:** `admin-service` becomes the BFF aggregator for `/api/admin/**` and `/api/moderator/**`. It calls `auth-service`, `marketplace-service`, and `subscription-service` through typed clients, then shapes responses into the existing frontend page DTOs. Downstream outages degrade read pages to empty/unavailable sections; write actions fail strictly.

**Tech Stack:** Java 21, Spring Boot 3.5.10, Spring MVC, Spring Data JPA, RestClient, H2, JUnit 5, Mockito, React 19, TypeScript, custom Rollup frontend scripts.

---

## File Structure

### auth-service

- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/controller/AdminUserController.java` — add `GET /api/admin/users`.
- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/repository/UserRepository.java` — add ordered user list method.
- Test: `backend/auth-service/src/test/java/com/tutor/authservice/service/UserServiceTest.java`.

### subscription-service

- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/AdminSubscriptionSnapshotController.java` — internal admin read endpoint.
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/AdminSubscriptionSnapshotDto.java` — snapshot DTO with tariffs, subscriptions, payments, periods.
- No repository changes required; use inherited `findAll()` from Spring Data repositories.
- Test: `backend/subscription-service/src/test/java/com/tutor/subscriptionservice/subscription/AdminSubscriptionSnapshotControllerTest.java`.

### marketplace-service

- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewService.java` — create reviews as `PENDING_MODERATION`.
- Do not modify `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileStatus.java`; use existing `ACTIVE` and `HIDDEN` statuses.
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileRepository.java` — add `findByStatusOrderByFullNameAsc`.
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/InternalTutorProfileSyncController.java` — add internal list endpoint for moderator queue.
- Test: `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/review/ReviewServiceTest.java`.

### admin-service

- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/client/AuthServiceClient.java` — expand into typed admin user client.
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/client/MarketplaceServiceClient.java` — add list reads and fail-strict write methods.
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/client/SubscriptionAdminClient.java` — read subscription snapshot.
- Create package: `backend/admin-service/src/main/java/com/tutor/adminservice/page/` — page DTO records and aggregators.
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/moderation/ModerationController.java` — expose admin page endpoints and moderator review/profile actions.
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogController.java` — add `/api/admin/audit` alias or page endpoint.
- Test: create focused tests under `backend/admin-service/src/test/java/com/tutor/adminservice/page/`.

### frontend

- Modify: `frontend/src/shared/api/admin.ts` — fix live paths and map backend page DTOs/actions.
- Test: create or extend `frontend/src/shared/api/admin.test.ts`.
- Verify: `cd frontend && npm run typecheck`.

---

## Task 1: Add Auth User List Endpoint

**Files:**
- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/repository/UserRepository.java`
- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/controller/AdminUserController.java`
- Test: `backend/auth-service/src/test/java/com/tutor/authservice/service/UserServiceTest.java`

- [ ] **Step 1: Add repository ordering method**

In `UserRepository.java`, add:

```java
import java.util.List;

List<User> findAllByOrderByEmailAsc();
```

- [ ] **Step 2: Add admin list endpoint**

In `AdminUserController.java`, add imports:

```java
import java.util.List;
```

Add method:

```java
@GetMapping
public List<UserAdminDto> listUsers() {
    return userService.listUsersForAdmin().stream()
            .map(UserAdminDto::from)
            .toList();
}
```

- [ ] **Step 3: Add service method**

In `UserService.java`, add:

```java
@Transactional(readOnly = true)
public java.util.List<User> listUsersForAdmin() {
    return userRepository.findAllByOrderByEmailAsc();
}
```

- [ ] **Step 4: Add unit test**

In `UserServiceTest.java`, add a test using the existing mock style:

```java
@Test
void shouldListUsersForAdminOrderedByEmail() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail("admin@tutorspace.local");
    user.setRole(Role.ROLE_ADMIN);
    user.setStatus(UserStatus.ACTIVE);

    when(userRepository.findAllByOrderByEmailAsc()).thenReturn(List.of(user));

    var result = userService.listUsersForAdmin();

    assertEquals(1, result.size());
    assertEquals("admin@tutorspace.local", result.get(0).getEmail());
}
```

Add imports if missing:

```java
import java.util.List;
```

- [ ] **Step 5: Run auth-service tests**

Run:

```powershell
cd backend/auth-service
.\mvnw.cmd test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Checkpoint**

If this directory is later initialized as git, commit:

```text
feat(auth-service): expose admin user list
```

If no `.git` directory exists, record the completed task in the final summary instead.

---

## Task 2: Add Subscription Admin Snapshot

**Files:**
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/AdminSubscriptionSnapshotDto.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/AdminSubscriptionSnapshotController.java`
- Test: `backend/subscription-service/src/test/java/com/tutor/subscriptionservice/subscription/AdminSubscriptionSnapshotControllerTest.java`

- [ ] **Step 1: Create snapshot DTO**

Create `AdminSubscriptionSnapshotDto.java`:

```java
package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.tariff.Tariff;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminSubscriptionSnapshotDto(
        List<TariffItem> tariffs,
        List<SubscriptionItem> subscriptions,
        List<PaymentItem> payments,
        List<PeriodItem> periods
) {
    public record TariffItem(String code, String name, int priceRub, int durationDays, boolean active) {
        public static TariffItem from(Tariff tariff) {
            return new TariffItem(
                    tariff.getCode(),
                    tariff.getName(),
                    tariff.getPriceRub(),
                    tariff.getDurationDays(),
                    tariff.isActive()
            );
        }
    }

    public record SubscriptionItem(UUID id, UUID tutorUserId, SubscriptionStatus status, String tariffCode, Instant activeUntil) {
        public static SubscriptionItem from(TutorSubscription subscription) {
            return new SubscriptionItem(
                    subscription.getId(),
                    subscription.getTutorUserId(),
                    subscription.getStatus(),
                    subscription.getCurrentTariffCode(),
                    subscription.getActiveUntil()
            );
        }
    }

    public record PaymentItem(UUID id, UUID tutorUserId, String tariffCode, int amountRub, PaymentStatus status, Instant createdAt) {
        public static PaymentItem from(Payment payment) {
            return new PaymentItem(
                    payment.getId(),
                    payment.getTutorUserId(),
                    payment.getTariffCode(),
                    payment.getAmountRub(),
                    payment.getStatus(),
                    payment.getCreatedAt()
            );
        }
    }

    public record PeriodItem(UUID id, UUID subscriptionId, UUID paymentId, String tariffCode, Instant startedAt, Instant activeUntil) {
        public static PeriodItem from(SubscriptionPeriod period) {
            return new PeriodItem(
                    period.getId(),
                    period.getSubscriptionId(),
                    period.getPaymentId(),
                    period.getTariffCode(),
                    period.getStartedAt(),
                    period.getActiveUntil()
            );
        }
    }
}
```

- [ ] **Step 2: Create snapshot controller**

Create `AdminSubscriptionSnapshotController.java`:

```java
package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.tariff.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/admin/subscriptions")
public class AdminSubscriptionSnapshotController {

    private final TariffRepository tariffRepository;
    private final TutorSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPeriodRepository periodRepository;

    @GetMapping
    public AdminSubscriptionSnapshotDto snapshot() {
        return new AdminSubscriptionSnapshotDto(
                tariffRepository.findAll().stream().map(AdminSubscriptionSnapshotDto.TariffItem::from).toList(),
                subscriptionRepository.findAll().stream().map(AdminSubscriptionSnapshotDto.SubscriptionItem::from).toList(),
                paymentRepository.findAll().stream().map(AdminSubscriptionSnapshotDto.PaymentItem::from).toList(),
                periodRepository.findAll().stream().map(AdminSubscriptionSnapshotDto.PeriodItem::from).toList()
        );
    }
}
```

- [ ] **Step 3: Add controller test**

Create `AdminSubscriptionSnapshotControllerTest.java`:

```java
package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.tariff.Tariff;
import com.tutor.subscriptionservice.tariff.TariffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSubscriptionSnapshotControllerTest {

    @Mock
    private TariffRepository tariffRepository;
    @Mock
    private TutorSubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionPeriodRepository periodRepository;

    @InjectMocks
    private AdminSubscriptionSnapshotController controller;

    @Test
    void shouldReturnRealSnapshotLists() {
        var tariff = Tariff.builder()
                .code("CRM_BASIC")
                .name("CRM Basic")
                .priceRub(990)
                .durationDays(30)
                .active(true)
                .build();
        var subscription = TutorSubscription.builder()
                .id(UUID.randomUUID())
                .tutorUserId(UUID.randomUUID())
                .status(SubscriptionStatus.ACTIVE)
                .currentTariffCode("CRM_BASIC")
                .activeUntil(Instant.now())
                .build();

        when(tariffRepository.findAll()).thenReturn(List.of(tariff));
        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(periodRepository.findAll()).thenReturn(List.of());

        var snapshot = controller.snapshot();

        assertEquals(1, snapshot.tariffs().size());
        assertEquals(1, snapshot.subscriptions().size());
        assertEquals("CRM_BASIC", snapshot.tariffs().get(0).code());
    }
}
```

- [ ] **Step 4: Run subscription-service tests**

Run:

```powershell
cd backend/subscription-service
mvn test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Checkpoint**

Commit message if git is available:

```text
feat(subscription-service): expose internal admin snapshot
```

---

## Task 3: Complete Marketplace Moderation Reads

**Files:**
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewService.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileRepository.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/InternalTutorProfileSyncController.java`
- Test: `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/review/ReviewServiceTest.java`

- [ ] **Step 1: Change review creation status**

In `ReviewService.create`, replace:

```java
review.setStatus(ReviewStatus.PUBLISHED);
```

with:

```java
review.setStatus(ReviewStatus.PENDING_MODERATION);
```

- [ ] **Step 2: Add repository method for profile moderation list**

In `TutorProfileRepository.java`, add:

```java
List<TutorProfile> findByStatusOrderByFullNameAsc(TutorProfileStatus status);
```

Use `ACTIVE` for currently visible profiles and `HIDDEN` for hidden profiles. Do not invent `PENDING_MODERATION` until the product has a profile submission state; the moderator screen can show active profiles and certificates/reviews as the real current queue.

- [ ] **Step 3: Add internal tutor list endpoint**

In `InternalTutorProfileSyncController.java`, add imports:

```java
import java.util.List;
```

Add method:

```java
@GetMapping
public List<TutorProfileDto> listByStatus(@RequestParam(defaultValue = "ACTIVE") TutorProfileStatus status) {
    return tutorProfileRepository.findByStatusOrderByFullNameAsc(status)
            .stream()
            .map(TutorProfileDto::from)
            .toList();
}
```

If `InternalTutorProfileSyncController` does not currently inject `TutorProfileRepository`, add it as a final constructor field alongside `TutorProfileService`.

- [ ] **Step 4: Extend review test**

In `ReviewServiceTest.java`, add or update the create-review assertion:

```java
assertEquals(ReviewStatus.PENDING_MODERATION, savedReview.getStatus());
```

Keep the existing public list assertions verifying only `PUBLISHED` reviews appear.

- [ ] **Step 5: Run marketplace-service tests**

Run:

```powershell
cd backend/marketplace-service
mvn test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Checkpoint**

Commit message if git is available:

```text
feat(marketplace-service): expose moderation read models
```

---

## Task 4: Add admin-service Downstream Clients

**Files:**
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/client/AuthServiceClient.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/client/MarketplaceServiceClient.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/client/SubscriptionAdminClient.java`
- Test: `backend/admin-service/src/test/java/com/tutor/adminservice/page/AdminClientsMappingTest.java`

- [ ] **Step 1: Make auth client return typed values**

In `AuthServiceClient.java`, add records:

```java
public record AdminUserDto(UUID id, String email, String role, String status) {}
public record UpdateRoleRequest(String role) {}
```

Add methods:

```java
public List<AdminUserDto> listUsers(String adminToken) {
    return restClient.get()
            .uri("/api/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .retrieve()
            .body(new org.springframework.core.ParameterizedTypeReference<List<AdminUserDto>>() {});
}

public void updateUserRole(UUID userId, String role, String adminToken) {
    restClient.patch()
            .uri("/api/admin/users/{userId}/role", userId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .body(new UpdateRoleRequest(role))
            .retrieve()
            .toBodilessEntity();
}
```

Update `updateUserStatus` so it does not swallow exceptions. Remove its `try/catch`; logging can happen in the controller/service that catches read failures.

- [ ] **Step 2: Add marketplace read records and methods**

In `MarketplaceServiceClient.java`, add records:

```java
public record ModerationReviewDto(UUID id, UUID tutorProfileId, UUID clientUserId, int rating, String text, String status, String createdAt) {}
public record ModerationCertificateDto(UUID id, String title, String issuer, String status) {}
public record ModerationTutorProfileDto(UUID id, UUID userId, String slug, String fullName, String headline, String status) {}
```

Add read methods:

```java
public List<ModerationReviewDto> listReviews(String status) {
    return restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/internal/reviews").queryParam("status", status).build())
            .retrieve()
            .body(new org.springframework.core.ParameterizedTypeReference<List<ModerationReviewDto>>() {});
}

public List<ModerationCertificateDto> listCertificates(String status) {
    return restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/internal/certificates").queryParam("status", status).build())
            .retrieve()
            .body(new org.springframework.core.ParameterizedTypeReference<List<ModerationCertificateDto>>() {});
}

    public List<ModerationTutorProfileDto> listTutorProfiles(String status) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/internal/marketplace/tutors").queryParam("status", status).build())
            .header("X-Internal-Token", internalToken)
            .retrieve()
            .body(new org.springframework.core.ParameterizedTypeReference<List<ModerationTutorProfileDto>>() {});
    }
```

Add an `internalToken` constructor field to `MarketplaceServiceClient`, populated with:

```java
@Value("${app.internal-service-token:${jwt.secret:}}") String internalToken
```

Remove `try/catch` from write methods that must fail strictly: `updateReviewStatus`, `updateCertificateStatus`, `restrictReviewPublication`, `hideTutorProfile`.

- [ ] **Step 3: Create subscription client**

Create `SubscriptionAdminClient.java`:

```java
package com.tutor.adminservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SubscriptionAdminClient {

    private final RestClient restClient;

    private final String internalToken;

    public SubscriptionAdminClient(@Value("${app.subscription-service-url:http://subscription-service:8085}") String subscriptionServiceUrl,
                                   @Value("${app.internal-service-token:${jwt.secret:}}") String internalToken) {
        this.restClient = RestClient.builder().baseUrl(subscriptionServiceUrl).build();
        this.internalToken = internalToken;
    }

    public Snapshot snapshot() {
        return restClient.get()
                .uri("/internal/admin/subscriptions")
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(Snapshot.class);
    }

    public record Snapshot(List<TariffItem> tariffs, List<SubscriptionItem> subscriptions, List<PaymentItem> payments, List<PeriodItem> periods) {}
    public record TariffItem(String code, String name, int priceRub, int durationDays, boolean active) {}
    public record SubscriptionItem(UUID id, UUID tutorUserId, String status, String tariffCode, Instant activeUntil) {}
    public record PaymentItem(UUID id, UUID tutorUserId, String tariffCode, int amountRub, String status, Instant createdAt) {}
    public record PeriodItem(UUID id, UUID subscriptionId, UUID paymentId, String tariffCode, Instant startedAt, Instant activeUntil) {}
}
```

- [ ] **Step 4: Add config value**

In `backend/admin-service/src/main/resources/application.yaml`, under `app:`, add:

```yaml
  subscription-service-url: ${SUBSCRIPTION_SERVICE_URL:http://subscription-service:8085}
  internal-service-token: ${INTERNAL_SERVICE_TOKEN:${JWT_SECRET:}}
```

- [ ] **Step 5: Run admin-service compile**

Run:

```powershell
cd backend/admin-service
mvn test
```

Expected: `BUILD SUCCESS` after adjusting imports.

- [ ] **Step 6: Checkpoint**

Commit message if git is available:

```text
feat(admin-service): add typed downstream admin clients
```

---

## Task 5: Create admin-service Page DTOs And Aggregators

**Files:**
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/page/AdminPageDtos.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/page/AdminPageService.java`
- Test: `backend/admin-service/src/test/java/com/tutor/adminservice/page/AdminPageServiceTest.java`

- [ ] **Step 1: Create compact frontend-shaped DTO records**

Create `AdminPageDtos.java` with records matching `frontend/src/shared/types.ts` field names:

```java
package com.tutor.adminservice.page;

import java.util.List;

public final class AdminPageDtos {
    private AdminPageDtos() {}

    public record Metric(String icon, String label, String value, String delta, String tone) {}
    public record ServiceStatus(String name, String status, String uptime, String latency, String tone) {}
    public record Alert(String id, String title, String description, String severity, String time) {}
    public record QueueItem(String id, String type, String target, String status, String owner) {}
    public record QuickAction(String title, String text, String to) {}
    public record AuditItem(String id, String actor, String action, String date) {}

    public record DashboardPage(
            List<Metric> metrics,
            List<ServiceStatus> services,
            List<Alert> alerts,
            List<QueueItem> moderationQueue,
            List<QuickAction> quickActions,
            List<AuditItem> audit
    ) {}

    public record UserMetric(String icon, String value, String title, String meta, String tone) {}
    public record UserRow(String id, String initials, String name, String email, String role, String status, String risk, String tone, boolean active) {}
    public record UserSignal(String title, String meta) {}
    public record SelectedUser(String initials, String name, String email, String role, String status, String riskTitle, List<UserSignal> signals, List<String> actions) {}
    public record UserCards(AuditCard audit, RoleCard roles, AuditCard moderation, AuditCard note) {}
    public record AuditCard(String title, String text, String action) {}
    public record RoleCard(String label, String value, String text, String action) {}
    public record UsersPage(List<UserMetric> metrics, List<String> tabs, List<UserRow> users, SelectedUser selected, UserCards cards) {}

    public record SimpleMetric(String icon, String value, String label, String hint, String tone) {}
    public record SubscriptionRow(String id, String tutor, String plan, String status, String expiresAt, String payment, String tone) {}
    public record PlanShare(String plan, String value, int percent, String tone) {}
    public record WebhookItem(String id, String event, String action, String tone) {}
    public record PlanSetting(String name, String price, String body) {}
    public record SubscriptionsPage(List<SimpleMetric> metrics, List<SubscriptionRow> subscriptions, List<PlanShare> revenueByPlan, List<WebhookItem> webhookQueue, List<PlanSetting> planSettings) {}
}
```

Add these records to the same file for the remaining admin pages:

```java
public record SiteStatsPage(List<UserMetric> metrics, List<String> tabs, Chart growth, Conversion conversion, List<Quality> quality, List<Quality> funnel, Finance finance, List<Geo> geo, List<Event> events, AuditCard note) {}
public record Chart(String title, String caption, List<String> labels, List<Integer> users, List<Integer> requests, String footer) {}
public record Conversion(String label, String value, String text) {}
public record Quality(String label, String value, int percent) {}
public record Finance(String title, String value, String caption, String trend) {}
public record Geo(String city, String value, int percent) {}
public record Event(String time, String title, String meta, String tone) {}
public record AuditPage(List<SimpleMetric> metrics, List<String> tabs, List<AuditEvent> events, SelectedAudit selected, List<Quality> policies, List<AuditCard> exports) {}
public record AuditEvent(String id, String time, String actor, String role, String action, String target, String service, String severity, String status, String tone, boolean active) {}
public record SelectedAudit(String id, String title, String actor, String description, List<Meta> meta, List<String> payload) {}
public record Meta(String label, String value) {}
public record SettingsPage(List<SimpleMetric> metrics, List<Toggle> security, List<RoleSetting> roles, List<Limit> limits, List<ServiceStatus> services, List<Policy> policies) {}
public record Toggle(String title, String description, boolean enabled) {}
public record RoleSetting(String title, String description, String users) {}
public record Limit(String label, String value, String description) {}
public record Policy(String title, String text, List<String> tags) {}
```

- [ ] **Step 2: Create aggregator service skeleton**

Create `AdminPageService.java`:

```java
package com.tutor.adminservice.page;

import com.tutor.adminservice.audit.AuditLogRepository;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.client.SubscriptionAdminClient;
import com.tutor.adminservice.complaint.ComplaintRepository;
import com.tutor.adminservice.complaint.ComplaintStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPageService {

    private final AuthServiceClient authClient;
    private final MarketplaceServiceClient marketplaceClient;
    private final SubscriptionAdminClient subscriptionClient;
    private final ComplaintRepository complaintRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminPageDtos.DashboardPage dashboard(String token) {
        var users = safeUsers(token);
        var pendingComplaints = complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING);
    var audit = auditLogRepository.findAll().stream()
                .limit(5)
                .map(log -> new AdminPageDtos.AuditItem(log.getId().toString(), log.getAdminUserId().toString(), log.getActionType(), log.getCreatedAt().toString()))
                .toList();

        return new AdminPageDtos.DashboardPage(
                List.of(
                        new AdminPageDtos.Metric("U", "Пользователи", String.valueOf(users.size()), "live", "teal"),
                        new AdminPageDtos.Metric("!", "Жалобы", String.valueOf(pendingComplaints.size()), "open", "amber")
                ),
                List.of(
                        new AdminPageDtos.ServiceStatus("auth-service", users.isEmpty() ? "Недоступен или пусто" : "Работает", "live", "-", users.isEmpty() ? "amber" : "green")
                ),
                List.of(),
                pendingComplaints.stream()
                        .map(c -> new AdminPageDtos.QueueItem(c.getId().toString(), c.getTargetType().name(), c.getReason(), c.getStatus().name(), c.getModeratorId() == null ? "Очередь" : "Назначен"))
                        .toList(),
                List.of(
                        new AdminPageDtos.QuickAction("Пользователи", "Управление ролями и блокировками", "/admin/users"),
                        new AdminPageDtos.QuickAction("Жалобы", "Очередь модерации", "/moderator/complaints")
                ),
                audit
        );
    }

    private List<AuthServiceClient.AdminUserDto> safeUsers(String token) {
        try {
            var users = authClient.listUsers(token);
            return users == null ? List.of() : users;
        } catch (Exception exception) {
            return List.of();
        }
    }
}
```

- [ ] **Step 3: Expand aggregator methods**

In `AdminPageService.java`, add methods:

```java
public AdminPageDtos.UsersPage usersPage(String token) {
    var users = safeUsers(token);
    var rows = users.stream()
            .map(user -> new AdminPageDtos.UserRow(
                    user.id().toString(),
                    initials(user.email()),
                    user.email(),
                    user.email(),
                    user.role(),
                    user.status(),
                    "Low",
                    "indigo",
                    false
            ))
            .toList();
    var selected = rows.isEmpty()
            ? new AdminPageDtos.SelectedUser("--", "Нет пользователей", "-", "-", "-", "Нет сигналов", List.of(), List.of())
            : new AdminPageDtos.SelectedUser(rows.get(0).initials(), rows.get(0).name(), rows.get(0).email(), rows.get(0).role(), rows.get(0).status(), "Низкий риск", List.of(), List.of("Заблокировать", "Сделать модератором"));

    return new AdminPageDtos.UsersPage(
            List.of(new AdminPageDtos.UserMetric("U", String.valueOf(users.size()), "Всего", "live auth-service", "indigo")),
            List.of("Все", "Админы", "Модераторы", "Репетиторы", "Клиенты"),
            rows,
            selected,
            new AdminPageDtos.UserCards(
                    new AdminPageDtos.AuditCard("Аудит", "Последние действия доступны в журнале.", "Открыть аудит"),
                    new AdminPageDtos.RoleCard("Роли", String.valueOf(users.size()), "Управление реальными ролями auth-service.", "Обновить"),
                    new AdminPageDtos.AuditCard("Модерация", "Очереди доступны в разделе модератора.", "Открыть"),
                    new AdminPageDtos.AuditCard("Заметка", "Демо-действия отключены.", "Понятно")
            )
    );
}

public AdminPageDtos.SubscriptionsPage subscriptionsPage() {
    var snapshot = subscriptionClient.snapshot();
    var subscriptions = snapshot == null || snapshot.subscriptions() == null ? List.<SubscriptionAdminClient.SubscriptionItem>of() : snapshot.subscriptions();
    var tariffs = snapshot == null || snapshot.tariffs() == null ? List.<SubscriptionAdminClient.TariffItem>of() : snapshot.tariffs();
    var rows = subscriptions.stream()
            .map(subscription -> new AdminPageDtos.SubscriptionRow(
                    subscription.id().toString(),
                    subscription.tutorUserId().toString(),
                    subscription.tariffCode(),
                    subscription.status(),
                    subscription.activeUntil() == null ? "-" : subscription.activeUntil().toString(),
                    "read-only",
                    "emerald"
            ))
            .toList();

    return new AdminPageDtos.SubscriptionsPage(
            List.of(new AdminPageDtos.SimpleMetric("S", String.valueOf(subscriptions.size()), "Подписки", "live subscription-service", "indigo")),
            rows,
            tariffs.stream().map(tariff -> new AdminPageDtos.PlanShare(tariff.name(), "0", 0, "slate")).toList(),
            List.of(),
            tariffs.stream().map(tariff -> new AdminPageDtos.PlanSetting(tariff.name(), tariff.priceRub() + " ₽", tariff.durationDays() + " дней")).toList()
    );
}
```

Use these exact fallback rules:

- no users: empty table and selected card titled `Нет пользователей`;
- no subscriptions: zero metrics and empty list;
- downstream exception on reads: empty section plus service status/alert where page type supports it.

- [ ] **Step 4: Add aggregator tests**

Create `AdminPageServiceTest.java` with Mockito. Include these concrete tests:

```java
@Test
void dashboardUsesComplaintsAndUsersWhenAvailable() {
    var token = "admin-token";
    when(authClient.listUsers(token)).thenReturn(List.of(
            new AuthServiceClient.AdminUserDto(UUID.randomUUID(), "admin@tutorspace.local", "ROLE_ADMIN", "ACTIVE")
    ));
    when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of(
            Complaint.builder().id(UUID.randomUUID()).targetType(ComplaintTargetType.REVIEW).reason("Spam").status(ComplaintStatus.PENDING).build()
    ));
    when(auditLogRepository.findAll()).thenReturn(List.of());

    var page = service.dashboard(token);

    assertEquals("1", page.metrics().get(0).value());
    assertEquals(1, page.moderationQueue().size());
}

@Test
void dashboardSurvivesAuthClientFailure() {
    var token = "admin-token";
    when(authClient.listUsers(token)).thenThrow(new RuntimeException("auth unavailable"));
    when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of());
    when(auditLogRepository.findAll()).thenReturn(List.of());

    var page = service.dashboard(token);

    assertEquals("0", page.metrics().get(0).value());
    assertTrue(page.services().stream().anyMatch(service -> service.name().equals("auth-service")));
}

@Test
void subscriptionsPageUsesSnapshotWithoutInventingActions() {
    when(subscriptionClient.snapshot()).thenReturn(new SubscriptionAdminClient.Snapshot(
            List.of(new SubscriptionAdminClient.TariffItem("CRM_BASIC", "CRM Basic", 990, 30, true)),
            List.of(),
            List.of(),
            List.of()
    ));

    var page = service.subscriptionsPage();

    assertEquals(1, page.planSettings().size());
    assertTrue(page.webhookQueue().isEmpty());
}
```

Assert that failures return empty values rather than throwing.

- [ ] **Step 5: Run admin-service tests**

Run:

```powershell
cd backend/admin-service
mvn test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Checkpoint**

Commit message if git is available:

```text
feat(admin-service): aggregate admin page read models
```

---

## Task 6: Wire admin-service Controllers And Strict Actions

**Files:**
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/moderation/ModerationController.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogController.java`
- Test: `backend/admin-service/src/test/java/com/tutor/adminservice/page/AdminActionServiceTest.java`

- [ ] **Step 1: Add page GET routes**

In `ModerationController.java`, inject `AdminPageService` and add:

```java
@GetMapping("/dashboard")
public AdminPageDtos.DashboardPage dashboard(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
    return adminPageService.dashboard(authHeader.substring(7));
}

@GetMapping("/alerts")
public List<AdminPageDtos.Alert> alerts(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
    return adminPageService.dashboard(authHeader.substring(7)).alerts();
}

@GetMapping("/users")
public AdminPageDtos.UsersPage users(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
    return adminPageService.usersPage(authHeader.substring(7));
}

@GetMapping("/subscriptions")
public AdminPageDtos.SubscriptionsPage subscriptions() {
    return adminPageService.subscriptionsPage();
}
```

Add the remaining page routes:

```java
@GetMapping("/site-stats")
public AdminPageDtos.SiteStatsPage siteStats(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
    return adminPageService.siteStats(authHeader.substring(7));
}

@GetMapping("/audit")
public AdminPageDtos.AuditPage auditPage() {
    return adminPageService.auditPage();
}

@GetMapping("/settings")
public AdminPageDtos.SettingsPage settings(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
    return adminPageService.settings(authHeader.substring(7));
}
```

Implement `siteStats`, `auditPage`, and `settings` in `AdminPageService` using real user/audit/subscription/service-status inputs and zero/empty arrays for metrics that no current service exposes.

- [ ] **Step 2: Replace block/unblock-only user API with action API**

Add request record inside `ModerationController`:

```java
public record AdminUserActionRequest(String action) {}
```

Add:

```java
@PatchMapping("/users/{id}")
public Map<String, String> updateUser(@PathVariable UUID id,
                                      @RequestBody AdminUserActionRequest request,
                                      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                      @AuthenticationPrincipal UUID adminUserId) {
    var token = authHeader.substring(7);
    var action = request.action();
    if ("block".equals(action)) {
        authClient.updateUserStatus(id, "BLOCKED", token);
        auditLogService.log(adminUserId, "USER_BLOCKED", "USER", id, null);
    } else if ("unblock".equals(action)) {
        authClient.updateUserStatus(id, "ACTIVE", token);
        auditLogService.log(adminUserId, "USER_UNBLOCKED", "USER", id, null);
    } else if ("make_admin".equals(action)) {
        authClient.updateUserRole(id, "ROLE_ADMIN", token);
        auditLogService.log(adminUserId, "USER_ROLE_UPDATED", "USER", id, "ROLE_ADMIN");
    } else if ("make_moderator".equals(action)) {
        authClient.updateUserRole(id, "ROLE_MODERATOR", token);
        auditLogService.log(adminUserId, "USER_ROLE_UPDATED", "USER", id, "ROLE_MODERATOR");
    } else {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                "Unsupported admin user action: " + action
        );
    }
    return Map.of("userId", id.toString(), "action", action, "updatedAt", java.time.Instant.now().toString());
}
```

Keep `/users/{id}/block` and `/users/{id}/unblock` as compatibility wrappers if desired, but frontend should move to `PATCH /admin/users/{id}`.

- [ ] **Step 3: Add audit page alias**

In `AuditLogController.java`, change the mapping to include both paths:

```java
@RequestMapping({"/api/admin/audit-log", "/api/admin/audit"})
```

If `/api/admin/audit` must return the full frontend page shape, build it through `AdminPageService` instead of returning raw `AuditLogDto`.

- [ ] **Step 4: Add strict action tests**

Test unsupported action:

```java
assertThrows(ResponseStatusException.class, () -> controller.updateUser(userId, new AdminUserActionRequest("remove_role"), "Bearer token", adminId));
verifyNoInteractions(auditLogService);
```

Test successful action writes audit only after client succeeds.

- [ ] **Step 5: Run admin-service tests**

Run:

```powershell
cd backend/admin-service
mvn test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Checkpoint**

Commit message if git is available:

```text
feat(admin-service): expose admin page endpoints and strict actions
```

---

## Task 7: Build Moderator Review/Profile Page

**Files:**
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/page/AdminPageDtos.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/page/AdminPageService.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/moderation/ModerationController.java`
- Test: `backend/admin-service/src/test/java/com/tutor/adminservice/page/ModeratorQueueServiceTest.java`

- [ ] **Step 1: Add moderator DTO records**

Add records to `AdminPageDtos.java`:

```java
public record ModeratorMetric(String icon, String value, String label, String hint, String trend, String tone) {}
public record ModeratorProfileRow(String id, String initials, String name, String subject, String type, String status, String summary, String score, String documents, String owner, String tone, boolean active) {}
public record ModeratorDocument(String title, String subtitle, String status) {}
public record ModeratorCheckItem(String label, boolean done) {}
public record ModeratorSelectedProfile(String id, String status, String initials, String name, String subject, String experience, String risk, List<ModeratorDocument> documents, List<ModeratorCheckItem> checklist, String score, String note) {}
public record ModeratorPipeline(String label, String value, int percent) {}
public record ModeratorDecision(String title, String body, List<String> tags) {}
public record ModeratorReviewPage(List<ModeratorMetric> metrics, List<String> tabs, List<ModeratorProfileRow> profiles, ModeratorSelectedProfile selected, List<ModeratorPipeline> pipeline, List<String> rubric, ModeratorDecision decision) {}
```

- [ ] **Step 2: Build moderator page from marketplace queues**

In `AdminPageService.java`, add:

```java
public AdminPageDtos.ModeratorReviewPage moderatorReviewPage() {
    var reviews = safeReviews();
    var certificates = safeCertificates();
    var profiles = safeProfiles();
    var total = reviews.size() + certificates.size() + profiles.size();

    var rows = profiles.stream()
            .map(profile -> new AdminPageDtos.ModeratorProfileRow(
                    profile.id().toString(),
                    initials(profile.fullName()),
                    profile.fullName(),
                    "Профиль",
                    "Анкета",
                    profile.status(),
                    profile.headline() == null ? "Описание не заполнено" : profile.headline(),
                    "0%",
                    certificates.size() + " документов в очереди",
                    "Очередь",
                    "amber",
                    false
            ))
            .toList();

    var selected = rows.isEmpty()
            ? new AdminPageDtos.ModeratorSelectedProfile("Нет анкет", "Пусто", "--", "Очередь пуста", "Нет данных", "-", "Низкий", List.of(), List.of(), "0%", "Нет профилей для проверки.")
            : new AdminPageDtos.ModeratorSelectedProfile(rows.get(0).id(), rows.get(0).status(), rows.get(0).initials(), rows.get(0).name(), rows.get(0).subject(), "-", "Средний", List.of(), List.of(), rows.get(0).score(), rows.get(0).summary());

    return new AdminPageDtos.ModeratorReviewPage(
            List.of(new AdminPageDtos.ModeratorMetric("!", String.valueOf(total), "в очереди", "Реальные элементы marketplace", String.valueOf(total), "amber")),
            List.of("Все", "Анкеты", "Отзывы", "Документы"),
            rows,
            selected,
            List.of(new AdminPageDtos.ModeratorPipeline("Всего", String.valueOf(total), total == 0 ? 0 : 100)),
            List.of("Проверить публичное описание", "Проверить документы", "Не публиковать спорные отзывы"),
            new AdminPageDtos.ModeratorDecision("Решение модератора", "Действие применяется только к поддерживаемой сущности.", List.of("live", "audit"))
    );
}
```

Add helper methods `safeReviews`, `safeCertificates`, `safeProfiles`, and `initials`.

- [ ] **Step 3: Add moderator endpoints**

Create a second controller for `/api/moderator` instead of expanding the existing `/api/admin` controller:

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/moderator")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERATOR')")
public class ModeratorPageController {
    private final AdminPageService adminPageService;
    private final MarketplaceServiceClient marketplaceClient;
    private final AuditLogService auditLogService;

    @GetMapping("/review-moderation")
    public AdminPageDtos.ModeratorReviewPage reviewModeration() {
        return adminPageService.moderatorReviewPage();
    }
}
```

- [ ] **Step 4: Add approve/reject endpoints**

In `ModeratorPageController`, add:

```java
@PostMapping("/review-moderation/{id}/approve")
public Map<String, String> approve(@PathVariable UUID id, @AuthenticationPrincipal UUID moderatorId) {
    marketplaceClient.updateReviewStatus(id, "PUBLISHED");
    auditLogService.log(moderatorId, "MODERATION_APPROVED", "REVIEW", id, null);
    return Map.of("profileId", id.toString(), "decision", "approve", "status", "approved", "decidedAt", Instant.now().toString());
}

@PostMapping("/review-moderation/{id}/reject")
public Map<String, String> reject(@PathVariable UUID id, @AuthenticationPrincipal UUID moderatorId) {
    marketplaceClient.updateReviewStatus(id, "REJECTED");
    auditLogService.log(moderatorId, "MODERATION_REJECTED", "REVIEW", id, null);
    return Map.of("profileId", id.toString(), "decision", "reject", "status", "rejected", "decidedAt", Instant.now().toString());
}
```

For this iteration, frontend should call approve/reject only for rows whose id came from the review queue. Hide approve/reject buttons for profile-only rows until marketplace exposes a dedicated profile approval state.

- [ ] **Step 5: Run admin-service tests**

Run:

```powershell
cd backend/admin-service
mvn test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Checkpoint**

Commit message if git is available:

```text
feat(admin-service): expose moderator review queue
```

---

## Task 8: Fix Frontend Admin API Paths And Action Mapping

**Files:**
- Modify: `frontend/src/shared/api/admin.ts`
- Test: `frontend/src/shared/api/admin.test.ts`

- [ ] **Step 1: Fix admin user path**

Change:

```ts
return apiRequest('/admin/users/:id', {
```

to:

```ts
return apiRequest(`/admin/users/${encodeURIComponent(userId)}`, {
```

Change body from `{ userId, action }` to:

```ts
body: JSON.stringify({ action: mapAdminUserAction(action) }),
```

Add helper:

```ts
function mapAdminUserAction(action: string) {
  if (action === 'Заблокировать' || action === 'block') return 'block';
  if (action === 'Разблокировать' || action === 'unblock') return 'unblock';
  if (action === 'Сделать админом' || action === 'make_admin') return 'make_admin';
  if (action === 'Сделать модератором' || action === 'make_moderator') return 'make_moderator';
  return action;
}
```

- [ ] **Step 2: Fix moderator review path**

Change:

```ts
return apiRequest('/moderator/review-moderation/:id/approve', {
```

to:

```ts
return apiRequest(`/moderator/review-moderation/${encodeURIComponent(profileId)}/${decision === 'approve' ? 'approve' : 'reject'}`, {
```

Keep response fallback shape unchanged.

- [ ] **Step 3: Verify complaint path**

Keep:

```ts
`/moderator/complaints/${encodeURIComponent(complaintId)}/moderate`
```

because `VITE_API_BASE_URL=/api` supplies the `/api` prefix.

- [ ] **Step 4: Add API helper tests**

In `admin.test.ts`, mock `fetch` and assert requested URLs include:

```text
/api/admin/users/user-1
/api/moderator/review-moderation/profile-1/approve
/api/moderator/review-moderation/profile-1/reject
```

Also assert there is no literal `:id` in the URL.

- [ ] **Step 5: Run frontend checks**

Run:

```powershell
cd frontend
npm run typecheck
npm test -- admin.test.ts
```

Expected: typecheck passes and targeted tests pass.

- [ ] **Step 6: Checkpoint**

Commit message if git is available:

```text
fix(frontend): call real admin and moderator action paths
```

---

## Task 9: Final Verification

**Files:**
- Review: `docs/superpowers/specs/2026-05-20-admin-panel-moderation-backend-design.md`
- Review: `docs/superpowers/plans/2026-05-20-admin-panel-moderation-backend.md`

- [ ] **Step 1: Run changed backend tests**

Run:

```powershell
cd backend/admin-service
mvn test
```

Run:

```powershell
cd backend/auth-service
.\mvnw.cmd test
```

Run:

```powershell
cd backend/marketplace-service
mvn test
```

Run:

```powershell
cd backend/subscription-service
mvn test
```

Expected for each: `BUILD SUCCESS`.

- [ ] **Step 2: Run frontend verification**

Run:

```powershell
cd frontend
npm run typecheck
npm test -- admin.test.ts
```

Expected: both pass.

- [ ] **Step 3: Optional docker syntax check**

Run:

```powershell
docker compose config
```

Expected: no syntax errors. If Docker is unavailable, report that this check was not run.

- [ ] **Step 4: Manual smoke path list**

With stack running, verify these routes render:

```text
http://localhost:4173/admin
http://localhost:4173/admin/users
http://localhost:4173/admin/site-stats
http://localhost:4173/admin/subscriptions
http://localhost:4173/admin/audit
http://localhost:4173/admin/settings
http://localhost:4173/moderator/complaints
http://localhost:4173/moderator/review-moderation
```

Expected: pages render from live API or show honest empty state; no literal `:id` requests appear in browser network logs.

- [ ] **Step 5: Final summary**

Report:

- files changed;
- tests run and results;
- any checks not run;
- any remaining non-goals, especially subscription write actions.

---

## Self-Review

### Spec Coverage

- Admin-service BFF aggregation: Tasks 4, 5, 6, 7.
- Auth user list and user actions: Tasks 1, 6.
- Subscription read-only admin data: Tasks 2, 5.
- Marketplace moderation queues and review status: Tasks 3, 7.
- Frontend placeholder path fixes: Task 8.
- Verification evidence: Task 9.

### Placeholder Scan

The plan avoids open-ended placeholders. Where page DTO expansion depends on existing frontend types, the plan names the exact source file and says to keep field names identical.

### Type Consistency

Backend public paths use `/api/**`; frontend helper paths omit `/api` because `VITE_API_BASE_URL=/api` supplies it. Review moderation status is `PENDING_MODERATION`, matching the current `ReviewStatus` enum.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-20-admin-panel-moderation-backend.md`.

Two execution options:

**1. Subagent-Driven (recommended)** - dispatch a fresh worker per task, review between tasks, fast iteration.

**2. Inline Execution** - execute tasks in this session using executing-plans, batch execution with checkpoints.
