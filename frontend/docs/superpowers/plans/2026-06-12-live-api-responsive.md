# TutorSpace Live API and Responsive Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every existing TutorSpace route use persisted backend data, replace the moderator placeholder, remove runtime mock fallbacks, and verify responsive behavior through the Docker gateway.

**Architecture:** Deliver vertical scenarios across the seven independent repositories. Backend services expose persisted domain DTOs through the existing `/api/**` ownership boundaries; frontend adapters add presentation-only labels and render explicit loading, empty, and error states. Every behavior change follows red-green-refactor and ends with a repository-scoped commit.

**Tech Stack:** Java 21, Spring Boot 3.5.10, Spring Data JPA, PostgreSQL 16, Maven, React 19, TypeScript 5.8, Vitest, Playwright, custom Rollup build scripts, nginx gateway, Docker Compose.

---

## Repository Map

- `C:\JavaMy\newDiploma\frontend`: browser UI, API adapters, gateway, responsive CSS, browser tests.
- `C:\JavaMy\newDiploma\backend\auth-service`: identity, sessions, password-reset lifecycle.
- `C:\JavaMy\newDiploma\backend\marketplace-service`: subjects, tutor catalog, profiles, reviews, public complaints.
- `C:\JavaMy\newDiploma\backend\crm-service`: client and tutor workspace data.
- `C:\JavaMy\newDiploma\backend\notification-service`: persisted in-app notifications.
- `C:\JavaMy\newDiploma\backend\subscription-service`: tariffs, subscriptions, YooKassa test checkout.
- `C:\JavaMy\newDiploma\backend\admin-service`: aggregation, audit, complaints, moderation.

## Task 1: Make the Frontend HTTP Client Live-Only

**Files:**
- Modify: `frontend/src/shared/api/http.ts`
- Modify: `frontend/src/shared/api/http.test.ts`
- Modify: `frontend/src/vite-env.d.ts`
- Modify: `frontend/.env.example`

- [ ] **Step 1: Write failing tests for live-only requests**

Add tests proving `apiRequest` always calls `fetch`, does not accept a fallback factory, preserves refresh retry, and returns structured errors:

```ts
it('always performs a network request', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => ({ items: [] }),
  }));

  await apiRequest('/client/applications');

  expect(fetch).toHaveBeenCalledWith('/api/client/applications', expect.any(Object));
});
```

- [ ] **Step 2: Run the focused test and verify RED**

Run from `frontend`:

```powershell
npm test -- src/shared/api/http.test.ts
```

Expected: FAIL because `apiRequest` still selects mock mode and requires the old signature.

- [ ] **Step 3: Remove runtime mode and mock arguments**

Refactor the exported API to:

```ts
export function apiRequest<T>(path: string, init?: RequestInit) {
  return request<T>(path, init);
}

export const liveApiRequest = apiRequest;
export const authRequest = apiRequest;
```

Keep the current bearer token, one-time refresh retry, `204` handling, and backend error parsing. Remove `MockValue`, `resolveMockValue`, `delay`, `apiMode`, and `authApiMode`.

- [ ] **Step 4: Run the focused test and verify GREEN**

```powershell
npm test -- src/shared/api/http.test.ts
```

Expected: all `http.test.ts` tests pass.

- [ ] **Step 5: Commit frontend HTTP changes**

```powershell
git add src/shared/api/http.ts src/shared/api/http.test.ts src/vite-env.d.ts .env.example
git commit -m "refactor: make frontend API client live only"
```

## Task 2: Add Real Password Reset to Auth Service

**Files:**
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/domain/PasswordResetToken.java`
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/repository/PasswordResetTokenRepository.java`
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/dto/PasswordResetRequestDto.java`
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/dto/PasswordResetConfirmDto.java`
- Create: `backend/auth-service/src/main/java/com/tutor/authservice/dto/PasswordResetResponseDto.java`
- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/controller/AuthController.java`
- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/service/UserService.java`
- Modify: `backend/auth-service/src/main/java/com/tutor/authservice/exceptionHandler/GlobalExceptionHandler.java`
- Modify: `backend/auth-service/src/test/java/com/tutor/authservice/service/UserServiceTest.java`

