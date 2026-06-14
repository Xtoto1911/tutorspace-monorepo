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
import com.tutor.crmservice.shared.ResourceNotFoundException;
import com.tutor.crmservice.student.StudentCard;
import com.tutor.crmservice.student.StudentCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PageViewService {

    private static final Locale RU = Locale.forLanguageTag("ru-RU");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", RU);
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd", RU);
    private static final DateTimeFormatter HISTORY_DAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM", RU);
    private static final DateTimeFormatter HISTORY_MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", RU);

    private static String applicationIdOf(Lesson lesson) {
        return lesson.getApplicationId() == null ? null : lesson.getApplicationId().toString();
    }

    private final CrmApplicationRepository crmApplicationRepository;
    private final StudentCardRepository studentCardRepository;
    private final LessonRepository lessonRepository;
    private final LessonRescheduleRequestRepository lessonRescheduleRequestRepository;
    private final MessageThreadRepository messageThreadRepository;
    private final MessageRepository messageRepository;
    private final AvailabilitySlotService availabilitySlotService;
    private final Clock clock;

    @Autowired
    public PageViewService(
            CrmApplicationRepository crmApplicationRepository,
            StudentCardRepository studentCardRepository,
            LessonRepository lessonRepository,
            LessonRescheduleRequestRepository lessonRescheduleRequestRepository,
            MessageThreadRepository messageThreadRepository,
            MessageRepository messageRepository,
            AvailabilitySlotService availabilitySlotService
    ) {
        this(
                crmApplicationRepository,
                studentCardRepository,
                lessonRepository,
                lessonRescheduleRequestRepository,
                messageThreadRepository,
                messageRepository,
                availabilitySlotService,
                Clock.systemDefaultZone()
        );
    }

    PageViewService(
            CrmApplicationRepository crmApplicationRepository,
            StudentCardRepository studentCardRepository,
            LessonRepository lessonRepository,
            LessonRescheduleRequestRepository lessonRescheduleRequestRepository,
            MessageThreadRepository messageThreadRepository,
            MessageRepository messageRepository,
            AvailabilitySlotService availabilitySlotService,
            Clock clock
    ) {
        this.crmApplicationRepository = crmApplicationRepository;
        this.studentCardRepository = studentCardRepository;
        this.lessonRepository = lessonRepository;
        this.lessonRescheduleRequestRepository = lessonRescheduleRequestRepository;
        this.messageThreadRepository = messageThreadRepository;
        this.messageRepository = messageRepository;
        this.availabilitySlotService = availabilitySlotService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ClientOverviewPageDto getClientOverviewPage(UUID clientUserId) {
        List<CrmApplication> applications = crmApplicationRepository.findByClientUserIdOrderByCreatedAtDesc(clientUserId);
        Lesson nextLesson = lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientUserId).stream()
                .filter(this::isActionableUpcomingOrInProgressLesson)
                .findFirst()
                .orElse(null);
        MessageThread recentThread = messageThreadRepository.findByClientUserIdOrderByUpdatedAtDesc(clientUserId).stream()
                .findFirst()
                .orElse(null);

        return new ClientOverviewPageDto(
                (int) applications.stream().filter(this::isActiveClientApplication).count(),
                applications.stream()
                        .limit(2)
                        .map(application -> new ClientOverviewPageDto.ApplicationDto(
                                application.getId().toString(),
                                application.getTutorName(),
                                application.getTutorSlug(),
                                application.getSubjectName(),
                                clientStatusLabel(application),
                                statusTone(application)
                        ))
                        .toList(),
                nextLesson == null ? null : new ClientOverviewPageDto.LessonDto(
                        nextLesson.getId().toString(),
                        applicationIdOf(nextLesson),
                        nextLesson.getTitle(),
                        nextLesson.getTutorName(),
                        nextLesson.getSubjectName(),
                        nextLesson.getFormat(),
                        nextLesson.getStartsAt().toString()
                ),
                recentThread == null ? null : new ClientOverviewPageDto.ConversationDto(
                        recentThread.getId().toString(),
                        recentThread.getParticipantName(),
                        recentThread.getSubject(),
                        recentThread.getPreview(),
                        recentThread.getUpdatedAt().toString()
                )
        );
    }

    @Transactional(readOnly = true)
    public TutorOverviewPageDto getTutorOverviewPage(UUID tutorUserId) {
        List<CrmApplication> applications = crmApplicationRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId);
        List<Lesson> upcomingLessons = lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorUserId).stream()
                .filter(this::isActionableUpcomingOrInProgressLesson)
                .toList();
        Lesson nextLesson = upcomingLessons.stream().findFirst().orElse(null);

        return new TutorOverviewPageDto(
                (int) applications.stream().filter(application -> application.getStatus() == ApplicationStatus.NEW).count(),
                (int) studentCardRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId).stream()
                        .filter(student -> "Активен".equalsIgnoreCase(student.getStatus()))
                        .count(),
                upcomingLessons.size(),
                nextLesson == null ? null : new TutorOverviewPageDto.LessonDto(
                        nextLesson.getId().toString(),
                        applicationIdOf(nextLesson),
                        nextLesson.getStudentName(),
                        nextLesson.getTitle(),
                        nextLesson.getSubjectName(),
                        nextLesson.getFormat(),
                        nextLesson.getStartsAt().toString()
                )
        );
    }

    @Transactional(readOnly = true)
    public ClientApplicationsPageDto getClientApplicationsPage(UUID clientUserId) {
        List<CrmApplication> applications = crmApplicationRepository.findByClientUserIdOrderByCreatedAtDesc(clientUserId);
        List<Lesson> lessons = lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientUserId);

        List<ClientApplicationsPageDto.ApplicationCardDto> cards = applications.stream()
                .map(application -> new ClientApplicationsPageDto.ApplicationCardDto(
                        application.getId().toString(),
                        initials(application.getTutorName()),
                        application.getTutorName(),
                        application.getSubjectName() + " · " + application.getGoal(),
                        List.of(formatDateTimeChip(application.getDesiredStartTime()), application.getFormat(), "60 минут"),
                        clientStatusLabel(application),
                        statusTone(application),
                        formatMoney(application.getPricePerHour()),
                        clientStatusNote(application),
                        application.getStatus() == ApplicationStatus.ACCEPTED ? "Перейти" : "Написать"
                ))
                .toList();

        Lesson nextLesson = lessons.stream()
                .filter(this::isActionableUpcomingOrInProgressLesson)
                .findFirst()
                .orElse(null);

        return new ClientApplicationsPageDto(
                List.of(
                        new ClientApplicationsPageDto.MetricDto(String.valueOf(applications.stream().filter(app -> app.getStatus() == ApplicationStatus.NEW).count()), "активные заявки", "indigo"),
                        new ClientApplicationsPageDto.MetricDto(String.valueOf(applications.stream().filter(app -> app.getStatus() == ApplicationStatus.ACCEPTED).count()), "подтверждено", "emerald"),
                        new ClientApplicationsPageDto.MetricDto(String.valueOf(lessons.size()), "занятие запланировано", "violet"),
                        new ClientApplicationsPageDto.MetricDto(String.valueOf(applications.size()), "всего заявок", "orange")
                ),
                List.of("Все", "Ожидают ответа", "Запланированы", "Завершены"),
                cards,
                new ClientApplicationsPageDto.SideLessonDto(
                        "Ближайшее занятие",
                        nextLesson == null ? "Пока нет урока" : formatDateTimeChip(nextLesson.getStartsAt()),
                        nextLesson == null ? "Сначала дождитесь подтверждения репетитора" : nextLesson.getTitle(),
                        nextLesson == null ? "После принятия заявки здесь появится ближайшее занятие." : "Ссылка и детали урока будут доступны в расписании."
                ),
                List.of(
                        new ClientApplicationsPageDto.ProgressDto("Математика", 100),
                        new ClientApplicationsPageDto.ProgressDto("Английский", 60),
                        new ClientApplicationsPageDto.ProgressDto("Физика", 80)
                ),
                List.of(
                        "Добавьте цель подготовки — репетитор быстрее оценит запрос.",
                        "Проверьте часовой пояс перед онлайн-занятием.",
                        "После урока оставьте отзыв — это помогает модерации."
                )
        );
    }

    @Transactional(readOnly = true)
    public ClientApplicationDetailsDto getClientApplication(UUID clientUserId, UUID applicationId) {
        CrmApplication application = crmApplicationRepository.findByIdAndClientUserId(applicationId, clientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена."));

        return new ClientApplicationDetailsDto(
                application.getId().toString(),
                application.getTutorName(),
                application.getTutorSlug(),
                clientStatusLabel(application),
                statusTone(application),
                application.getSubjectName(),
                List.of(formatDateTimeChip(application.getDesiredStartTime()), application.getFormat(), "60 минут"),
                clientStatusNote(application),
                formatMoney(application.getPricePerHour()),
                List.of(
                        new ClientApplicationDetailsDto.InfoRowDto("Предмет", application.getSubjectName()),
                        new ClientApplicationDetailsDto.InfoRowDto("Цель", application.getGoal()),
                        new ClientApplicationDetailsDto.InfoRowDto("Ученик", application.getStudentName() + ", " + application.getStudentGrade()),
                        new ClientApplicationDetailsDto.InfoRowDto("Формат", application.getFormat())
                )
        );
    }

    @Transactional(readOnly = true)
    public ClientSchedulePageDto getClientSchedulePage(UUID clientUserId) {
        return getClientSchedulePage(clientUserId, null, null);
    }

    @Transactional(readOnly = true)
    public ClientSchedulePageDto getClientSchedulePage(UUID clientUserId, LocalDate requestedFrom, LocalDate requestedTo) {
        List<Lesson> lessons = lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientUserId);
        List<LessonRescheduleRequest> requests = lessonRescheduleRequestRepository.findByClientUserIdOrderByCreatedAtDesc(clientUserId);
        LocalDate weekStart = requestedFrom == null ? resolveClientWeekStart(lessons) : requestedFrom;
        LocalDate weekEnd = requestedTo == null ? weekStart.plusDays(6) : requestedTo;
        LocalDate today = currentDate();

        List<Lesson> weekLessons = lessons.stream()
                .filter(lesson -> isWithinDisplayedWeek(lesson.getStartsAt().toLocalDate(), weekStart, weekEnd))
                .toList();
        List<LessonRescheduleRequest> weekRequests = requests.stream()
                .filter(request -> isWithinDisplayedWeek(request.getLessonStartsAt().toLocalDate(), weekStart, weekEnd))
                .toList();
        List<String> timeSlots = buildClientTimeSlots(weekLessons, weekRequests);
        Map<String, Integer> timeSlotRows = java.util.stream.IntStream.range(0, timeSlots.size())
                .boxed()
                .collect(Collectors.toMap(timeSlots::get, Function.identity()));

        List<ClientSchedulePageDto.EventDto> events = weekLessons.stream()
                .map(lesson -> {
                    MessageThread thread = lesson.getApplicationId() == null
                            ? null
                            : messageThreadRepository.findByApplicationId(lesson.getApplicationId()).orElse(null);
                    LocalDateTime endsAt = lessonEndsAt(lesson);
                    int dayIndex = Math.max(0, Math.min(6, lesson.getStartsAt().getDayOfWeek().getValue() - 1));
                    int row = timeSlotRows.getOrDefault(TIME_FORMAT.format(lesson.getStartsAt()), 0);
                    boolean canJoin = canJoinLesson(lesson);
                    boolean canReschedule = canRescheduleLesson(lesson);
                    return new ClientSchedulePageDto.EventDto(
                            lesson.getId().toString(),
                            lesson.getId().toString(),
                            applicationIdOf(lesson),
                            thread == null ? null : thread.getId().toString(),
                            lesson.getSubjectName(),
                            lesson.getTutorName(),
                            TIME_FORMAT.format(lesson.getStartsAt()),
                            lesson.getStartsAt().toString(),
                            endsAt.toString(),
                            "indigo",
                            dayIndex + 1,
                            row,
                            canJoin,
                            canReschedule,
                            thread != null
                    );
                })
                .toList();

        List<ClientSchedulePageDto.RescheduleDto> rescheduleRequests = requests.stream()
                .map(request -> new ClientSchedulePageDto.RescheduleDto(
                        request.getId().toString(),
                        request.getLessonId().toString(),
                        TIME_FORMAT.format(request.getLessonStartsAt()),
                        request.getLessonStartsAt().toString(),
                        request.getLessonTitle(),
                        request.getStatus() == LessonRescheduleStatus.PENDING ? "ожидает ответа" : "обновлено",
                        request.getStatus() == LessonRescheduleStatus.PENDING ? "orange" : "blue"
                ))
                .toList();

        List<ClientSchedulePageDto.UpcomingLessonDto> upcomingLessons = lessons.stream()
                .filter(this::isActionableUpcomingOrInProgressLesson)
                .limit(2)
                .map(lesson -> {
                    MessageThread thread = lesson.getApplicationId() == null
                            ? null
                            : messageThreadRepository.findByApplicationId(lesson.getApplicationId()).orElse(null);
                    return new ClientSchedulePageDto.UpcomingLessonDto(
                            lesson.getId().toString(),
                            lesson.getId().toString(),
                            applicationIdOf(lesson),
                            thread == null ? null : thread.getId().toString(),
                            TIME_FORMAT.format(lesson.getStartsAt()),
                            lesson.getSubjectName(),
                            lesson.getTutorName() + " · " + lesson.getFormat().toLowerCase(RU),
                            canJoinLesson(lesson),
                            canRescheduleLesson(lesson),
                            thread != null
                    );
                })
                .toList();

        long todayLessons = lessons.stream()
                .filter(lesson -> lesson.getStartsAt().toLocalDate().equals(today))
                .count();
        long upcomingWeekLessons = weekLessons.stream()
                .filter(this::isActionableUpcomingOrInProgressLesson)
                .count();
        long pendingReschedules = requests.stream()
                .filter(request -> request.getStatus() == LessonRescheduleStatus.PENDING)
                .count();

        return new ClientSchedulePageDto(
                List.of(
                        new ClientSchedulePageDto.MetricDto("С", String.valueOf(todayLessons), "Сегодня", "урока", "indigo"),
                        new ClientSchedulePageDto.MetricDto("Н", String.valueOf(weekLessons.size()), "Неделя", "занятий", "violet"),
                        new ClientSchedulePageDto.MetricDto("↗", String.valueOf(upcomingWeekLessons), "Впереди", "в расписании", "amber"),
                        new ClientSchedulePageDto.MetricDto("↺", String.valueOf(pendingReschedules), "Переносы", pendingReschedules == 0 ? "запросов" : "ожидают", pendingReschedules == 0 ? "emerald" : "orange")
                ),
                new ClientSchedulePageDto.PeriodDto(weekStart.toString(), weekEnd.toString(), weekStart.format(DAY_FORMAT) + "-" + weekEnd.format(DAY_FORMAT)),
                buildClientWeekDays(weekStart),
                timeSlots,
                events,
                rescheduleRequests,
                buildMiniMonthDays(weekStart),
                upcomingLessons,
                new ClientSchedulePageDto.EmptyStateDto(
                        lessons.isEmpty(),
                        "Уроков пока нет",
                        "Когда репетитор создаст урок, он появится в расписании."
                )
        );
    }

    @Transactional(readOnly = true)
    public ClientHistoryPageDto getClientHistoryPage(UUID clientUserId) {
        List<Lesson> lessons = lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientUserId);
        List<CrmApplication> applications = crmApplicationRepository.findByClientUserIdOrderByCreatedAtDesc(clientUserId);
        YearMonth activeMonth = YearMonth.from(currentDate());

        List<Lesson> completedLessons = lessons.stream()
                .filter(this::isCompletedHistoryLesson)
                .sorted(Comparator.comparing(Lesson::getStartsAt).reversed())
                .toList();

        List<HistoryEventDraft> lessonEvents = completedLessons.stream()
                .map(lesson -> new HistoryEventDraft(
                        lesson.getStartsAt(),
                        new ClientHistoryPageDto.EventDto(
                                lesson.getId().toString(),
                                "lesson",
                                formatHistoryDate(lesson.getStartsAt()),
                                "Занятие завершено",
                                lesson.getSubjectName() + " с " + lesson.getTutorName() + " · " + lesson.getFormat().toLowerCase(RU),
                                "завершено",
                                "emerald"
                        )
                ))
                .toList();

        List<HistoryEventDraft> applicationEvents = applications.stream()
                .map(application -> new HistoryEventDraft(
                        applicationHistoryTime(application),
                        new ClientHistoryPageDto.EventDto(
                                application.getId().toString(),
                                "application",
                                formatHistoryDate(applicationHistoryTime(application)),
                                applicationHistoryTitle(application),
                                application.getSubjectName() + " · " + application.getTutorName(),
                                applicationHistoryStatus(application),
                                applicationHistoryTone(application)
                        )
                ))
                .toList();

        List<ClientHistoryPageDto.EventDto> events = java.util.stream.Stream.concat(lessonEvents.stream(), applicationEvents.stream())
                .sorted(Comparator.comparing(HistoryEventDraft::occurredAt).reversed())
                .map(HistoryEventDraft::event)
                .toList();

        long monthLessons = completedLessons.stream()
                .filter(lesson -> YearMonth.from(lesson.getStartsAt()).equals(activeMonth))
                .count();
        long monthApplications = applications.stream()
                .filter(application -> YearMonth.from(applicationHistoryTime(application)).equals(activeMonth))
                .count();
        long closedApplications = applications.stream()
                .filter(application -> YearMonth.from(applicationHistoryTime(application)).equals(activeMonth))
                .filter(application -> application.getStatus() != ApplicationStatus.NEW)
                .count();

        return new ClientHistoryPageDto(
                List.of(
                        new ClientHistoryPageDto.StatDto("И", String.valueOf(events.size()), "записей в истории", "занятия и заявки из CRM", "indigo"),
                        new ClientHistoryPageDto.StatDto("У", String.valueOf(completedLessons.size()), "прошедших занятий", lastLessonCaption(completedLessons), "emerald"),
                        new ClientHistoryPageDto.StatDto("З", String.valueOf(applications.size()), "заявок всего", "без демо-архива", "amber")
                ),
                List.of("Все записи", "Занятия", "Заявки"),
                capitalize(activeMonth.atDay(1).format(HISTORY_MONTH_FORMAT)),
                events,
                List.of(
                        new ClientHistoryPageDto.SummaryRowDto("Прошедшие занятия", "завершены в этом месяце", String.valueOf(monthLessons), "emerald"),
                        new ClientHistoryPageDto.SummaryRowDto("Заявки за месяц", "созданы или обновлены", String.valueOf(monthApplications), "indigo"),
                        new ClientHistoryPageDto.SummaryRowDto("Закрытые заявки", "приняты, отменены или отклонены", String.valueOf(closedApplications), "violet")
                ),
                completedLessons.stream()
                        .map(lesson -> new ClientHistoryPageDto.CompletedLessonDto(
                                lesson.getId().toString(),
                                formatHistoryDate(lesson.getStartsAt()),
                                lesson.getTutorName(),
                                lesson.getSubjectName(),
                                lesson.getStatus(),
                                lesson.getApplicationId() == null ? "/app/client/schedule" : "/app/client/applications/" + lesson.getApplicationId()
                        ))
                        .toList(),
                new ClientHistoryPageDto.EmptyStateDto(
                        events.isEmpty(),
                        "История пока пустая",
                        "После заявок и проведенных уроков здесь появятся записи из CRM."
                )
        );
    }

    @Transactional(readOnly = true)
    public ClientMessagesPageDto getClientMessagesPage(UUID clientUserId) {
        return getClientMessagesPage(clientUserId, null);
    }

    @Transactional(readOnly = true)
    public ClientMessagesPageDto getClientMessagesPage(UUID clientUserId, UUID requestedThreadId) {
        List<MessageThread> threads = messageThreadRepository.findByClientUserIdOrderByUpdatedAtDesc(clientUserId);
        if (threads.isEmpty()) {
            return emptyClientMessagesPage();
        }

        MessageThread activeThread = resolveActiveThread(threads, requestedThreadId, "Диалог не найден.");
        List<Message> messages = messageRepository.findByThreadIdOrderByCreatedAtAsc(activeThread.getId());
        CrmApplication application = crmApplicationRepository.findById(activeThread.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Заявка диалога не найдена."));

        return new ClientMessagesPageDto(
                List.of(
                        new ClientMessagesPageDto.MetricDto(String.valueOf(threads.size()), "активных диалога", "indigo"),
                        new ClientMessagesPageDto.MetricDto("1", "непрочитанных", "amber"),
                        new ClientMessagesPageDto.MetricDto("12 мин", "средний ответ", "emerald")
                ),
                List.of("Все", "Репетиторы", "Заявки", "Архив"),
                threads.stream().map(thread -> new ClientMessagesPageDto.ConversationDto(
                        thread.getId().toString(),
                        thread.getParticipantName(),
                        thread.getParticipantInitials(),
                        thread.getSubject(),
                        thread.getPreview(),
                        formatConversationTime(thread.getUpdatedAt()),
                        0,
                        thread.getId().equals(activeThread.getId())
                )).toList(),
                new ClientMessagesPageDto.DialogHeaderDto(
                        activeThread.getId().toString(),
                        activeThread.getParticipantName(),
                        activeThread.getParticipantInitials(),
                        application.getFormat().toLowerCase(RU) + " · " + application.getSubjectName() + " · " + formatDateTimeChip(application.getDesiredStartTime()),
                        application.getTutorSlug(),
                        application.getId().toString()
                ),
                messages.stream().map(message -> new ClientMessagesPageDto.MessageDto(
                        message.getId().toString(),
                        clientMessageAuthor(message, activeThread),
                        TIME_FORMAT.format(message.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()),
                        message.getText(),
                        "client".equals(message.getAuthorRole())
                )).toList(),
                List.of(
                        new ClientMessagesPageDto.DetailDto("Предмет", application.getSubjectName()),
                        new ClientMessagesPageDto.DetailDto("Время", formatDateTimeChip(application.getDesiredStartTime())),
                        new ClientMessagesPageDto.DetailDto("Формат", application.getFormat()),
                        new ClientMessagesPageDto.DetailDto("Заявка", application.getId().toString())
                ),
                List.of("Запросить перенос", "Открыть заявку", "Профиль репетитора"),
                List.of(
                        new ClientMessagesPageDto.AttachmentDto("План занятия.pdf", "подготовит репетитор"),
                        new ClientMessagesPageDto.AttachmentDto("Диагностика.docx", "будет после урока")
                )
        );
    }

    @Transactional(readOnly = true)
    public TutorRequestsPageDto getTutorRequestsPage(UUID tutorUserId) {
        List<CrmApplication> applications = crmApplicationRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId);
        Map<UUID, Lesson> lessonsByApplicationId = lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorUserId).stream()
                .filter(lesson -> lesson.getApplicationId() != null)
                .collect(Collectors.toMap(
                        Lesson::getApplicationId,
                        Function.identity(),
                        (first, second) -> first.getStartsAt().isAfter(second.getStartsAt()) ? second : first
                ));

        return new TutorRequestsPageDto(
                List.of(
                        new TutorRequestsPageDto.StatDto("✦", String.valueOf(applications.stream().filter(app -> app.getStatus() == ApplicationStatus.NEW).count()), "Новые", "нужно ответить", "indigo"),
                        new TutorRequestsPageDto.StatDto("✓", String.valueOf(applications.stream().filter(this::isActiveFunnelStatus).count()), "Воронка", "активные заявки", "emerald"),
                        new TutorRequestsPageDto.StatDto("⏱", averageResponseTime(applications), "Скорость", "средний ответ", "amber"),
                        new TutorRequestsPageDto.StatDto("◎", String.valueOf(applications.size()), "Всего", "обращений в CRM", "cyan")
                ),
                applications.stream().map(application -> {
                    MessageThread thread = messageThreadRepository.findByApplicationId(application.getId()).orElse(null);
                    Lesson scheduledLesson = lessonsByApplicationId.get(application.getId());
                    return new TutorRequestsPageDto.RequestDto(
                            application.getId().toString(),
                            initials(application.getStudentName()),
                            application.getStudentName(),
                            application.getStudentGrade() + " · " + application.getGoal(),
                            application.getSubjectName(),
                            tutorStatusLabel(application),
                            application.getComment(),
                            formatMoney(application.getPricePerHour()),
                            application.getFormat(),
                            formatDateTimeChip(application.getDesiredStartTime()),
                            application.getStatus() == ApplicationStatus.NEW ? "indigo" : application.getStatus() == ApplicationStatus.ACCEPTED ? "emerald" : "amber",
                            application.getStatus() == ApplicationStatus.NEW,
                            application.getStatus().name(),
                            application.getSource() == null ? "MARKETPLACE" : application.getSource(),
                            formatDeadline(application.getResponseDeadline()),
                            isOverdue(application),
                            application.getLeadScore() == null ? 0 : application.getLeadScore(),
                            nextRequestAction(application, scheduledLesson),
                            application.getRejectionReason(),
                            thread == null ? null : thread.getId().toString(),
                            scheduledLesson == null ? null : scheduledLesson.getId().toString(),
                            scheduledLesson == null ? null : scheduledLesson.getStartsAt().toString(),
                            requestCompletenessLabel(application)
                    );
                }).toList(),
                new TutorRequestsPageDto.TodayDto(
                        "Сегодня в фокусе",
                        "Новые обращения и ближайшие слоты для пробных уроков.",
                        applications.stream().limit(3).map(application -> new TutorRequestsPageDto.TodayEventDto(
                                TIME_FORMAT.format(application.getDesiredStartTime()),
                                application.getSubjectName() + " · " + application.getStudentName(),
                                application.getStatus() == ApplicationStatus.NEW ? "indigo" : "emerald"
                        )).toList()
                ),
                new TutorRequestsPageDto.QualityDto(
                        "Качество ответа",
                        String.valueOf(responseQualityPercent(applications)) + "%",
                        "Ответ в течение 24 часов повышает шанс перевести заявку в пробный урок.",
                        responseQualityPercent(applications)
                ),
                buildFunnel(applications)
        );
    }

    @Transactional(readOnly = true)
    public List<TutorLessonCandidateDto> getTutorLessonCandidates(UUID tutorUserId) {
        List<CrmApplication> applications = crmApplicationRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId);
        List<StudentCard> studentCards = studentCardRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId);
        List<Lesson> lessons = lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorUserId);

        Map<UUID, StudentCard> studentCardsByApplicationId = studentCards.stream()
                .collect(Collectors.toMap(StudentCard::getApplicationId, Function.identity(), (first, second) -> first));
        Set<UUID> lessonApplicationIds = lessons.stream()
                .map(Lesson::getApplicationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return applications.stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .filter(application -> !lessonApplicationIds.contains(application.getId()))
                .map(application -> {
                    StudentCard studentCard = studentCardsByApplicationId.get(application.getId());
                    if (studentCard == null) {
                        return (TutorLessonCandidateDto) null;
                    }
                    return new TutorLessonCandidateDto(
                            application.getId().toString(),
                            studentCard.getId().toString(),
                            application.getStudentName(),
                            application.getSubjectName(),
                            application.getFormat(),
                            application.getDesiredStartTime().toString(),
                            application.getStudentGrade() + " · " + application.getGoal()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public TutorStudentsPageDto getTutorStudentsPage(UUID tutorUserId) {
        List<StudentCard> students = studentCardRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId);
        StudentCard selected = students.stream().findFirst()
                .orElseGet(() -> {
                    StudentCard placeholder = new StudentCard();
                    placeholder.setName("Пока нет учеников");
                    placeholder.setDetails("Примите первую заявку");
                    placeholder.setSubject("CRM");
                    placeholder.setStatus("Ожидание");
                    placeholder.setNote("После принятия заявки карточка ученика появится здесь.");
                    placeholder.setProgress(0);
                    return placeholder;
                });

        return new TutorStudentsPageDto(
                List.of(
                        new TutorStudentsPageDto.StatDto("👥", String.valueOf(students.size()), "Активные", "ученики в CRM", "indigo"),
                        new TutorStudentsPageDto.StatDto("⏳", String.valueOf(students.stream().filter(student -> student.getProgress() < 30).count()), "Новые", "требуют плана", "emerald"),
                        new TutorStudentsPageDto.StatDto("📝", "3", "Отчёты", "на неделе", "amber"),
                        new TutorStudentsPageDto.StatDto("↗", students.isEmpty() ? "0%" : "78%", "Прогресс", "средний по группе", "cyan")
                ),
                students.stream().map(student -> new TutorStudentsPageDto.StudentDto(
                        student.getId().toString(),
                        initials(student.getName()),
                        student.getName(),
                        student.getDetails(),
                        student.getSubject(),
                        student.getStatus(),
                        student.getNote(),
                        student.getProgress(),
                        student.getProgress() >= 75 ? "indigo" : student.getProgress() >= 50 ? "emerald" : "amber",
                        student.getId().equals(selected.getId())
                )).toList(),
                new TutorStudentsPageDto.SelectedStudentDto(
                        initials(selected.getName()),
                        selected.getName(),
                        selected.getDetails(),
                        List.of(selected.getSubject(), selected.getStatus()),
                        selected.getProgress(),
                        List.of(
                                new TutorStudentsPageDto.TaskDto("Подготовить план", "на ближайшие 4 недели", "indigo"),
                                new TutorStudentsPageDto.TaskDto("Назначить урок", "первое занятие в календаре", "emerald"),
                                new TutorStudentsPageDto.TaskDto("Собрать вопросы", "что проверить на старте", "amber")
                        )
                ),
                new TutorStudentsPageDto.CardsDto(
                        new TutorStudentsPageDto.HomeworkCardDto("Домашние задания", "Все задачи и обратная связь привязаны к карточке ученика."),
                        new TutorStudentsPageDto.PaymentsCardDto("Статус", "подписка CRM", "Платформа не ведет оплату уроков, только CRM-подписку."),
                        new TutorStudentsPageDto.ReportCardDto("Отчёты", "Короткие отчёты по прогрессу можно отправлять родителю после урока.")
                )
        );
    }

    @Transactional(readOnly = true)
    public TutorCalendarPageDto getTutorCalendarPage(UUID tutorUserId, LocalDate from, LocalDate to) {
        List<Lesson> lessons = lessonRepository.findByTutorUserIdOrderByStartsAtAsc(tutorUserId);
        List<TutorCalendarPageDto.PendingRequestDto> pendingRequests = buildPendingLessonRequests(tutorUserId, lessons);
        LocalDate fromDate = from != null ? from : currentDate();
        LocalDate toDate = to != null ? to : fromDate.plusDays(6);
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }
        LocalDate weekStart = fromDate.with(DayOfWeek.MONDAY);
        List<TutorCalendarPageDto.EventDto> events = new ArrayList<>();

        for (Lesson lesson : lessons) {
            int column = Math.max(1, Math.min(7, lesson.getStartsAt().getDayOfWeek().getValue()));
            int row = Math.max(0, Math.min(4, (lesson.getStartsAt().getHour() - 12) / 2));
            events.add(new TutorCalendarPageDto.EventDto(
                    lesson.getId().toString(),
                    lesson.getSubjectName(),
                    lesson.getStudentName() + " · " + TIME_FORMAT.format(lesson.getStartsAt()),
                    "indigo",
                    column,
                    row,
                    1,
                    lesson.getStartsAt().toString(),
                    lesson.getSeriesId() == null ? null : lesson.getSeriesId().toString(),
                    lesson.getSeriesId() == null ? "одноразовый урок" : "еженедельная серия",
                    applicationIdOf(lesson),
                    lesson.getApplicationId() == null
                            ? null
                            : messageThreadRepository.findByApplicationId(lesson.getApplicationId())
                                    .map(thread -> thread.getId().toString())
                                    .orElse(null),
                    lesson.getStudentId() == null ? null : lesson.getStudentId().toString()
            ));
        }

        Lesson nextLesson = lessons.stream()
                .filter(lesson -> lesson.getStartsAt().isAfter(currentDateTime().minusHours(1)))
                .findFirst()
                .orElse(null);

        List<SlotDto> generatedSlots = availabilitySlotService.generateSlots(tutorUserId, fromDate, toDate, null);

        return new TutorCalendarPageDto(
                List.of(
                        new TutorCalendarPageDto.StatDto("📅", String.valueOf(lessons.size()), "Занятия", "в календаре", "indigo"),
                        new TutorCalendarPageDto.StatDto("🪟", String.valueOf(Math.max(0, 6 - lessons.size())), "Окна", "для новых заявок", "slate"),
                        new TutorCalendarPageDto.StatDto("⏱", nextLesson == null ? "—" : TIME_FORMAT.format(nextLesson.getStartsAt()), "Следующее", "ближайший урок", "emerald"),
                        new TutorCalendarPageDto.StatDto("CRM", "sync", "Связано", "заявки, ученики, сообщения", "amber")
                ),
                buildTutorWeekDays(weekStart),
                List.of("12:00", "14:00", "16:00", "18:00", "20:00"),
                events,
                nextLesson == null
                        ? new TutorCalendarPageDto.NextLessonDto("Пока пусто", "Назначьте первый урок", "После принятия заявки создайте занятие.")
                        : new TutorCalendarPageDto.NextLessonDto(TIME_FORMAT.format(nextLesson.getStartsAt()), nextLesson.getTitle(), nextLesson.getStudentName() + " · " + nextLesson.getFormat()),
                lessons.stream().limit(3).map(lesson -> new TutorCalendarPageDto.UpcomingEventDto(
                        TIME_FORMAT.format(lesson.getStartsAt()),
                        lesson.getSubjectName() + " · " + lesson.getStudentName(),
                        "indigo"
                )).toList(),
                buildMonthDays(weekStart),
                generatedSlots,
                pendingRequests
        );
    }

    @Transactional(readOnly = true)
    public TutorMessagesPageDto getTutorMessagesPage(UUID tutorUserId) {
        return getTutorMessagesPage(tutorUserId, null);
    }

    @Transactional(readOnly = true)
    public TutorMessagesPageDto getTutorMessagesPage(UUID tutorUserId, UUID requestedThreadId) {
        List<MessageThread> threads = messageThreadRepository.findByTutorUserIdOrderByUpdatedAtDesc(tutorUserId);
        if (threads.isEmpty()) {
            return emptyTutorMessagesPage();
        }

        MessageThread activeThread = resolveActiveThread(threads, requestedThreadId, "Диалог не найден.");
        List<Message> messages = messageRepository.findByThreadIdOrderByCreatedAtAsc(activeThread.getId());
        CrmApplication application = crmApplicationRepository.findById(activeThread.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Заявка диалога не найдена."));
        StudentCard studentCard = studentCardRepository.findByApplicationId(application.getId()).orElse(null);

        return new TutorMessagesPageDto(
                List.of(
                        new TutorMessagesPageDto.MetricDto(String.valueOf(threads.size()), "активных диалогов", "indigo"),
                        new TutorMessagesPageDto.MetricDto(String.valueOf(crmApplicationRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId).stream().filter(app -> app.getStatus() == ApplicationStatus.NEW).count()), "новых заявок", "amber"),
                        new TutorMessagesPageDto.MetricDto("1", "готово к уроку", "emerald")
                ),
                threads.stream().map(thread -> {
                    CrmApplication threadApplication = crmApplicationRepository.findById(thread.getApplicationId()).orElse(null);
                    String participantName = tutorConversationName(threadApplication);
                    return new TutorMessagesPageDto.ConversationDto(
                            thread.getId().toString(),
                            participantName,
                            initials(participantName),
                            thread.getSubject(),
                            thread.getPreview(),
                            formatConversationTime(thread.getUpdatedAt()),
                            0,
                            thread.getId().equals(activeThread.getId())
                    );
                }).toList(),
                new TutorMessagesPageDto.ActiveDialogDto(
                        activeThread.getId().toString(),
                        initials(tutorConversationName(application)),
                        tutorConversationName(application),
                        application.getSubjectName() + " · " + application.getFormat().toLowerCase(RU),
                        application.getId().toString(),
                        studentCard == null ? "" : studentCard.getId().toString()
                ),
                messages.stream().map(message -> new TutorMessagesPageDto.MessageDto(
                        message.getId().toString(),
                        tutorMessageAuthor(message, application),
                        TIME_FORMAT.format(message.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()),
                        message.getText(),
                        "tutor".equals(message.getAuthorRole())
                )).toList(),
                List.of(
                        new TutorMessagesPageDto.DetailDto("Предмет", application.getSubjectName()),
                        new TutorMessagesPageDto.DetailDto("Время", formatDateTimeChip(application.getDesiredStartTime())),
                        new TutorMessagesPageDto.DetailDto("Формат", application.getFormat()),
                        new TutorMessagesPageDto.DetailDto("Заявка", application.getId().toString())
                ),
                List.of("Принять заявку", "Назначить урок", "Открыть карточку ученика"),
                List.of(
                        new TutorMessagesPageDto.MaterialDto("План пробного урока", "черновик CRM"),
                        new TutorMessagesPageDto.MaterialDto("Чек-лист диагностики", "внутренний шаблон")
                )
        );
    }

    private String clientMessageAuthor(Message message, MessageThread activeThread) {
        if ("client".equals(message.getAuthorRole())) {
            return "Вы";
        }

        if ("tutor".equals(message.getAuthorRole())) {
            return activeThread.getParticipantName();
        }

        return message.getAuthorName();
    }

    private String tutorMessageAuthor(Message message, CrmApplication application) {
        if ("tutor".equals(message.getAuthorRole())) {
            return "Вы";
        }

        if ("client".equals(message.getAuthorRole())) {
            return tutorConversationName(application);
        }

        return message.getAuthorName();
    }

    private String tutorConversationName(CrmApplication application) {
        if (application == null || application.getStudentName() == null || application.getStudentName().isBlank()) {
            return "Клиент TutorSpace";
        }
        return application.getStudentName();
    }

    private MessageThread resolveActiveThread(List<MessageThread> threads, UUID requestedThreadId, String notFoundMessage) {
        if (requestedThreadId == null) {
            return threads.get(0);
        }

        return threads.stream()
                .filter(thread -> thread.getId().equals(requestedThreadId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    private ClientMessagesPageDto emptyClientMessagesPage() {
        return new ClientMessagesPageDto(
                List.of(
                        new ClientMessagesPageDto.MetricDto("0", "активных диалога", "indigo"),
                        new ClientMessagesPageDto.MetricDto("0", "непрочитанных", "amber"),
                        new ClientMessagesPageDto.MetricDto("—", "средний ответ", "emerald")
                ),
                List.of("Все", "Репетиторы", "Заявки", "Архив"),
                List.of(),
                new ClientMessagesPageDto.DialogHeaderDto("", "Диалог не выбран", "", "Создайте заявку или дождитесь ответа репетитора", "", ""),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private TutorMessagesPageDto emptyTutorMessagesPage() {
        return new TutorMessagesPageDto(
                List.of(
                        new TutorMessagesPageDto.MetricDto("0", "активных диалогов", "indigo"),
                        new TutorMessagesPageDto.MetricDto("0", "новых заявок", "amber"),
                        new TutorMessagesPageDto.MetricDto("—", "готово к уроку", "emerald")
                ),
                List.of(),
                new TutorMessagesPageDto.ActiveDialogDto("", "", "Диалог не выбран", "Примите заявку, чтобы начать переписку", "", ""),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private List<ClientSchedulePageDto.WeekDayDto> buildClientWeekDays(LocalDate weekStart) {
        LocalDate today = currentDate();
        return List.of(
                new ClientSchedulePageDto.WeekDayDto("Пн", DAY_FORMAT.format(weekStart), weekStart.equals(today)),
                new ClientSchedulePageDto.WeekDayDto("Вт", DAY_FORMAT.format(weekStart.plusDays(1)), weekStart.plusDays(1).equals(today)),
                new ClientSchedulePageDto.WeekDayDto("Ср", DAY_FORMAT.format(weekStart.plusDays(2)), weekStart.plusDays(2).equals(today)),
                new ClientSchedulePageDto.WeekDayDto("Чт", DAY_FORMAT.format(weekStart.plusDays(3)), weekStart.plusDays(3).equals(today)),
                new ClientSchedulePageDto.WeekDayDto("Пт", DAY_FORMAT.format(weekStart.plusDays(4)), weekStart.plusDays(4).equals(today)),
                new ClientSchedulePageDto.WeekDayDto("Сб", DAY_FORMAT.format(weekStart.plusDays(5)), weekStart.plusDays(5).equals(today)),
                new ClientSchedulePageDto.WeekDayDto("Вс", DAY_FORMAT.format(weekStart.plusDays(6)), weekStart.plusDays(6).equals(today))
        );
    }

    private List<ClientSchedulePageDto.MiniMonthDayDto> buildMiniMonthDays(LocalDate weekStart) {
        LocalDate today = currentDate();
        return List.of(
                new ClientSchedulePageDto.MiniMonthDayDto(DAY_FORMAT.format(weekStart), weekStart.equals(today)),
                new ClientSchedulePageDto.MiniMonthDayDto(DAY_FORMAT.format(weekStart.plusDays(1)), weekStart.plusDays(1).equals(today)),
                new ClientSchedulePageDto.MiniMonthDayDto(DAY_FORMAT.format(weekStart.plusDays(2)), weekStart.plusDays(2).equals(today)),
                new ClientSchedulePageDto.MiniMonthDayDto(DAY_FORMAT.format(weekStart.plusDays(3)), weekStart.plusDays(3).equals(today)),
                new ClientSchedulePageDto.MiniMonthDayDto(DAY_FORMAT.format(weekStart.plusDays(4)), weekStart.plusDays(4).equals(today)),
                new ClientSchedulePageDto.MiniMonthDayDto(DAY_FORMAT.format(weekStart.plusDays(5)), weekStart.plusDays(5).equals(today)),
                new ClientSchedulePageDto.MiniMonthDayDto(DAY_FORMAT.format(weekStart.plusDays(6)), weekStart.plusDays(6).equals(today))
        );
    }

    private List<String> buildClientTimeSlots(List<Lesson> lessons, List<LessonRescheduleRequest> requests) {
        return java.util.stream.Stream.concat(
                        lessons.stream().map(Lesson::getStartsAt),
                        requests.stream().map(LessonRescheduleRequest::getLessonStartsAt)
                )
                .sorted(Comparator.naturalOrder())
                .map(TIME_FORMAT::format)
                .distinct()
                .toList();
    }

    private boolean isWithinDisplayedWeek(LocalDate date, LocalDate weekStart, LocalDate weekEnd) {
        return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
    }

    private List<TutorCalendarPageDto.WeekDayDto> buildTutorWeekDays(LocalDate weekStart) {
        return List.of(
                new TutorCalendarPageDto.WeekDayDto("Пн", DAY_FORMAT.format(weekStart), false),
                new TutorCalendarPageDto.WeekDayDto("Вт", DAY_FORMAT.format(weekStart.plusDays(1)), true),
                new TutorCalendarPageDto.WeekDayDto("Ср", DAY_FORMAT.format(weekStart.plusDays(2)), false),
                new TutorCalendarPageDto.WeekDayDto("Чт", DAY_FORMAT.format(weekStart.plusDays(3)), false),
                new TutorCalendarPageDto.WeekDayDto("Пт", DAY_FORMAT.format(weekStart.plusDays(4)), false)
        );
    }

    private List<TutorCalendarPageDto.MonthDayDto> buildMonthDays(LocalDate weekStart) {
        return List.of(
                new TutorCalendarPageDto.MonthDayDto(DAY_FORMAT.format(weekStart.minusDays(1)), true, false),
                new TutorCalendarPageDto.MonthDayDto(DAY_FORMAT.format(weekStart), false, false),
                new TutorCalendarPageDto.MonthDayDto(DAY_FORMAT.format(weekStart.plusDays(1)), false, true),
                new TutorCalendarPageDto.MonthDayDto(DAY_FORMAT.format(weekStart.plusDays(2)), false, false),
                new TutorCalendarPageDto.MonthDayDto(DAY_FORMAT.format(weekStart.plusDays(3)), false, false),
                new TutorCalendarPageDto.MonthDayDto(DAY_FORMAT.format(weekStart.plusDays(4)), false, false),
                new TutorCalendarPageDto.MonthDayDto(DAY_FORMAT.format(weekStart.plusDays(5)), false, false)
        );
    }

    private String initials(String name) {
        return java.util.Arrays.stream(name.split(" "))
                .filter(part -> !part.isBlank())
                .map(part -> String.valueOf(part.charAt(0)))
                .limit(2)
                .reduce("", String::concat)
                .toUpperCase(RU);
    }

    private TutorRequestsPageDto.FunnelDto buildFunnel(List<CrmApplication> applications) {
        int total = applications.size();
        int newCount = countStatus(applications, ApplicationStatus.NEW);
        int viewedCount = countStatus(applications, ApplicationStatus.VIEWED);
        int respondedCount = countStatus(applications, ApplicationStatus.ACCEPTED);
        int contactCount = countStatus(applications, ApplicationStatus.CONTACT_ESTABLISHED);
        int trialCount = countStatus(applications, ApplicationStatus.TRIAL_SCHEDULED);
        int studentCount = countStatus(applications, ApplicationStatus.STUDENT);
        int closedCount = (int) applications.stream().filter(this::isClosedRequestStatus).count();
        int overdueCount = (int) applications.stream().filter(this::isOverdue).count();

        return new TutorRequestsPageDto.FunnelDto(
                newCount,
                viewedCount,
                respondedCount,
                contactCount,
                trialCount,
                studentCount,
                closedCount,
                percent(trialCount + studentCount, total),
                percent(studentCount, total),
                overdueCount,
                averageResponseTime(applications)
        );
    }

    private int countStatus(List<CrmApplication> applications, ApplicationStatus status) {
        return (int) applications.stream().filter(application -> application.getStatus() == status).count();
    }

    private int percent(int value, int total) {
        return total == 0 ? 0 : Math.round((value * 100f) / total);
    }

    private boolean isActiveFunnelStatus(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW, VIEWED, ACCEPTED, CONTACT_ESTABLISHED, TRIAL_SCHEDULED -> true;
            case STUDENT, REJECTED, CANCELLED, ARCHIVED -> false;
        };
    }

    private boolean isActiveClientApplication(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW, VIEWED, ACCEPTED, CONTACT_ESTABLISHED, TRIAL_SCHEDULED, STUDENT -> true;
            case REJECTED, CANCELLED, ARCHIVED -> false;
        };
    }

    private boolean isClosedRequestStatus(CrmApplication application) {
        return switch (application.getStatus()) {
            case REJECTED, CANCELLED, ARCHIVED -> true;
            case NEW, VIEWED, ACCEPTED, CONTACT_ESTABLISHED, TRIAL_SCHEDULED, STUDENT -> false;
        };
    }

    private boolean isOverdue(CrmApplication application) {
        return application.getRespondedAt() == null
                && application.getResponseDeadline() != null
                && application.getResponseDeadline().isBefore(Instant.now(clock))
                && !isClosedRequestStatus(application)
                && application.getStatus() != ApplicationStatus.STUDENT;
    }

    private String formatDeadline(Instant deadline) {
        if (deadline == null) {
            return "24 часа";
        }
        return formatDateTimeChip(LocalDateTime.ofInstant(deadline, clock.getZone()));
    }

    private String nextRequestAction(CrmApplication application, Lesson scheduledLesson) {
        if (scheduledLesson != null) {
            return "Пробный урок назначен";
        }
        return switch (application.getStatus()) {
            case NEW -> "Ответить на заявку";
            case VIEWED -> "Откликнуться";
            case ACCEPTED, CONTACT_ESTABLISHED -> "Назначить пробный урок";
            case TRIAL_SCHEDULED -> "Отметить учеником";
            case STUDENT -> "Вести обучение";
            case REJECTED, CANCELLED, ARCHIVED -> "Закрыта";
        };
    }

    private String requestCompletenessLabel(CrmApplication application) {
        int score = application.getLeadScore() == null ? 0 : application.getLeadScore();
        boolean hasCoreDetails = application.getSubjectName() != null && !application.getSubjectName().isBlank()
                && application.getDesiredStartTime() != null
                && application.getStudentName() != null && !application.getStudentName().isBlank()
                && application.getStudentGrade() != null && !application.getStudentGrade().isBlank()
                && application.getComment() != null && !application.getComment().isBlank();
        if (score >= 80 || hasCoreDetails) {
            return "Полная заявка";
        }
        if (application.getDesiredStartTime() == null) {
            return "Нужно уточнить время";
        }
        if (application.getComment() == null || application.getComment().isBlank()) {
            return "Нет комментария";
        }
        return "Нужно уточнение";
    }

    private List<TutorCalendarPageDto.PendingRequestDto> buildPendingLessonRequests(UUID tutorUserId, List<Lesson> lessons) {
        Set<UUID> lessonApplicationIds = lessons.stream()
                .map(Lesson::getApplicationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, StudentCard> studentCardsByApplicationId = studentCardRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId).stream()
                .filter(card -> card.getApplicationId() != null)
                .collect(Collectors.toMap(StudentCard::getApplicationId, Function.identity(), (first, second) -> first));

        return crmApplicationRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .filter(application -> !lessonApplicationIds.contains(application.getId()))
                .map(application -> {
                    StudentCard studentCard = studentCardsByApplicationId.get(application.getId());
                    if (studentCard == null) {
                        return null;
                    }
                    MessageThread thread = messageThreadRepository.findByApplicationId(application.getId()).orElse(null);
                    return new TutorCalendarPageDto.PendingRequestDto(
                            application.getId().toString(),
                            studentCard.getId().toString(),
                            application.getStudentName(),
                            application.getSubjectName(),
                            application.getFormat(),
                            application.getDesiredStartTime().toString(),
                            application.getStudentGrade() + " · " + application.getGoal(),
                            thread == null ? null : thread.getId().toString()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private int responseQualityPercent(List<CrmApplication> applications) {
        long withDeadline = applications.stream()
                .filter(application -> application.getResponseDeadline() != null)
                .filter(application -> application.getRespondedAt() != null || isOverdue(application))
                .count();
        if (withDeadline == 0) {
            return 100;
        }
        long onTime = applications.stream()
                .filter(application -> application.getResponseDeadline() != null)
                .filter(application -> application.getRespondedAt() != null)
                .filter(application -> !application.getRespondedAt().isAfter(application.getResponseDeadline()))
                .count();
        return percent((int) onTime, (int) withDeadline);
    }

    private String averageResponseTime(List<CrmApplication> applications) {
        List<Duration> durations = applications.stream()
                .filter(application -> application.getCreatedAt() != null && application.getRespondedAt() != null)
                .map(application -> Duration.between(application.getCreatedAt(), application.getRespondedAt()))
                .toList();
        if (durations.isEmpty()) {
            return "—";
        }
        long averageMinutes = Math.max(1, Math.round(durations.stream()
                .mapToLong(Duration::toMinutes)
                .average()
                .orElse(0)));
        if (averageMinutes < 60) {
            return averageMinutes + " мин";
        }
        return (averageMinutes / 60) + " ч";
    }

    private String clientStatusLabel(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW, VIEWED -> "Ждет ответа";
            case ACCEPTED, CONTACT_ESTABLISHED -> "Репетитор откликнулся";
            case TRIAL_SCHEDULED -> lessonRepository.findByClientUserIdOrderByStartsAtAsc(application.getClientUserId()).stream()
                    .anyMatch(lesson -> lesson.getApplicationId().equals(application.getId())) ? "Занятие назначено" : "Заявка принята";
            case STUDENT -> "Стал учеником";
            case REJECTED -> "Отклонена";
            case CANCELLED, ARCHIVED -> "Отменена";
        };
    }

    private String tutorStatusLabel(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW -> "Новая";
            case VIEWED -> "Просмотрена";
            case ACCEPTED -> "Принята";
            case CONTACT_ESTABLISHED -> "Контакт установлен";
            case TRIAL_SCHEDULED -> "Пробный урок";
            case STUDENT -> "Стал учеником";
            case REJECTED -> "Отклонена";
            case CANCELLED -> "Отменена";
            case ARCHIVED -> "Архив";
        };
    }

    private String clientStatusNote(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW, VIEWED -> "Заявка отправлена. Обычно репетитор отвечает в течение суток.";
            case ACCEPTED, CONTACT_ESTABLISHED -> "Репетитор откликнулся. Следующим шагом можно назначить первое занятие.";
            case TRIAL_SCHEDULED -> "Пробный урок назначен.";
            case STUDENT -> "Заявка перешла в обучение.";
            case REJECTED -> "Репетитор отклонил заявку. Можно выбрать другого преподавателя из каталога.";
            case CANCELLED, ARCHIVED -> "Заявка закрыта.";
        };
    }

    private String statusTone(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW, VIEWED -> "amber";
            case ACCEPTED, CONTACT_ESTABLISHED, TRIAL_SCHEDULED, STUDENT -> "green";
            case REJECTED, CANCELLED, ARCHIVED -> "slate";
        };
    }

    private String formatDateTimeChip(LocalDateTime dateTime) {
        LocalDate today = currentDate();
        if (dateTime.toLocalDate().equals(today)) {
            return "Сегодня · " + TIME_FORMAT.format(dateTime);
        }
        if (dateTime.toLocalDate().equals(today.plusDays(1))) {
            return "Завтра · " + TIME_FORMAT.format(dateTime);
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM · HH:mm", RU));
    }

    private String formatConversationTime(java.time.Instant instant) {
        return TIME_FORMAT.format(instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
    }

    private String formatMoney(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString().replace('.', ',') + " ₽";
    }

    private boolean isCompletedHistoryLesson(Lesson lesson) {
        return hasActionableStatus(lesson) && !lessonEndsAt(lesson).isAfter(currentDateTime());
    }

    private String formatHistoryDate(LocalDateTime dateTime) {
        return dateTime.format(HISTORY_DAY_FORMAT).replace(".", "");
    }

    private LocalDateTime applicationHistoryTime(CrmApplication application) {
        Instant instant = application.getUpdatedAt() != null ? application.getUpdatedAt() : application.getCreatedAt();
        if (instant == null) {
            return application.getDesiredStartTime();
        }
        return LocalDateTime.ofInstant(instant, clock.getZone());
    }

    private String applicationHistoryTitle(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW -> "Заявка отправлена";
            case VIEWED -> "Заявка просмотрена";
            case ACCEPTED -> "Репетитор откликнулся";
            case CONTACT_ESTABLISHED -> "Контакт установлен";
            case TRIAL_SCHEDULED -> "Пробный урок назначен";
            case STUDENT -> "Стал учеником";
            case REJECTED -> "Заявка отклонена";
            case CANCELLED -> "Заявка отменена";
            case ARCHIVED -> "Заявка в архиве";
        };
    }

    private String applicationHistoryStatus(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW -> "ожидает";
            case VIEWED -> "просмотрена";
            case ACCEPTED -> "отклик";
            case CONTACT_ESTABLISHED -> "контакт";
            case TRIAL_SCHEDULED -> "пробный урок";
            case STUDENT -> "ученик";
            case REJECTED -> "отклонена";
            case CANCELLED -> "отменена";
            case ARCHIVED -> "архив";
        };
    }

    private String applicationHistoryTone(CrmApplication application) {
        return switch (application.getStatus()) {
            case NEW, VIEWED -> "amber";
            case ACCEPTED, CONTACT_ESTABLISHED, TRIAL_SCHEDULED, STUDENT -> "indigo";
            case REJECTED, CANCELLED, ARCHIVED -> "slate";
        };
    }

    private String lastLessonCaption(List<Lesson> completedLessons) {
        return completedLessons.isEmpty() ? "пока нет завершенных уроков" : "последнее " + formatHistoryDate(completedLessons.get(0).getStartsAt());
    }

    private String capitalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.substring(0, 1).toUpperCase(RU) + input.substring(1);
    }

    private LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    private LocalDateTime currentDateTime() {
        return LocalDateTime.now(clock);
    }

    private LocalDate resolveClientWeekStart(List<Lesson> lessons) {
        LocalDate currentWeekStart = currentDate().with(DayOfWeek.MONDAY);
        DayOfWeek today = currentDate().getDayOfWeek();
        if (today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY) {
            return currentWeekStart;
        }

        return lessons.stream()
                .filter(this::isActionableUpcomingOrInProgressLesson)
                .map(lesson -> lesson.getStartsAt().toLocalDate().with(DayOfWeek.MONDAY))
                .filter(lessonWeekStart -> lessonWeekStart.isAfter(currentWeekStart))
                .findFirst()
                .orElse(currentWeekStart);
    }

    private boolean isUpcomingOrInProgressLesson(Lesson lesson) {
        return lessonEndsAt(lesson).isAfter(currentDateTime());
    }

    private boolean isActionableUpcomingOrInProgressLesson(Lesson lesson) {
        return hasActionableStatus(lesson) && isUpcomingOrInProgressLesson(lesson);
    }

    private boolean canJoinLesson(Lesson lesson) {
        return isActionableUpcomingOrInProgressLesson(lesson);
    }

    private boolean canRescheduleLesson(Lesson lesson) {
        return hasActionableStatus(lesson) && lesson.getStartsAt().isAfter(currentDateTime());
    }

    private boolean hasActionableStatus(Lesson lesson) {
        return "Запланировано".equals(lesson.getStatus());
    }

    private int lessonDurationMinutes(Lesson lesson) {
        return lesson.getDurationMinutes() == null ? 60 : lesson.getDurationMinutes();
    }

    private LocalDateTime lessonEndsAt(Lesson lesson) {
        return lesson.getStartsAt().plusMinutes(lessonDurationMinutes(lesson));
    }

    private record HistoryEventDraft(LocalDateTime occurredAt, ClientHistoryPageDto.EventDto event) {
    }
}
