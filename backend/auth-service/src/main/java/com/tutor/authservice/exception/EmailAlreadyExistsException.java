package com.tutor.authservice.exception;

public class EmailAlreadyExistsException extends RuntimeException{
    public EmailAlreadyExistsException(String email){
        super("Этот email уже зарегистрирован.");
    }
}
