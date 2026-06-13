package com.tutor.crmservice.service;

import java.time.LocalDateTime;

public record RescheduleLessonRequest(
        LocalDateTime startsAt,
        Scope scope
) {
    public enum Scope {
        SINGLE,
        SERIES
    }
}
