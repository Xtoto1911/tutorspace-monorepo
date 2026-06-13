package com.tutor.marketplaceservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.tutor.marketplaceservice.shared.ConflictException;
import com.tutor.marketplaceservice.shared.ServiceUnavailableException;

@Component
@Slf4j
@RequiredArgsConstructor
public class CrmSyncClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.crm-service-url:}")
    private String crmServiceUrl;

    public CrmRequestSyncResponse syncRequest(RequestCreatedEvent event) {
        if (!StringUtils.hasText(crmServiceUrl)) {
            return new CrmRequestSyncResponse(event.requestId(), event.requestId());
        }

        try {
            CrmRequestSyncResponse response = restClientBuilder.build()
                    .post()
                    .uri(crmServiceUrl + "/internal/marketplace/requests")
                    .body(event)
                    .retrieve()
                    .body(CrmRequestSyncResponse.class);
            return response == null ? new CrmRequestSyncResponse(event.requestId(), event.requestId()) : response;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new ConflictException("У клиента уже есть активная заявка этому репетитору.");
            }
            log.warn("CRM request sync failed for request {}: {}", event.requestId(), exception.getMessage());
            throw new ServiceUnavailableException("CRM request sync failed. Retry later.", exception);
        } catch (RuntimeException exception) {
            log.warn("CRM request sync failed for request {}: {}", event.requestId(), exception.getMessage());
            throw new ServiceUnavailableException("CRM request sync failed. Retry later.", exception);
        }
    }
}
