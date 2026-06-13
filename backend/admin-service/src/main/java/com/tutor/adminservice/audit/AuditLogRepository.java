package com.tutor.adminservice.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTargetTypeOrderByCreatedAtDesc(String targetType);
    List<AuditLog> findByAdminUserIdOrderByCreatedAtDesc(UUID adminUserId);
}
