package com.tutor.marketplaceservice.tutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/marketplace/tutors")
public class InternalTutorProfileSyncController {

    private final TutorProfileService tutorProfileService;
    private final String internalServiceToken;

    public InternalTutorProfileSyncController(
            TutorProfileService tutorProfileService,
            @Value("${app.internal-service-token:}") String internalServiceToken
    ) {
        this.tutorProfileService = tutorProfileService;
        this.internalServiceToken = internalServiceToken;
    }

    @GetMapping
    public List<TutorProfileDto> listByStatus(
            @RequestParam(defaultValue = "ACTIVE") TutorProfileStatus status,
            @RequestHeader(name = "X-Internal-Token", required = false) String internalToken
    ) {
        validateInternalToken(internalToken);
        return tutorProfileService.listByStatus(status);
    }

    @PostMapping("/profile-sync")
    public TutorProfileDto syncProfile(@RequestBody SyncTutorProfileRequest request) {
        return tutorProfileService.syncFromCrmSettings(request);
    }

    @PatchMapping("/{idOrSlug}/hide")
    public void hideProfile(@PathVariable String idOrSlug) {
        tutorProfileService.hideProfile(idOrSlug);
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request,
            @RequestHeader(name = "X-Internal-Token", required = false) String internalToken
    ) {
        validateInternalToken(internalToken);
        tutorProfileService.updateStatus(id, request.status());
    }

    private void validateInternalToken(String internalToken) {
        if (internalServiceToken == null || internalServiceToken.isBlank() || internalToken == null) {
            throwInvalidInternalToken();
        }

        byte[] expectedToken = internalServiceToken.getBytes(StandardCharsets.UTF_8);
        byte[] actualToken = internalToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedToken, actualToken)) {
            throwInvalidInternalToken();
        }
    }

    private void throwInvalidInternalToken() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
    }

    public record UpdateStatusRequest(TutorProfileStatus status) {}
}
