package com.tutor.crmservice.integration;

import java.util.UUID;

public record MarketplaceRequestIngestResponse(
        UUID requestId,
        UUID applicationId
) {
}
