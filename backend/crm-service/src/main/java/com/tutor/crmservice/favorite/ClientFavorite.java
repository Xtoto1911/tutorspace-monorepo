package com.tutor.crmservice.favorite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "client_favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"client_user_id", "tutor_profile_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class ClientFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_user_id", nullable = false)
    private UUID clientUserId;

    @Column(name = "tutor_profile_id", nullable = false)
    private UUID tutorProfileId;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
