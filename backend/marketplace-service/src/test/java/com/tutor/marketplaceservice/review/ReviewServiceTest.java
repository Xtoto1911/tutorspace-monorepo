package com.tutor.marketplaceservice.review;

import com.tutor.marketplaceservice.tutor.TutorProfile;
import com.tutor.marketplaceservice.tutor.TutorProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TutorProfileService tutorProfileService;

    @Mock
    private ReviewEligibilityClient reviewEligibilityClient;

    @Mock
    private AdminComplaintClient adminComplaintClient;

    @Mock
    private ReviewPublicationRestrictionRepository reviewPublicationRestrictionRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createSavesReviewPendingModerationWhenClientHadFinishedLesson() {
        UUID tutorProfileId = UUID.randomUUID();
        UUID clientUserId = UUID.randomUUID();
        TutorProfile tutorProfile = new TutorProfile();
        tutorProfile.setId(tutorProfileId);
        tutorProfile.setUserId(UUID.randomUUID());
        tutorProfile.setSlug("ivan-math");
        tutorProfile.setFullName("Ivan Petrov");
        tutorProfile.setDescription("Math tutor");
        tutorProfile.setExperienceYears(5);
        tutorProfile.setPricePerHour(new BigDecimal("1500.00"));
        tutorProfile.setRating(4.0);
        tutorProfile.setReviewsCount(1);

        when(tutorProfileService.getProfile(tutorProfileId)).thenReturn(tutorProfile);
        when(reviewEligibilityClient.hasFinishedLesson(clientUserId, tutorProfile.getUserId())).thenReturn(true);
        when(reviewRepository.findByTutorProfileIdAndClientUserId(tutorProfileId, clientUserId)).thenReturn(Optional.empty());
        when(reviewRepository.findByTutorProfileIdAndStatus(tutorProfileId, ReviewStatus.PUBLISHED))
                .thenReturn(List.of(review(tutorProfile, UUID.randomUUID(), 4)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDto dto = reviewService.create(new CreateReviewRequest(
                tutorProfileId,
                5,
                "Great tutor"
        ), clientUserId);

        assertThat(dto.status()).isEqualTo(ReviewStatus.PENDING_MODERATION);
        assertThat(dto.clientUserId()).isEqualTo(clientUserId);
        assertThat(dto.tutorName()).isEqualTo("Ivan Petrov");
        assertThat(dto.tutorSlug()).isEqualTo("ivan-math");
        assertThat(tutorProfile.getReviewsCount()).isEqualTo(1);
        assertThat(tutorProfile.getRating()).isEqualTo(4.0);
    }

    @Test
    void updateExistingReviewDoesNotIncreaseReviewCount() {
        UUID tutorProfileId = UUID.randomUUID();
        UUID clientUserId = UUID.randomUUID();
        TutorProfile tutorProfile = tutorProfile(tutorProfileId, UUID.randomUUID(), 4.0, 1);
        Review existing = review(tutorProfile, clientUserId, 3);
        existing.setText("Old text");

        when(tutorProfileService.getProfile(tutorProfileId)).thenReturn(tutorProfile);
        when(reviewEligibilityClient.hasFinishedLesson(clientUserId, tutorProfile.getUserId())).thenReturn(true);
        when(reviewRepository.findByTutorProfileIdAndClientUserId(tutorProfileId, clientUserId)).thenReturn(Optional.of(existing));
        when(reviewRepository.findByTutorProfileIdAndStatus(tutorProfileId, ReviewStatus.PUBLISHED))
                .thenReturn(List.of());
        when(reviewRepository.save(existing)).thenReturn(existing);

        ReviewDto dto = reviewService.create(new CreateReviewRequest(tutorProfileId, 5, "Updated text"), clientUserId);

        assertThat(dto.rating()).isEqualTo(5);
        assertThat(dto.text()).isEqualTo("Updated text");
        assertThat(dto.status()).isEqualTo(ReviewStatus.PENDING_MODERATION);
        assertThat(tutorProfile.getReviewsCount()).isEqualTo(0);
        assertThat(tutorProfile.getRating()).isEqualTo(0.0);
    }

    @Test
    void listPublishedReturnsPublishedReviewsOnly() {
        UUID tutorProfileId = UUID.randomUUID();
        TutorProfile tutorProfile = tutorProfile(tutorProfileId, UUID.randomUUID(), 4.0, 1);
        Review published = review(tutorProfile, UUID.randomUUID(), 4);

        when(reviewRepository.findByTutorProfileIdAndStatusOrderByCreatedAtDesc(tutorProfileId, ReviewStatus.PUBLISHED))
                .thenReturn(List.of(published));

        List<ReviewDto> reviews = reviewService.listPublished(tutorProfileId);

        assertThat(reviews).hasSize(1);
        assertThat(reviews.getFirst().status()).isEqualTo(ReviewStatus.PUBLISHED);
        verify(reviewRepository).findByTutorProfileIdAndStatusOrderByCreatedAtDesc(tutorProfileId, ReviewStatus.PUBLISHED);
    }

    @Test
    void rejectsReviewWhenClientHasNoFinishedLessonsWithTutor() {
        UUID tutorProfileId = UUID.randomUUID();
        UUID clientUserId = UUID.randomUUID();
        TutorProfile tutorProfile = tutorProfile(tutorProfileId, UUID.randomUUID(), 0.0, 0);

        when(tutorProfileService.getProfile(tutorProfileId)).thenReturn(tutorProfile);
        when(reviewEligibilityClient.hasFinishedLesson(clientUserId, tutorProfile.getUserId())).thenReturn(false);

        ReviewEligibilityDto eligibility = reviewService.getEligibility(tutorProfileId, clientUserId);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.reason()).isEqualTo("NO_FINISHED_LESSON");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsReviewWhenClientHasActivePublicationRestriction() {
        UUID tutorProfileId = UUID.randomUUID();
        UUID clientUserId = UUID.randomUUID();
        TutorProfile tutorProfile = tutorProfile(tutorProfileId, UUID.randomUUID(), 0.0, 0);

        when(tutorProfileService.getProfile(tutorProfileId)).thenReturn(tutorProfile);
        when(reviewPublicationRestrictionRepository.existsByUserIdAndActiveTrue(clientUserId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(new CreateReviewRequest(
                tutorProfileId,
                5,
                "Helpful lesson"
        ), clientUserId))
                .isInstanceOf(ReviewNotAllowedException.class)
                .hasMessageContaining("запрещена публикация отзывов");

        verify(reviewEligibilityClient, never()).hasFinishedLesson(any(), any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void hideRejectedReviewRecalculatesRatingFromPublishedReviewsOnly() {
        UUID tutorProfileId = UUID.randomUUID();
        TutorProfile tutorProfile = tutorProfile(tutorProfileId, UUID.randomUUID(), 4.5, 2);
        Review rejected = review(tutorProfile, UUID.randomUUID(), 5);
        rejected.setId(UUID.randomUUID());

        when(reviewRepository.findById(rejected.getId())).thenReturn(Optional.of(rejected));
        when(reviewRepository.findByTutorProfileIdAndStatus(tutorProfileId, ReviewStatus.PUBLISHED))
                .thenReturn(List.of(review(tutorProfile, UUID.randomUUID(), 4)));

        reviewService.updateStatus(rejected.getId(), ReviewStatus.REJECTED);

        assertThat(rejected.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(tutorProfile.getReviewsCount()).isEqualTo(1);
        assertThat(tutorProfile.getRating()).isEqualTo(4.0);
    }

    @Test
    void createsReviewComplaintWithReviewSnapshot() {
        UUID reviewId = UUID.randomUUID();
        UUID reporterUserId = UUID.randomUUID();
        TutorProfile tutorProfile = tutorProfile(UUID.randomUUID(), UUID.randomUUID(), 5.0, 1);
        Review review = review(tutorProfile, UUID.randomUUID(), 5);
        review.setId(reviewId);
        review.setText("Suspicious review text");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(adminComplaintClient.createReviewComplaint(any())).thenReturn(new ReviewComplaintDto(
                UUID.randomUUID(),
                "PENDING"
        ));

        ReviewComplaintDto dto = reviewService.createComplaint(
                reviewId,
                reporterUserId,
                new CreateReviewComplaintRequest("Некорректный отзыв", "Похоже на спам")
        );

        assertThat(dto.status()).isEqualTo("PENDING");
        verify(adminComplaintClient).createReviewComplaint(any());
    }

    @Test
    void restrictPublicationCreatesActiveRestrictionForUser() {
        UUID userId = UUID.randomUUID();
        UUID moderatorId = UUID.randomUUID();

        when(reviewPublicationRestrictionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(reviewPublicationRestrictionRepository.save(any(ReviewPublicationRestriction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.restrictPublication(userId, moderatorId, "Repeated fake reviews");

        verify(reviewPublicationRestrictionRepository).save(argThat(restriction ->
                restriction.getUserId().equals(userId)
                        && restriction.getModeratorId().equals(moderatorId)
                        && restriction.isActive()
                        && restriction.getReason().equals("Repeated fake reviews")
        ));
    }

    private TutorProfile tutorProfile(UUID tutorProfileId, UUID tutorUserId, double rating, int reviewsCount) {
        TutorProfile tutorProfile = new TutorProfile();
        tutorProfile.setId(tutorProfileId);
        tutorProfile.setUserId(tutorUserId);
        tutorProfile.setSlug("ivan-math");
        tutorProfile.setFullName("Ivan Petrov");
        tutorProfile.setDescription("Math tutor");
        tutorProfile.setExperienceYears(5);
        tutorProfile.setPricePerHour(new BigDecimal("1500.00"));
        tutorProfile.setRating(rating);
        tutorProfile.setReviewsCount(reviewsCount);
        return tutorProfile;
    }

    private Review review(TutorProfile tutorProfile, UUID clientUserId, int rating) {
        Review review = new Review();
        review.setTutorProfile(tutorProfile);
        review.setClientUserId(clientUserId);
        review.setRating(rating);
        review.setText("Review text");
        review.setStatus(ReviewStatus.PUBLISHED);
        return review;
    }
}
