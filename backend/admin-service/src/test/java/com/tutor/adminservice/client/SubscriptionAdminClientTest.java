package com.tutor.adminservice.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionAdminClientTest {

    @Test
    void snapshotSendsInternalTokenHeader() throws Exception {
        try (var server = TestHttpServer.start(exchange -> {
            assertEquals("internal-secret", exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            TestHttpServer.respondJson(exchange, 200, """
                    {
                      "tariffs": [],
                      "subscriptions": [],
                      "payments": [],
                      "periods": []
                    }
                    """);
        })) {
            var client = new SubscriptionAdminClient(server.baseUrl(), "internal-secret");

            var snapshot = client.snapshot();

            assertEquals(0, snapshot.tariffs().size());
            assertEquals(0, snapshot.subscriptions().size());
            assertEquals(0, snapshot.payments().size());
            assertEquals(0, snapshot.periods().size());
        }
    }
}
