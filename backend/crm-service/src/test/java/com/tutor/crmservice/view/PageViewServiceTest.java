package com.tutor.crmservice.view;

import com.tutor.crmservice.lesson.AvailabilitySlotService;
import com.tutor.crmservice.lesson.Lesson;
import com.tutor.crmservice.lesson.LessonRepository;
import com.tutor.crmservice.lesson.LessonRescheduleRequest;
import com.tutor.crmservice.lesson.LessonRescheduleRequestRepository;
import com.tutor.crmservice.lesson.LessonRescheduleStatus;
import com.tutor.crmservice.message.Message;
import com.tutor.crmservice.message.MessageRepository;
import com.tutor.crmservice.message.MessageThread;
import com.tutor.crmservice.message.MessageThreadRepository;
import com.tutor.crmservice.request.ApplicationStatus;
import com.tutor.crmservice.request.CrmApplication;
import com.tutor.crmservice.request.CrmApplicationRepository;
import com.tutor.crmservice.student.StudentCard;
import com.tutor.crmservice.student.StudentCardRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageViewServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneId.of("UTC"));

    @Test
    void getClientOverviewPage_aggregatesApplicationsNextLessonAndRecentConversation() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();

        CrmApplication activeApplication = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.ACCEPTED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        activeApplication.setClientUserId(clientId);
        CrmApplication closedApplication = createApplication(
                UUID.randomUUID(),
                tutorId,
                ApplicationStatus.CANCELLED,
                "Иван Петров",
                "9 класс",
                "Разобрать домашнее задание",
                "Физика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 7, 18, 0)
        );
        closedApplication.setClientUserId(clientId);

        Lesson nextLesson = createLesson(
                UUID.randomUUID(),
                applicationId,
                tutorId,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Пробный урок",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 6, 12, 0),
                60,
                "Запланировано"
        );
        MessageThread thread = createThread(
                threadId,
                applicationId,
                clientId,
                tutorId,
                "Анна Смирнова",
                "АС",
                "Математика",
                "Подтверждаю занятие в 12:00"
        );

        context.applicationsByClient.put(clientId, List.of(activeApplication, closedApplication));
        context.lessonsByClient.put(clientId, List.of(nextLesson));
        context.threadsByClient.put(clientId, List.of(thread));

        ClientOverviewPageDto page = service.getClientOverviewPage(clientId);

        assertEquals(1, page.activeApplications());
        assertEquals(2, page.recentApplications().size());
        assertEquals(applicationId.toString(), page.recentApplications().get(0).id());
        assertEquals(nextLesson.getId().toString(), page.nextLesson().id());
        assertEquals("Анна Смирнова", page.recentConversation().participantName());
        assertEquals("Подтверждаю занятие в 12:00", page.recentConversation().preview());
    }

    @Test
    void getTutorOverviewPage_aggregatesNewRequestsStudentsAndUpcomingLessons() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        CrmApplication newApplication = createApplication(
                UUID.randomUUID(),
                tutorId,
                ApplicationStatus.NEW,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        CrmApplication acceptedApplication = createApplication(
                UUID.randomUUID(),
                tutorId,
                ApplicationStatus.ACCEPTED,
                "Мария Смирнова",
                "10 класс",
                "Подготовка к ЕГЭ",
                "Физика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 9, 18, 0)
        );
        Lesson nextLesson = createLesson(
                UUID.randomUUID(),
                acceptedApplication.getId(),
                tutorId,
                clientId,
                "Анна Смирнова",
                "Мария Смирнова",
                "Физика с Марией",
                "Физика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 6, 14, 0),
                60,
                "Запланировано"
        );

        context.applicationsByTutor.put(tutorId, List.of(newApplication, acceptedApplication));
        context.studentCardsByTutor.put(tutorId, List.of(
                createStudentCard(UUID.randomUUID(), acceptedApplication.getId(), tutorId, "Мария Смирнова")
        ));
        context.lessonsByTutor.put(tutorId, List.of(nextLesson));

        TutorOverviewPageDto page = service.getTutorOverviewPage(tutorId);

        assertEquals(1, page.newRequests());
        assertEquals(1, page.activeStudents());
        assertEquals(1, page.upcomingLessons());
        assertEquals(nextLesson.getId().toString(), page.nextLesson().id());
        assertEquals("Мария Смирнова", page.nextLesson().studentName());
    }

    @Test
    void getTutorOverviewPage_handlesLessonWithoutApplicationId() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Lesson nextLesson = createLesson(
                UUID.randomUUID(),
                null,
                tutorId,
                clientId,
                "Анна Смирнова",
                "Мария Смирнова",
                "Физика с Марией",
                "Физика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 6, 14, 0),
                60,
                "Запланировано"
        );

        context.lessonsByTutor.put(tutorId, List.of(nextLesson));

        TutorOverviewPageDto page = service.getTutorOverviewPage(tutorId);

        assertEquals(0, page.newRequests());
        assertEquals(0, page.activeStudents());
        assertEquals(1, page.upcomingLessons());
        assertEquals(nextLesson.getId().toString(), page.nextLesson().id());
        assertEquals(null, page.nextLesson().applicationId());
    }

    @Test
    void getTutorRequestsPage_mapsFunnelSlaAndLeadScore() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID newApplicationId = UUID.randomUUID();
        UUID studentApplicationId = UUID.randomUUID();

        CrmApplication overdue = createApplication(
                newApplicationId,
                tutorId,
                ApplicationStatus.NEW,
                "Иван Петров",
                "9 класс",
                "Пробный урок",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        overdue.setResponseDeadline(Instant.parse("2026-05-06T09:00:00Z"));
        overdue.setLeadScore(80);
        overdue.setSource("MARKETPLACE");

        CrmApplication student = createApplication(
                studentApplicationId,
                tutorId,
                ApplicationStatus.STUDENT,
                "Мария Смирнова",
                "10 класс",
                "ЕГЭ",
                "Физика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 9, 18, 0)
        );
        student.setRespondedAt(Instant.parse("2026-05-05T10:30:00Z"));
        student.setTrialScheduledAt(Instant.parse("2026-05-05T11:00:00Z"));
        student.setConvertedAt(Instant.parse("2026-05-06T08:00:00Z"));
        student.setLeadScore(100);
        student.setSource("MARKETPLACE");

        context.applicationsByTutor.put(tutorId, List.of(overdue, student));

        TutorRequestsPageDto page = service.getTutorRequestsPage(tutorId);

        TutorRequestsPageDto.RequestDto overdueDto = page.requests().get(0);
        assertEquals("NEW", overdueDto.stage());
        assertEquals("MARKETPLACE", overdueDto.source());
        assertTrue(overdueDto.overdue());
        assertEquals(80, overdueDto.leadScore());
        assertEquals("Ответить на заявку", overdueDto.nextAction());

        assertEquals(1, page.funnel().newCount());
        assertEquals(1, page.funnel().studentCount());
        assertEquals(50, page.funnel().studentConversionPercent());
    }

    @Test
    void getTutorLessonCandidates_returnsAcceptedApplicationWithStudentCardAndNoLesson() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        LocalDateTime desiredStartTime = LocalDateTime.of(2026, 5, 8, 16, 0);

        CrmApplication application = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.ACCEPTED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                desiredStartTime
        );
        StudentCard studentCard = createStudentCard(studentId, applicationId, tutorId, "Иван Петров");
        context.applicationsByTutor.put(tutorId, List.of(application));
        context.studentCardsByTutor.put(tutorId, List.of(studentCard));
        context.lessonsByTutor.put(tutorId, List.of());

        List<TutorLessonCandidateDto> candidates = service.getTutorLessonCandidates(tutorId);

        assertEquals(1, candidates.size());
        assertEquals(applicationId.toString(), candidates.get(0).applicationId());
        assertEquals(studentId.toString(), candidates.get(0).studentId());
        assertEquals("Иван Петров", candidates.get(0).studentName());
        assertEquals("Математика", candidates.get(0).subject());
        assertEquals("Онлайн", candidates.get(0).format());
        assertEquals("2026-05-08T16:00", candidates.get(0).desiredStartTime());
        assertEquals("9 класс · Подготовка к ОГЭ", candidates.get(0).gradeGoal());
    }

    @Test
    void getTutorRequestsPage_mapsAcceptedRequestLabelAndThreadLink() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();

        CrmApplication application = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.ACCEPTED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        MessageThread thread = createThread(threadId, applicationId, application.getClientUserId(), tutorId, "Анна Смирнова", "АС", "Математика", "Текст");
        context.applicationsByTutor.put(tutorId, List.of(application));
        context.messageThreadByApplication.put(applicationId, thread);

        TutorRequestsPageDto page = service.getTutorRequestsPage(tutorId);

        assertEquals("Принята", page.requests().get(0).status());
        assertEquals("ACCEPTED", page.requests().get(0).stage());
        assertEquals(threadId.toString(), page.requests().get(0).threadId());
        assertEquals("Назначить пробный урок", page.requests().get(0).nextAction());
        assertEquals("Полная заявка", page.requests().get(0).completenessLabel());
        assertEquals(null, page.requests().get(0).scheduledLessonId());
        assertEquals(null, page.requests().get(0).scheduledLessonAt());
    }

    @Test
    void getTutorRequestsPage_includesScheduledLessonContext() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        CrmApplication application = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.TRIAL_SCHEDULED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        Lesson lesson = createLesson(
                lessonId,
                applicationId,
                tutorId,
                application.getClientUserId(),
                "Анна Смирнова",
                "Иван Петров",
                "Математика · пробный урок",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 9, 10, 0),
                60,
                "Запланировано"
        );
        context.applicationsByTutor.put(tutorId, List.of(application));
        context.lessonsByTutor.put(tutorId, List.of(lesson));

        TutorRequestsPageDto page = service.getTutorRequestsPage(tutorId);

        assertEquals("Пробный урок назначен", page.requests().get(0).nextAction());
        assertEquals(lessonId.toString(), page.requests().get(0).scheduledLessonId());
        assertEquals("2026-05-09T10:00", page.requests().get(0).scheduledLessonAt());
    }

    @Test
    void getTutorCalendarPage_includesAcceptedRequestsWaitingForLesson() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();

        CrmApplication application = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.ACCEPTED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        StudentCard studentCard = createStudentCard(studentId, applicationId, tutorId, "Иван Петров");
        MessageThread thread = createThread(threadId, applicationId, application.getClientUserId(), tutorId, "Иван Петров", "ИП", "Математика", "Текст");
        context.applicationsByTutor.put(tutorId, List.of(application));
        context.studentCardsByTutor.put(tutorId, List.of(studentCard));
        context.messageThreadByApplication.put(applicationId, thread);
        context.lessonsByTutor.put(tutorId, List.of());

        TutorCalendarPageDto page = service.getTutorCalendarPage(tutorId, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10));

        assertEquals(1, page.pendingRequests().size());
        assertEquals(applicationId.toString(), page.pendingRequests().get(0).applicationId());
        assertEquals(studentId.toString(), page.pendingRequests().get(0).studentId());
        assertEquals(threadId.toString(), page.pendingRequests().get(0).threadId());
        assertEquals("Иван Петров", page.pendingRequests().get(0).studentName());
        assertEquals("2026-05-08T16:00", page.pendingRequests().get(0).desiredStartTime());
    }

    @Test
    void getTutorLessonCandidates_excludesAcceptedApplicationWithExistingLesson() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        CrmApplication application = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.ACCEPTED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        StudentCard studentCard = createStudentCard(UUID.randomUUID(), applicationId, tutorId, "Иван Петров");
        Lesson lesson = createLesson(
                UUID.randomUUID(),
                applicationId,
                tutorId,
                null,
                "Анна Смирнова",
                "Иван Петров",
                "Математика",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 9, 10, 0),
                60,
                "Запланировано"
        );
        context.applicationsByTutor.put(tutorId, List.of(application));
        context.studentCardsByTutor.put(tutorId, List.of(studentCard));
        context.lessonsByTutor.put(tutorId, List.of(lesson));

        List<TutorLessonCandidateDto> candidates = service.getTutorLessonCandidates(tutorId);

        assertTrue(candidates.isEmpty());
    }

    @Test
    void getTutorLessonCandidates_excludesCandidateWhenCanceledLessonExists() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        CrmApplication application = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.ACCEPTED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        StudentCard studentCard = createStudentCard(UUID.randomUUID(), applicationId, tutorId, "Иван Петров");
        Lesson canceledLesson = createLesson(
                UUID.randomUUID(),
                applicationId,
                tutorId,
                null,
                "Анна Смирнова",
                "Иван Петров",
                "Отмененный урок",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 9, 10, 0),
                60,
                "Отменено"
        );
        context.applicationsByTutor.put(tutorId, List.of(application));
        context.studentCardsByTutor.put(tutorId, List.of(studentCard));
        context.lessonsByTutor.put(tutorId, List.of(canceledLesson));

        List<TutorLessonCandidateDto> candidates = service.getTutorLessonCandidates(tutorId);

        assertTrue(candidates.isEmpty());
    }

    @Test
    void getTutorLessonCandidates_excludesRejectedApplication() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        CrmApplication application = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.REJECTED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        StudentCard studentCard = createStudentCard(UUID.randomUUID(), applicationId, tutorId, "Иван Петров");
        context.applicationsByTutor.put(tutorId, List.of(application));
        context.studentCardsByTutor.put(tutorId, List.of(studentCard));
        context.lessonsByTutor.put(tutorId, List.of());

        List<TutorLessonCandidateDto> candidates = service.getTutorLessonCandidates(tutorId);

        assertTrue(candidates.isEmpty());
    }

    @Test
    void getClientSchedulePage_mapsLessonActionsAndPeriod() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDate weekStart = LocalDate.now(clock).with(DayOfWeek.MONDAY);
        LocalDateTime lessonStart = weekStart.plusDays(2).atTime(18, 0);

        Lesson lesson = createLesson(
                lessonId,
                applicationId,
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Математика с Анной Смирновой",
                "Математика",
                "Онлайн",
                lessonStart,
                60,
                "Запланировано"
        );

        MessageThread thread = new MessageThread();
        thread.setId(UUID.randomUUID());
        thread.setApplicationId(applicationId);

        context.lessonsByClient.put(clientId, List.of(lesson));
        context.messageThreadByApplication.put(applicationId, thread);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(1, page.events().size());
        assertEquals(thread.getId().toString(), page.events().get(0).threadId());
        assertEquals(applicationId.toString(), page.events().get(0).applicationId());
        assertTrue(page.events().get(0).canJoin());
        assertTrue(page.events().get(0).canReschedule());
        assertTrue(page.events().get(0).canMessageTutor());
        assertFalse(page.emptyState().show());
        assertEquals(1, page.upcomingLessons().size());
        assertTrue(page.upcomingLessons().get(0).canJoin());
        assertTrue(page.upcomingLessons().get(0).canReschedule());
        assertEquals(weekStart.toString(), page.period().from());
        assertEquals(weekStart.plusDays(4).toString(), page.period().to());
        assertEquals(
                String.format("%02d-%02d", weekStart.getDayOfMonth(), weekStart.plusDays(4).getDayOfMonth()),
                page.period().label()
        );
    }

    @Test
    void getClientSchedulePage_whenNoLessons_returnsEmptyState() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();

        context.lessonsByClient.put(clientId, List.of());
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertTrue(page.emptyState().show());
        assertEquals("Уроков пока нет", page.emptyState().title());
        assertEquals(0, page.events().size());
        assertEquals(0, page.upcomingLessons().size());
        assertEquals("0", page.metrics().get(3).value());
        assertNotEquals("ok", page.metrics().get(3).value());
    }

    @Test
    void getClientSchedulePage_surfacesPendingRescheduleRequests() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDate weekStart = LocalDate.now(clock).with(DayOfWeek.MONDAY);
        LocalDate today = LocalDate.now(clock);

        Lesson lesson = createLesson(
                lessonId,
                applicationId,
                null,
                clientId,
                "Марина Белова",
                "Иван Петров",
                "Английский язык с Мариной Беловой",
                "Английский язык",
                "Онлайн",
                today.atTime(17, 0),
                60,
                "Запланировано"
        );

        Lesson nextWeekLesson = createLesson(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                clientId,
                "Марина Белова",
                "Иван Петров",
                "Следующая неделя",
                "Английский язык",
                "Онлайн",
                weekStart.plusDays(7).atTime(19, 30),
                60,
                "Запланировано"
        );

        LessonRescheduleRequest request = new LessonRescheduleRequest();
        request.setId(UUID.randomUUID());
        request.setLessonId(lessonId);
        request.setApplicationId(applicationId);
        request.setLessonTitle("Английский язык");
        request.setLessonStartsAt(today.plusDays(1).atTime(19, 30));
        request.setStatus(LessonRescheduleStatus.PENDING);

        context.lessonsByClient.put(clientId, List.of(lesson, nextWeekLesson));
        context.messageThreadByApplication.put(applicationId, null);
        context.reschedulesByClient.put(clientId, List.of(request));

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(1, page.rescheduleRequests().size());
        assertEquals(request.getId().toString(), page.rescheduleRequests().get(0).id());
        assertEquals("ожидает ответа", page.rescheduleRequests().get(0).status());
        assertEquals(request.getLessonStartsAt().toString(), page.rescheduleRequests().get(0).startsAt());
        assertEquals(List.of("17:00", "19:30"), page.timeSlots());
        assertEquals("1", page.metrics().get(0).value());
        assertEquals("1", page.metrics().get(1).value());
        assertEquals("1", page.metrics().get(3).value());
        assertNotEquals("Домашка", page.metrics().get(2).label());
        assertEquals(1, page.events().size());
        assertFalse(page.events().get(0).canMessageTutor());
        assertEquals(0, page.events().get(0).row());
        assertEquals(3, page.events().get(0).column());
    }

    @Test
    void getClientSchedulePage_pastLessonsDisableActions() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDateTime lessonStart = LocalDateTime.now(clock).minusHours(2);

        Lesson lesson = createLesson(
                lessonId,
                applicationId,
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Прошедший урок",
                "Математика",
                "Онлайн",
                lessonStart,
                60,
                "Запланировано"
        );

        context.lessonsByClient.put(clientId, List.of(lesson));
        context.messageThreadByApplication.put(applicationId, null);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(1, page.events().size());
        assertFalse(page.events().get(0).canJoin());
        assertFalse(page.events().get(0).canReschedule());
        assertFalse(page.events().get(0).canMessageTutor());
        assertEquals(0, page.upcomingLessons().size());
    }

    @Test
    void getClientSchedulePage_inProgressLessonStaysJoinableInUpcomingList() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDateTime lessonStart = LocalDateTime.now(clock).minusMinutes(30);

        Lesson lesson = createLesson(
                lessonId,
                applicationId,
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Идет урок",
                "Математика",
                "Онлайн",
                lessonStart,
                60,
                "Запланировано"
        );

        context.lessonsByClient.put(clientId, List.of(lesson));
        context.messageThreadByApplication.put(applicationId, null);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(1, page.events().size());
        assertTrue(page.events().get(0).canJoin());
        assertFalse(page.events().get(0).canReschedule());
        assertEquals(1, page.upcomingLessons().size());
        assertTrue(page.upcomingLessons().get(0).canJoin());
        assertFalse(page.upcomingLessons().get(0).canReschedule());
    }

    @Test
    void getClientSchedulePage_nullDurationUsesDefaultDurationWithoutFailure() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDateTime lessonStart = LocalDateTime.now(clock).minusMinutes(30);

        Lesson lesson = createLesson(
                UUID.randomUUID(),
                applicationId,
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Урок без длительности",
                "Математика",
                "Онлайн",
                lessonStart,
                null,
                "Запланировано"
        );

        context.lessonsByClient.put(clientId, List.of(lesson));
        context.messageThreadByApplication.put(applicationId, null);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(1, page.events().size());
        assertEquals(lessonStart.plusMinutes(60).toString(), page.events().get(0).endsAt());
        assertTrue(page.events().get(0).canJoin());
        assertEquals(1, page.upcomingLessons().size());
    }

    @Test
    void getClientSchedulePage_onWeekendShowsUpcomingWorkWeek() {
        Clock weekendClock = Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneId.of("UTC"));
        TestContext context = new TestContext();
        PageViewService service = createService(context, weekendClock);
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDate nextWeekStart = LocalDate.now(weekendClock).with(DayOfWeek.MONDAY).plusWeeks(1);

        Lesson lesson = createLesson(
                lessonId,
                applicationId,
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Урок на следующей неделе",
                "Математика",
                "Онлайн",
                nextWeekStart.atTime(18, 0),
                60,
                "Запланировано"
        );

        context.lessonsByClient.put(clientId, List.of(lesson));
        context.messageThreadByApplication.put(applicationId, null);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(nextWeekStart.toString(), page.period().from());
        assertEquals(nextWeekStart.plusDays(4).toString(), page.period().to());
        assertEquals(1, page.events().size());
        assertEquals(nextWeekStart.atTime(18, 0).toString(), page.events().get(0).startsAt());
    }

    @Test
    void getClientSchedulePage_onWeekendIgnoresCanceledLessonsForWeekSelection() {
        Clock weekendClock = Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneId.of("UTC"));
        TestContext context = new TestContext();
        PageViewService service = createService(context, weekendClock);
        UUID clientId = UUID.randomUUID();
        LocalDate currentWeekStart = LocalDate.now(weekendClock).with(DayOfWeek.MONDAY);
        LocalDate nextWeekStart = currentWeekStart.plusWeeks(1);

        Lesson canceledNextWeekLesson = createLesson(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Отмененный урок",
                "Математика",
                "Онлайн",
                nextWeekStart.atTime(18, 0),
                60,
                "Отменено"
        );

        context.lessonsByClient.put(clientId, List.of(canceledNextWeekLesson));
        context.messageThreadByApplication.put(canceledNextWeekLesson.getApplicationId(), null);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(currentWeekStart.toString(), page.period().from());
        assertTrue(page.events().isEmpty());
        assertTrue(page.upcomingLessons().isEmpty());
        assertEquals("0", page.metrics().get(2).value());
    }

    @Test
    void getClientSchedulePage_usesLessonDurationForJoinabilityAndUpcomingState() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();

        Lesson longLesson = createLesson(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Длинный урок",
                "Математика",
                "Онлайн",
                LocalDateTime.now(clock).minusMinutes(90),
                120,
                "Запланировано"
        );

        Lesson shortLesson = createLesson(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                clientId,
                "Марина Белова",
                "Иван Петров",
                "Короткий урок",
                "Английский язык",
                "Онлайн",
                LocalDateTime.now(clock).minusMinutes(45),
                30,
                "Запланировано"
        );

        context.lessonsByClient.put(clientId, List.of(longLesson, shortLesson));
        context.messageThreadByApplication.put(longLesson.getApplicationId(), null);
        context.messageThreadByApplication.put(shortLesson.getApplicationId(), null);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(2, page.events().size());
        assertTrue(page.events().stream()
                .filter(event -> event.lessonId().equals(longLesson.getId().toString()))
                .findFirst()
                .orElseThrow()
                .canJoin());
        assertFalse(page.events().stream()
                .filter(event -> event.lessonId().equals(shortLesson.getId().toString()))
                .findFirst()
                .orElseThrow()
                .canJoin());
        assertEquals(List.of(longLesson.getId().toString()), page.upcomingLessons().stream().map(ClientSchedulePageDto.UpcomingLessonDto::lessonId).toList());
    }

    @Test
    void getClientSchedulePage_nonScheduledLessonsDisableActions() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDate weekStart = LocalDate.now(clock).with(DayOfWeek.MONDAY);
        LocalDateTime lessonStart = weekStart.plusDays(1).atTime(13, 30);

        Lesson lesson = createLesson(
                lessonId,
                applicationId,
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Урок со статусом неактивен",
                "Математика",
                "Онлайн",
                lessonStart,
                60,
                "Отменено"
        );

        context.lessonsByClient.put(clientId, List.of(lesson));
        context.messageThreadByApplication.put(applicationId, null);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(1, page.events().size());
        assertFalse(page.events().get(0).canJoin());
        assertFalse(page.events().get(0).canReschedule());
        assertEquals(0, page.events().get(0).row());
        assertEquals(2, page.events().get(0).column());
    }

    @Test
    void getClientSchedulePage_excludesCanceledLessonsFromUpcomingViewsAndMetrics() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        LocalDateTime futureStart = LocalDate.now(clock).atTime(17, 0);

        Lesson scheduledLesson = createLesson(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Активный урок",
                "Математика",
                "Онлайн",
                futureStart,
                60,
                "Запланировано"
        );
        Lesson canceledLesson = createLesson(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                clientId,
                "Марина Белова",
                "Иван Петров",
                "Отмененный урок",
                "Английский язык",
                "Онлайн",
                futureStart.plusHours(2),
                60,
                "Отменено"
        );

        context.lessonsByClient.put(clientId, List.of(scheduledLesson, canceledLesson));
        context.messageThreadByApplication.put(scheduledLesson.getApplicationId(), null);
        context.messageThreadByApplication.put(canceledLesson.getApplicationId(), null);
        context.reschedulesByClient.put(clientId, List.of());

        ClientSchedulePageDto page = service.getClientSchedulePage(clientId);

        assertEquals(List.of(scheduledLesson.getId().toString()), page.upcomingLessons().stream().map(ClientSchedulePageDto.UpcomingLessonDto::lessonId).toList());
        assertEquals("1", page.metrics().get(2).value());
    }

    @Test
    void getClientHistoryPage_mapsPastLessonsApplicationsAndMonthSummary() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID pastApplicationId = UUID.randomUUID();
        UUID futureApplicationId = UUID.randomUUID();

        Lesson pastLesson = createLesson(
                UUID.randomUUID(),
                pastApplicationId,
                null,
                clientId,
                "Анна Смирнова",
                "Иван Петров",
                "Математика с Анной",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 2, 14, 0),
                60,
                "Запланировано"
        );
        Lesson futureLesson = createLesson(
                UUID.randomUUID(),
                futureApplicationId,
                null,
                clientId,
                "Марина Белова",
                "Иван Петров",
                "Будущий урок",
                "Английский язык",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0),
                60,
                "Запланировано"
        );
        CrmApplication cancelledApplication = createApplication(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ApplicationStatus.CANCELLED,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Физика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 4, 18, 0)
        );
        cancelledApplication.setClientUserId(clientId);
        cancelledApplication.setCreatedAt(Instant.parse("2026-05-03T12:00:00Z"));
        cancelledApplication.setUpdatedAt(Instant.parse("2026-05-03T13:00:00Z"));

        context.lessonsByClient.put(clientId, List.of(pastLesson, futureLesson));
        context.applicationsByClient.put(clientId, List.of(cancelledApplication));

        ClientHistoryPageDto page = service.getClientHistoryPage(clientId);

        assertEquals(List.of("Все записи", "Занятия", "Заявки"), page.filters());
        assertEquals("Май 2026", page.activeMonth());
        assertEquals(List.of("2", "1", "1"), page.stats().stream().map(ClientHistoryPageDto.StatDto::value).toList());
        assertEquals(2, page.events().size());
        assertEquals(List.of("application", "lesson"), page.events().stream().map(ClientHistoryPageDto.EventDto::type).toList());
        assertEquals(1, page.completedLessons().size());
        assertEquals(pastLesson.getId().toString(), page.completedLessons().get(0).lessonId());
        assertEquals("Анна Смирнова", page.completedLessons().get(0).tutor());
        assertEquals("/app/client/applications/" + pastApplicationId, page.completedLessons().get(0).href());
        assertEquals(List.of("Прошедшие занятия", "Заявки за месяц", "Закрытые заявки"), page.monthSummary().stream().map(ClientHistoryPageDto.SummaryRowDto::title).toList());
        assertFalse(page.emptyState().show());
    }

    @Test
    void getClientMessagesPage_canOpenRequestedThreadFromQuery() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID firstApplicationId = UUID.randomUUID();
        UUID secondApplicationId = UUID.randomUUID();
        UUID firstThreadId = UUID.randomUUID();
        UUID secondThreadId = UUID.randomUUID();

        CrmApplication firstApplication = createApplication(
                firstApplicationId,
                UUID.randomUUID(),
                ApplicationStatus.NEW,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        firstApplication.setClientUserId(clientId);
        CrmApplication secondApplication = createApplication(
                secondApplicationId,
                UUID.randomUUID(),
                ApplicationStatus.NEW,
                "Мария Иванова",
                "8 класс",
                "Разговорная практика",
                "Английский язык",
                "Онлайн",
                LocalDateTime.of(2026, 5, 9, 17, 0)
        );
        secondApplication.setClientUserId(clientId);

        MessageThread firstThread = createThread(firstThreadId, firstApplicationId, clientId, firstApplication.getTutorUserId(), "Анна Смирнова", "АС", "Математика", "Первый диалог");
        MessageThread secondThread = createThread(secondThreadId, secondApplicationId, clientId, secondApplication.getTutorUserId(), "Марина Белова", "МБ", "Английский язык", "Второй диалог");
        Message secondMessage = createMessage(secondThreadId, "tutor", "Марина Белова", "Здравствуйте, можем начать завтра.");

        context.applicationsById.put(firstApplicationId, firstApplication);
        context.applicationsById.put(secondApplicationId, secondApplication);
        context.threadsByClient.put(clientId, List.of(firstThread, secondThread));
        context.messagesByThread.put(secondThreadId, List.of(secondMessage));

        ClientMessagesPageDto page = service.getClientMessagesPage(clientId, secondThreadId);

        assertEquals(secondThreadId.toString(), page.activeDialog().threadId());
        assertEquals(List.of(firstThreadId.toString(), secondThreadId.toString()), page.conversations().stream().map(ClientMessagesPageDto.ConversationDto::id).toList());
        assertFalse(page.conversations().get(0).active());
        assertTrue(page.conversations().get(1).active());
        assertEquals("Марина Белова", page.activeDialog().name());
        assertEquals("Здравствуйте, можем начать завтра.", page.messages().get(0).text());
    }

    @Test
    void getTutorMessagesPage_withoutThreadsReturnsEmptyStatePayload() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();

        context.threadsByTutor.put(tutorId, List.of());

        TutorMessagesPageDto page = service.getTutorMessagesPage(tutorId, null);

        assertEquals("0", page.metrics().get(0).value());
        assertTrue(page.conversations().isEmpty());
        assertEquals("", page.activeDialog().threadId());
        assertTrue(page.messages().isEmpty());
        assertTrue(page.details().isEmpty());
    }

    @Test
    void getMessagesPageLabelsAuthorsFromCurrentUserPerspective() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID clientId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();

        CrmApplication application = createApplication(
                applicationId,
                tutorId,
                ApplicationStatus.NEW,
                "Иван Петров",
                "9 класс",
                "Подготовка к ОГЭ",
                "Математика",
                "Онлайн",
                LocalDateTime.of(2026, 5, 8, 16, 0)
        );
        application.setClientUserId(clientId);
        MessageThread thread = createThread(threadId, applicationId, clientId, tutorId, "Анна Смирнова", "АС", "Математика", "Текст");
        Message clientMessage = createMessage(threadId, "client", "Вы", "Здравствуйте");
        Message tutorMessage = createMessage(threadId, "tutor", "Репетитор", "Здравствуйте, готова помочь.");

        context.applicationsById.put(applicationId, application);
        context.threadsByClient.put(clientId, List.of(thread));
        context.threadsByTutor.put(tutorId, List.of(thread));
        context.messagesByThread.put(threadId, List.of(clientMessage, tutorMessage));

        ClientMessagesPageDto clientPage = service.getClientMessagesPage(clientId, threadId);
        TutorMessagesPageDto tutorPage = service.getTutorMessagesPage(tutorId, threadId);

        assertEquals(List.of("Вы", "Анна Смирнова"), clientPage.messages().stream().map(ClientMessagesPageDto.MessageDto::author).toList());
        assertEquals(List.of("Иван Петров", "Вы"), tutorPage.messages().stream().map(TutorMessagesPageDto.MessageDto::author).toList());
        assertEquals("Иван Петров", tutorPage.activeDialog().name());
        assertEquals("ИП", tutorPage.activeDialog().initials());
    }

    @Test
    void getTutorCalendarPage_includesSlotsWhenSchedulesExist() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        LocalDate today = LocalDate.now(clock);

        context.lessonsByTutor.put(tutorId, List.of());
        context.generatedSlots = List.of(
                new SlotDto(today, LocalTime.of(15, 0), LocalTime.of(16, 0), true, "RULE"),
                new SlotDto(today, LocalTime.of(16, 0), LocalTime.of(17, 0), false, "RULE")
        );

        TutorCalendarPageDto page = service.getTutorCalendarPage(tutorId, null, null);

        assertEquals(2, page.slots().size());
        assertEquals(LocalTime.of(15, 0), page.slots().get(0).startTime());
        assertTrue(page.slots().get(0).available());
        assertFalse(page.slots().get(1).available());
    }

    @Test
    void getTutorCalendarPage_withExplicitDates_passesExactRangeToSlotService() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 10);

        context.lessonsByTutor.put(tutorId, List.of());
        context.generatedSlots = List.of();

        service.getTutorCalendarPage(tutorId, from, to);

        assertEquals(tutorId, context.availabilitySlotService.lastTutorUserId);
        assertEquals(from, context.availabilitySlotService.lastFrom);
        assertEquals(to, context.availabilitySlotService.lastTo);
        assertEquals(null, context.availabilitySlotService.lastSubjectId);
    }

    @Test
    void getTutorCalendarPage_withOnlyFrom_defaultsToToPlusSixDays() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate expectedTo = from.plusDays(6);

        context.lessonsByTutor.put(tutorId, List.of());
        context.generatedSlots = List.of();

        service.getTutorCalendarPage(tutorId, from, null);

        assertEquals(tutorId, context.availabilitySlotService.lastTutorUserId);
        assertEquals(from, context.availabilitySlotService.lastFrom);
        assertEquals(expectedTo, context.availabilitySlotService.lastTo);
        assertEquals(null, context.availabilitySlotService.lastSubjectId);
    }

    @Test
    void getTutorCalendarPage_whenFromAfterTo_throwsIllegalArgumentException() {
        TestContext context = new TestContext();
        PageViewService service = createService(context);
        UUID tutorId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 10);
        LocalDate to = LocalDate.of(2026, 6, 1);

        context.lessonsByTutor.put(tutorId, List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.getTutorCalendarPage(tutorId, from, to)
        );

        assertEquals("'from' date must not be after 'to' date", exception.getMessage());
    }

    private PageViewService createService(TestContext context) {
        return createService(context, clock);
    }

    private PageViewService createService(TestContext context, Clock serviceClock) {
        return new PageViewService(
                context.crmApplicationRepository,
                context.studentCardRepository,
                context.lessonRepository,
                context.lessonRescheduleRequestRepository,
                context.messageThreadRepository,
                context.messageRepository,
                context.availabilitySlotService,
                serviceClock
        );
    }

    private CrmApplication createApplication(UUID applicationId,
                                             UUID tutorId,
                                             ApplicationStatus status,
                                             String studentName,
                                             String studentGrade,
                                             String goal,
                                             String subjectName,
                                             String format,
                                             LocalDateTime desiredStartTime) {
        CrmApplication application = new CrmApplication();
        application.setId(applicationId);
        application.setTutorUserId(tutorId);
        application.setClientUserId(UUID.randomUUID());
        application.setMarketplaceRequestId(UUID.randomUUID());
        application.setTutorProfileId(UUID.randomUUID());
        application.setTutorSlug("anna-smirnova");
        application.setTutorName("Анна Смирнова");
        application.setStudentName(studentName);
        application.setStudentGrade(studentGrade);
        application.setSubjectId(UUID.randomUUID());
        application.setSubjectName(subjectName);
        application.setFormat(format);
        application.setGoal(goal);
        application.setComment("Комментарий");
        application.setDesiredStartTime(desiredStartTime);
        application.setPricePerHour(BigDecimal.valueOf(1500));
        application.setStatus(status);
        return application;
    }

    private StudentCard createStudentCard(UUID studentId,
                                          UUID applicationId,
                                          UUID tutorId,
                                          String studentName) {
        StudentCard studentCard = new StudentCard();
        studentCard.setId(studentId);
        studentCard.setApplicationId(applicationId);
        studentCard.setClientUserId(UUID.randomUUID());
        studentCard.setTutorUserId(tutorId);
        studentCard.setName(studentName);
        studentCard.setDetails("9 класс");
        studentCard.setSubject("Математика");
        studentCard.setStatus("Активен");
        studentCard.setNote("Заметка");
        studentCard.setProgress(20);
        return studentCard;
    }

    private Lesson createLesson(UUID lessonId,
                                UUID applicationId,
                                UUID tutorId,
                                UUID clientId,
                                String tutorName,
                                String studentName,
                                String title,
                                String subjectName,
                                String format,
                                LocalDateTime startsAt,
                                Integer durationMinutes,
                                String status) {
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setApplicationId(applicationId);
        lesson.setTutorUserId(tutorId == null ? UUID.randomUUID() : tutorId);
        lesson.setClientUserId(clientId);
        lesson.setTutorName(tutorName);
        lesson.setStudentName(studentName);
        lesson.setTitle(title);
        lesson.setSubjectName(subjectName);
        lesson.setFormat(format);
        lesson.setStartsAt(startsAt);
        lesson.setDurationMinutes(durationMinutes);
        lesson.setStatus(status);
        return lesson;
    }

    private MessageThread createThread(UUID threadId,
                                       UUID applicationId,
                                       UUID clientId,
                                       UUID tutorId,
                                       String participantName,
                                       String participantInitials,
                                       String subject,
                                       String preview) {
        MessageThread thread = new MessageThread();
        thread.setId(threadId);
        thread.setApplicationId(applicationId);
        thread.setClientUserId(clientId);
        thread.setTutorUserId(tutorId);
        thread.setParticipantName(participantName);
        thread.setParticipantInitials(participantInitials);
        thread.setSubject(subject);
        thread.setSubtitle("заявка отправлена");
        thread.setPreview(preview);
        thread.setCreatedAt(Instant.parse("2026-05-06T09:00:00Z"));
        thread.setUpdatedAt(Instant.parse("2026-05-06T09:30:00Z"));
        return thread;
    }

    private Message createMessage(UUID threadId, String authorRole, String authorName, String text) {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setThreadId(threadId);
        message.setAuthorRole(authorRole);
        message.setAuthorName(authorName);
        message.setText(text);
        message.setCreatedAt(Instant.parse("2026-05-06T09:45:00Z"));
        return message;
    }

    private static final class TestContext {
        private final Map<UUID, List<CrmApplication>> applicationsByTutor = new HashMap<>();
        private final Map<UUID, List<CrmApplication>> applicationsByClient = new HashMap<>();
        private final Map<UUID, List<StudentCard>> studentCardsByTutor = new HashMap<>();
        private final Map<UUID, List<Lesson>> lessonsByTutor = new HashMap<>();
        private final Map<UUID, List<Lesson>> lessonsByClient = new HashMap<>();
        private final Map<UUID, List<LessonRescheduleRequest>> reschedulesByClient = new HashMap<>();
        private final Map<UUID, MessageThread> messageThreadByApplication = new HashMap<>();
        private final Map<UUID, CrmApplication> applicationsById = new HashMap<>();
        private final Map<UUID, List<MessageThread>> threadsByClient = new HashMap<>();
        private final Map<UUID, List<MessageThread>> threadsByTutor = new HashMap<>();
        private final Map<UUID, List<Message>> messagesByThread = new HashMap<>();
        private List<SlotDto> generatedSlots = List.of();

        private final CrmApplicationRepository crmApplicationRepository = proxy(CrmApplicationRepository.class, (method, args) -> switch (method) {
            case "findByTutorUserIdOrderByCreatedAtDesc" -> applicationsByTutor.getOrDefault((UUID) args[0], List.of());
            case "findByClientUserIdOrderByCreatedAtDesc" -> applicationsByClient.getOrDefault((UUID) args[0], List.of());
            case "findById" -> Optional.ofNullable(applicationsById.get((UUID) args[0]));
            default -> unsupported(method);
        });

        private final StudentCardRepository studentCardRepository = proxy(StudentCardRepository.class, (method, args) -> switch (method) {
            case "findByTutorUserIdOrderByCreatedAtDesc" -> studentCardsByTutor.getOrDefault((UUID) args[0], List.of());
            case "findByApplicationId" -> studentCardsByTutor.values().stream()
                    .flatMap(List::stream)
                    .filter(card -> card.getApplicationId().equals(args[0]))
                    .findFirst();
            default -> unsupported(method);
        });

        private final LessonRepository lessonRepository = proxy(LessonRepository.class, (method, args) -> switch (method) {
            case "findByTutorUserIdOrderByStartsAtAsc" -> lessonsByTutor.getOrDefault((UUID) args[0], List.of());
            case "findByClientUserIdOrderByStartsAtAsc" -> lessonsByClient.getOrDefault((UUID) args[0], List.of());
            case "findByTutorUserIdAndStartsAtBetween" -> List.of();
            default -> unsupported(method);
        });

        private final LessonRescheduleRequestRepository lessonRescheduleRequestRepository = proxy(LessonRescheduleRequestRepository.class, (method, args) -> switch (method) {
            case "findByClientUserIdOrderByCreatedAtDesc" -> reschedulesByClient.getOrDefault((UUID) args[0], List.of());
            case "findTopByLessonIdOrderByCreatedAtDesc" -> Optional.empty();
            case "existsByLessonIdAndStatus" -> false;
            default -> unsupported(method);
        });

        private final MessageThreadRepository messageThreadRepository = proxy(MessageThreadRepository.class, (method, args) -> switch (method) {
            case "findByApplicationId" -> Optional.ofNullable(messageThreadByApplication.get(args[0]));
            case "findByTutorUserIdOrderByUpdatedAtDesc" -> threadsByTutor.getOrDefault((UUID) args[0], List.of());
            case "findByClientUserIdOrderByUpdatedAtDesc" -> threadsByClient.getOrDefault((UUID) args[0], List.of());
            case "findByIdAndTutorUserId" -> Optional.empty();
            case "findByIdAndClientUserId" -> threadsByClient.values().stream()
                    .flatMap(List::stream)
                    .filter(thread -> thread.getId().equals(args[0]) && thread.getClientUserId().equals(args[1]))
                    .findFirst();
            default -> unsupported(method);
        });

        private final MessageRepository messageRepository = proxy(MessageRepository.class, (method, args) -> switch (method) {
            case "findByThreadIdOrderByCreatedAtAsc" -> messagesByThread.getOrDefault((UUID) args[0], List.of());
            default -> unsupported(method);
        });

        private final FakeAvailabilitySlotService availabilitySlotService = new FakeAvailabilitySlotService(this);
    }

    private static final class FakeAvailabilitySlotService extends AvailabilitySlotService {
        private final TestContext context;
        private UUID lastTutorUserId;
        private LocalDate lastFrom;
        private LocalDate lastTo;
        private UUID lastSubjectId;

        private FakeAvailabilitySlotService(TestContext context) {
            super(null, null, null);
            this.context = context;
        }

        @Override
        public List<SlotDto> generateSlots(UUID tutorUserId, LocalDate from, LocalDate to, UUID subjectId) {
            this.lastTutorUserId = tutorUserId;
            this.lastFrom = from;
            this.lastTo = to;
            this.lastSubjectId = subjectId;
            return context.generatedSlots;
        }
    }

    @FunctionalInterface
    private interface MethodBehavior {
        Object apply(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, MethodBehavior behavior) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                (instance, method, args) -> {
                    String name = method.getName();
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (name) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(instance);
                            case "equals" -> instance == args[0];
                            default -> null;
                        };
                    }
                    return behavior.apply(name, args == null ? new Object[0] : args);
                }
        );
    }

    private static Object unsupported(String method) {
        throw new UnsupportedOperationException("Unsupported test repository method: " + method);
    }
}
