package com.tutor.subscriptionservice.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tutor_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tutorUserId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(nullable = false)
    private Instant activeUntil;

    @Column(nullable = false)
    private String currentTariffCode;

    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
