package com.tutor.marketplaceservice.tutor;

import com.tutor.marketplaceservice.application.CreateApplicationRequest;
import com.tutor.marketplaceservice.application.TutorApplicationResponse;
import com.tutor.marketplaceservice.application.TutorApplicationService;
import com.tutor.marketplaceservice.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/marketplace/tutors")
public class TutorProfileController {

    private final TutorProfileService tutorProfileService;
    private final TutorApplicationService tutorApplicationService;

    @GetMapping
    public List<TutorProfileDto> search(@RequestParam(required = false) String subject,
                                        @RequestParam(required = false) Integer minExperienceYears,
                                        @RequestParam(required = false) BigDecimal maxPricePerHour) {
        return tutorProfileService.search(subject, minExperienceYears, maxPricePerHour);
    }

    @GetMapping("/{idOrSlug}")
    public TutorProfileDto getProfile(@PathVariable String idOrSlug) {
        return tutorProfileService.getProfileByIdOrSlug(idOrSlug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TutorProfileDto create(@Valid @RequestBody CreateTutorProfileRequest request) {
        return tutorProfileService.create(request);
    }

    @PostMapping("/{tutorId}/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TutorApplicationResponse createRequest(@PathVariable UUID tutorId,
                                                  @Valid @RequestBody CreateApplicationRequest request) {
        return tutorApplicationService.createRequest(tutorId, request);
    }

    @PostMapping("/{idOrSlug}/complaints")
    @ResponseStatus(HttpStatus.CREATED)
    public TutorProfileComplaintDto createComplaint(@PathVariable String idOrSlug,
                                                    @Valid @RequestBody CreateTutorProfileComplaintRequest request,
                                                    @AuthenticationPrincipal JwtPrincipal principal) {
        return tutorProfileService.createComplaint(idOrSlug, principal.userId(), request);
    }
}
