package com.tutor.crmservice.subscription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionActivatedListenerTest {

    @Mock
    private CrmAccessRepository crmAccessRepository;

    @InjectMocks
    private SubscriptionActivatedListener listener;

    @Test
    void shouldCreateCrmAccessForNewTutor() {
        var tutorUserId = UUID.randomUUID();
        var activeUntil = Instant.now().plusSeconds(3600);
        var envelope = new SubscriptionEventEnvelope<>(
                UUID.randomUUID(),
                "SUBSCRIPTION_ACTIVATED",
                Instant.now(),
                "subscription-service",
                new SubscriptionActivatedPayload(tutorUserId, "CRM_BASIC", activeUntil)
        );
        when(crmAccessRepository.findByTutorUserId(tutorUserId)).thenReturn(Optional.empty());
        when(crmAccessRepository.save(any(CrmAccess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onSubscriptionEvent(envelope);

        verify(crmAccessRepository).save(argThat(access ->
                access.getTutorUserId().equals(tutorUserId)
                        && access.getTariffCode().equals("CRM_BASIC")
                        && access.getActiveUntil().equals(activeUntil)
        ));
    }

    @Test
    void shouldUpdateExistingCrmAccess() {
        var tutorUserId = UUID.randomUUID();
        var activeUntil = Instant.now().plusSeconds(7200);
        var existing = CrmAccess.builder()
                .id(UUID.randomUUID())
                .tutorUserId(tutorUserId)
                .tariffCode("CRM_BASIC")
                .activeUntil(Instant.now())
                .build();
        var envelope = new SubscriptionEventEnvelope<>(
                UUID.randomUUID(),
                "SUBSCRIPTION_ACTIVATED",
                Instant.now(),
                "subscription-service",
                new SubscriptionActivatedPayload(tutorUserId, "CRM_PRO", activeUntil)
        );
        when(crmAccessRepository.findByTutorUserId(tutorUserId)).thenReturn(Optional.of(existing));
        when(crmAccessRepository.save(any(CrmAccess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onSubscriptionEvent(envelope);

        assertEquals("CRM_PRO", existing.getTariffCode());
        assertEquals(activeUntil, existing.getActiveUntil());
        verify(crmAccessRepository).save(existing);
    }

    @Test
    void shouldIgnoreUnknownEventType() {
        var envelope = new SubscriptionEventEnvelope<SubscriptionActivatedPayload>(
                UUID.randomUUID(),
                "OTHER_EVENT",
                Instant.now(),
                "subscription-service",
                null
        );

        listener.onSubscriptionEvent(envelope);

        verifyNoInteractions(crmAccessRepository);
    }
}
