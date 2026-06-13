package com.tutor.adminservice.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketplaceServiceClientTest {

    @Test
    void listTutorProfilesSendsInternalTokenHeader() throws Exception {
        var internalToken = new AtomicReference<String>();
        try (var server = TestHttpServer.start(exchange -> {
            internalToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            TestHttpServer.respondJson(exchange, 200, """
                    [{
                      "id": "%s",
                      "userId": "%s",
                      "slug": "math-tutor",
                      "fullName": "Math Tutor",
                      "status": "ACTIVE"
                    }]
                    """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        })) {
            var client = new MarketplaceServiceClient(server.baseUrl(), "internal-secret");

            client.listTutorProfiles("ACTIVE");

            assertEquals("internal-secret", internalToken.get());
        }
    }

    @Test
    void updateReviewStatusSendsInternalTokenHeaderAndPropagatesDownstreamFailure() throws Exception {
        var internalToken = new AtomicReference<String>();
        try (var server = TestHttpServer.start(exchange -> {
            internalToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        })) {
            var client = new MarketplaceServiceClient(server.baseUrl(), "internal-secret");

            assertThrows(RestClientResponseException.class,
                    () -> client.updateReviewStatus(UUID.randomUUID(), "PUBLISHED"));

            assertEquals("internal-secret", internalToken.get());
        }
    }

    @Test
    void restrictReviewPublicationSendsInternalTokenHeader() throws Exception {
        var internalToken = new AtomicReference<String>();
        try (var server = TestHttpServer.start(exchange -> {
            internalToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        })) {
            var client = new MarketplaceServiceClient(server.baseUrl(), "internal-secret");

            client.restrictReviewPublication(UUID.randomUUID(), UUID.randomUUID(), "Policy violation");

            assertEquals("internal-secret", internalToken.get());
        }
    }

    @Test
    void hideTutorProfileSendsInternalTokenHeader() throws Exception {
        var internalToken = new AtomicReference<String>();
        try (var server = TestHttpServer.start(exchange -> {
            internalToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        })) {
            var client = new MarketplaceServiceClient(server.baseUrl(), "internal-secret");

            client.hideTutorProfile(UUID.randomUUID());

            assertEquals("internal-secret", internalToken.get());
        }
    }

    @Test
    void updateTutorProfileStatusSendsInternalTokenHeader() throws Exception {
        var internalToken = new AtomicReference<String>();
        try (var server = TestHttpServer.start(exchange -> {
            internalToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        })) {
            var client = new MarketplaceServiceClient(server.baseUrl(), "internal-secret");

            client.updateTutorProfileStatus(UUID.randomUUID(), "ACTIVE");

            assertEquals("internal-secret", internalToken.get());
        }
    }
}
