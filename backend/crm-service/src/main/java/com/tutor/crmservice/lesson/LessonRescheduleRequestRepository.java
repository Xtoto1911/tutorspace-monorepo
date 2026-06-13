package com.tutor.crmservice.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRescheduleRequestRepository extends JpaRepository<LessonRescheduleRequest, UUID> {
    Optional<LessonRescheduleRequest> findTopByLessonIdOrderByCreatedAtDesc(UUID lessonId);
    Optional<LessonRescheduleRequest> findTopByLessonIdAndStatusOrderByCreatedAtDesc(UUID lessonId, LessonRescheduleStatus status);
    List<LessonRescheduleRequest> findByClientUserIdOrderByCreatedAtDesc(UUID clientUserId);
    boolean existsByLessonIdAndStatus(UUID lessonId, LessonRescheduleStatus status);
}
