package com.tutor.authservice.exceptionHandler;

public record ApiError(String message, int status) {
}
