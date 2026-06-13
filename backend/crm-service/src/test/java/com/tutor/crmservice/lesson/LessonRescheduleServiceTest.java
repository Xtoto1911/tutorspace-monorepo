package com.tutor.crmservice.lesson;

import com.tutor.crmservice.kafka.NotificationEventPublisher;
import com.tutor.crmservice.service.RequestLessonRescheduleDto;
import com.tutor.crmservice.shared.ConflictException;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonRescheduleServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock LessonRescheduleRequestRepository rescheduleRequestRepository;
    @Mock NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    LessonRescheduleService service;

    @Test
    void createRequest_forOwnLesson_savesPendingRequest() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        Lesson lesson = lesson(lessonId, clientId);

        when(lessonRepository.findByIdForUpdate(lessonId)).thenReturn(Optional.of(lesson));
        when(rescheduleRequestRepository.existsByLessonIdAndStatus(lessonId, LessonRescheduleStatus.PENDING)).thenReturn(false);
        when(rescheduleRequestRepository.save(any(LessonRescheduleRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonRescheduleRequest saved = service.createClientRequest(
                clientId,
                lessonId,
                new RequestLessonRescheduleDto("Можно на 19:00?", LocalDateTime.of(2026, 5, 7, 19, 0))
        );

        assertEquals(LessonRescheduleStatus.PENDING, saved.getStatus());
        assertEquals("Можно на 19:00?", saved.getMessage());
        verify(notificationEventPublisher).publishRescheduleRequested(
                lesson.getTutorUserId(),
                lesson.getApplicationId(),
                lesson.getId(),
                lesson.getTitle(),
                "Можно на 19:00?"
        );
    }

    @Test
    void createRequest_forForeignLesson_throwsNotFound() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = lesson(lessonId, UUID.randomUUID());

        when(lessonRepository.findByIdForUpdate(lessonId)).thenReturn(Optional.of(lesson));

        assertThrows(ResourceNotFoundException.class, () ->
                service.createClientRequest(UUID.randomUUID(), lessonId, new RequestLessonRescheduleDto("message", null)));
    }

    @Test
    void createRequest_whenPendingAlreadyExists_throwsConflict() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        Lesson lesson = lesson(lessonId, clientId);

        LessonRescheduleRequest existing = new LessonRescheduleRequest();
        existing.setStatus(LessonRescheduleStatus.PENDING);

        when(lessonRepository.findByIdForUpdate(lessonId)).thenReturn(Optional.of(lesson));
        when(rescheduleRequestRepository.existsByLessonIdAndStatus(lessonId, LessonRescheduleStatus.PENDING)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                service.createClientRequest(clientId, lessonId, new RequestLessonRescheduleDto("message", null)));
    }

    @Test
    void createRequest_withNullMessage_rejectsPayload() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        when(lessonRepository.findByIdForUpdate(lessonId)).thenReturn(Optional.of(lesson(lessonId, clientId)));

        assertThrows(IllegalArgumentException.class, () ->
                service.createClientRequest(clientId, lessonId, new RequestLessonRescheduleDto(null, null)));

        verify(rescheduleRequestRepository, never()).save(any(LessonRescheduleRequest.class));
    }

    @Test
    void createRequest_withBlankMessage_rejectsPayload() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        when(lessonRepository.findByIdForUpdate(lessonId)).thenReturn(Optional.of(lesson(lessonId, clientId)));

        assertThrows(IllegalArgumentException.class, () ->
                service.createClientRequest(clientId, lessonId, new RequestLessonRescheduleDto("   ", null)));

        verify(rescheduleRequestRepository, never()).save(any(LessonRescheduleRequest.class));
    }

    @Test
    void createRequest_withTooLongMessage_rejectsPayload() {
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        when(lessonRepository.findByIdForUpdate(lessonId)).thenReturn(Optional.of(lesson(lessonId, clientId)));

        assertThrows(IllegalArgumentException.class, () ->
                service.createClientRequest(clientId, lessonId, new RequestLessonRescheduleDto("x".repeat(1001), null)));

        verify(rescheduleRequestRepository, never()).save(any(LessonRescheduleRequest.class));
    }

    private Lesson lesson(UUID lessonId, UUID clientId) {
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setClientUserId(clientId);
        lesson.setTutorUserId(UUID.randomUUID());
        lesson.setApplicationId(UUID.randomUUID());
        lesson.setTitle("Математика");
        lesson.setSubjectName("Математика");
        lesson.setStartsAt(LocalDateTime.of(2026, 5, 7, 18, 0));
        return lesson;
    }
}
