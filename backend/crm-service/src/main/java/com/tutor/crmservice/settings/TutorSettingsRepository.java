package com.tutor.crmservice.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TutorSettingsRepository extends JpaRepository<TutorSettings, UUID> {
    Optional<TutorSettings> findByUserId(UUID userId);
}
