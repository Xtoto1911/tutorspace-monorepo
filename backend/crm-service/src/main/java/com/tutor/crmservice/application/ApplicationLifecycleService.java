package com.tutor.crmservice.application;

import com.tutor.crmservice.integration.MarketplaceRequestCreatedDto;
import com.tutor.crmservice.message.MessageService;
import com.tutor.crmservice.kafka.NotificationEventPublisher;
import com.tutor.crmservice.request.ApplicationStatus;
import com.tutor.crmservice.request.CrmApplication;
import com.tutor.crmservice.request.CrmApplicationRepository;
import com.tutor.crmservice.shared.ConflictException;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import com.tutor.crmservice.student.StudentCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationLifecycleService {

    private static final Set<ApplicationStatus> ACTIVE_REQUEST_STATUSES = Set.of(
            ApplicationStatus.NEW,
            ApplicationStatus.VIEWED,
            ApplicationStatus.ACCEPTED,
            ApplicationStatus.CONTACT_ESTABLISHED,
            ApplicationStatus.TRIAL_SCHEDULED,
            ApplicationStatus.STUDENT
    );

    private static final Set<ApplicationStatus> MANUAL_STAGE_STATUSES = Set.of(
            ApplicationStatus.VIEWED,
            ApplicationStatus.CONTACT_ESTABLISHED,
            ApplicationStatus.STUDENT,
            ApplicationStatus.ARCHIVED
    );

    private final CrmApplicationRepository applicationRepository;
    private final StudentCardService studentCardService;
    private final MessageService messageService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public UUID ingestMarketplaceRequest(MarketplaceRequestCreatedDto request) {
        var existingApplication = applicationRepository.findByMarketplaceRequestId(request.requestId());
        if (existingApplication.isPresent()) {
            return existingApplication.get().getId();
        }

        boolean duplicateActive = applicationRepository.existsByClientUserIdAndTutorProfileIdAndStatusIn(
                request.clientUserId(), request.tutorProfileId(), ACTIVE_REQUEST_STATUSES);
        if (duplicateActive) {
            throw new ConflictException("У клиента уже есть активная заявка этому репетитору.");
        }

        CrmApplication app = new CrmApplication();
        app.setMarketplaceRequestId(request.requestId());
        app.setClientUserId(request.clientUserId());
        app.setTutorUserId(request.tutorUserId());
        app.setTutorProfileId(request.tutorProfileId());
        app.setTutorSlug(request.tutorSlug());
        app.setTutorName(request.tutorFullName());
        app.setStudentName(normalizeText(request.studentName(), "Ученик"));
        app.setStudentGrade(normalizeText(request.studentGrade(), "класс уточняется"));
        app.setSubjectId(request.subjectId());
        app.setSubjectName(request.subjectName());
        app.setFormat("Онлайн");
        app.setGoal("Пробный урок");
        app.setComment(normalizeComment(request.comment()));
        app.setDesiredStartTime(request.desiredStartTime());
        app.setPricePerHour(request.pricePerHour() == null ? new BigDecimal("1800") : request.pricePerHour());
        Instant createdAt = request.createdAt() == null ? Instant.now() : request.createdAt();
        app.setSource("MARKETPLACE");
        app.setResponseDeadline(createdAt.plusSeconds(24 * 60 * 60));
        app.setLeadScore(calculateLeadScore(request, app));
        CrmApplication saved = applicationRepository.save(app);
        messageService.createThreadForApplication(saved);
        return saved.getId();
    }

    @Transactional
    public void acceptTutorRequest(UUID tutorUserId, UUID requestId) {
        CrmApplication app = applicationRepository.findByIdAndTutorUserId(requestId, tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена."));
        if (app.getStatus() != ApplicationStatus.NEW && app.getStatus() != ApplicationStatus.VIEWED) {
            throw new ConflictException("Заявку можно принять только из статуса NEW или VIEWED.");
        }
        app.setStatus(ApplicationStatus.ACCEPTED);
        app.setRespondedAt(Instant.now());
        applicationRepository.save(app);
        if (studentCardService.findByApplicationId(app.getId()).isEmpty()) {
            studentCardService.createFromApplication(app);
        }
        messageService.createAcceptNotification(app);
        notificationEventPublisher.publishApplicationAccepted(
                app.getClientUserId(), app.getId(), app.getTutorName(), app.getSubjectName());
    }

    @Transactional
    public void rejectTutorRequest(UUID tutorUserId, UUID requestId, String reason) {
        CrmApplication app = applicationRepository.findByIdAndTutorUserId(requestId, tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена."));
        if (app.getStatus() != ApplicationStatus.NEW && app.getStatus() != ApplicationStatus.VIEWED) {
            throw new ConflictException("Заявку можно отклонить только из статуса NEW или VIEWED.");
        }
        app.setStatus(ApplicationStatus.REJECTED);
        app.setRespondedAt(Instant.now());
        app.setRejectionReason(reason == null || reason.isBlank() ? "Не подходит по параметрам" : reason);
        applicationRepository.save(app);
        messageService.createRejectNotification(app);
        notificationEventPublisher.publishApplicationRejected(
                app.getClientUserId(), app.getId(), app.getTutorName(), app.getSubjectName(), app.getRejectionReason());
    }

    @Transactional
    public void cancelClientApplication(UUID clientUserId, UUID applicationId) {
        CrmApplication app = applicationRepository.findByIdAndClientUserId(applicationId, clientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена."));
        if (!ACTIVE_REQUEST_STATUSES.contains(app.getStatus())) {
            throw new ConflictException("Заявку можно отменить только из активных статусов.");
        }
        app.setStatus(ApplicationStatus.CANCELLED);
        applicationRepository.save(app);
    }

    @Transactional
    public void updateTutorRequestStage(UUID tutorUserId, UUID requestId, ApplicationStatus status, String reason) {
        if (!MANUAL_STAGE_STATUSES.contains(status)) {
            throw new ConflictException("Этот этап заявки нельзя выставить вручную.");
        }
        CrmApplication app = applicationRepository.findByIdAndTutorUserId(requestId, tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена."));

        Instant now = Instant.now();
        switch (status) {
            case VIEWED -> {
                if (app.getStatus() == ApplicationStatus.NEW) {
                    app.setStatus(ApplicationStatus.VIEWED);
                }
                if (app.getViewedAt() == null) {
                    app.setViewedAt(now);
                }
            }
            case CONTACT_ESTABLISHED -> {
                ensureNotClosed(app);
                app.setStatus(ApplicationStatus.CONTACT_ESTABLISHED);
                if (app.getContactEstablishedAt() == null) {
                    app.setContactEstablishedAt(now);
                }
            }
            case STUDENT -> {
                ensureNotClosed(app);
                app.setStatus(ApplicationStatus.STUDENT);
                if (app.getConvertedAt() == null) {
                    app.setConvertedAt(now);
                }
            }
            case ARCHIVED -> {
                app.setStatus(ApplicationStatus.ARCHIVED);
                app.setArchivedAt(now);
                if (reason != null && !reason.isBlank()) {
                    app.setRejectionReason(reason);
                }
            }
            default -> throw new ConflictException("Этот этап заявки нельзя выставить вручную.");
        }

        applicationRepository.save(app);
    }

    @Transactional
    public void markTrialScheduled(UUID tutorUserId, UUID requestId) {
        CrmApplication app = applicationRepository.findByIdAndTutorUserId(requestId, tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена."));
        ensureNotClosed(app);
        app.setStatus(ApplicationStatus.TRIAL_SCHEDULED);
        if (app.getTrialScheduledAt() == null) {
            app.setTrialScheduledAt(Instant.now());
        }
        applicationRepository.save(app);
    }

    private void ensureNotClosed(CrmApplication app) {
        if (app.getStatus() == ApplicationStatus.REJECTED
                || app.getStatus() == ApplicationStatus.CANCELLED
                || app.getStatus() == ApplicationStatus.ARCHIVED) {
            throw new ConflictException("Закрытую заявку нельзя перевести в активный этап.");
        }
    }

    private int calculateLeadScore(MarketplaceRequestCreatedDto request, CrmApplication app) {
        int score = 0;
        if (request.subjectId() != null && hasText(request.subjectName())) {
            score += 20;
        }
        if (request.pricePerHour() != null || app.getPricePerHour() != null) {
            score += 20;
        }
        if (request.desiredStartTime() != null) {
            score += 20;
        }
        if (hasText(app.getStudentName()) && !"Ученик".equalsIgnoreCase(app.getStudentName())) {
            score += 20;
        }
        if (hasText(app.getStudentGrade()) && !"класс уточняется".equalsIgnoreCase(app.getStudentGrade())) {
            score += 10;
        }
        if (hasText(app.getComment()) && app.getComment().length() >= 12) {
            score += 10;
        }
        return Math.min(100, score);
    }

    private String normalizeComment(String comment) {
        return (comment == null || comment.isBlank())
                ? "Хочу обсудить удобный формат и цели обучения."
                : comment.trim();
    }

    private String normalizeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
