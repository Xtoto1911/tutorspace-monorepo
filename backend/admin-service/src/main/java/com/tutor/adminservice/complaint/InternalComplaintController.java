package com.tutor.adminservice.complaint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/complaints")
public class InternalComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintDto create(@Valid @RequestBody InternalCreateComplaintRequest request) {
        CreateComplaintRequest complaintRequest = new CreateComplaintRequest(
                request.reportedUserId(),
                request.category(),
                request.reason(),
                request.description(),
                request.priority(),
                request.targetType(),
                request.targetId(),
                request.targetSnapshot()
        );
        return ComplaintDto.from(complaintService.create(complaintRequest, request.reporterUserId()));
    }

    public record InternalCreateComplaintRequest(
            UUID reportedUserId,
            UUID reporterUserId,
            ComplaintCategory category,
            String reason,
            String description,
            ComplaintPriority priority,
            ComplaintTargetType targetType,
            UUID targetId,
            String targetSnapshot
    ) {
    }
}
