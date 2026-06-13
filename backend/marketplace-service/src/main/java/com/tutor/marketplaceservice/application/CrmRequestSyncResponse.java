package com.tutor.marketplaceservice.application;

import java.util.UUID;

public record CrmRequestSyncResponse(
        UUID requestId,
        UUID applicationId
) {
}
