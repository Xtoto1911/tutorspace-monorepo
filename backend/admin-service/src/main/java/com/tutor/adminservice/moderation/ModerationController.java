package com.tutor.adminservice.moderation;

import com.tutor.adminservice.audit.AuditLogService;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.page.AdminPageDtos;
import com.tutor.adminservice.page.AdminPageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ModerationController {

    private final MarketplaceServiceClient marketplaceClient;
    private final AuthServiceClient authClient;
    private final AuditLogService auditLogService;
    private final AdminPageService adminPageService;

    @GetMapping("/dashboard")
    public AdminPageDtos.DashboardPage dashboard(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return adminPageService.dashboard(extractToken(authHeader));
    }

    @GetMapping("/alerts")
    public List<AdminPageDtos.Alert> alerts(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return adminPageService.dashboard(extractToken(authHeader)).alerts();
    }

    @GetMapping("/users")
    public AdminPageDtos.UsersPage users(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return adminPageService.usersPage(extractToken(authHeader));
    }

    @PostMapping("/users/moderators")
    public AuthServiceClient.AdminUserDto createModerator(@Valid @RequestBody CreateModeratorRequest request,
                                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                          @AuthenticationPrincipal UUID adminUserId) {
        var created = authClient.createModerator(request.email(), request.password(), extractToken(authHeader));
        auditLogService.log(adminUserId, "MODERATOR_CREATED", "USER", created.id(), created.email());
        return created;
    }

    @GetMapping("/site-stats")
    public AdminPageDtos.SiteStatsPage siteStats(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return adminPageService.siteStats(extractToken(authHeader));
    }

    @GetMapping("/subscriptions")
    public AdminPageDtos.SubscriptionsPage subscriptions() {
        return adminPageService.subscriptionsPage();
    }

    @GetMapping("/audit")
    public AdminPageDtos.AuditPage auditPage() {
        return adminPageService.auditPage();
    }

    @GetMapping("/settings")
    public AdminPageDtos.SettingsPage settings(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return adminPageService.settings(extractToken(authHeader));
    }

    @PostMapping("/moderation/reviews/{id}/approve")
    public void approveReview(@PathVariable UUID id,
                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                              @AuthenticationPrincipal UUID adminUserId) {
        marketplaceClient.updateReviewStatus(id, "PUBLISHED");
        auditLogService.log(adminUserId, "REVIEW_APPROVED", "REVIEW", id, null);
    }

    @PostMapping("/moderation/reviews/{id}/reject")
    public void rejectReview(@PathVariable UUID id,
                             @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                             @AuthenticationPrincipal UUID adminUserId) {
        marketplaceClient.updateReviewStatus(id, "REJECTED");
        auditLogService.log(adminUserId, "REVIEW_REJECTED", "REVIEW", id, null);
    }

    @PostMapping("/users/{id}/block")
    public void blockUser(@PathVariable UUID id,
                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                          @AuthenticationPrincipal UUID adminUserId) {
        updateUser(id, new AdminUserActionRequest("block"), authHeader, adminUserId);
    }

    @PostMapping("/users/{id}/unblock")
    public void unblockUser(@PathVariable UUID id,
                            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                            @AuthenticationPrincipal UUID adminUserId) {
        updateUser(id, new AdminUserActionRequest("unblock"), authHeader, adminUserId);
    }

    @PatchMapping("/users/{id}")
    public Map<String, String> updateUser(@PathVariable UUID id,
                                          @RequestBody AdminUserActionRequest request,
                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                          @AuthenticationPrincipal UUID adminUserId) {
        var token = extractToken(authHeader);
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
        } else {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported admin user action: " + action
            );
        }

        return Map.of(
                "userId", id.toString(),
                "action", action,
                "updatedAt", Instant.now().toString()
        );
    }

    private String extractToken(String authHeader) {
        return authHeader.substring(7);
    }

    public record AdminUserActionRequest(String action) {}

    public record CreateModeratorRequest(
            @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
            @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password
    ) {}
}
