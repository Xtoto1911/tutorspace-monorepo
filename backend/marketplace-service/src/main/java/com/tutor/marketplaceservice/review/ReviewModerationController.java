package com.tutor.marketplaceservice.review;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/reviews")
public class ReviewModerationController {

    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;

    @GetMapping
    public List<ReviewDto> listByStatus(@RequestParam ReviewStatus status) {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(ReviewDto::from).toList();
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        reviewService.updateStatus(id, request.status());
    }

    @PostMapping("/publication-restrictions")
    public void restrictPublication(@RequestBody RestrictPublicationRequest request) {
        reviewService.restrictPublication(request.userId(), request.moderatorId(), request.reason());
    }

    public record UpdateStatusRequest(ReviewStatus status) {}

    public record RestrictPublicationRequest(UUID userId, UUID moderatorId, String reason) {}
}