- [ ] **Step 1: Write failing reset lifecycle tests**

Cover known and unknown emails without account enumeration, one-use tokens, expiry, password encoding, and active-session revocation:

```java
@Test
void confirmsPasswordResetAndRevokesExistingSessions() {
    var token = service.requestPasswordReset("client@example.com").testToken();
    service.confirmPasswordReset(token, "new-password-123");

    verify(passwordEncoder).encode("new-password-123");
    verify(userSessionRepository).findByUserIdAndRevokedAtIsNull(user.getId());
}
```

- [ ] **Step 2: Run the service test and verify RED**

```powershell
.\mvnw.cmd -Dtest=UserServiceTest test
```

Expected: compilation failure because reset methods and entity do not exist.

- [ ] **Step 3: Implement persisted reset tokens**

Store a SHA-256 token hash, `user`, `expiresAt`, and `usedAt`. `POST /api/auth/password-reset` always returns HTTP 200 with a neutral message. In local/test mode the response may include `testToken` so the diploma flow can be exercised without email delivery. `POST /api/auth/password-reset/confirm` validates token and password, updates the encoded password, marks the token used, and revokes sessions.

- [ ] **Step 4: Run auth tests and verify GREEN**

```powershell
.\mvnw.cmd test
```

Expected: exit code 0.

- [ ] **Step 5: Commit auth reset API**

```powershell
git add src/main/java src/test/java
git commit -m "feat: add password reset lifecycle"
```

## Task 3: Complete Marketplace Public DTOs

**Files:**
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/subject/Subject.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/subject/SubjectDto.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfile.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileDto.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/tutor/TutorProfileService.java`
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/config/MarketplaceSeedConfig.java`
- Modify: `backend/marketplace-service/src/test/java/com/tutor/marketplaceservice/tutor/TutorProfileServiceTest.java`

- [ ] **Step 1: Write failing DTO mapping tests**

Require real subject descriptions and tutor public fields currently borrowed from mock data:

```java
assertThat(dto.city()).isEqualTo("Москва");
assertThat(dto.education()).contains("МПГУ");
assertThat(dto.tags()).contains("Онлайн");
assertThat(dto.responseTimeMinutes()).isEqualTo(30);
```

- [ ] **Step 2: Run marketplace tests and verify RED**

```powershell
mvn -Dtest=TutorProfileServiceTest test
```

Expected: compilation failure because the new DTO fields are absent.

- [ ] **Step 3: Persist public profile metadata**

Add only fields displayed by the existing public pages: `city`, `education`, `highlights`, `tags`, `responseTimeMinutes`, and subject `description`. Derive `activeStudents`, lesson count, and repeat-rate from CRM later; represent them as nullable until a real source exists.

- [ ] **Step 4: Run marketplace tests and verify GREEN**

```powershell
mvn test
```

Expected: exit code 0.

- [ ] **Step 5: Commit public DTO completion**

```powershell
git add src/main/java src/test/java
git commit -m "feat: expose complete public marketplace profiles"
```

## Task 4: Remove Public and Auth Mock Fallbacks

**Files:**
- Modify: `frontend/src/shared/api/public.ts`
- Modify: `frontend/src/shared/api/public.test.ts`
- Modify: `frontend/src/pages/public/AuthPages.tsx`
- Modify: `frontend/src/pages/public/CatalogPage.tsx`
- Modify: `frontend/src/pages/public/TutorProfilePage.tsx`
- Modify: `frontend/src/shared/types.ts`

- [ ] **Step 1: Write failing adapter tests**

Assert that subjects and tutor profiles map only backend fields, unavailable slots reject instead of becoming empty silently, auth reset uses `/auth/password-reset`, and registration has no subject fallback:

```ts
expect(apiRequest).toHaveBeenCalledWith('/marketplace/subjects');
expect(result[0]).toMatchObject({ title: 'Математика', description: 'Алгебра и геометрия' });
expect(result[0]).not.toHaveProperty('mock');
```

- [ ] **Step 2: Run public tests and verify RED**

