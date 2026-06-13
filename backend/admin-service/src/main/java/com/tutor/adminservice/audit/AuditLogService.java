package com.tutor.adminservice.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog log(UUID adminUserId, String actionType, String targetType, UUID targetId, String details) {
        var log = AuditLog.builder()
                .adminUserId(adminUserId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();
        return auditLogRepository.save(log);
    }

    public List<AuditLog> listAll() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> listByTargetType(String targetType) {
        return auditLogRepository.findByTargetTypeOrderByCreatedAtDesc(targetType);
    }
}
