package com.tutor.marketplaceservice.application;

import java.util.UUID;

public record TutorApplicationResponse(
        UUID requestId,
        UUID applicationId
) {
}
