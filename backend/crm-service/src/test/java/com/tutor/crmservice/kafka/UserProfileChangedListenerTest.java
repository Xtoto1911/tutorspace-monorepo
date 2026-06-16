package com.tutor.crmservice.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserProfileChangedListenerTest {

    @Mock
    private TutorNamePropagationService propagationService;

    @InjectMocks
    private UserProfileChangedListener listener;

    @Test
    void onUserProfileChangedDelegatesToPropagationService() {
        UUID userId = UUID.randomUUID();
        UserProfileChangedEvent event = new UserProfileChangedEvent(userId, "Пётр", Instant.parse("2026-06-15T10:00:00Z"));

        listener.onUserProfileChanged(event);

        verify(propagationService).propagateTutorName(userId, "Пётр");
    }

    @Test
    void onUserProfileChangedIgnoresEventsWithNullUserId() {
        UserProfileChangedEvent event = new UserProfileChangedEvent(null, "Пётр", Instant.now());

        listener.onUserProfileChanged(event);

        verifyNoInteractions(propagationService);
    }
}
