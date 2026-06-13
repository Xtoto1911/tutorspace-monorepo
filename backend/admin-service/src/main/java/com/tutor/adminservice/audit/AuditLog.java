package com.tutor.adminservice.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID adminUserId;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
