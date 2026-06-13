package com.tutor.adminservice.moderation;

import com.tutor.adminservice.audit.AuditLogService;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.page.AdminPageDtos;
import com.tutor.adminservice.page.AdminPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/moderator")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERATOR')")
public class ModeratorPageController {

    private final AdminPageService adminPageService;
    private final MarketplaceServiceClient marketplaceClient;
    private final AuditLogService auditLogService;

    @GetMapping("/dashboard")
    public AdminPageDtos.ModeratorDashboard dashboard() {
        return adminPageService.moderatorDashboard();
    }

    @GetMapping("/review-moderation")
    public AdminPageDtos.ModeratorReviewPage reviewModeration() {
        return adminPageService.moderatorReviewPage();
    }

    @PostMapping("/review-moderation/{id}/approve")
    public Map<String, String> approve(@PathVariable UUID id, @AuthenticationPrincipal UUID moderatorId) {
        marketplaceClient.updateReviewStatus(id, "PUBLISHED");
        auditLogService.log(moderatorId, "MODERATION_APPROVED", "REVIEW", id, null);
        return Map.of(
                "profileId", id.toString(),
                "decision", "approve",
                "status", "approved",
                "decidedAt", Instant.now().toString()
        );
    }

    @PostMapping("/review-moderation/{id}/reject")
    public Map<String, String> reject(@PathVariable UUID id, @AuthenticationPrincipal UUID moderatorId) {
        marketplaceClient.updateReviewStatus(id, "REJECTED");
        auditLogService.log(moderatorId, "MODERATION_REJECTED", "REVIEW", id, null);
        return Map.of(
                "profileId", id.toString(),
                "decision", "reject",
                "status", "rejected",
                "decidedAt", Instant.now().toString()
        );
    }

    @PostMapping("/profile-moderation/{id}/approve")
    public Map<String, String> approveProfile(@PathVariable UUID id, @AuthenticationPrincipal UUID moderatorId) {
        marketplaceClient.updateTutorProfileStatus(id, "ACTIVE");
        auditLogService.log(moderatorId, "MODERATION_APPROVED", "TUTOR_PROFILE", id, null);
        return decisionResponse(id, "approve", "approved");
    }

    @PostMapping("/profile-moderation/{id}/reject")
    public Map<String, String> rejectProfile(@PathVariable UUID id, @AuthenticationPrincipal UUID moderatorId) {
        marketplaceClient.updateTutorProfileStatus(id, "HIDDEN");
        auditLogService.log(moderatorId, "MODERATION_REJECTED", "TUTOR_PROFILE", id, null);
        return decisionResponse(id, "reject", "rejected");
    }

    private Map<String, String> decisionResponse(UUID id, String decision, String status) {
        return Map.of(
                "profileId", id.toString(),
                "decision", decision,
                "status", status,
                "decidedAt", Instant.now().toString()
        );
    }

}