```powershell
npm test -- src/shared/api/public.test.ts
```

Expected: failures from old fallback factories and mock imports.

- [ ] **Step 3: Refactor public adapters and page states**

Remove imports from `mock-data.ts`. Keep `getHomeContent()` and FAQ local by moving editorial constants into `src/shared/config/site.ts`. Map backend tutor fields directly. Make catalog, tutor profile, tutor registration subjects, forgot-password, and reset-password show loading, empty, retryable error, and success states.

- [ ] **Step 4: Run public tests and typecheck**

```powershell
npm test -- src/shared/api/public.test.ts
npm run typecheck
```

Expected: both exit 0.

- [ ] **Step 5: Commit public live integration**

```powershell
git add src/shared src/pages/public
git commit -m "feat: use live public and auth APIs"
```

## Task 5: Add Client and Tutor Overview APIs

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/view/ClientOverviewPageDto.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/view/TutorOverviewPageDto.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/ClientCrmController.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/TutorCrmController.java`
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/view/PageViewServiceTest.java`

- [ ] **Step 1: Write failing overview aggregation tests**

Require overview responses to derive application counts, next lesson, unread conversation summary, tutor request counts, students, and calendar items from repositories:

```java
var overview = service.getClientOverview(clientId);
assertThat(overview.activeApplications()).isEqualTo(1);
assertThat(overview.upcomingLessons()).hasSize(1);
```

- [ ] **Step 2: Run PageViewService tests and verify RED**

```powershell
mvn -Dtest=PageViewServiceTest test
```

Expected: compilation failure for missing overview methods.

- [ ] **Step 3: Implement overview DTOs and endpoints**

Add `GET /api/client/dashboard` and `GET /api/tutor/dashboard`. Reuse repository queries and existing status rules; do not store dashboard snapshots.

- [ ] **Step 4: Run CRM tests and verify GREEN**

```powershell
mvn test
```

Expected: exit code 0.

- [ ] **Step 5: Commit CRM overview APIs**

```powershell
git add src/main/java src/test/java
git commit -m "feat: add client and tutor overview APIs"
```

## Task 6: Finish Missing CRM Mutations

