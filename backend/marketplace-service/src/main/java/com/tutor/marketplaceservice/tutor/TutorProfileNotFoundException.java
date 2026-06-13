package com.tutor.marketplaceservice.tutor;

public class TutorProfileNotFoundException extends RuntimeException {
    public TutorProfileNotFoundException(String identifier) {
        super("Tutor profile not found: " + identifier);
    }
}
