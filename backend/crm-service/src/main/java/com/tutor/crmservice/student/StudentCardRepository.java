package com.tutor.crmservice.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentCardRepository extends JpaRepository<StudentCard, UUID> {
    Optional<StudentCard> findByApplicationId(UUID applicationId);
    Optional<StudentCard> findByIdAndTutorUserId(UUID id, UUID tutorUserId);
    List<StudentCard> findByTutorUserIdOrderByCreatedAtDesc(UUID tutorUserId);
}
