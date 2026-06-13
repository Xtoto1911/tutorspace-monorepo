package com.tutor.marketplaceservice.subject;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findBySlug(String slug);
    Optional<Subject> findByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
}
