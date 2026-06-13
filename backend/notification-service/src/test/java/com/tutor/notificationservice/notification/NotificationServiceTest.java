package com.tutor.notificationservice.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void ingest_shouldSaveNotification() {
        NotificationIngestDto dto = new NotificationIngestDto(
                "user-123",
                "APPLICATION_ACCEPTED",
                "Заявка принята",
                "Репетитор принял заявку",
                Map.of("tutorName", "Анна")
        );

        when(repository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(java.util.UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.ingest(dto);

        assertThat(result.getUserId()).isEqualTo("user-123");
        assertThat(result.getEventType()).isEqualTo("APPLICATION_ACCEPTED");
        assertThat(result.getTitle()).isEqualTo("Заявка принята");
        assertThat(result.isRead()).isFalse();
        verify(repository).save(any(Notification.class));
    }

    @Test
    void listByUser_shouldReturnOrderedList() {
        notificationService.listByUser("user-123");
        verify(repository).findByUserIdOrderByCreatedAtDesc("user-123");
    }

    @Test
    void countUnread_shouldDelegateToRepository() {
        notificationService.countUnread("user-123");
        verify(repository).countByUserIdAndReadFalse("user-123");
    }

    @Test
    void listDtosByUser_shouldReturnActionUrlForClientLesson() {
        Notification notification = notification(
                "user-123",
                "LESSON_CREATED",
                Map.of("lessonId", UUID.randomUUID().toString())
        );
        when(repository.findByUserIdOrderByCreatedAtDesc("user-123")).thenReturn(List.of(notification));

        var result = notificationService.listDtosByUser("user-123");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().actionUrl()).isEqualTo("/app/client/schedule");
    }

    @Test
    void markRead_shouldUpdateOnlyOwnNotification() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = notification("user-123", "CLIENT_MESSAGE_SENT", Map.of("threadId", "thread-1"));
        notification.setId(notificationId);
        when(repository.findByIdAndUserId(notificationId, "user-123")).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        NotificationDto result = notificationService.markRead("user-123", notificationId);

        assertThat(notification.isRead()).isTrue();
        assertThat(result.actionUrl()).isEqualTo("/app/tutor/messages?threadId=thread-1");
        verify(repository).save(notification);
    }

    @Test
    void markRead_forForeignNotificationThrowsNotFound() {
        UUID notificationId = UUID.randomUUID();
        when(repository.findByIdAndUserId(notificationId, "user-123")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.markRead("user-123", notificationId));

        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void markAllRead_shouldSaveUnreadNotifications() {
        Notification first = notification("user-123", "REQUEST_CREATED", Map.of());
        Notification second = notification("user-123", "TUTOR_MESSAGE_SENT", Map.of("threadId", "thread-2"));
        when(repository.findByUserIdAndReadFalseOrderByCreatedAtDesc("user-123")).thenReturn(List.of(first, second));

        notificationService.markAllRead("user-123");

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        verify(repository).saveAll(List.of(first, second));
    }

    private Notification notification(String userId, String eventType, Map<String, String> metadata) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setEventType(eventType);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setMetadata(metadata);
        return notification;
    }
}
