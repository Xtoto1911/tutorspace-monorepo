package com.tutor.crmservice.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientSettingsRepository extends JpaRepository<ClientSettings, UUID> {
    Optional<ClientSettings> findByUserId(UUID userId);
}
