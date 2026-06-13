package com.tutor.marketplaceservice.application;

import com.tutor.marketplaceservice.kafka.NotificationEventPublisher;
import com.tutor.marketplaceservice.subject.Subject;
import com.tutor.marketplaceservice.subject.SubjectService;
import com.tutor.marketplaceservice.tutor.TutorProfile;
import com.tutor.marketplaceservice.tutor.TutorProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorApplicationServiceTest {

    @Mock TutorProfileService tutorProfileService;
    @Mock SubjectService subjectService;
    @Mock CrmSyncClient crmSyncClient;
    @Mock NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    TutorApplicationService service;

    @Test
    void createRequestIncludesStudentFullNameInCrmSyncEvent() {
        UUID tutorProfileId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        TutorProfile tutorProfile = new TutorProfile();
        tutorProfile.setId(tutorProfileId);
        tutorProfile.setUserId(UUID.randomUUID());
        tutorProfile.setSlug("anna-smirnova");
        tutorProfile.setFullName("Анна Смирнова");
        tutorProfile.setPricePerHour(new BigDecimal("1800"));
        when(tutorProfileService.getProfile(tutorProfileId)).thenReturn(tutorProfile);
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName("Математика");
        subject.setSlug("matematika");
        when(subjectService.getSubject(subjectId)).thenReturn(subject);
        when(crmSyncClient.syncRequest(any(RequestCreatedEvent.class)))
                .thenReturn(new CrmRequestSyncResponse(UUID.randomUUID(), applicationId));

        TutorApplicationResponse response = service.createRequest(tutorProfileId, new CreateApplicationRequest(
                UUID.randomUUID(),
                subjectId,
                LocalDateTime.of(2026, 5, 18, 18, 30),
                "Мария Петрова",
                "8 класс",
                "Хочу пробный урок"
        ));

        ArgumentCaptor<RequestCreatedEvent> eventCaptor = ArgumentCaptor.forClass(RequestCreatedEvent.class);
        verify(crmSyncClient).syncRequest(eventCaptor.capture());
        assertThat(eventCaptor.getValue().studentName()).isEqualTo("Мария Петрова");
        assertThat(eventCaptor.getValue().studentGrade()).isEqualTo("8 класс");
        assertThat(response.applicationId()).isEqualTo(applicationId);
        verify(notificationEventPublisher).publishRequestCreated(any(), any(), eq(applicationId), any(), any(), any());
    }
}
