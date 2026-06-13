package com.tutor.marketplaceservice.tutor;

import com.tutor.marketplaceservice.subject.SubjectDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TutorProfileDto(
        UUID id,
        UUID userId,
        String slug,
        String fullName,
        String description,
        Integer experienceYears,
        BigDecimal pricePerHour,
        String photoUrl,
        String city,
        List<String> education,
        List<String> highlights,
        List<String> tags,
        Integer responseTimeMinutes,
        Integer activeStudents,
        Integer lessonsCount,
        Integer repeatRatePercent,
        Double rating,
        Integer reviewsCount,
        TutorProfileStatus status,
        List<SubjectDto> subjects
) {
    public static TutorProfileDto from(TutorProfile profile) {
        return new TutorProfileDto(
                profile.getId(),
                profile.getUserId(),
                profile.getSlug(),
                profile.getFullName(),
                profile.getDescription(),
                profile.getExperienceYears(),
                profile.getPricePerHour(),
                profile.getPhotoUrl(),
                profile.getCity(),
                List.copyOf(profile.getEducation()),
                List.copyOf(profile.getHighlights()),
                List.copyOf(profile.getTags()),
                profile.getResponseTimeMinutes(),
                null,
                null,
                null,
                profile.getRating(),
                profile.getReviewsCount(),
                profile.getStatus(),
                profile.getSubjects().stream().map(SubjectDto::from).toList()
        );
    }
}
