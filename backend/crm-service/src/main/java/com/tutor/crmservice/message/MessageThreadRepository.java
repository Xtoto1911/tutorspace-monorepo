package com.tutor.crmservice.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {
    List<MessageThread> findByTutorUserIdOrderByUpdatedAtDesc(UUID tutorUserId);
    List<MessageThread> findByClientUserIdOrderByUpdatedAtDesc(UUID clientUserId);
    Optional<MessageThread> findByApplicationId(UUID applicationId);
    Optional<MessageThread> findByIdAndTutorUserId(UUID id, UUID tutorUserId);
    Optional<MessageThread> findByIdAndClientUserId(UUID id, UUID clientUserId);
}
