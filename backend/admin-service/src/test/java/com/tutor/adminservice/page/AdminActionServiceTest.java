package com.tutor.adminservice.page;

import com.tutor.adminservice.audit.AuditLogService;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.moderation.ModerationController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AdminActionServiceTest {

    @Mock
    private MarketplaceServiceClient marketplaceClient;

    @Mock
    private AuthServiceClient authClient;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AdminPageService adminPageService;

    @InjectMocks
    private ModerationController controller;

    @Test
    void unsupportedAdminUserActionIsRejectedWithoutDownstreamCalls() {
        var userId = UUID.randomUUID();
        var adminId = UUID.randomUUID();

        assertThrows(ResponseStatusException.class, () -> controller.updateUser(
                userId,
                new ModerationController.AdminUserActionRequest("remove_role"),
                "Bearer admin-token",
                adminId
        ));

        verifyNoInteractions(authClient, auditLogService, marketplaceClient, adminPageService);
    }

    @Test
    void successfulBlockActionUpdatesAuthBeforeWritingAudit() {
        var userId = UUID.randomUUID();
        var adminId = UUID.randomUUID();

        var response = controller.updateUser(
                userId,
                new ModerationController.AdminUserActionRequest("block"),
                "Bearer admin-token",
                adminId
        );

        assertEquals(userId.toString(), response.get("userId"));
        assertEquals("block", response.get("action"));

        InOrder ordered = inOrder(authClient, auditLogService);
        ordered.verify(authClient).updateUserStatus(userId, "BLOCKED", "admin-token");
        ordered.verify(auditLogService).log(adminId, "USER_BLOCKED", "USER", userId, null);
        verifyNoMoreInteractions(authClient, auditLogService);
    }

    @Test
    void makeModeratorActionIsRejectedBecauseModeratorsAreCreatedAsDedicatedAccounts() {
        var userId = UUID.randomUUID();
        var adminId = UUID.randomUUID();

        assertThrows(ResponseStatusException.class, () -> controller.updateUser(
                userId,
                new ModerationController.AdminUserActionRequest("make_moderator"),
                "Bearer admin-token",
                adminId
        ));

        verifyNoInteractions(authClient, auditLogService, marketplaceClient, adminPageService);
    }

    @Test
    void createModeratorAccountCreatesDedicatedModeratorAndWritesAudit() {
        var adminId = UUID.randomUUID();
        var moderatorId = UUID.randomUUID();
        var created = new AuthServiceClient.AdminUserDto(
                moderatorId,
                "moderator@example.com",
                "ROLE_MODERATOR",
                "ACTIVE"
        );

        when(authClient.createModerator("moderator@example.com", "moderator-password", "admin-token"))
                .thenReturn(created);

        var response = controller.createModerator(
                new ModerationController.CreateModeratorRequest("moderator@example.com", "moderator-password"),
                "Bearer admin-token",
                adminId
        );

        assertEquals(created, response);
        InOrder ordered = inOrder(authClient, auditLogService);
        ordered.verify(authClient).createModerator("moderator@example.com", "moderator-password", "admin-token");
        ordered.verify(auditLogService).log(adminId, "MODERATOR_CREATED", "USER", moderatorId, "moderator@example.com");
        verifyNoMoreInteractions(authClient, auditLogService);
    }

    @Test
    void downstreamFailureDoesNotWriteSuccessfulAuditEntry() {
        var userId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        doThrow(new RuntimeException("auth unavailable"))
                .when(authClient).updateUserStatus(userId, "ACTIVE", "admin-token");

        assertThrows(RuntimeException.class, () -> controller.updateUser(
                userId,
                new ModerationController.AdminUserActionRequest("unblock"),
                "Bearer admin-token",
                adminId
        ));

        verifyNoInteractions(auditLogService);
    }
}
