package com.tutor.authservice.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("Ссылка для сброса пароля устарела или недействительна.");
    }
}
