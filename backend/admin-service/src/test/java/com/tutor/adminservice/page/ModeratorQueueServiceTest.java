package com.tutor.adminservice.page;

import com.tutor.adminservice.audit.AuditLogRepository;
import com.tutor.adminservice.audit.AuditLog;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.client.SubscriptionAdminClient;
import com.tutor.adminservice.complaint.Complaint;
import com.tutor.adminservice.complaint.ComplaintStatus;
import com.tutor.adminservice.complaint.ComplaintRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeratorQueueServiceTest {

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
    void moderatorReviewPageUsesMarketplaceReviewAndProfileQueues() {
        var reviewId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var tutorUserId = UUID.randomUUID();
        when(marketplaceClient.listReviews("PENDING_MODERATION")).thenReturn(List.of(
                new MarketplaceServiceClient.ReviewModerationDto(
                        reviewId,
                        profileId,
                        UUID.randomUUID(),
                        5,
                        "Слишком рекламный отзыв",
                        "PENDING_MODERATION",
                        Instant.parse("2026-05-20T08:00:00Z"),
                        null,
                        "Клиент",
                        "Анна Иванова",
                        "anna-math"
                )
        ));
        when(marketplaceClient.listTutorProfiles("HIDDEN")).thenReturn(List.of(
                new MarketplaceServiceClient.TutorProfileModerationDto(
                        profileId,
                        tutorUserId,
                        "anna-math",
                        "Анна Иванова",
                        "Готовлю к ЕГЭ по математике",
                        7,
                        BigDecimal.valueOf(2500),
                        null,
                        4.8,
                        12,
                        "ACTIVE",
                        List.of(new MarketplaceServiceClient.SubjectDto(UUID.randomUUID(), "Математика", "math"))
                )
        ));

        var page = service.moderatorReviewPage();

        assertEquals("2", page.metrics().get(0).value());
        assertEquals(2, page.profiles().size());
        assertTrue(page.profiles().stream().anyMatch(row -> row.id().equals(reviewId.toString()) && row.itemType().equals("REVIEW") && row.type().equals("Отзыв") && row.name().equals("Отзыв о Анна Иванова")));
        assertTrue(page.profiles().stream().anyMatch(row -> row.id().equals(profileId.toString()) && row.itemType().equals("TUTOR_PROFILE") && row.type().equals("Анкета")));
        assertEquals(reviewId.toString(), page.selected().id());
        assertEquals("Всего", page.pipeline().get(0).label());
    }

    @Test
    void moderatorReviewPageSurvivesMarketplaceFailureWithEmptyQueue() {
        when(marketplaceClient.listReviews("PENDING_MODERATION")).thenThrow(new RuntimeException("marketplace unavailable"));
        when(marketplaceClient.listTutorProfiles("HIDDEN")).thenThrow(new RuntimeException("marketplace unavailable"));

        var page = service.moderatorReviewPage();

        assertEquals("0", page.metrics().get(0).value());
        assertTrue(page.profiles().isEmpty());
        assertEquals("Нет анкет", page.selected().id());
    }

    @Test
    void moderatorDashboardReportsPersistedQueuesAndRecentDecisions() {
        var complaint = new Complaint();
        complaint.setStatus(ComplaintStatus.PENDING);
        var decision = AuditLog.builder()
                .id(UUID.randomUUID())
                .adminUserId(UUID.randomUUID())
                .actionType("MODERATION_APPROVED")
                .targetType("REVIEW")
                .targetId(UUID.randomUUID())
                .createdAt(Instant.parse("2026-06-13T06:00:00Z"))
                .build();
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING))
                .thenReturn(List.of(complaint));
        when(marketplaceClient.listReviews("PENDING_MODERATION")).thenReturn(List.of(
                new MarketplaceServiceClient.ReviewModerationDto(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 4, "Текст",
                        "PENDING_MODERATION", Instant.now(), null, "Клиент", "Репетитор", "tutor"
                )
        ));
        when(marketplaceClient.listTutorProfiles("HIDDEN")).thenReturn(List.of(
                new MarketplaceServiceClient.TutorProfileModerationDto(
                        UUID.randomUUID(), UUID.randomUUID(), "tutor", "Репетитор", "Описание",
                        5, BigDecimal.valueOf(2000), null, 4.5, 3, "HIDDEN", List.of()
                )
        ));
        when(auditLogRepository.findAll()).thenReturn(List.of(decision));

        var dashboard = service.moderatorDashboard();

        assertEquals(1, dashboard.pendingComplaints());
        assertEquals(1, dashboard.pendingReviews());
        assertEquals(1, dashboard.pendingProfiles());
        assertEquals(3, dashboard.totalPending());
        assertEquals("MODERATION_APPROVED", dashboard.recentDecisions().getFirst().action());
        assertEquals("REVIEW", dashboard.recentDecisions().getFirst().targetType());
    }
}
