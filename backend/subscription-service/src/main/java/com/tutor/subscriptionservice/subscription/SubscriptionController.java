package com.tutor.subscriptionservice.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @GetMapping("/current")
    public CurrentSubscriptionDto current(@RequestParam UUID tutorUserId) {
        return subscriptionService.getCurrent(tutorUserId);
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CreateCheckoutRequest request) {
        return subscriptionService.createCheckout(request);
    }

    @GetMapping("/checkout/{paymentId}")
    public CheckoutPageDto checkoutPage(@PathVariable UUID paymentId) {
        return subscriptionService.getCheckoutPage(paymentId);
    }

    @PostMapping("/payments/{paymentId}/refresh")
    public SubscriptionActivationResult refresh(@PathVariable UUID paymentId) {
        return subscriptionService.refreshPayment(paymentId);
    }

    @PostMapping("/yookassa/webhook")
    public void yookassaWebhook(@RequestBody JsonNode payload) {
        var event = payload.path("event").asText();
        var objectId = payload.path("object").path("id").asText();
        subscriptionService.handleWebhook(new YooKassaWebhookRequest(
                event,
                new YooKassaWebhookObject(objectId),
                toRawJson(payload)
        ));
    }

    private String toRawJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return payload.toString();
        }
    }
}
