package com.tutor.crmservice.lesson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class LessonRepositoryTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Test
    void findByIdForUpdate_returnsPersistedLesson() {
        Lesson lesson = new Lesson();
        lesson.setApplicationId(UUID.randomUUID());
        lesson.setStudentId(UUID.randomUUID());
        lesson.setClientUserId(UUID.randomUUID());
        lesson.setTutorUserId(UUID.randomUUID());
        lesson.setTutorName("Tutor Name");
        lesson.setStudentName("Student Name");
        lesson.setTitle("Mathematics");
        lesson.setSubjectName("Mathematics");
        lesson.setStartsAt(LocalDateTime.of(2026, 5, 7, 18, 0));
        lesson.setDurationMinutes(60);
        lesson.setFormat("ONLINE");
        lesson.setStatus("SCHEDULED");

        Lesson savedLesson = lessonRepository.saveAndFlush(lesson);

        Optional<Lesson> lockedLesson = lessonRepository.findByIdForUpdate(savedLesson.getId());

        assertTrue(lockedLesson.isPresent());
        assertEquals(savedLesson.getId(), lockedLesson.get().getId());
    }
}
