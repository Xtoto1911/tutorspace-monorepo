package com.tutor.adminservice.complaint;

import com.tutor.adminservice.client.MarketplaceServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final MarketplaceServiceClient marketplaceServiceClient;

    public Complaint create(CreateComplaintRequest request, UUID reporterUserId) {
        var complaint = Complaint.builder()
                .reportedUserId(request.reportedUserId())
                .reporterUserId(reporterUserId)
                .targetType(request.targetType() == null ? ComplaintTargetType.USER : request.targetType())
                .targetId(request.targetId())
                .targetSnapshot(request.targetSnapshot())
                .category(request.category())
                .reason(request.reason())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : ComplaintPriority.MEDIUM)
                .build();
        return complaintRepository.save(complaint);
    }

    public Complaint moderate(UUID id, ModerateComplaintRequest request, UUID moderatorId) {
        var complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        complaint.setStatus(request.status());
        complaint.setResolutionNotes(request.resolutionNotes());
        complaint.setModeratorId(moderatorId);
        complaint.setModeratedAt(Instant.now());

        if (request.status() == ComplaintStatus.APPROVED) {
            applyApprovedAction(complaint, request, moderatorId);
        }
        return complaintRepository.save(complaint);
    }

    private void applyApprovedAction(Complaint complaint, ModerateComplaintRequest request, UUID moderatorId) {
        ComplaintModerationAction action = request.action() != null
                ? request.action()
                : defaultActionFor(complaint.getTargetType());

        switch (action) {
            case HIDE_REVIEW -> {
                if (complaint.getTargetType() == ComplaintTargetType.REVIEW && complaint.getTargetId() != null) {
                    marketplaceServiceClient.updateReviewStatus(complaint.getTargetId(), "REJECTED");
                }
            }
            case RESTRICT_REVIEW_PUBLICATION -> {
                if (complaint.getReportedUserId() != null) {
                    marketplaceServiceClient.restrictReviewPublication(
                            complaint.getReportedUserId(),
                            moderatorId,
                            request.resolutionNotes()
                    );
                }
            }
            case HIDE_TUTOR_PROFILE -> {
                if (complaint.getTargetType() == ComplaintTargetType.TUTOR_PROFILE && complaint.getTargetId() != null) {
                    marketplaceServiceClient.hideTutorProfile(complaint.getTargetId());
                }
            }
            case DISMISS -> {
            }
        }
    }

    private ComplaintModerationAction defaultActionFor(ComplaintTargetType targetType) {
        return targetType == ComplaintTargetType.REVIEW
                ? ComplaintModerationAction.HIDE_REVIEW
                : ComplaintModerationAction.DISMISS;
    }

    public List<Complaint> listAll() {
        return complaintRepository.findAll();
    }

    public List<Complaint> listByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Complaint getById(UUID id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
    }
}
