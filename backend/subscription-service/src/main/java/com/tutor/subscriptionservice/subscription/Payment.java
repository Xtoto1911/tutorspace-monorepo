package com.tutor.subscriptionservice.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tutorUserId;

    @Column(nullable = false)
    private String tariffCode;

    @Column(nullable = false)
    private int amountRub;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "RUB";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private String checkoutUrl;

    @Column(unique = true)
    private String providerPaymentId;

    private String providerStatus;

    @Builder.Default
    @Column(nullable = false)
    private boolean test = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean paid = false;

    @Lob
    @Column(length = 8192)
    private String webhookPayload;

    private String errorReason;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant confirmedAt;
}
