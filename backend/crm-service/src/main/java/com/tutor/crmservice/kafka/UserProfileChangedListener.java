package com.tutor.crmservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileChangedListener {

    private final TutorNamePropagationService propagationService;

    @KafkaListener(
            topics = "user-profile-events",
            groupId = "crm-user-profile-changed",
            containerFactory = "userProfileListenerContainerFactory"
    )
    public void onUserProfileChanged(UserProfileChangedEvent event) {
        if (event == null || event.userId() == null) {
            log.warn("Ignoring UserProfileChanged event with no userId: {}", event);
            return;
        }
        log.debug("Received UserProfileChanged for user {} (changedAt={})", event.userId(), event.changedAt());
        propagationService.propagateTutorName(event.userId(), event.name());
    }
}
