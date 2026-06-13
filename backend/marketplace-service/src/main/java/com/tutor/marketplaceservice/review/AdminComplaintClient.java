package com.tutor.marketplaceservice.review;

import com.tutor.marketplaceservice.shared.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminComplaintClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.admin-service-url:}")
    private String adminServiceUrl;

    public AdminComplaintResponse createComplaint(AdminComplaintRequest request) {
        if (!StringUtils.hasText(adminServiceUrl)) {
            throw new ServiceUnavailableException(
                    "Admin service URL is not configured.",
                    new IllegalStateException("app.admin-service-url is empty")
            );
        }

        try {
            AdminComplaintDto response = restClientBuilder.build()
                    .post()
                    .uri(adminServiceUrl + "/internal/complaints")
                    .body(request)
                    .retrieve()
                    .body(AdminComplaintDto.class);
            if (response == null) {
                throw new ServiceUnavailableException(
                        "Admin complaint response is empty.",
                        new IllegalStateException("Admin service returned empty body")
                );
            }
            return new AdminComplaintResponse(response.id(), response.status());
        } catch (RuntimeException exception) {
            log.warn("Complaint sync failed for target {}: {}", request.targetId(), exception.getMessage());
            throw new ServiceUnavailableException("Не удалось отправить жалобу модераторам. Попробуйте позже.", exception);
        }
    }

    public ReviewComplaintDto createReviewComplaint(AdminComplaintRequest request) {
        AdminComplaintResponse response = createComplaint(request);
        return new ReviewComplaintDto(response.id(), response.status());
    }

    public record AdminComplaintRequest(
            UUID reportedUserId,
            UUID reporterUserId,
            String category,
            String reason,
            String description,
            String priority,
            String targetType,
            UUID targetId,
            String targetSnapshot
    ) {
    }

    public record AdminComplaintResponse(UUID id, String status) {
    }

    private record AdminComplaintDto(UUID id, String status) {
    }
}
