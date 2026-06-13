package com.tutor.authservice.exception;

import com.tutor.authservice.domain.Role;

public class InvalidRoleRegistrationException extends RuntimeException {
    public InvalidRoleRegistrationException(Role role) {
        super("Public registration is not allowed for role: " + role);
    }
}
