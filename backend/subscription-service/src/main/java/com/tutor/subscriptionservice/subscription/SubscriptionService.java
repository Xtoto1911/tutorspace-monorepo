package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.kafka.SubscriptionEventPublisher;
import com.tutor.subscriptionservice.tariff.TariffRepository;
import com.tutor.subscriptionservice.yookassa.YooKassaCreatePaymentRequest;
import com.tutor.subscriptionservice.yookassa.YooKassaPayment;
import com.tutor.subscriptionservice.yookassa.YooKassaPaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final TariffRepository tariffRepository;
    private final PaymentRepository paymentRepository;
    private final TutorSubscriptionRepository subscriptionRepository;
    private final SubscriptionPeriodRepository periodRepository;
    private final SubscriptionEventPublisher eventPublisher;
    private final YooKassaPaymentClient yooKassaPaymentClient;

    @Transactional
    public CheckoutResponse createCheckout(CreateCheckoutRequest request) {
        var tariff = tariffRepository.findByCode(request.tariffCode())
                .filter(t -> t.isActive())
                .orElseThrow(() -> new RuntimeException("Active tariff not found: " + request.tariffCode()));

        var payment = Payment.builder()
                .tutorUserId(request.tutorUserId())
                .tariffCode(tariff.getCode())
                .amountRub(tariff.getPriceRub())
                .currency("RUB")
                .status(PaymentStatus.PENDING)
                .checkoutUrl("pending")
                .build();

        var saved = paymentRepository.save(payment);
        var providerPayment = yooKassaPaymentClient.createPayment(
                new YooKassaCreatePaymentRequest(
                        saved.getId(),
                        saved.getTutorUserId(),
                        tariff.getCode(),
                        tariff.getName(),
                        tariff.getPriceRub()
                ),
                saved.getId().toString()
        );
        applyProviderPayment(saved, providerPayment);
        if (providerPayment.confirmationUrl() == null || providerPayment.confirmationUrl().isBlank()) {
            throw new IllegalStateException("YooKassa payment confirmation URL is missing");
        }
        saved.setCheckoutUrl(providerPayment.confirmationUrl());
        return toCheckoutResponse(paymentRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public CurrentSubscriptionDto getCurrent(UUID tutorUserId) {
        var now = Instant.now();
        return subscriptionRepository.findByTutorUserId(tutorUserId)
                .map(subscription -> new CurrentSubscriptionDto(
                        tutorUserId,
                        subscription.getActiveUntil().isAfter(now),
                        subscription.getCurrentTariffCode(),
                        subscription.getActiveUntil()))
                .orElseGet(() -> CurrentSubscriptionDto.inactive(tutorUserId));
    }

    @Transactional(readOnly = true)
    public CheckoutPageDto getCheckoutPage(UUID paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        var tariff = tariffRepository.findByCode(payment.getTariffCode())
                .orElseThrow(() -> new RuntimeException("Tariff not found: " + payment.getTariffCode()));

        return new CheckoutPageDto(
                payment.getId(),
                payment.getTariffCode(),
                tariff.getName(),
                payment.getAmountRub(),
                payment.getCurrency(),
                tariff.getDurationDays(),
                payment.getStatus(),
                payment.getProviderStatus(),
                payment.getCheckoutUrl(),
                payment.isTest()
        );
    }

    @Transactional
    public SubscriptionActivationResult confirmPayment(UUID paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            var subscription = subscriptionRepository.findByTutorUserId(payment.getTutorUserId())
                    .orElseThrow(() -> new RuntimeException("Subscription not found for confirmed payment: " + paymentId));
            var period = periodRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new RuntimeException("Subscription period not found for confirmed payment: " + paymentId));
            return toActivationResult(subscription, payment, period);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not pending: " + paymentId);
        }

        var tariff = tariffRepository.findByCode(payment.getTariffCode())
                .orElseThrow(() -> new RuntimeException("Tariff not found: " + payment.getTariffCode()));

        var now = Instant.now();
        var subscription = subscriptionRepository.findByTutorUserId(payment.getTutorUserId())
                .orElseGet(() -> TutorSubscription.builder()
                        .tutorUserId(payment.getTutorUserId())
                        .status(SubscriptionStatus.ACTIVE)
                        .activeUntil(now)
                        .currentTariffCode(payment.getTariffCode())
                        .build());

        var start = subscription.getActiveUntil() != null && subscription.getActiveUntil().isAfter(now)
                ? subscription.getActiveUntil()
                : now;
        var activeUntil = start.plus(tariff.getDurationDays(), ChronoUnit.DAYS);

        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setConfirmedAt(now);
        paymentRepository.save(payment);

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentTariffCode(tariff.getCode());
        subscription.setActiveUntil(activeUntil);
        subscription.setUpdatedAt(now);
        var savedSubscription = subscriptionRepository.save(subscription);

        var period = periodRepository.save(SubscriptionPeriod.builder()
                .subscriptionId(savedSubscription.getId())
                .paymentId(payment.getId())
                .tariffCode(tariff.getCode())
                .startedAt(start)
                .activeUntil(activeUntil)
                .build());

        eventPublisher.publishSubscriptionActivated(payment.getTutorUserId(), tariff.getCode(), activeUntil);
        return toActivationResult(savedSubscription, payment, period);
    }

    @Transactional
    public SubscriptionActivationResult refreshPayment(UUID paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return refreshPayment(payment);
    }

    @Transactional
    public void handleWebhook(YooKassaWebhookRequest request) {
        if (request.object() == null || request.object().id() == null || request.object().id().isBlank()) {
            throw new IllegalArgumentException("YooKassa webhook object id is missing");
        }
        var payment = paymentRepository.findByProviderPaymentId(request.object().id())
                .orElseThrow(() -> new RuntimeException("Payment not found for YooKassa id: " + request.object().id()));
        payment.setWebhookPayload(request.rawPayload());
        refreshPayment(payment);
    }

    private SubscriptionActivationResult refreshPayment(Payment payment) {
        if (payment.getProviderPaymentId() == null || payment.getProviderPaymentId().isBlank()) {
            throw new IllegalStateException("Provider payment id is missing");
        }

        var providerPayment = yooKassaPaymentClient.getPayment(payment.getProviderPaymentId());
        applyProviderPayment(payment, providerPayment);

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            if ("succeeded".equals(providerPayment.status())) {
                validateSucceededProviderPayment(payment, providerPayment);
            }
            paymentRepository.save(payment);
            return confirmedResult(payment);
        }

        if ("canceled".equals(providerPayment.status())) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setErrorReason(providerPayment.cancellationReason());
            paymentRepository.save(payment);
            return inactiveResult(payment);
        }

        if (!"succeeded".equals(providerPayment.status())) {
            paymentRepository.save(payment);
            return inactiveResult(payment);
        }

        validateSucceededProviderPayment(payment, providerPayment);
        paymentRepository.save(payment);
        return confirmPayment(payment.getId());
    }

    private void validateSucceededProviderPayment(Payment payment, YooKassaPayment providerPayment) {
        if (!providerPayment.paid()) {
            failPayment(payment, "YooKassa payment is not paid");
        }
        if (!providerPayment.test()) {
            failPayment(payment, "YooKassa payment is not a sandbox payment");
        }
        if (providerPayment.amountRub() != payment.getAmountRub() || !"RUB".equals(providerPayment.currency())) {
            failPayment(payment, "Payment amount or currency does not match tariff");
        }
    }

    private void failPayment(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorReason(reason);
        paymentRepository.save(payment);
        throw new IllegalStateException(reason);
    }

    private void applyProviderPayment(Payment payment, YooKassaPayment providerPayment) {
        payment.setProviderPaymentId(providerPayment.id());
        payment.setProviderStatus(providerPayment.status());
        payment.setPaid(providerPayment.paid());
        payment.setTest(providerPayment.test());
        payment.setCurrency(providerPayment.currency());
        if (providerPayment.confirmationUrl() != null && !providerPayment.confirmationUrl().isBlank()) {
            payment.setCheckoutUrl(providerPayment.confirmationUrl());
        }
        if (providerPayment.cancellationReason() != null && !providerPayment.cancellationReason().isBlank()) {
            payment.setErrorReason(providerPayment.cancellationReason());
        }
    }

    private SubscriptionActivationResult confirmedResult(Payment payment) {
        var subscription = subscriptionRepository.findByTutorUserId(payment.getTutorUserId())
                .orElseThrow(() -> new RuntimeException("Subscription not found for confirmed payment: " + payment.getId()));
        var period = periodRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> new RuntimeException("Subscription period not found for confirmed payment: " + payment.getId()));
        return toActivationResult(subscription, payment, period);
    }

    private SubscriptionActivationResult inactiveResult(Payment payment) {
        return new SubscriptionActivationResult(
                null,
                payment.getId(),
                null,
                payment.getTutorUserId(),
                payment.getTariffCode(),
                null
        );
    }

    private CheckoutResponse toCheckoutResponse(Payment payment) {
        return new CheckoutResponse(payment.getId(), payment.getCheckoutUrl(), payment.getStatus());
    }

    private SubscriptionActivationResult toActivationResult(TutorSubscription subscription, Payment payment, SubscriptionPeriod period) {
        return new SubscriptionActivationResult(
                subscription.getId(),
                payment.getId(),
                period.getId(),
                payment.getTutorUserId(),
                subscription.getCurrentTariffCode(),
                subscription.getActiveUntil()
        );
    }
}
