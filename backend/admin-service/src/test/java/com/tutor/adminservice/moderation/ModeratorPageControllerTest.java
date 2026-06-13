package com.tutor.adminservice.moderation;

import com.tutor.adminservice.audit.AuditLogService;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.page.AdminPageDtos;
import com.tutor.adminservice.page.AdminPageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeratorPageControllerTest {

    @Mock
    private AdminPageService adminPageService;

    @Mock
    private MarketplaceServiceClient marketplaceClient;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ModeratorPageController controller;

    @Test
    void reviewModerationDelegatesToAdminPageService() {
        var page = new AdminPageDtos.ModeratorReviewPage(
                List.of(),
                List.of(),
                List.of(),
                new AdminPageDtos.ModeratorSelectedProfile("Нет анкет", "Пусто", "--", "Очередь пуста", "Нет данных", "-", "Низкий", List.of(), "0%", "Нет профилей для проверки."),
                List.of(),
                List.of(),
                new AdminPageDtos.ModeratorDecision("Решение модератора", "Действие применяется только к поддерживаемой сущности.", List.of("live"))
        );
        when(adminPageService.moderatorReviewPage()).thenReturn(page);

        assertSame(page, controller.reviewModeration());
    }

    @Test
    void dashboardDelegatesToAdminPageService() {
        var page = new AdminPageDtos.ModeratorDashboard(
                2,
                1,
                3,
                6,
                List.of()
        );
        when(adminPageService.moderatorDashboard()).thenReturn(page);

        assertSame(page, controller.dashboard());
    }

    @Test
    void approveReviewUpdatesMarketplaceBeforeWritingAudit() {
        var reviewId = UUID.randomUUID();
        var moderatorId = UUID.randomUUID();

        var response = controller.approve(reviewId, moderatorId);

        assertEquals(reviewId.toString(), response.get("profileId"));
        assertEquals("approve", response.get("decision"));
        assertEquals("approved", response.get("status"));

        InOrder ordered = inOrder(marketplaceClient, auditLogService);
        ordered.verify(marketplaceClient).updateReviewStatus(reviewId, "PUBLISHED");
        ordered.verify(auditLogService).log(moderatorId, "MODERATION_APPROVED", "REVIEW", reviewId, null);
        verifyNoMoreInteractions(marketplaceClient, auditLogService);
    }

    @Test
    void rejectReviewUpdatesMarketplaceBeforeWritingAudit() {
        var reviewId = UUID.randomUUID();
        var moderatorId = UUID.randomUUID();

        var response = controller.reject(reviewId, moderatorId);

        assertEquals(reviewId.toString(), response.get("profileId"));
        assertEquals("reject", response.get("decision"));
        assertEquals("rejected", response.get("status"));

        InOrder ordered = inOrder(marketplaceClient, auditLogService);
        ordered.verify(marketplaceClient).updateReviewStatus(reviewId, "REJECTED");
        ordered.verify(auditLogService).log(moderatorId, "MODERATION_REJECTED", "REVIEW", reviewId, null);
        verifyNoMoreInteractions(marketplaceClient, auditLogService);
    }

    @Test
    void downstreamFailureDoesNotWriteModerationAudit() {
        var reviewId = UUID.randomUUID();
        var moderatorId = UUID.randomUUID();
        doThrow(new RuntimeException("marketplace unavailable"))
                .when(marketplaceClient).updateReviewStatus(reviewId, "PUBLISHED");

        assertThrows(RuntimeException.class, () -> controller.approve(reviewId, moderatorId));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void approveTutorProfileUpdatesProfileBeforeWritingAudit() {
        var profileId = UUID.randomUUID();
        var moderatorId = UUID.randomUUID();

        var response = controller.approveProfile(profileId, moderatorId);

        assertEquals(profileId.toString(), response.get("profileId"));
        assertEquals("approve", response.get("decision"));
        InOrder ordered = inOrder(marketplaceClient, auditLogService);
        ordered.verify(marketplaceClient).updateTutorProfileStatus(profileId, "ACTIVE");
        ordered.verify(auditLogService).log(moderatorId, "MODERATION_APPROVED", "TUTOR_PROFILE", profileId, null);
        verifyNoMoreInteractions(marketplaceClient, auditLogService);
    }

    @Test
    void rejectTutorProfileUpdatesProfileBeforeWritingAudit() {
        var profileId = UUID.randomUUID();
        var moderatorId = UUID.randomUUID();

        controller.rejectProfile(profileId, moderatorId);

        InOrder ordered = inOrder(marketplaceClient, auditLogService);
        ordered.verify(marketplaceClient).updateTutorProfileStatus(profileId, "HIDDEN");
        ordered.verify(auditLogService).log(moderatorId, "MODERATION_REJECTED", "TUTOR_PROFILE", profileId, null);
        verifyNoMoreInteractions(marketplaceClient, auditLogService);
    }
}
