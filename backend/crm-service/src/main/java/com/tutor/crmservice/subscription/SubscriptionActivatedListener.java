package com.tutor.crmservice.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SubscriptionActivatedListener {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionActivatedListener.class);

    private final CrmAccessRepository crmAccessRepository;

    @Transactional
    @KafkaListener(topics = "crm-events")
    public void onSubscriptionEvent(SubscriptionEventEnvelope<SubscriptionActivatedPayload> envelope) {
        if (!"SUBSCRIPTION_ACTIVATED".equals(envelope.eventType())) {
            log.debug("Ignoring subscription event type {}", envelope.eventType());
            return;
        }

        var payload = envelope.payload();
        var access = crmAccessRepository.findByTutorUserId(payload.tutorUserId())
                .orElseGet(() -> {
                    CrmAccess crmAccess = new CrmAccess();
                    crmAccess.setTutorUserId(payload.tutorUserId());
                    return crmAccess;
                });

        access.setTariffCode(payload.tariffCode());
        access.setActiveUntil(payload.activeUntil());
        access.setUpdatedAt(Instant.now());
        crmAccessRepository.save(access);
        log.info("Updated CRM access for tutor {} until {}", payload.tutorUserId(), payload.activeUntil());
    }
}
