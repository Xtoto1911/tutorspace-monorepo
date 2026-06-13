package com.tutor.subscriptionservice.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_periods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false)
    private String tariffCode;

    @Column(nullable = false, unique = true)
    private UUID paymentId;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant activeUntil;
}
