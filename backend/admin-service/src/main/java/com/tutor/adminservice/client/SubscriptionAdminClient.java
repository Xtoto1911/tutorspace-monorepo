package com.tutor.adminservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SubscriptionAdminClient {

    private final RestClient restClient;
    private final String internalToken;

    public SubscriptionAdminClient(
            @Value("${app.subscription-service-url:http://subscription-service:8085}") String subscriptionServiceUrl,
            @Value("${app.internal-service-token:${jwt.secret:}}") String internalToken
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(subscriptionServiceUrl)
                .build();
        this.internalToken = internalToken;
    }

    public Snapshot snapshot() {
        return restClient.get()
                .uri("/internal/admin/subscriptions")
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(Snapshot.class);
    }

    public record Snapshot(
            List<TariffItem> tariffs,
            List<SubscriptionItem> subscriptions,
            List<PaymentItem> payments,
            List<PeriodItem> periods
    ) {}

    public record TariffItem(String code, String name, int priceRub, int durationDays, boolean active) {}

    public record SubscriptionItem(UUID id, UUID tutorUserId, String status, String tariffCode, Instant activeUntil) {}

    public record PaymentItem(UUID id, UUID tutorUserId, String tariffCode, int amountRub, String status, Instant createdAt) {}

    public record PeriodItem(UUID id, UUID subscriptionId, UUID paymentId, String tariffCode, Instant startedAt, Instant activeUntil) {}
}
