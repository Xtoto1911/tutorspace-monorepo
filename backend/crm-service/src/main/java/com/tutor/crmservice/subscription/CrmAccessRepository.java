package com.tutor.crmservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CrmAccessRepository extends JpaRepository<CrmAccess, UUID> {
    Optional<CrmAccess> findByTutorUserId(UUID tutorUserId);
}
