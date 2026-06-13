package com.tutor.notificationservice.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDto> listAll(
            @RequestParam(name = "userId", required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            return notificationService.listDtosByUser(userId);
        }
        return notificationService.listAllDtos();
    }
}
