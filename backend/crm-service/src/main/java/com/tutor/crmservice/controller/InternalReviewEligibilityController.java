package com.tutor.crmservice.controller;

import com.tutor.crmservice.lesson.ReviewEligibilityDto;
import com.tutor.crmservice.lesson.ReviewEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/review-eligibility")
public class InternalReviewEligibilityController {

    private final ReviewEligibilityService reviewEligibilityService;

    @GetMapping
    public ReviewEligibilityDto getEligibility(@RequestParam UUID clientUserId,
                                               @RequestParam UUID tutorUserId) {
        return reviewEligibilityService.getEligibility(clientUserId, tutorUserId);
    }
}