**Files:**
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/service/CreateStudentRequest.java`
- Create: `backend/crm-service/src/main/java/com/tutor/crmservice/service/CreateProgressReportRequest.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/student/StudentCardService.java`
- Modify: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/TutorCrmController.java`
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/application/ApplicationLifecycleServiceTest.java`
- Modify: `backend/crm-service/src/test/java/com/tutor/crmservice/message/MessageServiceTest.java`
- Create: `backend/crm-service/src/test/java/com/tutor/crmservice/student/StudentCardServiceTest.java`

- [ ] **Step 1: Write failing mutation tests**

Cover manual student creation, persisted parent reports, ownership checks, and invalid message thread access.

```java
@Test
void rejectsSendingMessageToAnotherTutorsThread() {
    assertThatThrownBy(() -> service.sendTutorMessage(otherTutorId, threadId, "Здравствуйте"))
            .isInstanceOf(ResourceNotFoundException.class);
}
```

- [ ] **Step 2: Run focused tests and verify RED**

```powershell
mvn -Dtest=StudentCardServiceTest,MessageServiceTest,ApplicationLifecycleServiceTest test
```

Expected: missing methods or failing ownership assertions.

- [ ] **Step 3: Implement the smallest persisted mutations**

Add `POST /api/tutor/students` and `POST /api/tutor/students/{id}/reports`. Keep report data in the CRM-owned student domain. Return domain DTOs that can be reloaded immediately.

- [ ] **Step 4: Run CRM tests and verify GREEN**

```powershell
mvn test
```

Expected: exit code 0.

- [ ] **Step 5: Commit CRM mutations**

```powershell
git add src/main/java src/test/java
git commit -m "feat: complete tutor CRM mutations"
```

## Task 7: Remove Client Workspace Fallbacks

**Files:**
- Modify: `frontend/src/shared/api/client.ts`
- Modify: `frontend/src/shared/api/client.test.ts`
- Modify: `frontend/src/pages/app/ClientOverviewPage.tsx`
- Modify: `frontend/src/pages/app/ClientApplicationsPage.tsx`
- Modify: `frontend/src/pages/app/ClientApplicationDetailsPage.tsx`
- Modify: `frontend/src/pages/app/ClientSchedulePage.tsx`
- Modify: `frontend/src/pages/app/ClientMessagesPage.tsx`
- Modify: `frontend/src/pages/app/ClientFavoritesPage.tsx`
- Modify: `frontend/src/pages/app/ClientHistoryPage.tsx`
- Modify: `frontend/src/pages/app/ClientSettingsPage.tsx`

- [ ] **Step 1: Write failing live client tests**

Assert exact endpoints, no third fallback argument, empty DTO handling, and mutation reload behavior.

```ts
it('loads an empty applications page from the backend without invented rows', async () => {
  apiRequest.mockResolvedValue({ metrics: [], tabs: [], applications: [] });
  const result = await getClientApplicationsPage();
  expect(result.applications).toEqual([]);
  expect(apiRequest).toHaveBeenCalledWith('/client/applications');
});
```

- [ ] **Step 2: Run client tests and verify RED**

```powershell
npm test -- src/shared/api/client.test.ts
```

Expected: failures because fallback factories and invented initial state still exist.

- [ ] **Step 3: Convert all client pages to explicit async state**

Initialize data as `null`, not fabricated DTOs. Render skeleton while loading, instructional empty state for empty arrays, retry action on failure, and content only after successful responses.

- [ ] **Step 4: Run client tests and typecheck**

```powershell
npm test -- src/shared/api/client.test.ts src/pages/app/client-schedule-helpers.test.ts
npm run typecheck
```

Expected: exit code 0.

- [ ] **Step 5: Commit client workspace integration**

```powershell
git add src/shared/api/client* src/pages/app/Client*
git commit -m "feat: use live client workspace data"
```

## Task 8: Remove Tutor Workspace Fallbacks

**Files:**
- Modify: `frontend/src/shared/api/tutor.ts`
- Modify: `frontend/src/shared/api/tutor.test.ts`
- Modify: `frontend/src/pages/app/TutorOverviewPage.tsx`
- Modify: `frontend/src/pages/app/TutorRequestsPage.tsx`
- Modify: `frontend/src/pages/app/TutorCalendarPage.tsx`
- Modify: `frontend/src/pages/app/TutorStudentsPage.tsx`
- Modify: `frontend/src/pages/app/TutorMessagesPage.tsx`
- Modify: `frontend/src/pages/app/TutorWorkspaceProfilePage.tsx`
- Modify: `frontend/src/pages/app/TutorSettingsPage.tsx`

- [ ] **Step 1: Write failing tutor API tests**

Assert live dashboard, requests, calendar, students, messages, profile, settings, availability, and mutations without fallback factories.

```ts
it('loads tutor requests from the live endpoint', async () => {
  apiRequest.mockResolvedValue({ requests: [], funnel: {} });
  await getTutorRequests();
  expect(apiRequest).toHaveBeenCalledWith('/tutor/requests');
});
```

- [ ] **Step 2: Run tutor tests and verify RED**

```powershell
npm test -- src/shared/api/tutor.test.ts
```

Expected: failures from mock imports and fallback arguments.

- [ ] **Step 3: Refactor tutor pages**

Remove all `mock-data.ts` imports, including initial calendar, messages, students, and requests state. Preserve optimistic request actions only with rollback/reload after API failure.

- [ ] **Step 4: Run tutor tests and typecheck**

```powershell
npm test -- src/shared/api/tutor.test.ts src/pages/app/tutor-calendar-helpers.test.ts src/pages/app/tutor-workday-helpers.test.ts
npm run typecheck
```

Expected: exit code 0.

- [ ] **Step 5: Commit tutor workspace integration**

```powershell
git add src/shared/api/tutor* src/pages/app/Tutor*
git commit -m "feat: use live tutor workspace data"
```

## Task 9: Make Subscription Pages Entirely Live

**Files:**
- Modify: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionController.java`
- Create: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/CheckoutPageDto.java`
- Modify: `backend/subscription-service/src/main/java/com/tutor/subscriptionservice/subscription/SubscriptionService.java`
- Modify: `backend/subscription-service/src/test/java/com/tutor/subscriptionservice/subscription/SubscriptionServiceTest.java`
- Modify: `frontend/src/shared/api/tutor.ts`
- Modify: `frontend/src/pages/app/TutorSubscriptionPage.tsx`
- Modify: `frontend/src/pages/app/TutorTariffCheckoutPage.tsx`
- Modify: `frontend/src/shared/api/tutor.test.ts`

- [ ] **Step 1: Write failing checkout read-model tests**

Require `GET /api/subscriptions/checkout/{paymentId}` to return persisted payment, tariff, amount, status, confirmation URL, and test-mode flag.

- [ ] **Step 2: Run subscription tests and verify RED**

```powershell
mvn -Dtest=SubscriptionServiceTest test
```

Expected: missing checkout query behavior.

- [ ] **Step 3: Implement checkout read model and frontend mapping**

Remove `/tutor/subscription/checkout` mock endpoint usage. Build checkout UI from `/subscriptions/current`, `/subscriptions/tariffs`, and the persisted payment endpoint. Keep all YooKassa operations test-only.

- [ ] **Step 4: Verify backend and frontend**

```powershell
mvn test
```

From `frontend`:

```powershell
npm test -- src/shared/api/tutor.test.ts
npm run typecheck
```

Expected: all commands exit 0.

- [ ] **Step 5: Commit subscription changes in each repository**

Subscription service:

```powershell
git add src/main/java src/test/java
git commit -m "feat: expose persisted checkout state"
```

Frontend:

```powershell
git add src/shared/api/tutor* src/pages/app/TutorSubscriptionPage.tsx src/pages/app/TutorTariffCheckoutPage.tsx
git commit -m "feat: use live subscription checkout data"
```

## Task 10: Add Moderator Overview and Correct Moderation Targets

**Files:**
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/page/AdminPageDtos.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/page/AdminPageService.java`
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/moderation/ModeratorPageController.java`
- Modify: `backend/admin-service/src/test/java/com/tutor/adminservice/page/ModeratorQueueServiceTest.java`
- Modify: `backend/admin-service/src/test/java/com/tutor/adminservice/moderation/ModeratorPageControllerTest.java`
- Create: `frontend/src/pages/app/ModeratorOverviewPage.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/shared/api/admin.ts`
- Modify: `frontend/src/shared/api/admin.test.ts`

- [ ] **Step 1: Write failing moderator overview tests**

Require queue counts, complaint summary, review/profile summary, and recent decisions from persisted sources. Add a test that profile rows cannot be sent to review-only endpoints.

```java
@Test
void dashboardReportsPersistedQueueCounts() {
    var page = service.moderatorDashboard();
    assertThat(page.pendingComplaints()).isEqualTo(2);
    assertThat(page.pendingContent()).isEqualTo(1);
}
```

- [ ] **Step 2: Run admin tests and verify RED**

```powershell
mvn -Dtest=ModeratorQueueServiceTest,ModeratorPageControllerTest test
```

Expected: missing `/api/moderator/dashboard` and incorrect entity-type handling.

- [ ] **Step 3: Implement moderator overview and typed decisions**

Add `GET /api/moderator/dashboard`. Return an explicit item type and route review decisions to review moderation; profile decisions route to tutor-profile moderation endpoints. Audit every decision.

- [ ] **Step 4: Wire the real frontend overview**

Replace `SectionPlaceholderPage` for moderator index with `ModeratorOverviewPage`. Remove moderator mock fallbacks and render empty/error states.

- [ ] **Step 5: Verify and commit**

Admin service:

```powershell
mvn test
git add src/main/java src/test/java
git commit -m "feat: add moderator overview"
```

Frontend:

```powershell
npm test -- src/shared/api/admin.test.ts src/pages/app/admin-moderation-helpers.test.ts
npm run typecheck
git add src/App.tsx src/shared/api/admin* src/pages/app/Moderator*
git commit -m "feat: complete moderator workspace"
```

## Task 11: Remove Admin Mock Fallbacks and Preserve Partial Failures

**Files:**
- Modify: `frontend/src/shared/api/admin.ts`
- Modify: `frontend/src/shared/api/admin.test.ts`
- Modify: `frontend/src/pages/app/AdminDashboardPage.tsx`
- Modify: `frontend/src/pages/app/AdminUsersPage.tsx`
- Modify: `frontend/src/pages/app/AdminSiteStatsPage.tsx`
- Modify: `frontend/src/pages/app/AdminSubscriptionsPage.tsx`
- Modify: `frontend/src/pages/app/AdminAuditPage.tsx`
- Modify: `frontend/src/pages/app/AdminSettingsPage.tsx`
- Modify: `frontend/src/pages/app/admin-live-helpers.ts`

- [ ] **Step 1: Write failing admin live tests**

Assert every admin function calls the live endpoint without fallback data and pages distinguish an empty response from a rejected request.

```ts
it('propagates dashboard API failures', async () => {
  apiRequest.mockRejectedValue(new Error('admin-service unavailable'));
  await expect(getAdminDashboard()).rejects.toThrow('admin-service unavailable');
});
```

- [ ] **Step 2: Run admin frontend tests and verify RED**

```powershell
npm test -- src/shared/api/admin.test.ts src/pages/app/admin-moderation-helpers.test.ts
```

Expected: failures from `mock-data.ts` imports and fallback arguments.

- [ ] **Step 3: Refactor admin pages**

Use backend-provided partial availability signals. Keep presentation-only empty DTO builders, but invoke them only for successful empty responses, never inside API adapters and never after errors.

- [ ] **Step 4: Run admin tests and typecheck**

```powershell
npm test -- src/shared/api/admin.test.ts src/pages/app/admin-moderation-helpers.test.ts
npm run typecheck
```

Expected: exit code 0.

- [ ] **Step 5: Commit admin live integration**

```powershell
git add src/shared/api/admin* src/pages/app/Admin* src/pages/app/admin-live-helpers.ts
git commit -m "feat: use live admin workspace data"
```

## Task 12: Remove Remaining Production Mock Data

**Files:**
- Delete: `frontend/src/shared/api/mock-data.ts`
- Modify: every production import reported by `rg`
- Keep: test-local fixtures in `*.test.ts`

- [ ] **Step 1: Add a production-source guard test**

Create `frontend/src/shared/api/no-runtime-mocks.test.ts` that scans `src` and fails when a non-test file imports `mock-data` or passes a third argument to `apiRequest`.

- [ ] **Step 2: Run the guard and verify RED**

```powershell
npm test -- src/shared/api/no-runtime-mocks.test.ts
```

Expected: FAIL listing remaining production imports.

- [ ] **Step 3: Move editorial constants and delete mock-data**

Move home/FAQ copy to `src/shared/config/site.ts`. Replace all mutable fixtures with empty initial state or backend results. Delete `mock-data.ts`.

- [ ] **Step 4: Run all frontend unit tests**

```powershell
npm test
npm run typecheck
npm run build
```

Expected: all commands exit 0.

- [ ] **Step 5: Commit mock removal**

```powershell
git add src
git commit -m "refactor: remove runtime mock data"
```

## Task 13: Complete Responsive and Accessible Layouts

**Files:**
- Modify: `frontend/src/components/layout.tsx`
- Modify: `frontend/src/components/primitives.tsx`
- Modify: `frontend/src/styles/global.css`
- Modify: affected page components under `frontend/src/pages`
- Create: `frontend/tests/responsive.spec.ts`
- Create: `frontend/playwright.config.ts`

- [ ] **Step 1: Write failing responsive browser checks**

For public, client, tutor, admin, and moderator representative routes, assert no horizontal document overflow at `1440`, `1024`, `768`, and `390` widths:

```ts
expect(await page.evaluate(() => document.documentElement.scrollWidth))
  .toBeLessThanOrEqual(await page.evaluate(() => document.documentElement.clientWidth));
