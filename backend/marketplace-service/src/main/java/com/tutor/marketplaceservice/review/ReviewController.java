package com.tutor.marketplaceservice.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.tutor.marketplaceservice.security.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/marketplace/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public List<ReviewDto> listPublished(@RequestParam(required = false) UUID tutorProfileId) {
        return reviewService.listPublished(tutorProfileId);
    }

    @GetMapping("/eligibility")
    public ReviewEligibilityDto getEligibility(@RequestParam UUID tutorProfileId,
                                               @AuthenticationPrincipal JwtPrincipal principal) {
        return reviewService.getEligibility(tutorProfileId, principal.userId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto create(@Valid @RequestBody CreateReviewRequest request,
                            @AuthenticationPrincipal JwtPrincipal principal) {
        return reviewService.create(request, principal.userId());
    }

    @PostMapping("/{reviewId}/complaints")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewComplaintDto createComplaint(@PathVariable UUID reviewId,
                                              @Valid @RequestBody CreateReviewComplaintRequest request,
                                              @AuthenticationPrincipal JwtPrincipal principal) {
        return reviewService.createComplaint(reviewId, principal.userId(), request);
    }
}
