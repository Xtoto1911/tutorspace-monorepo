package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.kafka.SubscriptionEventPublisher;
import com.tutor.subscriptionservice.tariff.Tariff;
import com.tutor.subscriptionservice.tariff.TariffRepository;
import com.tutor.subscriptionservice.yookassa.YooKassaPayment;
import com.tutor.subscriptionservice.yookassa.YooKassaPaymentClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private TariffRepository tariffRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TutorSubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPeriodRepository periodRepository;
    @Mock
    private SubscriptionEventPublisher eventPublisher;
    @Mock
    private YooKassaPaymentClient yooKassaPaymentClient;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void shouldReturnPersistedCheckoutPageWithoutCallingProvider() {
        var paymentId = UUID.randomUUID();
        var tutorUserId = UUID.randomUUID();
        var payment = Payment.builder()
                .id(paymentId)
                .tutorUserId(tutorUserId)
                .tariffCode("CRM_PRO")
                .amountRub(1990)
                .currency("RUB")
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://yoomoney.ru/pay/checkout-1")
                .providerPaymentId("yk_payment_1")
                .providerStatus("pending")
                .test(true)
                .paid(false)
                .build();
        var tariff = Tariff.builder()
                .code("CRM_PRO")
                .name("CRM Pro")
                .priceRub(1990)
                .durationDays(30)
                .active(true)
                .build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(tariffRepository.findByCode("CRM_PRO")).thenReturn(Optional.of(tariff));

        var page = subscriptionService.getCheckoutPage(paymentId);

        assertEquals(paymentId, page.paymentId());
        assertEquals("CRM_PRO", page.tariffCode());
        assertEquals("CRM Pro", page.tariffName());
        assertEquals(1990, page.amountRub());
        assertEquals("RUB", page.currency());
        assertEquals(30, page.durationDays());
        assertEquals(PaymentStatus.PENDING, page.status());
        assertEquals("pending", page.providerStatus());
        assertEquals("https://yoomoney.ru/pay/checkout-1", page.confirmationUrl());
        assertTrue(page.testMode());
        verifyNoInteractions(yooKassaPaymentClient);
    }

    @Test
    void shouldCreatePendingCheckoutPaymentWithYooKassaConfirmationUrl() {
        var tutorUserId = UUID.randomUUID();
        var tariff = Tariff.builder().code("CRM_BASIC").name("CRM Basic").priceRub(990).durationDays(30).active(true).build();
        when(tariffRepository.findByCode("CRM_BASIC")).thenReturn(Optional.of(tariff));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(UUID.randomUUID());
            }
            return payment;
        });
        when(yooKassaPaymentClient.createPayment(any(), any())).thenReturn(new YooKassaPayment(
                "yk_payment_1",
                "pending",
                false,
                true,
                990,
                "RUB",
                "https://yoomoney.ru/checkout/payments/v2/contract?orderId=yk_payment_1",
                null
        ));

        var response = subscriptionService.createCheckout(new CreateCheckoutRequest(tutorUserId, "CRM_BASIC"));

        assertNotNull(response.paymentId());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("https://yoomoney.ru/checkout/payments/v2/contract?orderId=yk_payment_1", response.confirmationUrl());

        var paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());
        var saved = paymentCaptor.getAllValues().getLast();
        assertEquals("yk_payment_1", saved.getProviderPaymentId());
        assertEquals("pending", saved.getProviderStatus());
        assertEquals("RUB", saved.getCurrency());
        assertTrue(saved.isTest());
        assertFalse(saved.isPaid());
    }

    @Test
    void shouldRefreshSucceededPaymentAndActivateSubscription() {
        var tutorUserId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var tariff = Tariff.builder().code("CRM_BASIC").name("CRM Basic").priceRub(990).durationDays(30).active(true).build();
        var payment = Payment.builder()
                .id(paymentId)
                .tutorUserId(tutorUserId)
                .tariffCode("CRM_BASIC")
                .amountRub(990)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://yoomoney.ru/pay/yk_payment_2")
                .providerPaymentId("yk_payment_2")
                .providerStatus("pending")
                .currency("RUB")
                .test(true)
                .paid(false)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(tariffRepository.findByCode("CRM_BASIC")).thenReturn(Optional.of(tariff));
        when(subscriptionRepository.findByTutorUserId(tutorUserId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.save(any(TutorSubscription.class))).thenAnswer(invocation -> {
            TutorSubscription subscription = invocation.getArgument(0);
            subscription.setId(UUID.randomUUID());
            return subscription;
        });
        when(periodRepository.save(any(SubscriptionPeriod.class))).thenAnswer(invocation -> {
            SubscriptionPeriod period = invocation.getArgument(0);
            period.setId(UUID.randomUUID());
            return period;
        });
        when(yooKassaPaymentClient.getPayment("yk_payment_2")).thenReturn(new YooKassaPayment(
                "yk_payment_2",
                "succeeded",
                true,
                true,
                990,
                "RUB",
                null,
                null
        ));

        var result = subscriptionService.refreshPayment(paymentId);

        assertEquals(paymentId, result.paymentId());
        assertEquals(tutorUserId, result.tutorUserId());
        assertEquals("CRM_BASIC", result.tariffCode());
        assertNotNull(result.activeUntil());
        assertEquals(PaymentStatus.CONFIRMED, payment.getStatus());
        verify(eventPublisher).publishSubscriptionActivated(eq(tutorUserId), eq("CRM_BASIC"), any());
    }

    @Test
    void shouldRefreshPendingPaymentWithoutActivatingSubscription() {
        var tutorUserId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var payment = Payment.builder()
                .id(paymentId)
                .tutorUserId(tutorUserId)
                .tariffCode("CRM_PRO")
                .amountRub(1990)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://yoomoney.ru/pay/yk_payment_pending")
                .providerPaymentId("yk_payment_pending")
                .providerStatus("pending")
                .currency("RUB")
                .test(true)
                .paid(false)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(yooKassaPaymentClient.getPayment("yk_payment_pending")).thenReturn(new YooKassaPayment(
                "yk_payment_pending",
                "pending",
                false,
                true,
                1990,
                "RUB",
                null,
                null
        ));

        var result = subscriptionService.refreshPayment(paymentId);

        assertEquals(paymentId, result.paymentId());
        assertEquals(tutorUserId, result.tutorUserId());
        assertEquals("CRM_PRO", result.tariffCode());
        assertNull(result.activeUntil());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(subscriptionRepository, periodRepository, eventPublisher);
    }

    @Test
    void shouldHandleSucceededWebhookIdempotently() {
        var tutorUserId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var subscriptionId = UUID.randomUUID();
        var periodId = UUID.randomUUID();
        var activeUntil = java.time.Instant.now().plusSeconds(3600);
        var payment = Payment.builder()
                .id(paymentId)
                .tutorUserId(tutorUserId)
                .tariffCode("CRM_PRO")
                .amountRub(1990)
                .status(PaymentStatus.CONFIRMED)
                .checkoutUrl("https://yoomoney.ru/pay/yk_payment_3")
                .providerPaymentId("yk_payment_3")
                .providerStatus("succeeded")
                .currency("RUB")
                .test(true)
                .paid(true)
                .build();
        var subscription = TutorSubscription.builder()
                .id(subscriptionId)
                .tutorUserId(tutorUserId)
                .status(SubscriptionStatus.ACTIVE)
                .currentTariffCode("CRM_PRO")
                .activeUntil(activeUntil)
                .build();
        var period = SubscriptionPeriod.builder()
                .id(periodId)
                .subscriptionId(subscriptionId)
                .paymentId(paymentId)
                .tariffCode("CRM_PRO")
                .startedAt(activeUntil.minusSeconds(3600))
                .activeUntil(activeUntil)
                .build();
        when(paymentRepository.findByProviderPaymentId("yk_payment_3")).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findByTutorUserId(tutorUserId)).thenReturn(Optional.of(subscription));
        when(periodRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(period));
        when(yooKassaPaymentClient.getPayment("yk_payment_3")).thenReturn(new YooKassaPayment(
                "yk_payment_3",
                "succeeded",
                true,
                true,
                1990,
                "RUB",
                null,
                null
        ));

        subscriptionService.handleWebhook(new YooKassaWebhookRequest(
                "payment.succeeded",
                new YooKassaWebhookObject("yk_payment_3"),
                "{\"event\":\"payment.succeeded\"}"
        ));

        assertEquals("{\"event\":\"payment.succeeded\"}", payment.getWebhookPayload());
        verify(eventPublisher, never()).publishSubscriptionActivated(any(), any(), any());
    }

    @Test
    void shouldCancelPaymentFromWebhookWithoutActivatingSubscription() {
        var paymentId = UUID.randomUUID();
        var payment = Payment.builder()
                .id(paymentId)
                .tutorUserId(UUID.randomUUID())
                .tariffCode("CRM_BASIC")
                .amountRub(990)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://yoomoney.ru/pay/yk_payment_cancelled")
                .providerPaymentId("yk_payment_cancelled")
                .providerStatus("pending")
                .currency("RUB")
                .test(true)
                .paid(false)
                .build();
        when(paymentRepository.findByProviderPaymentId("yk_payment_cancelled")).thenReturn(Optional.of(payment));
        when(yooKassaPaymentClient.getPayment("yk_payment_cancelled")).thenReturn(new YooKassaPayment(
                "yk_payment_cancelled",
                "canceled",
                false,
                true,
                990,
                "RUB",
                null,
                "insufficient_funds"
        ));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.handleWebhook(new YooKassaWebhookRequest(
                "payment.canceled",
                new YooKassaWebhookObject("yk_payment_cancelled"),
                "{\"event\":\"payment.canceled\"}"
        ));

        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        assertEquals("insufficient_funds", payment.getErrorReason());
        verifyNoInteractions(subscriptionRepository, periodRepository, eventPublisher);
    }

    @Test
    void shouldRejectSucceededPaymentWithUnexpectedAmount() {
        var paymentId = UUID.randomUUID();
        var payment = Payment.builder()
                .id(paymentId)
                .tutorUserId(UUID.randomUUID())
                .tariffCode("CRM_BASIC")
                .amountRub(990)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://yoomoney.ru/pay/yk_payment_wrong_amount")
                .providerPaymentId("yk_payment_wrong_amount")
                .providerStatus("pending")
                .currency("RUB")
                .test(true)
                .paid(false)
                .build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(yooKassaPaymentClient.getPayment("yk_payment_wrong_amount")).thenReturn(new YooKassaPayment(
                "yk_payment_wrong_amount",
                "succeeded",
                true,
                true,
                1,
                "RUB",
                null,
                null
        ));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalStateException.class, () -> subscriptionService.refreshPayment(paymentId));

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("Payment amount or currency does not match tariff", payment.getErrorReason());
        verifyNoInteractions(subscriptionRepository, periodRepository, eventPublisher);
    }
}
