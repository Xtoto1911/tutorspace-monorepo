package com.tutor.marketplaceservice.subject;

import java.util.UUID;

public record SubjectDto(UUID id, String name, String slug, String description) {
    public static SubjectDto from(Subject subject) {
        return new SubjectDto(subject.getId(), subject.getName(), subject.getSlug(), subject.getDescription());
    }
}