```

Also assert mobile navigation is keyboard reachable and tables have either a mobile representation or an accessible scroll region.

- [ ] **Step 2: Run responsive tests and verify RED**

With the Docker stack running:

```powershell
npm run test:e2e -- tests/responsive.spec.ts
```

Expected: failures identify overflowing routes and inaccessible controls.

- [ ] **Step 3: Implement structural breakpoints**

Add an accessible workspace navigation toggle below tablet width; collapse public header actions; convert split messaging views to list/detail flow; provide labeled table scrolling or row cards; make calendar controls wrap without clipping; remove pure-white modal/input colors in favor of existing OKLCH tokens.

- [ ] **Step 4: Run responsive tests and frontend checks**

```powershell
npm run test:e2e -- tests/responsive.spec.ts
npm test
npm run typecheck
npm run build
```

Expected: all commands exit 0.

- [ ] **Step 5: Commit responsive work**

```powershell
git add src tests playwright.config.ts
git commit -m "fix: make all routes responsive and accessible"
```

## Task 14: Full Docker Regression and Final Commits

**Files:**
- Modify when evidence requires: `docker-compose.yml`
- Modify when routing requires: `frontend/gateway/nginx.conf`
- Modify when local parity requires: `frontend/scripts/serve.mjs`
- Create: `frontend/tests/role-journeys.spec.ts`

- [ ] **Step 1: Write principal role journeys**

Cover:

1. Client registration or login, catalog, tutor request, client application.
2. Tutor login, request acceptance, lesson creation, notification.
3. Moderator login, dashboard, complaint or review decision.
4. Admin login, users, audit, subscription snapshot.

```ts
test('client submits a tutor request and sees the application', async ({ page }) => {
  await page.goto('/auth/sign-in');
  await page.getByLabel('Email').fill(clientEmail);
  await page.getByLabel('Пароль').fill(clientPassword);
  await page.getByRole('button', { name: 'Войти' }).click();
  await page.goto('/catalog');
  await page.getByRole('link', { name: /профиль/i }).first().click();
  await page.getByRole('button', { name: /отправить заявку/i }).click();
  await expect(page).toHaveURL(/\/app\/client\/applications/);
});
```

- [ ] **Step 2: Rebuild the complete stack**

From `C:\JavaMy\newDiploma`:

```powershell
docker compose up --build
```

Expected: gateway at `http://localhost:4173`; PostgreSQL, Kafka, and all six services start successfully.

