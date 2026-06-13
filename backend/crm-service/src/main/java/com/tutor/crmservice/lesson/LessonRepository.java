package com.tutor.crmservice.lesson;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByTutorUserIdOrderByStartsAtAsc(UUID tutorUserId);
    List<Lesson> findByClientUserIdOrderByStartsAtAsc(UUID clientUserId);
    List<Lesson> findBySeriesIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(UUID seriesId, LocalDateTime startsAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lesson from Lesson lesson where lesson.id = :lessonId")
    Optional<Lesson> findByIdForUpdate(@Param("lessonId") UUID lessonId);

    List<Lesson> findByTutorUserIdAndStartsAtBetween(UUID tutorUserId, LocalDateTime from, LocalDateTime to);
}
