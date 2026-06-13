package com.tutor.adminservice.complaint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.tutor.adminservice.client.MarketplaceServiceClient;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private MarketplaceServiceClient marketplaceServiceClient;

    @InjectMocks
    private ComplaintService complaintService;

    @Test
    void shouldCreateComplaint() {
        var request = new CreateComplaintRequest(
                UUID.randomUUID(),
                ComplaintCategory.SPAM,
                "Spam content",
                "Details",
                ComplaintPriority.HIGH,
                ComplaintTargetType.REVIEW,
                UUID.randomUUID(),
                "Review text snapshot"
        );
        var reporterId = UUID.randomUUID();

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = complaintService.create(request, reporterId);

        assertEquals(request.reportedUserId(), result.getReportedUserId());
        assertEquals(ComplaintTargetType.REVIEW, result.getTargetType());
        assertEquals(request.targetId(), result.getTargetId());
        assertEquals("Review text snapshot", result.getTargetSnapshot());
        assertEquals(reporterId, result.getReporterUserId());
        assertEquals(ComplaintStatus.PENDING, result.getStatus());
        assertEquals(ComplaintPriority.HIGH, result.getPriority());
    }

    @Test
    void shouldModerateComplaint() {
        var complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .status(ComplaintStatus.PENDING)
                .targetType(ComplaintTargetType.REVIEW)
                .targetId(UUID.randomUUID())
                .build();
        var moderatorId = UUID.randomUUID();

        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new ModerateComplaintRequest(
                ComplaintStatus.APPROVED,
                "Confirmed spam",
                ComplaintModerationAction.HIDE_REVIEW
        );
        var result = complaintService.moderate(complaint.getId(), request, moderatorId);

        assertEquals(ComplaintStatus.APPROVED, result.getStatus());
        assertEquals("Confirmed spam", result.getResolutionNotes());
        assertEquals(moderatorId, result.getModeratorId());
        assertNotNull(result.getModeratedAt());
        verify(marketplaceServiceClient).updateReviewStatus(complaint.getTargetId(), "REJECTED");
    }

    @Test
    void approvedReviewComplaintCanRestrictReportedUserFromPublishingReviews() {
        var reportedUserId = UUID.randomUUID();
        var complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .reportedUserId(reportedUserId)
                .status(ComplaintStatus.PENDING)
                .targetType(ComplaintTargetType.REVIEW)
                .targetId(UUID.randomUUID())
                .build();
        var moderatorId = UUID.randomUUID();

        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new ModerateComplaintRequest(
                ComplaintStatus.APPROVED,
                "Fake reviews",
                ComplaintModerationAction.RESTRICT_REVIEW_PUBLICATION
        );

        complaintService.moderate(complaint.getId(), request, moderatorId);

        verify(marketplaceServiceClient).restrictReviewPublication(reportedUserId, moderatorId, "Fake reviews");
        verify(marketplaceServiceClient, never()).updateReviewStatus(complaint.getTargetId(), "REJECTED");
    }

    @Test
    void approvedTutorProfileComplaintCanHideTutorProfile() {
        var tutorProfileId = UUID.randomUUID();
        var complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .status(ComplaintStatus.PENDING)
                .targetType(ComplaintTargetType.TUTOR_PROFILE)
                .targetId(tutorProfileId)
                .build();
        var moderatorId = UUID.randomUUID();

        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new ModerateComplaintRequest(
                ComplaintStatus.APPROVED,
                "Profile is misleading",
                ComplaintModerationAction.HIDE_TUTOR_PROFILE
        );

        complaintService.moderate(complaint.getId(), request, moderatorId);

        verify(marketplaceServiceClient).hideTutorProfile(tutorProfileId);
    }

    @Test
    void rejectedComplaintDoesNotCallMarketplaceActions() {
        var complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .reportedUserId(UUID.randomUUID())
                .status(ComplaintStatus.PENDING)
                .targetType(ComplaintTargetType.REVIEW)
                .targetId(UUID.randomUUID())
                .build();
        var moderatorId = UUID.randomUUID();

        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new ModerateComplaintRequest(
                ComplaintStatus.REJECTED,
                "No violation",
                ComplaintModerationAction.RESTRICT_REVIEW_PUBLICATION
        );

        complaintService.moderate(complaint.getId(), request, moderatorId);

        verify(marketplaceServiceClient, never()).restrictReviewPublication(any(), any(), any());
        verify(marketplaceServiceClient, never()).updateReviewStatus(any(), any());
        verify(marketplaceServiceClient, never()).hideTutorProfile(any());
    }
}
