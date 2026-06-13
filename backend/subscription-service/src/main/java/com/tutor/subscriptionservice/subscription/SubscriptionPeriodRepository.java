package com.tutor.subscriptionservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPeriodRepository extends JpaRepository<SubscriptionPeriod, UUID> {
    Optional<SubscriptionPeriod> findByPaymentId(UUID paymentId);
}
