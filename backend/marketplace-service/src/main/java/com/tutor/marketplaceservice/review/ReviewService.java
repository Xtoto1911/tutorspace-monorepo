package com.tutor.marketplaceservice.review;

import com.tutor.marketplaceservice.tutor.TutorProfile;
import com.tutor.marketplaceservice.tutor.TutorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TutorProfileService tutorProfileService;
    private final ReviewEligibilityClient reviewEligibilityClient;
    private final AdminComplaintClient adminComplaintClient;
    private final ReviewPublicationRestrictionRepository reviewPublicationRestrictionRepository;

    @Transactional(readOnly = true)
    public List<ReviewDto> listPublished(UUID tutorProfileId) {
        List<Review> reviews = tutorProfileId == null
                ? reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PUBLISHED)
                : reviewRepository.findByTutorProfileIdAndStatusOrderByCreatedAtDesc(tutorProfileId, ReviewStatus.PUBLISHED);

        return reviews.stream().map(ReviewDto::from).toList();
    }

    @Transactional
    public ReviewDto create(CreateReviewRequest request, UUID clientUserId) {
        TutorProfile tutorProfile = tutorProfileService.getProfile(request.tutorProfileId());
        if (reviewPublicationRestrictionRepository.existsByUserIdAndActiveTrue(clientUserId)) {
            throw new ReviewNotAllowedException("Пользователю запрещена публикация отзывов.");
        }

        if (!reviewEligibilityClient.hasFinishedLesson(clientUserId, tutorProfile.getUserId())) {
            throw new ReviewNotAllowedException("Отзыв можно оставить только после прошедшего занятия с репетитором.");
        }

        Review review = reviewRepository
                .findByTutorProfileIdAndClientUserId(request.tutorProfileId(), clientUserId)
                .orElseGet(() -> {
                    Review newReview = new Review();
                    newReview.setTutorProfile(tutorProfile);
                    newReview.setClientUserId(clientUserId);
                    return newReview;
                });
        review.setRating(request.rating());
        review.setText(request.text());
        review.setStatus(ReviewStatus.PENDING_MODERATION);

        Review savedReview = reviewRepository.save(review);
        recalculateTutorRating(tutorProfile);

        return ReviewDto.from(savedReview);
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityDto getEligibility(UUID tutorProfileId, UUID clientUserId) {
        TutorProfile tutorProfile = tutorProfileService.getProfile(tutorProfileId);
        ReviewDto existingReview = reviewRepository.findByTutorProfileIdAndClientUserId(tutorProfileId, clientUserId)
                .map(ReviewDto::from)
                .orElse(null);

        if (!reviewEligibilityClient.hasFinishedLesson(clientUserId, tutorProfile.getUserId())) {
            return ReviewEligibilityDto.denied("NO_FINISHED_LESSON");
        }

        return ReviewEligibilityDto.allowed(existingReview);
    }

    @Transactional
    public void updateStatus(UUID reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));
        review.setStatus(status);
        reviewRepository.save(review);
        recalculateTutorRating(review.getTutorProfile());
    }

    @Transactional
    public void restrictPublication(UUID userId, UUID moderatorId, String reason) {
        ReviewPublicationRestriction restriction = reviewPublicationRestrictionRepository.findByUserId(userId)
                .orElseGet(ReviewPublicationRestriction::new);
        restriction.setUserId(userId);
        restriction.setModeratorId(moderatorId);
        restriction.setReason(reason == null || reason.isBlank() ? "Ограничение по решению модерации." : reason.trim());
        restriction.setActive(true);
        reviewPublicationRestrictionRepository.save(restriction);
    }

    @Transactional(readOnly = true)
    public ReviewComplaintDto createComplaint(UUID reviewId,
                                              UUID reporterUserId,
                                              CreateReviewComplaintRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));
        AdminComplaintClient.AdminComplaintRequest complaintRequest =
                new AdminComplaintClient.AdminComplaintRequest(
                        review.getTutorProfile().getUserId(),
                        reporterUserId,
                        "INAPPROPRIATE_CONTENT",
                        request.reason(),
                        request.description(),
                        "MEDIUM",
                        "REVIEW",
                        review.getId(),
                        review.getText()
                );
        return adminComplaintClient.createReviewComplaint(complaintRequest);
    }

    private void recalculateTutorRating(TutorProfile tutorProfile) {
        List<Review> publishedReviews = reviewRepository.findByTutorProfileIdAndStatus(
                tutorProfile.getId(),
                ReviewStatus.PUBLISHED
        );

        tutorProfile.setReviewsCount(publishedReviews.size());
        tutorProfile.setRating(publishedReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0));
    }
}
