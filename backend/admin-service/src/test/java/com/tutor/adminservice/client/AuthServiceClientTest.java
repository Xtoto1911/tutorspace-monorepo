package com.tutor.adminservice.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceClientTest {

    @Test
    void updateUserStatusPropagatesDownstreamFailures() throws Exception {
        try (var server = TestHttpServer.start(exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        })) {
            var client = new AuthServiceClient(server.baseUrl());

            assertThrows(RestClientResponseException.class,
                    () -> client.updateUserStatus(UUID.randomUUID(), "BLOCKED", "admin-token"));
        }
    }
}
