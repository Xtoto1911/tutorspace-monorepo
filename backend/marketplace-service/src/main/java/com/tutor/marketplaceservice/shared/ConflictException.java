package com.tutor.marketplaceservice.shared;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
