package com.tutor.crmservice.application;

import com.tutor.crmservice.integration.MarketplaceRequestCreatedDto;
import com.tutor.crmservice.message.MessageService;
import com.tutor.crmservice.kafka.NotificationEventPublisher;
import com.tutor.crmservice.request.ApplicationStatus;
import com.tutor.crmservice.request.CrmApplication;
import com.tutor.crmservice.request.CrmApplicationRepository;
import com.tutor.crmservice.shared.ConflictException;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import com.tutor.crmservice.student.StudentCard;
import com.tutor.crmservice.student.StudentCardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationLifecycleServiceTest {

    @Mock CrmApplicationRepository applicationRepository;
    @Mock StudentCardService studentCardService;
    @Mock MessageService messageService;
    @Mock NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    ApplicationLifecycleService service;

    private MarketplaceRequestCreatedDto sampleRequest() {
        return new MarketplaceRequestCreatedDto(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "tutor-slug", "Иван Иванов",
                UUID.randomUUID(), "Математика", new BigDecimal("1800"),
                "Мария Петрова", "8 класс",
                LocalDateTime.now(), "Комментарий", java.time.Instant.now()
        );
    }

    @Test
    void ingest_duplicateActiveRequest_throwsConflict() {
        var request = sampleRequest();
        when(applicationRepository.findByMarketplaceRequestId(request.requestId())).thenReturn(Optional.empty());
        when(applicationRepository.existsByClientUserIdAndTutorProfileIdAndStatusIn(any(), any(), any()))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.ingestMarketplaceRequest(request));
    }

    @Test
    void ingest_newRequest_ok() {
        var request = sampleRequest();
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findByMarketplaceRequestId(request.requestId())).thenReturn(Optional.empty());
        when(applicationRepository.existsByClientUserIdAndTutorProfileIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(applicationRepository.save(any(CrmApplication.class))).thenAnswer(inv -> {
            CrmApplication saved = inv.getArgument(0);
            saved.setId(applicationId);
            return saved;
        });

        assertEquals(applicationId, service.ingestMarketplaceRequest(request));
        verify(applicationRepository).save(any(CrmApplication.class));
        verify(messageService).createThreadForApplication(any(CrmApplication.class));
    }

    @Test
    void ingest_newRequestStoresStudentFullNameFromMarketplace() {
        var request = new MarketplaceRequestCreatedDto(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "tutor-slug", "Иван Иванов",
                UUID.randomUUID(), "Математика", new BigDecimal("1800"),
                "Мария Петрова", "8 класс",
                LocalDateTime.now(), "Комментарий", java.time.Instant.now()
        );
        when(applicationRepository.findByMarketplaceRequestId(request.requestId())).thenReturn(Optional.empty());
        when(applicationRepository.existsByClientUserIdAndTutorProfileIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(applicationRepository.save(any(CrmApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ingestMarketplaceRequest(request);

        var captor = org.mockito.ArgumentCaptor.forClass(CrmApplication.class);
        verify(applicationRepository).save(captor.capture());
        assertEquals("Мария Петрова", captor.getValue().getStudentName());
        assertEquals("8 класс", captor.getValue().getStudentGrade());
    }

    @Test
    void ingest_newRequestAddsSlaSourceAndLeadScore() {
        Instant createdAt = Instant.parse("2026-05-20T10:15:00Z");
        var request = new MarketplaceRequestCreatedDto(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "tutor-slug", "Иван Иванов",
                UUID.randomUUID(), "Математика", new BigDecimal("1800"),
                "Мария Петрова", "8 класс",
                LocalDateTime.of(2026, 5, 22, 18, 0), "Нужно подтянуть алгебру перед ОГЭ", createdAt
        );
        when(applicationRepository.findByMarketplaceRequestId(request.requestId())).thenReturn(Optional.empty());
        when(applicationRepository.existsByClientUserIdAndTutorProfileIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(applicationRepository.save(any(CrmApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ingestMarketplaceRequest(request);

        var captor = org.mockito.ArgumentCaptor.forClass(CrmApplication.class);
        verify(applicationRepository).save(captor.capture());
        CrmApplication saved = captor.getValue();
        assertEquals("MARKETPLACE", saved.getSource());
        assertEquals(createdAt.plus(Duration.ofHours(24)), saved.getResponseDeadline());
        assertEquals(100, saved.getLeadScore());
    }

    @Test
    void ingest_duplicateMarketplaceRequestId_idempotent() {
        var request = sampleRequest();
        UUID applicationId = UUID.randomUUID();
        CrmApplication existing = new CrmApplication();
        existing.setId(applicationId);
        when(applicationRepository.findByMarketplaceRequestId(request.requestId())).thenReturn(Optional.of(existing));

        assertEquals(applicationId, service.ingestMarketplaceRequest(request));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void accept_fromNew_ok() {
        UUID tutorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.NEW);
        when(applicationRepository.findByIdAndTutorUserId(requestId, tutorId)).thenReturn(Optional.of(app));
        when(studentCardService.findByApplicationId(any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.acceptTutorRequest(tutorId, requestId));
        assertEquals(ApplicationStatus.ACCEPTED, app.getStatus());
        assertNotNull(app.getRespondedAt());
        verify(studentCardService).createFromApplication(app);
        verify(messageService).createAcceptNotification(app);
    }

    @Test
    void updateStage_marksContactEstablished() {
        UUID tutorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.ACCEPTED);
        when(applicationRepository.findByIdAndTutorUserId(requestId, tutorId)).thenReturn(Optional.of(app));

        service.updateTutorRequestStage(tutorId, requestId, ApplicationStatus.CONTACT_ESTABLISHED, null);

        assertEquals(ApplicationStatus.CONTACT_ESTABLISHED, app.getStatus());
        assertNotNull(app.getContactEstablishedAt());
        verify(applicationRepository).save(app);
    }

    @Test
    void updateStage_rejectsManualTrialScheduledTransition() {
        UUID tutorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        assertThrows(ConflictException.class, () ->
                service.updateTutorRequestStage(tutorId, requestId, ApplicationStatus.TRIAL_SCHEDULED, null));
    }

    @Test
    void accept_fromAccepted_throwsConflict() {
        UUID tutorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.ACCEPTED);
        when(applicationRepository.findByIdAndTutorUserId(requestId, tutorId)).thenReturn(Optional.of(app));

        assertThrows(ConflictException.class, () -> service.acceptTutorRequest(tutorId, requestId));
    }

    @Test
    void accept_fromRejected_throwsConflict() {
        UUID tutorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.REJECTED);
        when(applicationRepository.findByIdAndTutorUserId(requestId, tutorId)).thenReturn(Optional.of(app));

        assertThrows(ConflictException.class, () -> service.acceptTutorRequest(tutorId, requestId));
    }

    @Test
    void reject_fromNew_ok() {
        UUID tutorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.NEW);
        when(applicationRepository.findByIdAndTutorUserId(requestId, tutorId)).thenReturn(Optional.of(app));

        assertDoesNotThrow(() -> service.rejectTutorRequest(tutorId, requestId, "Нет времени"));
        assertEquals(ApplicationStatus.REJECTED, app.getStatus());
        assertEquals("Нет времени", app.getRejectionReason());
        verify(messageService).createRejectNotification(app);
    }

    @Test
    void reject_fromAccepted_throwsConflict() {
        UUID tutorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.ACCEPTED);
        when(applicationRepository.findByIdAndTutorUserId(requestId, tutorId)).thenReturn(Optional.of(app));

        assertThrows(ConflictException.class, () -> service.rejectTutorRequest(tutorId, requestId, null));
    }

    @Test
    void cancel_fromNew_ok() {
        UUID clientId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.NEW);
        when(applicationRepository.findByIdAndClientUserId(appId, clientId)).thenReturn(Optional.of(app));

        assertDoesNotThrow(() -> service.cancelClientApplication(clientId, appId));
        assertEquals(ApplicationStatus.CANCELLED, app.getStatus());
    }

    @Test
    void cancel_fromAccepted_ok() {
        UUID clientId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.ACCEPTED);
        when(applicationRepository.findByIdAndClientUserId(appId, clientId)).thenReturn(Optional.of(app));

        assertDoesNotThrow(() -> service.cancelClientApplication(clientId, appId));
        assertEquals(ApplicationStatus.CANCELLED, app.getStatus());
    }

    @Test
    void cancel_fromRejected_throwsConflict() {
        UUID clientId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        CrmApplication app = new CrmApplication();
        app.setStatus(ApplicationStatus.REJECTED);
        when(applicationRepository.findByIdAndClientUserId(appId, clientId)).thenReturn(Optional.of(app));

        assertThrows(ConflictException.class, () -> service.cancelClientApplication(clientId, appId));
    }

    @Test
    void accept_notFound_throwsResourceNotFound() {
        UUID tutorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(applicationRepository.findByIdAndTutorUserId(requestId, tutorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.acceptTutorRequest(tutorId, requestId));
    }
}
