package com.tutor.authservice.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("Invalid or expired password reset token");
    }
}
