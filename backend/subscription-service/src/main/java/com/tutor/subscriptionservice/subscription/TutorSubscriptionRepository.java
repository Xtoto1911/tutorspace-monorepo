package com.tutor.subscriptionservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TutorSubscriptionRepository extends JpaRepository<TutorSubscription, UUID> {
    Optional<TutorSubscription> findByTutorUserId(UUID tutorUserId);
}
