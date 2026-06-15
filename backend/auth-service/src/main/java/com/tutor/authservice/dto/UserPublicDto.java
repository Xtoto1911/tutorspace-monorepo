package com.tutor.authservice.dto;

import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.domain.UserStatus;

import java.util.UUID;

public record UserPublicDto(
        UUID id,
        String email,
        String name,
        Role role,
        UserStatus status
) {
    public static UserPublicDto from(User user) {
        return new UserPublicDto(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getStatus());
    }
}
