package com.tutor.notificationservice.notification;

import lombok.RequiredArgsConstructor;
import com.tutor.notificationservice.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public Notification ingest(NotificationIngestDto dto) {
        Notification n = new Notification();
        n.setUserId(dto.userId());
        n.setEventType(dto.eventType());
        n.setTitle(dto.title());
        n.setMessage(dto.message());
        n.setMetadata(dto.metadata());
        return repository.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> listByUser(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listDtosByUser(String userId) {
        return listByUser(userId).stream().map(NotificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Notification> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listAllDtos() {
        return listAll().stream().map(NotificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDto markRead(String userId, UUID id) {
        Notification notification = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Уведомление не найдено."));
        notification.setRead(true);
        return NotificationDto.from(repository.save(notification));
    }

    @Transactional
    public void markAllRead(String userId) {
        List<Notification> notifications = repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        notifications.forEach(notification -> notification.setRead(true));
        repository.saveAll(notifications);
    }
}
