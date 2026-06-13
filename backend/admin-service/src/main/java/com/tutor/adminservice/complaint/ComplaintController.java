package com.tutor.adminservice.complaint;

import com.tutor.adminservice.audit.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/admin/complaints", "/api/moderator/complaints"})
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERATOR')")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final AuditLogService auditLogService;

    @GetMapping
    public List<ComplaintDto> list(@RequestParam(required = false) ComplaintStatus status) {
        var complaints = status != null
                ? complaintService.listByStatus(status)
                : complaintService.listAll();
        return complaints.stream().map(ComplaintDto::from).toList();
    }

    @GetMapping("/{id}")
    public ComplaintDto get(@PathVariable UUID id) {
        return ComplaintDto.from(complaintService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintDto create(@Valid @RequestBody CreateComplaintRequest request,
                               @AuthenticationPrincipal UUID reporterUserId) {
        return ComplaintDto.from(complaintService.create(request, reporterUserId));
    }

    @PatchMapping("/{id}/moderate")
    public ComplaintDto moderate(@PathVariable UUID id,
                                 @Valid @RequestBody ModerateComplaintRequest request,
                                 @AuthenticationPrincipal UUID moderatorId) {
        var result = complaintService.moderate(id, request, moderatorId);
        auditLogService.log(
                moderatorId,
                request.status() == ComplaintStatus.APPROVED ? "COMPLAINT_APPROVED" : "COMPLAINT_REJECTED",
                "COMPLAINT",
                id,
                request.resolutionNotes()
        );
        return ComplaintDto.from(result);
    }
}
