package com.tutor.notificationservice.notification;

import com.tutor.notificationservice.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class UserNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return notificationService.listDtosByUser(principal.userId().toString());
    }

    @GetMapping("/unread-count")
    public UnreadCountDto unreadCount(@AuthenticationPrincipal JwtPrincipal principal) {
        return new UnreadCountDto(notificationService.countUnread(principal.userId().toString()));
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(@AuthenticationPrincipal JwtPrincipal principal,
                                    @PathVariable UUID id) {
        return notificationService.markRead(principal.userId().toString(), id);
    }

    @PatchMapping("/read-all")
    public UnreadCountDto markAllRead(@AuthenticationPrincipal JwtPrincipal principal) {
        notificationService.markAllRead(principal.userId().toString());
        return new UnreadCountDto(0);
    }
}
