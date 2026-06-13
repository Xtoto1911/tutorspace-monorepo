package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.tariff.Tariff;
import com.tutor.subscriptionservice.tariff.TariffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSubscriptionSnapshotControllerTest {

    @Mock
    private TariffRepository tariffRepository;
    @Mock
    private TutorSubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionPeriodRepository periodRepository;

    @Test
    void shouldReturnRealSnapshotLists() {
        var controller = new AdminSubscriptionSnapshotController(
                tariffRepository,
                subscriptionRepository,
                paymentRepository,
                periodRepository,
                "service-token"
        );
        var tariff = Tariff.builder()
                .code("CRM_BASIC")
                .name("CRM Basic")
                .priceRub(990)
                .durationDays(30)
                .active(true)
                .build();
        var subscriptionId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var tutorUserId = UUID.randomUUID();
        var activeUntil = Instant.parse("2026-06-20T10:15:30Z");
        var subscription = TutorSubscription.builder()
                .id(subscriptionId)
                .tutorUserId(tutorUserId)
                .status(SubscriptionStatus.ACTIVE)
                .currentTariffCode("CRM_BASIC")
                .activeUntil(activeUntil)
                .build();
        var payment = Payment.builder()
                .id(paymentId)
                .tutorUserId(tutorUserId)
                .tariffCode("CRM_BASIC")
                .amountRub(990)
                .status(PaymentStatus.CONFIRMED)
                .checkoutUrl("https://yoomoney.ru/pay/payment-1")
                .createdAt(Instant.parse("2026-05-20T10:15:30Z"))
                .build();
        var period = SubscriptionPeriod.builder()
                .id(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .paymentId(paymentId)
                .tariffCode("CRM_BASIC")
                .startedAt(Instant.parse("2026-05-20T10:15:30Z"))
                .activeUntil(activeUntil)
                .build();

        when(tariffRepository.findAll()).thenReturn(List.of(tariff));
        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(paymentRepository.findAll()).thenReturn(List.of(payment));
        when(periodRepository.findAll()).thenReturn(List.of(period));

        var snapshot = controller.snapshot("service-token");

        assertEquals(1, snapshot.tariffs().size());
        assertEquals(1, snapshot.subscriptions().size());
        assertEquals(1, snapshot.payments().size());
        assertEquals(1, snapshot.periods().size());
        assertEquals("CRM_BASIC", snapshot.tariffs().getFirst().code());
        assertEquals(tutorUserId, snapshot.subscriptions().getFirst().tutorUserId());
        assertEquals(PaymentStatus.CONFIRMED, snapshot.payments().getFirst().status());
        assertEquals(paymentId, snapshot.periods().getFirst().paymentId());
    }

    @Test
    void shouldRejectInvalidInternalToken() {
        var controller = new AdminSubscriptionSnapshotController(
                tariffRepository,
                subscriptionRepository,
                paymentRepository,
                periodRepository,
                "service-token"
        );

        var error = assertThrows(ResponseStatusException.class, () -> controller.snapshot("wrong-token"));

        assertEquals(FORBIDDEN, error.getStatusCode());
        assertEquals("Invalid internal token", error.getReason());
    }
}
