package com.tutor.crmservice.integration;

import com.tutor.crmservice.application.ApplicationLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/marketplace")
public class MarketplaceRequestIngestController {

    private final ApplicationLifecycleService applicationService;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MarketplaceRequestIngestResponse ingestRequest(@Valid @RequestBody MarketplaceRequestCreatedDto request) {
        return new MarketplaceRequestIngestResponse(
                request.requestId(),
                applicationService.ingestMarketplaceRequest(request)
        );
    }
}
