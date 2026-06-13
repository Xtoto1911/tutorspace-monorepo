package com.tutor.authservice.domain;

public enum Role {
    ROLE_CLIENT,
    ROLE_TUTOR,
    ROLE_ADMIN,
    ROLE_MODERATOR;

    public boolean canRegisterPublicly() {
        return this == ROLE_CLIENT || this == ROLE_TUTOR;
    }
}
