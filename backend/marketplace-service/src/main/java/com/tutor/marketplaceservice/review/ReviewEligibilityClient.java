package com.tutor.marketplaceservice.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewEligibilityClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.crm-service-url:}")
    private String crmServiceUrl;

    public boolean hasFinishedLesson(UUID clientUserId, UUID tutorUserId) {
        if (!StringUtils.hasText(crmServiceUrl)) {
            return false;
        }

        try {
            CrmReviewEligibilityDto response = restClientBuilder.build()
                    .get()
                    .uri(crmServiceUrl + "/internal/review-eligibility?clientUserId={clientUserId}&tutorUserId={tutorUserId}",
                            clientUserId,
                            tutorUserId)
                    .retrieve()
                    .body(CrmReviewEligibilityDto.class);
            return response != null && response.eligible();
        } catch (RestClientResponseException exception) {
            log.warn("CRM review eligibility failed for client {} and tutor {}: {}",
                    clientUserId,
                    tutorUserId,
                    exception.getMessage());
            return false;
        } catch (RuntimeException exception) {
            log.warn("CRM review eligibility unavailable for client {} and tutor {}: {}",
                    clientUserId,
                    tutorUserId,
                    exception.getMessage());
            return false;
        }
    }

    private record CrmReviewEligibilityDto(boolean eligible, String reason) {
    }
}
