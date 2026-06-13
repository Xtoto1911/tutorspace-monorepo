package com.tutor.crmservice.lesson;

public record ReviewEligibilityDto(boolean eligible, String reason) {
    public static ReviewEligibilityDto allowed() {
        return new ReviewEligibilityDto(true, "FINISHED_LESSON_FOUND");
    }

    public static ReviewEligibilityDto denied() {
        return new ReviewEligibilityDto(false, "NO_FINISHED_LESSON");
    }
}
