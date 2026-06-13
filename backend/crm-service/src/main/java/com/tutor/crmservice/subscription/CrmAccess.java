package com.tutor.crmservice.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tutorUserId;

    @Column(nullable = false)
    private String tariffCode;

    @Column(nullable = false)
    private Instant activeUntil;

    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTutorUserId() {
        return tutorUserId;
    }

    public void setTutorUserId(UUID tutorUserId) {
        this.tutorUserId = tutorUserId;
    }

    public String getTariffCode() {
        return tariffCode;
    }

    public void setTariffCode(String tariffCode) {
        this.tariffCode = tariffCode;
    }

    public Instant getActiveUntil() {
        return activeUntil;
    }

    public void setActiveUntil(Instant activeUntil) {
        this.activeUntil = activeUntil;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
