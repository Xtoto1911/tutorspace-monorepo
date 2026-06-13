package com.tutor.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class MarketplaceServiceClient {

    private final RestClient restClient;
    private final String internalToken;

    public MarketplaceServiceClient(
            @Value("${app.marketplace-service-url}") String marketplaceServiceUrl,
            @Value("${app.internal-service-token:${jwt.secret:}}") String internalToken
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(marketplaceServiceUrl)
                .build();
        this.internalToken = internalToken;
    }

    public List<ReviewModerationDto> listReviews(String status) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/reviews")
                        .queryParam("status", status)
                        .build())
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<TutorProfileModerationDto> listTutorProfiles(String status) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/marketplace/tutors")
                        .queryParam("status", status)
                        .build())
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public void updateReviewStatus(UUID reviewId, String status) {
        restClient.patch()
                .uri("/internal/reviews/{reviewId}/status", reviewId)
                .header("X-Internal-Token", internalToken)
                .body(new UpdateStatusRequest(status))
                .retrieve()
                .toBodilessEntity();
        log.info("Updated review {} status to {}", reviewId, status);
    }

    public void updateTutorProfileStatus(UUID tutorProfileId, String status) {
        restClient.patch()
                .uri("/internal/marketplace/tutors/{tutorProfileId}/status", tutorProfileId)
                .header("X-Internal-Token", internalToken)
                .body(new UpdateStatusRequest(status))
                .retrieve()
                .toBodilessEntity();
        log.info("Updated tutor profile {} status to {}", tutorProfileId, status);
    }

    public void restrictReviewPublication(UUID userId, UUID moderatorId, String reason) {
        restClient.post()
                .uri("/internal/reviews/publication-restrictions")
                .header("X-Internal-Token", internalToken)
                .body(new RestrictPublicationRequest(userId, moderatorId, reason))
                .retrieve()
                .toBodilessEntity();
        log.info("Restricted review publication for user {}", userId);
    }

    public void hideTutorProfile(UUID tutorProfileId) {
        restClient.patch()
                .uri("/internal/marketplace/tutors/{tutorProfileId}/hide", tutorProfileId)
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .toBodilessEntity();
        log.info("Hid tutor profile {}", tutorProfileId);
    }

    public record ReviewModerationDto(
            UUID id,
            UUID tutorProfileId,
            UUID clientUserId,
            Integer rating,
            String text,
            String status,
            Instant createdAt,
            Instant updatedAt,
            String authorName,
            String tutorName,
            String tutorSlug
    ) {}

    public record TutorProfileModerationDto(
            UUID id,
            UUID userId,
            String slug,
            String fullName,
            String description,
            Integer experienceYears,
            BigDecimal pricePerHour,
            String photoUrl,
            Double rating,
            Integer reviewsCount,
            String status,
            List<SubjectDto> subjects
    ) {}

    public record SubjectDto(UUID id, String name, String slug) {}

    public record UpdateStatusRequest(String status) {}

    public record RestrictPublicationRequest(UUID userId, UUID moderatorId, String reason) {}
}
