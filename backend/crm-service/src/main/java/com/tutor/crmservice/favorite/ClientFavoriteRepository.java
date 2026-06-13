package com.tutor.crmservice.favorite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientFavoriteRepository extends JpaRepository<ClientFavorite, UUID> {
    List<ClientFavorite> findByClientUserIdOrderByCreatedAtDesc(UUID clientUserId);

    Optional<ClientFavorite> findByClientUserIdAndTutorProfileId(UUID clientUserId, UUID tutorProfileId);
}
