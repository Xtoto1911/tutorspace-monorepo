package com.tutor.marketplaceservice.subject;

import java.util.UUID;

public class SubjectNotFoundException extends RuntimeException {
    public SubjectNotFoundException(UUID subjectId) {
        super("Subject not found: " + subjectId);
    }
}
