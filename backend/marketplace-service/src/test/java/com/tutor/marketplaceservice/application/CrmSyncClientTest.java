package com.tutor.marketplaceservice.application;

import com.tutor.marketplaceservice.shared.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CrmSyncClientTest {

    @Test
    void syncRequestReturnsCrmApplicationId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CrmSyncClient client = new CrmSyncClient(builder);
        ReflectionTestUtils.setField(client, "crmServiceUrl", "http://crm-service:8082");
        UUID marketplaceRequestId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        RequestCreatedEvent event = sampleEvent(marketplaceRequestId);

        server.expect(requestTo("http://crm-service:8082/internal/marketplace/requests"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"requestId\":\"" + marketplaceRequestId + "\",\"applicationId\":\"" + applicationId + "\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON
                ));

        CrmRequestSyncResponse response = client.syncRequest(event);

        assertThat(response.requestId()).isEqualTo(marketplaceRequestId);
        assertThat(response.applicationId()).isEqualTo(applicationId);
        server.verify();
    }

    @Test
    void syncRequestThrowsConflictForDuplicateActiveRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CrmSyncClient client = new CrmSyncClient(builder);
        ReflectionTestUtils.setField(client, "crmServiceUrl", "http://crm-service:8082");

        server.expect(requestTo("http://crm-service:8082/internal/marketplace/requests"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.syncRequest(sampleEvent()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("У клиента уже есть активная заявка этому репетитору.");

        server.verify();
    }

    @Test
    void syncRequestFailsWhenCrmIsUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CrmSyncClient client = new CrmSyncClient(builder);
        ReflectionTestUtils.setField(client, "crmServiceUrl", "http://crm-service:8082");

        server.expect(requestTo("http://crm-service:8082/internal/marketplace/requests"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.syncRequest(sampleEvent()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CRM request sync failed");

        server.verify();
    }

    private RequestCreatedEvent sampleEvent() {
        return sampleEvent(UUID.randomUUID());
    }

    private RequestCreatedEvent sampleEvent(UUID requestId) {
        return new RequestCreatedEvent(
                requestId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "anna-petrova",
                "Анна Смирнова",
                UUID.randomUUID(),
                "Математика",
                new BigDecimal("1800"),
                "Мария Петрова",
                "8 класс",
                LocalDateTime.of(2026, 5, 10, 18, 0),
                "Нужна подготовка к ОГЭ",
                Instant.parse("2026-05-01T10:00:00Z")
        );
    }
}
