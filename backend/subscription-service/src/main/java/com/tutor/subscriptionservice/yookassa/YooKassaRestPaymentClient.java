package com.tutor.subscriptionservice.yookassa;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Component
public class YooKassaRestPaymentClient implements YooKassaPaymentClient {
    private final RestClient restClient;
    private final YooKassaProperties properties;

    public YooKassaRestPaymentClient(YooKassaProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeader(properties))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public YooKassaPayment createPayment(YooKassaCreatePaymentRequest request, String idempotenceKey) {
        assertCredentialsConfigured();
        var body = Map.of(
                "amount", Map.of(
                        "value", rubles(request.amountRub()),
                        "currency", "RUB"
                ),
                "capture", true,
                "confirmation", Map.of(
                        "type", "redirect",
                        "return_url", properties.getReturnUrl()
                ),
                "description", "TutorSpace CRM subscription: " + request.tariffName(),
                "metadata", Map.of(
                        "localPaymentId", request.localPaymentId().toString(),
                        "tutorUserId", request.tutorUserId().toString(),
                        "tariffCode", request.tariffCode()
                )
        );

        try {
            var response = restClient.post()
                    .uri("/v3/payments")
                    .header("Idempotence-Key", idempotenceKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return toPayment(response);
        } catch (RestClientException exception) {
            throw new YooKassaPaymentClientException("Failed to create YooKassa payment", exception);
        }
    }

    @Override
    public YooKassaPayment getPayment(String providerPaymentId) {
        assertCredentialsConfigured();
        try {
            var response = restClient.get()
                    .uri("/v3/payments/{paymentId}", providerPaymentId)
                    .retrieve()
                    .body(JsonNode.class);
            return toPayment(response);
        } catch (RestClientException exception) {
            throw new YooKassaPaymentClientException("Failed to fetch YooKassa payment " + providerPaymentId, exception);
        }
    }

    private YooKassaPayment toPayment(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            throw new YooKassaPaymentClientException("YooKassa returned an empty payment response");
        }
        return new YooKassaPayment(
                text(node, "id"),
                text(node, "status"),
                node.path("paid").asBoolean(false),
                node.path("test").asBoolean(false),
                amountRub(node.path("amount").path("value").asText("0")),
                node.path("amount").path("currency").asText("RUB"),
                node.path("confirmation").path("confirmation_url").asText(null),
                node.path("cancellation_details").path("reason").asText(null)
        );
    }

    private String text(JsonNode node, String field) {
        var value = node.path(field).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new YooKassaPaymentClientException("YooKassa payment response is missing field: " + field);
        }
        return value;
    }

    private int amountRub(String value) {
        return (int) Math.round(Double.parseDouble(value));
    }

    private String rubles(int amountRub) {
        return String.format(Locale.US, "%d.00", amountRub);
    }

    private String authorizationHeader(YooKassaProperties properties) {
        var credentials = properties.getShopId() + ":" + properties.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private void assertCredentialsConfigured() {
        if (!StringUtils.hasText(properties.getShopId()) || !StringUtils.hasText(properties.getSecretKey())) {
            throw new YooKassaPaymentClientException("YooKassa credentials are not configured");
        }
    }
}
