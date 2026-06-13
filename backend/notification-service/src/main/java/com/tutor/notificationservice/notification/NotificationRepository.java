package com.tutor.notificationservice.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Notification> findByIdAndUserId(UUID id, String userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    long countByUserIdAndReadFalse(String userId);
}
