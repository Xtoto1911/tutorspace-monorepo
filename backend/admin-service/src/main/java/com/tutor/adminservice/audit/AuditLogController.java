package com.tutor.adminservice.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-log")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public List<AuditLogDto> list(@RequestParam(required = false) String targetType) {
        var logs = targetType != null
                ? auditLogService.listByTargetType(targetType)
                : auditLogService.listAll();
        return logs.stream().map(AuditLogDto::from).toList();
    }
}
