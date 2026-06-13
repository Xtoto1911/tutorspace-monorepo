package com.tutor.marketplaceservice.tutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalTutorProfileSyncControllerTest {

    @Mock
    private TutorProfileService tutorProfileService;

    @Test
    void listByStatusUsesTransactionalServiceMapping() {
        InternalTutorProfileSyncController controller =
                new InternalTutorProfileSyncController(tutorProfileService, "secret-token");
        TutorProfile activeProfile = tutorProfile("Anna Tutor", TutorProfileStatus.ACTIVE);
        TutorProfileDto activeProfileDto = TutorProfileDto.from(activeProfile);

        when(tutorProfileService.listByStatus(TutorProfileStatus.ACTIVE))
                .thenReturn(List.of(activeProfileDto));

        List<TutorProfileDto> profiles = controller.listByStatus(TutorProfileStatus.ACTIVE, "secret-token");

        assertThat(profiles).hasSize(1);
        assertThat(profiles.getFirst().fullName()).isEqualTo("Anna Tutor");
        assertThat(profiles.getFirst().status()).isEqualTo(TutorProfileStatus.ACTIVE);
    }

    @Test
    void listByStatusRejectsInvalidInternalToken() {
        InternalTutorProfileSyncController controller =
                new InternalTutorProfileSyncController(tutorProfileService, "secret-token");

        assertThatThrownBy(() -> controller.listByStatus(TutorProfileStatus.HIDDEN, "wrong-token"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("Invalid internal token");
                });
    }

    @Test
    void updateStatusDelegatesValidatedModeratorDecision() {
        InternalTutorProfileSyncController controller =
                new InternalTutorProfileSyncController(tutorProfileService, "secret-token");
        UUID profileId = UUID.randomUUID();

        controller.updateStatus(
                profileId,
                new InternalTutorProfileSyncController.UpdateStatusRequest(TutorProfileStatus.ACTIVE),
                "secret-token"
        );

        verify(tutorProfileService).updateStatus(profileId, TutorProfileStatus.ACTIVE);
    }

    private TutorProfile tutorProfile(String fullName, TutorProfileStatus status) {
        TutorProfile tutorProfile = new TutorProfile();
        tutorProfile.setId(UUID.randomUUID());
        tutorProfile.setUserId(UUID.randomUUID());
        tutorProfile.setSlug(fullName.toLowerCase().replace(" ", "-"));
        tutorProfile.setFullName(fullName);
        tutorProfile.setDescription("Math tutor");
        tutorProfile.setExperienceYears(5);
        tutorProfile.setPricePerHour(new BigDecimal("1500.00"));
        tutorProfile.setRating(4.5);
        tutorProfile.setReviewsCount(3);
        tutorProfile.setStatus(status);
        return tutorProfile;
    }
}
