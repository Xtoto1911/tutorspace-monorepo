package com.tutor.crmservice.lesson;

import com.tutor.crmservice.kafka.NotificationEventPublisher;
import com.tutor.crmservice.request.ApplicationStatus;
import com.tutor.crmservice.request.CrmApplication;
import com.tutor.crmservice.request.CrmApplicationRepository;
import com.tutor.crmservice.service.CreateLessonRequest;
import com.tutor.crmservice.service.RescheduleLessonRequest;
import com.tutor.crmservice.shared.ConflictException;
import com.tutor.crmservice.student.StudentCard;
import com.tutor.crmservice.student.StudentCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock CrmApplicationRepository applicationRepository;
    @Mock StudentCardRepository studentCardRepository;
    @Mock NotificationEventPublisher notificationEventPublisher;
    @Mock AvailabilitySlotService availabilitySlotService;
    @Mock LessonRescheduleRequestRepository lessonRescheduleRequestRepository;

    @InjectMocks
    LessonService service;

    @Test
    void createLesson_overlappingTime_throwsConflict() {
        UUID tutorId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        CrmApplication app = new CrmApplication();
        app.setId(appId);
        app.setClientUserId(UUID.randomUUID());
        app.setTutorUserId(tutorId);
        app.setTutorName("Иван");
        app.setSubjectName("Математика");
        app.setFormat("Онлайн");
        app.setDesiredStartTime(LocalDateTime.of(2026, 5, 1, 14, 0));

        StudentCard card = new StudentCard();
        card.setId(UUID.randomUUID());
        card.setName("Ученик");

        when(applicationRepository.findByIdAndTutorUserId(appId, tutorId)).thenReturn(Optional.of(app));
        when(studentCardRepository.findByApplicationId(appId)).thenReturn(Optional.of(card));
        when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of(
                createLesson(tutorId, LocalDateTime.of(2026, 5, 1, 13, 30), 60)
        ));

        assertThrows(ConflictException.class, () ->
                service.createTutorLesson(tutorId, new CreateLessonRequest(appId, null, LocalDateTime.of(2026, 5, 1, 14, 0), 60, null, null, null, null, null)));
    }

    @Test
    void createLesson_nonOverlapping_ok() {
        UUID tutorId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        CrmApplication app = new CrmApplication();
        app.setId(appId);
        app.setClientUserId(UUID.randomUUID());
        app.setTutorUserId(tutorId);
        app.setTutorName("Иван");
        app.setSubjectName("Математика");
        app.setFormat("Онлайн");
        app.setDesiredStartTime(LocalDateTime.of(2026, 5, 1, 14, 0));

        StudentCard card = new StudentCard();
        card.setId(UUID.randomUUID());
        card.setName("Ученик");

        when(applicationRepository.findByIdAndTutorUserId(appId, tutorId)).thenReturn(Optional.of(app));
        when(studentCardRepository.findByApplicationId(appId)).thenReturn(Optional.of(card));
        when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of(
                createLesson(tutorId, LocalDateTime.of(2026, 5, 1, 10, 0), 60)
        ));
        when(lessonRepository.save(org.mockito.ArgumentMatchers.any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() ->
                service.createTutorLesson(tutorId, new CreateLessonRequest(appId, null, LocalDateTime.of(2026, 5, 1, 14, 0), 60, null, null, null, null, null)));
        assertEquals(ApplicationStatus.TRIAL_SCHEDULED, app.getStatus());
        assertNotNull(app.getTrialScheduledAt());
    }

    @Test
    void createLesson_withWeeklyRecurrenceForExistingStudent_createsSeries() {
        UUID tutorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentCard card = new StudentCard();
        card.setId(studentId);
        card.setTutorUserId(tutorId);
        card.setName("Мария");
        card.setSubject("Математика");

        when(studentCardRepository.findByIdAndTutorUserId(studentId, tutorId)).thenReturn(Optional.of(card));
        when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of());
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createTutorLesson(tutorId, new CreateLessonRequest(
                null,
                studentId,
                LocalDateTime.of(2026, 5, 4, 18, 0),
                60,
                null,
                null,
                "Математика",
                "Онлайн",
                new CreateLessonRequest.RecurrenceRequest(CreateLessonRequest.RecurrenceType.WEEKLY, 4)
        ));

        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(4)).save(lessonCaptor.capture());
        List<Lesson> savedLessons = lessonCaptor.getAllValues();

        assertNotNull(savedLessons.getFirst().getSeriesId());
        assertEquals(savedLessons.getFirst().getSeriesId(), savedLessons.get(3).getSeriesId());
        assertEquals(LocalDateTime.of(2026, 5, 4, 18, 0), savedLessons.get(0).getStartsAt());
        assertEquals(LocalDateTime.of(2026, 5, 11, 18, 0), savedLessons.get(1).getStartsAt());
        assertEquals(LocalDateTime.of(2026, 5, 25, 18, 0), savedLessons.get(3).getStartsAt());
        assertEquals(studentId, savedLessons.getFirst().getStudentId());
    }

    @Test
    void createLesson_forManualStudent_createsStudentCardAndLinksLesson() {
        UUID tutorId = UUID.randomUUID();

        when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of());
        when(studentCardRepository.save(any(StudentCard.class))).thenAnswer(inv -> {
            StudentCard card = inv.getArgument(0);
            card.setId(UUID.randomUUID());
            return card;
        });
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        Lesson lesson = service.createTutorLesson(tutorId, new CreateLessonRequest(
                null,
                null,
                LocalDateTime.of(2026, 5, 4, 18, 0),
                60,
                null,
                "Иван Иванов",
                "Физика",
                "Офлайн",
                null
        ));

        assertNotNull(lesson.getStudentId());
        assertEquals("Иван Иванов", lesson.getStudentName());
        verify(studentCardRepository).save(any(StudentCard.class));
    }

    @Test
    void rescheduleLesson_singleKeepsOtherSeriesLessonsInPlace() {
        UUID tutorId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = createLesson(tutorId, LocalDateTime.of(2026, 5, 4, 18, 0), 60);
        lesson.setId(lessonId);
        lesson.setSeriesId(seriesId);
        Lesson nextLesson = createLesson(tutorId, LocalDateTime.of(2026, 5, 11, 18, 0), 60);
        nextLesson.setSeriesId(seriesId);

        when(lessonRepository.findByIdForUpdate(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of(lesson, nextLesson));

        service.rescheduleTutorLesson(tutorId, lessonId, new RescheduleLessonRequest(
                LocalDateTime.of(2026, 5, 5, 19, 0),
                RescheduleLessonRequest.Scope.SINGLE
        ));

        assertEquals(LocalDateTime.of(2026, 5, 5, 19, 0), lesson.getStartsAt());
        assertEquals(LocalDateTime.of(2026, 5, 11, 18, 0), nextLesson.getStartsAt());
    }

    @Test
    void rescheduleLesson_closesPendingClientRescheduleRequest() {
        UUID tutorId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = createLesson(tutorId, LocalDateTime.of(2026, 5, 4, 18, 0), 60);
        lesson.setId(lessonId);
        LessonRescheduleRequest request = new LessonRescheduleRequest();
        request.setStatus(LessonRescheduleStatus.PENDING);

        when(lessonRepository.findByIdForUpdate(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of(lesson));
        when(lessonRescheduleRequestRepository.findTopByLessonIdAndStatusOrderByCreatedAtDesc(lessonId, LessonRescheduleStatus.PENDING))
                .thenReturn(Optional.of(request));

        service.rescheduleTutorLesson(tutorId, lessonId, new RescheduleLessonRequest(
                LocalDateTime.of(2026, 5, 5, 19, 0),
                RescheduleLessonRequest.Scope.SINGLE
        ));

        assertEquals(LessonRescheduleStatus.RESOLVED, request.getStatus());
        verify(lessonRescheduleRequestRepository).save(request);
    }

    @Test
    void createLesson_fromApplicationKeepsTutorNameForClientSchedule() {
        UUID tutorId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        CrmApplication app = new CrmApplication();
        app.setId(appId);
        app.setClientUserId(UUID.randomUUID());
        app.setTutorUserId(tutorId);
        app.setTutorName("E2E Репетитор");
        app.setSubjectName("Математика");
        app.setFormat("Онлайн");
        app.setDesiredStartTime(LocalDateTime.of(2026, 5, 1, 14, 0));

        StudentCard card = new StudentCard();
        card.setId(UUID.randomUUID());
        card.setName("Ученик");

        when(applicationRepository.findByIdAndTutorUserId(appId, tutorId)).thenReturn(Optional.of(app));
        when(studentCardRepository.findByApplicationId(appId)).thenReturn(Optional.of(card));
        when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of());
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        Lesson lesson = service.createTutorLesson(tutorId, new CreateLessonRequest(
                appId,
                null,
                LocalDateTime.of(2026, 5, 1, 14, 0),
                60,
                null,
                null,
                null,
                null,
                null
        ));

        assertEquals("E2E Репетитор", lesson.getTutorName());
    }

    @Test
    void rescheduleLesson_seriesMovesSelectedAndFutureSeriesLessons() {
        UUID tutorId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        UUID firstLessonId = UUID.randomUUID();
        Lesson first = createLesson(tutorId, LocalDateTime.of(2026, 5, 4, 18, 0), 60);
        first.setId(firstLessonId);
        first.setSeriesId(seriesId);
        Lesson second = createLesson(tutorId, LocalDateTime.of(2026, 5, 11, 18, 0), 60);
        second.setSeriesId(seriesId);
        Lesson third = createLesson(tutorId, LocalDateTime.of(2026, 5, 18, 18, 0), 60);
        third.setSeriesId(seriesId);

        when(lessonRepository.findByIdForUpdate(firstLessonId)).thenReturn(Optional.of(first));
        when(lessonRepository.findBySeriesIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(seriesId, first.getStartsAt()))
                .thenReturn(List.of(first, second, third));
        when(lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorId)).thenReturn(List.of(first, second, third));

        service.rescheduleTutorLesson(tutorId, firstLessonId, new RescheduleLessonRequest(
                LocalDateTime.of(2026, 5, 6, 19, 30),
                RescheduleLessonRequest.Scope.SERIES
        ));

        assertEquals(LocalDateTime.of(2026, 5, 6, 19, 30), first.getStartsAt());
        assertEquals(LocalDateTime.of(2026, 5, 13, 19, 30), second.getStartsAt());
        assertEquals(LocalDateTime.of(2026, 5, 20, 19, 30), third.getStartsAt());
    }

    private Lesson createLesson(UUID tutorId, LocalDateTime start, int duration) {
        Lesson lesson = new Lesson();
        lesson.setTutorUserId(tutorId);
        lesson.setStartsAt(start);
        lesson.setDurationMinutes(duration);
        return lesson;
    }
}
