package com.tutor.adminservice.page;

import com.tutor.adminservice.audit.AuditLog;
import com.tutor.adminservice.audit.AuditLogRepository;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.client.SubscriptionAdminClient;
import com.tutor.adminservice.complaint.Complaint;
import com.tutor.adminservice.complaint.ComplaintRepository;
import com.tutor.adminservice.complaint.ComplaintStatus;
import com.tutor.adminservice.complaint.ComplaintTargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPageServiceTest {

    @Mock
    private AuthServiceClient authClient;

    @Mock
    private MarketplaceServiceClient marketplaceClient;

    @Mock
    private SubscriptionAdminClient subscriptionClient;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminPageService service;

    @Test
    void dashboardUsesRealUsersAndPendingComplaintsWhenAvailable() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenReturn(List.of(
                new AuthServiceClient.AdminUserDto(UUID.randomUUID(), "admin@tutorspace.local", "ROLE_ADMIN", "ACTIVE")
        ));
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of(
                Complaint.builder()
                        .id(UUID.randomUUID())
                        .reportedUserId(UUID.randomUUID())
                        .reporterUserId(UUID.randomUUID())
                        .targetType(ComplaintTargetType.REVIEW)
                        .reason("Spam")
                        .status(ComplaintStatus.PENDING)
                        .createdAt(Instant.parse("2026-05-20T08:00:00Z"))
                        .build()
        ));
        when(auditLogRepository.findAll()).thenReturn(List.of());

        var page = service.dashboard(token);

        assertEquals("1", page.metrics().get(0).value());
        assertEquals(1, page.moderationQueue().size());
        assertEquals("REVIEW", page.moderationQueue().get(0).type());
    }

    @Test
    void dashboardSurvivesAuthClientFailure() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenThrow(new RuntimeException("auth unavailable"));
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of());
        when(auditLogRepository.findAll()).thenReturn(List.of());

        var page = service.dashboard(token);

        assertEquals("0", page.metrics().get(0).value());
        assertTrue(page.services().stream().anyMatch(service -> service.name().equals("auth-service")
                && service.tone().equals("red")));
    }

    @Test
    void dashboardMarksSubscriptionMetricUnavailableWhenSnapshotFails() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenReturn(List.of());
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of());
        when(auditLogRepository.findAll()).thenReturn(List.of());
        when(subscriptionClient.snapshot()).thenThrow(new RuntimeException("subscription unavailable"));

        var page = service.dashboard(token);

        assertTrue(page.metrics().stream()
                .anyMatch(metric -> metric.label().equals("Подписки") && metric.delta().contains("недоступен")));
    }

    @Test
    void subscriptionsPageUsesSnapshotAndDoesNotInventActions() {
        var tutorId = UUID.randomUUID();
        var subscriptionId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        when(subscriptionClient.snapshot()).thenReturn(new SubscriptionAdminClient.Snapshot(
                List.of(
                        new SubscriptionAdminClient.TariffItem("CRM_BASIC", "CRM Basic", 990, 30, true),
                        new SubscriptionAdminClient.TariffItem("CRM_PRO", "CRM Pro", 1990, 30, true)
                ),
                List.of(new SubscriptionAdminClient.SubscriptionItem(
                        subscriptionId,
                        tutorId,
                        "ACTIVE",
                        "CRM_BASIC",
                        Instant.parse("2026-06-20T08:00:00Z")
                )),
                List.of(
                        new SubscriptionAdminClient.PaymentItem(
                                paymentId,
                                tutorId,
                                "CRM_BASIC",
                                990,
                                "CONFIRMED",
                                Instant.parse("2026-05-20T08:00:00Z")
                        ),
                        new SubscriptionAdminClient.PaymentItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "CRM_PRO",
                                1990,
                                "CONFIRMED",
                                Instant.parse("2026-05-20T09:00:00Z")
                        ),
                        new SubscriptionAdminClient.PaymentItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "CRM_PRO",
                                1990,
                                "PENDING",
                                Instant.parse("2026-05-20T10:00:00Z")
                        )
                ),
                List.of(new SubscriptionAdminClient.PeriodItem(
                        UUID.randomUUID(),
                        subscriptionId,
                        paymentId,
                        "CRM_BASIC",
                        Instant.parse("2026-05-20T08:00:00Z"),
                        Instant.parse("2026-06-20T08:00:00Z")
                ))
        ));

        var page = service.subscriptionsPage();

        assertEquals("1", page.metrics().get(0).value());
        assertEquals(1, page.subscriptions().size());
        assertEquals("990 ₽", page.subscriptions().get(0).payment());
        assertTrue(page.webhookQueue().isEmpty());
        assertEquals(2, page.planSettings().size());
        assertEquals("990 ₽", page.revenueByPlan().get(0).value());
        assertEquals(33, page.revenueByPlan().get(0).percent());
        assertEquals("1990 ₽", page.revenueByPlan().get(1).value());
        assertEquals(67, page.revenueByPlan().get(1).percent());
    }

    @Test
    void siteStatsFinanceUsesSnapshotPaymentsWhenAvailable() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenReturn(List.of());
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of());
        when(auditLogRepository.findAll()).thenReturn(List.of());
        when(subscriptionClient.snapshot()).thenReturn(new SubscriptionAdminClient.Snapshot(
                List.of(),
                List.of(),
                List.of(
                        new SubscriptionAdminClient.PaymentItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "CRM_BASIC",
                                990,
                                "CONFIRMED",
                                Instant.parse("2026-05-20T08:00:00Z")
                        ),
                        new SubscriptionAdminClient.PaymentItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "CRM_PRO",
                                1990,
                                "CONFIRMED",
                                Instant.parse("2026-05-20T09:00:00Z")
                        )
                ),
                List.of()
        ));

        var page = service.siteStats(token);

        assertEquals("2980 ₽", page.finance().value());
    }

    @Test
    void subscriptionRowPaymentUsesLinkedPeriodPaymentInsteadOfTutorTariffSum() {
        var tutorId = UUID.randomUUID();
        var subscriptionId = UUID.randomUUID();
        var oldPaymentId = UUID.randomUUID();
        var currentPaymentId = UUID.randomUUID();
        when(subscriptionClient.snapshot()).thenReturn(new SubscriptionAdminClient.Snapshot(
                List.of(new SubscriptionAdminClient.TariffItem("CRM_BASIC", "CRM Basic", 990, 30, true)),
                List.of(new SubscriptionAdminClient.SubscriptionItem(
                        subscriptionId,
                        tutorId,
                        "ACTIVE",
                        "CRM_BASIC",
                        Instant.parse("2026-07-20T08:00:00Z")
                )),
                List.of(
                        new SubscriptionAdminClient.PaymentItem(
                                oldPaymentId,
                                tutorId,
                                "CRM_BASIC",
                                990,
                                "CONFIRMED",
                                Instant.parse("2026-05-20T08:00:00Z")
                        ),
                        new SubscriptionAdminClient.PaymentItem(
                                currentPaymentId,
                                tutorId,
                                "CRM_BASIC",
                                1990,
                                "CONFIRMED",
                                Instant.parse("2026-06-20T08:00:00Z")
                        )
                ),
                List.of(
                        new SubscriptionAdminClient.PeriodItem(
                                UUID.randomUUID(),
                                subscriptionId,
                                oldPaymentId,
                                "CRM_BASIC",
                                Instant.parse("2026-05-20T08:00:00Z"),
                                Instant.parse("2026-06-20T08:00:00Z")
                        ),
                        new SubscriptionAdminClient.PeriodItem(
                                UUID.randomUUID(),
                                subscriptionId,
                                currentPaymentId,
                                "CRM_BASIC",
                                Instant.parse("2026-06-20T08:00:00Z"),
                                Instant.parse("2026-07-20T08:00:00Z")
                        )
                )
        ));

        var page = service.subscriptionsPage();

        assertEquals("1990 ₽", page.subscriptions().get(0).payment());
    }

    @Test
    void revenueIgnoresPendingFailedAndCancelledPayments() {
        var token = "admin-token";
        var snapshot = new SubscriptionAdminClient.Snapshot(
                List.of(new SubscriptionAdminClient.TariffItem("CRM_BASIC", "CRM Basic", 990, 30, true)),
                List.of(),
                List.of(
                        new SubscriptionAdminClient.PaymentItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "CRM_BASIC",
                                990,
                                "PENDING",
                                Instant.parse("2026-05-20T08:00:00Z")
                        ),
                        new SubscriptionAdminClient.PaymentItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "CRM_BASIC",
                                990,
                                "FAILED",
                                Instant.parse("2026-05-20T09:00:00Z")
                        ),
                        new SubscriptionAdminClient.PaymentItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "CRM_BASIC",
                                990,
                                "CANCELLED",
                                Instant.parse("2026-05-20T10:00:00Z")
                        )
                ),
                List.of()
        );
        when(subscriptionClient.snapshot()).thenReturn(snapshot, snapshot);
        when(authClient.listUsers(token)).thenReturn(List.of());
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of());
        when(auditLogRepository.findAll()).thenReturn(List.of());

        var subscriptionsPage = service.subscriptionsPage();
        var siteStatsPage = service.siteStats(token);

        assertEquals("0 ₽", subscriptionsPage.revenueByPlan().get(0).value());
        assertEquals(0, subscriptionsPage.revenueByPlan().get(0).percent());
        assertEquals("0 ₽", siteStatsPage.finance().value());
    }

    @Test
    void subscriptionsPageMarksSnapshotFailureAsUnavailable() {
        when(subscriptionClient.snapshot()).thenThrow(new RuntimeException("subscription unavailable"));

        var page = service.subscriptionsPage();

        assertTrue(page.metrics().stream().anyMatch(metric -> metric.hint().contains("недоступен")));
        assertTrue(page.webhookQueue().stream().anyMatch(item -> item.event().contains("subscription-service")));
    }

    @Test
    void subscriptionOutagePlanSettingsUseUnavailableValues() {
        when(subscriptionClient.snapshot()).thenThrow(new RuntimeException("subscription unavailable"));

        var page = service.subscriptionsPage();

        assertTrue(page.planSettings().isEmpty());
    }

    @Test
    void siteStatsMarksSubscriptionSnapshotFailureAsUnavailable() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenReturn(List.of());
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of());
        when(subscriptionClient.snapshot()).thenThrow(new RuntimeException("subscription unavailable"));
        when(auditLogRepository.findAll()).thenReturn(List.of());

        var page = service.siteStats(token);

        assertTrue(page.metrics().stream().anyMatch(metric -> metric.meta().contains("недоступен")));
        assertTrue(page.finance().caption().contains("недоступен"));
    }

    @Test
    void usersPageAuthFailureMarksUnavailableDistinctFromSuccessfulEmptyUsers() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenReturn(List.of()).thenThrow(new RuntimeException("auth unavailable"));

        var emptyPage = service.usersPage(token);
        var failedPage = service.usersPage(token);

        assertEquals("live auth-service", emptyPage.metrics().get(0).meta());
        assertTrue(failedPage.metrics().get(0).meta().contains("недоступен"));
        assertTrue(failedPage.selected().riskTitle().contains("недоступен"));
    }

    @Test
    void siteStatsAuthFailureMarksUserMetricUnavailable() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenThrow(new RuntimeException("auth unavailable"));
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING)).thenReturn(List.of());
        when(subscriptionClient.snapshot()).thenReturn(new SubscriptionAdminClient.Snapshot(List.of(), List.of(), List.of(), List.of()));
        when(auditLogRepository.findAll()).thenReturn(List.of());

        var page = service.siteStats(token);

        assertTrue(page.metrics().stream()
                .anyMatch(metric -> metric.title().equals("Пользователи") && metric.meta().contains("недоступен")));
    }

    @Test
    void settingsAuthFailureMarksMetricUnavailableDistinctFromSuccessfulEmptyUsers() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenReturn(List.of()).thenThrow(new RuntimeException("auth unavailable"));
        when(subscriptionClient.snapshot()).thenReturn(new SubscriptionAdminClient.Snapshot(List.of(), List.of(), List.of(), List.of()));
        when(marketplaceClient.listTutorProfiles("ACTIVE")).thenReturn(List.of());

        var emptyPage = service.settings(token);
        var failedPage = service.settings(token);

        assertEquals("live auth-service", emptyPage.metrics().get(0).hint());
        assertTrue(failedPage.metrics().get(0).hint().contains("недоступен"));
        assertEquals("rose", failedPage.metrics().get(0).tone());
    }

    @Test
    void usersPageReturnsEmptySelectedCardWhenAuthHasNoUsers() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenReturn(List.of());

        var page = service.usersPage(token);

        assertTrue(page.users().isEmpty());
        assertEquals("Нет пользователей", page.selected().name());
        assertEquals("Нет пользователей", page.cards().audit().title());
    }

    @Test
    void usersPageSelectedUserExposesOnlySupportedActions() {
        var token = "admin-token";
        when(authClient.listUsers(token)).thenReturn(List.of(
                new AuthServiceClient.AdminUserDto(UUID.randomUUID(), "tutor@tutorspace.local", "ROLE_TUTOR", "ACTIVE")
        )).thenReturn(List.of(
                new AuthServiceClient.AdminUserDto(UUID.randomUUID(), "admin@tutorspace.local", "ROLE_ADMIN", "ACTIVE")
        )).thenReturn(List.of(
                new AuthServiceClient.AdminUserDto(UUID.randomUUID(), "client@tutorspace.local", "ROLE_CLIENT", "BLOCKED")
        ));

        var tutorPage = service.usersPage(token);
        var adminPage = service.usersPage(token);
        var blockedPage = service.usersPage(token);

        assertEquals(List.of("Заблокировать", "Сделать админом"), tutorPage.selected().actions());
        assertTrue(adminPage.selected().actions().isEmpty());
        assertEquals(List.of("Разблокировать", "Сделать админом"), blockedPage.selected().actions());
    }

    @Test
    void auditPageMapsActualAuditLogEntries() {
        var logId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        when(auditLogRepository.findAll()).thenReturn(List.of(
                AuditLog.builder()
                        .id(logId)
                        .adminUserId(adminId)
                        .actionType("USER_BLOCKED")
                        .targetType("USER")
                        .targetId(targetId)
                        .details("Policy violation")
                        .createdAt(Instant.parse("2026-05-20T08:00:00Z"))
                        .build()
        ));

        var page = service.auditPage();

        assertEquals(1, page.events().size());
        assertEquals(logId.toString(), page.events().get(0).id());
        assertEquals("USER_BLOCKED", page.events().get(0).action());
        assertEquals("Policy violation", page.selected().payload().get(0));
    }
}
