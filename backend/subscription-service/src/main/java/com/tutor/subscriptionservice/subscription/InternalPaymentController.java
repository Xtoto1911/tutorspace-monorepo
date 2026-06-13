package com.tutor.subscriptionservice.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payments/yookassa")
public class InternalPaymentController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/test-confirm/{paymentId}")
    public SubscriptionActivationResult testConfirm(@PathVariable UUID paymentId) {
        return subscriptionService.confirmPayment(paymentId);
    }
}
