package com.tutor.crmservice.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MarketplaceClient {

    private final RestTemplate restTemplate;

    @Value("${marketplace.service.url:http://marketplace-service:8081}")
    private String marketplaceUrl;

    public List<String> fetchAvailableSubjects() {
        try {
            record SubjectDto(String name, String slug) {}
            SubjectDto[] subjects = restTemplate.getForObject(
                marketplaceUrl + "/api/marketplace/subjects",
                SubjectDto[].class
            );
            if (subjects == null) {
                return getDefaultSubjects();
            }
            return Arrays.stream(subjects)
                .map(SubjectDto::name)
                .toList();
        } catch (Exception e) {
            return getDefaultSubjects();
        }
    }

    public void syncTutorProfile(UUID userId,
                                 String fullName,
                                 String city,
                                 String headline,
                                 String description,
                                 Integer experienceYears,
                                 BigDecimal pricePerHour,
                                 boolean hidden,
                                 List<String> subjects) {
        try {
            SyncTutorProfileRequest request = new SyncTutorProfileRequest(
                    userId,
                    fullName,
                    city,
                    headline,
                    description,
                    experienceYears,
                    pricePerHour,
                    hidden,
                    subjects == null ? List.of() : subjects
            );
            restTemplate.postForObject(
                    marketplaceUrl + "/internal/marketplace/tutors/profile-sync",
                    request,
                    Object.class
            );
        } catch (Exception ignored) {
            // CRM remains the source of truth; marketplace sync will retry on the next profile save.
        }
    }

    public List<MarketplaceTutorProfileDto> fetchTutorProfilesByIds(Collection<UUID> tutorProfileIds) {
        if (tutorProfileIds == null || tutorProfileIds.isEmpty()) {
            return List.of();
        }

        return tutorProfileIds.stream()
                .map(this::fetchTutorProfileById)
                .filter(Objects::nonNull)
                .toList();
    }

    private MarketplaceTutorProfileDto fetchTutorProfileById(UUID tutorProfileId) {
        try {
            return restTemplate.getForObject(
                    marketplaceUrl + "/api/marketplace/tutors/" + tutorProfileId,
                    MarketplaceTutorProfileDto.class
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private record SyncTutorProfileRequest(
            UUID userId,
            String fullName,
            String city,
            String headline,
            String description,
            Integer experienceYears,
            BigDecimal pricePerHour,
            boolean hidden,
            List<String> subjects
    ) {
    }

    private List<String> getDefaultSubjects() {
        return List.of(
            "Математика", "Английский язык", "Русский язык",
            "Физика", "Химия", "Информатика",
            "История", "Биология", "Литература", "Обществознание"
        );
    }
}
