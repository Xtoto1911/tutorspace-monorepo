package com.tutor.marketplaceservice.review;

public record ReviewEligibilityDto(boolean eligible, String reason, ReviewDto existingReview) {
    public static ReviewEligibilityDto allowed(ReviewDto existingReview) {
        return new ReviewEligibilityDto(true, "FINISHED_LESSON_FOUND", existingReview);
    }

    public static ReviewEligibilityDto denied(String reason) {
        return new ReviewEligibilityDto(false, reason, null);
    }
}