- [ ] **Step 3: Run every repository test suite**

```powershell
.\backend\auth-service\mvnw.cmd -f .\backend\auth-service\pom.xml test
mvn -f .\backend\marketplace-service\pom.xml test
mvn -f .\backend\crm-service\pom.xml test
mvn -f .\backend\notification-service\pom.xml test
mvn -f .\backend\admin-service\pom.xml test
mvn -f .\backend\subscription-service\pom.xml test
```

From `frontend`:

```powershell
npm test
npm run typecheck
npm run build
npm run test:e2e
```

Expected: every command exits 0.

- [ ] **Step 4: Verify browser routes and target widths**

Use the in-app Browser against `http://localhost:4173`. Verify all routes from `src/App.tsx`, role-protected redirects, empty states, errors, mutations, and widths `1440`, `1024`, `768`, and `390`.

- [ ] **Step 5: Confirm repository cleanliness**

```powershell
$repos = Get-ChildItem -Path . -Directory -Force -Recurse -Filter .git | ForEach-Object { $_.Parent.FullName }
foreach ($repo in $repos) {
  git -C $repo status --short --branch
}
```

Expected: all seven repositories are on `feature/live-api-responsive` with no uncommitted files.

## Implementation Order and Checkpoints

1. Tasks 1-4: public/auth journey works without runtime mocks.
2. Tasks 5-7: client journey works end to end.
3. Tasks 6, 8, 9: tutor CRM and subscription work end to end.
4. Tasks 10-11: admin and moderator workspaces are complete.
5. Tasks 12-14: global mock guard, responsive audit, Docker regression.

Do not begin the next checkpoint while the current checkpoint has failing focused tests.
