package com.tutor.crmservice.lesson;

import com.tutor.crmservice.kafka.NotificationEventPublisher;
import com.tutor.crmservice.request.ApplicationStatus;
import com.tutor.crmservice.request.CrmApplication;
import com.tutor.crmservice.request.CrmApplicationRepository;
import com.tutor.crmservice.service.CreateLessonRequest;
import com.tutor.crmservice.service.RescheduleLessonRequest;
import com.tutor.crmservice.shared.ConflictException;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import com.tutor.crmservice.student.StudentCard;
import com.tutor.crmservice.student.StudentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CrmApplicationRepository applicationRepository;
    private final StudentCardRepository studentCardRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final AvailabilitySlotService availabilitySlotService;
    private final LessonRescheduleRequestRepository lessonRescheduleRequestRepository;

    @Transactional
    public Lesson createTutorLesson(UUID tutorUserId, CreateLessonRequest request) {
        LocalDateTime startTime;
        int duration;
        String studentName;
        String tutorName;
        String subjectName;
        String format;
        UUID applicationId;
        UUID studentId;
        UUID clientUserId;
        String title;
        CrmApplication sourceApplication = null;

        if (request.applicationId() != null) {
            // Старая логика через заявку
            CrmApplication application = resolveApplication(tutorUserId, request.applicationId());
            sourceApplication = application;
            StudentCard studentCard = studentCardRepository.findByApplicationId(application.getId())
                    .orElseThrow(() -> new ConflictException("Сначала примите заявку, чтобы создать карточку ученика."));

            if (request.slot() != null) {
                boolean available = availabilitySlotService.isSlotAvailable(
                        tutorUserId,
                        request.slot().date(),
                        request.slot().startTime(),
                        request.slot().durationMinutes()
                );
                if (!available) {
                    throw new ConflictException("Выбранный слот недоступен или уже забронирован.");
                }
                startTime = request.slot().date().atTime(request.slot().startTime());
                duration = request.slot().durationMinutes();
            } else {
                startTime = request.startsAt() == null ? application.getDesiredStartTime() : request.startsAt();
                duration = request.durationMinutes() == null ? 60 : request.durationMinutes();
            }

            applicationId = application.getId();
            studentId = studentCard.getId();
            clientUserId = application.getClientUserId();
            studentName = studentCard.getName();
            subjectName = application.getSubjectName();
            format = application.getFormat();
            tutorName = application.getTutorName();
            title = application.getSubjectName() + " · пробный урок";
        } else {
            if (request.startsAt() == null) {
                throw new ConflictException("Укажите время начала урока.");
            }
            startTime = request.startsAt();
            duration = request.durationMinutes() == null ? 60 : request.durationMinutes();
            if (request.studentId() != null) {
                StudentCard studentCard = studentCardRepository.findByIdAndTutorUserId(request.studentId(), tutorUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ученик не найден."));
                studentId = studentCard.getId();
                studentName = studentCard.getName();
                subjectName = request.subject() == null ? studentCard.getSubject() : request.subject();
            } else {
                studentName = request.studentName() == null ? "Ученик" : request.studentName();
                subjectName = request.subject() == null ? "Предмет" : request.subject();
                StudentCard manualStudentCard = createManualStudentCard(tutorUserId, studentName, subjectName);
                studentId = manualStudentCard.getId();
            }
            format = request.format() == null ? "Онлайн" : request.format();
            tutorName = "Репетитор";
            title = subjectName + " · " + studentName;
            applicationId = null;
            clientUserId = null;
        }

        int occurrences = recurrenceOccurrences(request.recurrence());
        UUID seriesId = occurrences > 1 ? UUID.randomUUID() : null;
        Lesson firstSaved = null;

        for (int index = 0; index < occurrences; index++) {
            LocalDateTime lessonStart = startTime.plusWeeks(index);
            ensureNoOverlap(tutorUserId, lessonStart, duration, Set.of());
            Lesson saved = lessonRepository.save(buildLesson(
                    applicationId,
                    studentId,
                    clientUserId,
                    tutorUserId,
                    studentName,
                    tutorName,
                    title,
                    subjectName,
                    lessonStart,
                    duration,
                    format,
                    seriesId
            ));
            if (firstSaved == null) {
                firstSaved = saved;
            }
        }

        if (clientUserId != null) {
            notificationEventPublisher.publishLessonCreated(
                    clientUserId,
                    applicationId,
                    firstSaved == null ? null : firstSaved.getId(),
                    tutorName,
                    subjectName,
                startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }
        if (sourceApplication != null) {
            sourceApplication.setStatus(ApplicationStatus.TRIAL_SCHEDULED);
            if (sourceApplication.getTrialScheduledAt() == null) {
                sourceApplication.setTrialScheduledAt(Instant.now());
            }
            applicationRepository.save(sourceApplication);
        }
        return firstSaved;
    }

    @Transactional
    public Lesson rescheduleTutorLesson(UUID tutorUserId, UUID lessonId, RescheduleLessonRequest request) {
        if (request == null || request.startsAt() == null) {
            throw new IllegalArgumentException("Новое время урока обязательно.");
        }
        Lesson lesson = lessonRepository.findByIdForUpdate(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Урок не найден."));
        if (!lesson.getTutorUserId().equals(tutorUserId)) {
            throw new ResourceNotFoundException("Урок не найден.");
        }

        RescheduleLessonRequest.Scope scope = request.scope() == null ? RescheduleLessonRequest.Scope.SINGLE : request.scope();
        if (scope == RescheduleLessonRequest.Scope.SERIES && lesson.getSeriesId() != null) {
            List<Lesson> seriesLessons = lessonRepository.findBySeriesIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
                    lesson.getSeriesId(),
                    lesson.getStartsAt()
            );
            Set<UUID> movingIds = lessonIds(seriesLessons);
            LocalDateTime selectedStart = lesson.getStartsAt();
            for (Lesson seriesLesson : seriesLessons) {
                long weeksFromSelected = java.time.temporal.ChronoUnit.WEEKS.between(selectedStart.toLocalDate(), seriesLesson.getStartsAt().toLocalDate());
                LocalDateTime newStart = request.startsAt().plusWeeks(weeksFromSelected);
                ensureNoOverlap(tutorUserId, newStart, seriesLesson.getDurationMinutes(), movingIds);
                seriesLesson.setStartsAt(newStart);
                seriesLesson.setStatus("Перенесено");
            }
            closePendingRescheduleRequest(lessonId);
            publishLessonRescheduled(lesson, request.startsAt());
            return lesson;
        }

        ensureNoOverlap(tutorUserId, request.startsAt(), lesson.getDurationMinutes(), Set.of(lesson.getId()));
        lesson.setStartsAt(request.startsAt());
        lesson.setStatus("Перенесено");
        closePendingRescheduleRequest(lessonId);
        publishLessonRescheduled(lesson, request.startsAt());
        return lesson;
    }

    private void publishLessonRescheduled(Lesson lesson, LocalDateTime startTime) {
        if (lesson.getClientUserId() == null) {
            return;
        }
        notificationEventPublisher.publishLessonRescheduled(
                lesson.getClientUserId(),
                lesson.getApplicationId(),
                lesson.getId(),
                lesson.getSubjectName(),
                startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );
    }

    private void closePendingRescheduleRequest(UUID lessonId) {
        lessonRescheduleRequestRepository
                .findTopByLessonIdAndStatusOrderByCreatedAtDesc(lessonId, LessonRescheduleStatus.PENDING)
                .ifPresent(request -> {
                    request.setStatus(LessonRescheduleStatus.RESOLVED);
                    lessonRescheduleRequestRepository.save(request);
                });
    }

    private CrmApplication resolveApplication(UUID tutorUserId, UUID applicationId) {
        if (applicationId != null) {
            return applicationRepository.findByIdAndTutorUserId(applicationId, tutorUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена."));
        }
        return applicationRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId).stream()
                .filter(app -> app.getStatus() == ApplicationStatus.ACCEPTED)
                .findFirst()
                .orElseThrow(() -> new ConflictException("Нет принятой заявки, из которой можно создать урок."));
    }

    private StudentCard createManualStudentCard(UUID tutorUserId, String studentName, String subjectName) {
        StudentCard card = new StudentCard();
        card.setTutorUserId(tutorUserId);
        card.setName(studentName);
        card.setDetails(subjectName + " · добавлен вручную");
        card.setSubject(subjectName);
        card.setStatus("Активен");
        card.setNote("Ученик добавлен из календаря.");
        card.setProgress(0);
        return studentCardRepository.save(card);
    }

    private Lesson buildLesson(
            UUID applicationId,
            UUID studentId,
            UUID clientUserId,
            UUID tutorUserId,
            String studentName,
            String tutorName,
            String title,
            String subjectName,
            LocalDateTime startTime,
            int duration,
            String format,
            UUID seriesId
    ) {
        Lesson lesson = new Lesson();
        lesson.setApplicationId(applicationId);
        lesson.setStudentId(studentId);
        lesson.setClientUserId(clientUserId);
        lesson.setTutorUserId(tutorUserId);
        lesson.setTutorName(tutorName == null || tutorName.isBlank() ? "Репетитор" : tutorName);
        lesson.setStudentName(studentName);
        lesson.setTitle(title);
        lesson.setSubjectName(subjectName);
        lesson.setStartsAt(startTime);
        lesson.setDurationMinutes(duration);
        lesson.setFormat(format);
        lesson.setStatus("Запланировано");
        lesson.setBookingSource(BookingSource.MANUAL);
        lesson.setSeriesId(seriesId);
        return lesson;
    }

    private int recurrenceOccurrences(CreateLessonRequest.RecurrenceRequest recurrence) {
        if (recurrence == null || recurrence.type() == null || recurrence.type() == CreateLessonRequest.RecurrenceType.NONE) {
            return 1;
        }
        if (recurrence.type() != CreateLessonRequest.RecurrenceType.WEEKLY) {
            throw new ConflictException("Поддерживается только еженедельная периодичность.");
        }
        int occurrences = recurrence.occurrences() == null ? 8 : recurrence.occurrences();
        if (occurrences < 2 || occurrences > 52) {
            throw new ConflictException("Количество уроков в серии должно быть от 2 до 52.");
        }
        return occurrences;
    }

    private Set<UUID> lessonIds(List<Lesson> lessons) {
        Set<UUID> ids = new HashSet<>();
        lessons.forEach(lesson -> {
            if (lesson.getId() != null) {
                ids.add(lesson.getId());
            }
        });
        return ids;
    }

    private void ensureNoOverlap(UUID tutorUserId, LocalDateTime startTime, int durationMinutes, Set<UUID> ignoredLessonIds) {
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        boolean overlap = lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorUserId).stream()
                .filter(lesson -> lesson.getId() == null || !ignoredLessonIds.contains(lesson.getId()))
                .anyMatch(lesson -> {
                    LocalDateTime lessonEnd = lesson.getStartsAt().plusMinutes(lesson.getDurationMinutes());
                    return startTime.isBefore(lessonEnd) && endTime.isAfter(lesson.getStartsAt());
                });
        if (overlap) {
            throw new ConflictException("У репетитора уже есть занятие в это время.");
        }
    }
}
