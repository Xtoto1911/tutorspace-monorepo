package com.tutor.crmservice.request;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmApplicationRepository extends JpaRepository<CrmApplication, UUID> {
    Optional<CrmApplication> findByMarketplaceRequestId(UUID marketplaceRequestId);
    List<CrmApplication> findByClientUserIdOrderByCreatedAtDesc(UUID clientUserId);
    List<CrmApplication> findByTutorUserIdOrderByCreatedAtDesc(UUID tutorUserId);
    Optional<CrmApplication> findByIdAndClientUserId(UUID id, UUID clientUserId);
    Optional<CrmApplication> findByIdAndTutorUserId(UUID id, UUID tutorUserId);
    boolean existsByClientUserIdAndTutorProfileIdAndStatusIn(UUID clientUserId,
                                                             UUID tutorProfileId,
                                                             Collection<ApplicationStatus> statuses);
}
