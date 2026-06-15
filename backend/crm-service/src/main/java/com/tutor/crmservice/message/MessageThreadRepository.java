package com.tutor.crmservice.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {
    List<MessageThread> findByTutorUserIdOrderByUpdatedAtDesc(UUID tutorUserId);
    List<MessageThread> findByClientUserIdOrderByUpdatedAtDesc(UUID clientUserId);
    Optional<MessageThread> findByApplicationId(UUID applicationId);
    Optional<MessageThread> findByIdAndTutorUserId(UUID id, UUID tutorUserId);
    Optional<MessageThread> findByIdAndClientUserId(UUID id, UUID clientUserId);

    @Modifying
    @Query("UPDATE MessageThread t SET t.participantName = :newName WHERE t.tutorUserId = :userId AND t.participantName <> :newName")
    int updateParticipantNameByTutorUserId(@Param("userId") UUID userId, @Param("newName") String newName);
}
