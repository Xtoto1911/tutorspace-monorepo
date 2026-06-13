package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.tariff.Tariff;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminSubscriptionSnapshotDto(
        List<TariffItem> tariffs,
        List<SubscriptionItem> subscriptions,
        List<PaymentItem> payments,
        List<PeriodItem> periods
) {
    public record TariffItem(String code, String name, int priceRub, int durationDays, boolean active) {
        public static TariffItem from(Tariff tariff) {
            return new TariffItem(
                    tariff.getCode(),
                    tariff.getName(),
                    tariff.getPriceRub(),
                    tariff.getDurationDays(),
                    tariff.isActive()
            );
        }
    }

    public record SubscriptionItem(UUID id, UUID tutorUserId, SubscriptionStatus status, String tariffCode, Instant activeUntil) {
        public static SubscriptionItem from(TutorSubscription subscription) {
            return new SubscriptionItem(
                    subscription.getId(),
                    subscription.getTutorUserId(),
                    subscription.getStatus(),
                    subscription.getCurrentTariffCode(),
                    subscription.getActiveUntil()
            );
        }
    }

    public record PaymentItem(UUID id, UUID tutorUserId, String tariffCode, int amountRub, PaymentStatus status, Instant createdAt) {
        public static PaymentItem from(Payment payment) {
            return new PaymentItem(
                    payment.getId(),
                    payment.getTutorUserId(),
                    payment.getTariffCode(),
                    payment.getAmountRub(),
                    payment.getStatus(),
                    payment.getCreatedAt()
            );
        }
    }

    public record PeriodItem(UUID id, UUID subscriptionId, UUID paymentId, String tariffCode, Instant startedAt, Instant activeUntil) {
        public static PeriodItem from(SubscriptionPeriod period) {
            return new PeriodItem(
                    period.getId(),
                    period.getSubscriptionId(),
                    period.getPaymentId(),
                    period.getTariffCode(),
                    period.getStartedAt(),
                    period.getActiveUntil()
            );
        }
    }
}
