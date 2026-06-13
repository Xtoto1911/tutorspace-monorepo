package com.tutor.marketplaceservice.application;

import com.tutor.marketplaceservice.kafka.NotificationEventPublisher;
import com.tutor.marketplaceservice.subject.SubjectService;
import com.tutor.marketplaceservice.tutor.TutorProfile;
import com.tutor.marketplaceservice.tutor.TutorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorApplicationService {

    private final TutorProfileService tutorProfileService;
    private final SubjectService subjectService;
    private final CrmSyncClient crmSyncClient;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional(readOnly = true)
    public TutorApplicationResponse createRequest(UUID tutorProfileId, CreateApplicationRequest request) {
        TutorProfile tutorProfile = tutorProfileService.getProfile(tutorProfileId);
        var subject = subjectService.getSubject(request.subjectId());

        RequestCreatedEvent event = new RequestCreatedEvent(
                UUID.randomUUID(),
                request.clientUserId(),
                tutorProfile.getId(),
                tutorProfile.getUserId(),
                tutorProfile.getSlug(),
                tutorProfile.getFullName(),
                request.subjectId(),
                subject.getName(),
                tutorProfile.getPricePerHour(),
                normalizeText(request.studentName(), "Ученик"),
                normalizeText(request.studentGrade(), "класс уточняется"),
                request.desiredStartTime(),
                request.comment(),
                Instant.now()
        );

        CrmRequestSyncResponse syncResponse = crmSyncClient.syncRequest(event);
        UUID applicationId = syncResponse == null || syncResponse.applicationId() == null
                ? event.requestId()
                : syncResponse.applicationId();
        notificationEventPublisher.publishRequestCreated(
                tutorProfile.getUserId(), event.requestId(), applicationId,
                tutorProfile.getSlug(), tutorProfile.getFullName(), subject.getName());
        return new TutorApplicationResponse(event.requestId(), applicationId);
    }

    private String normalizeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
