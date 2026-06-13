package com.tutor.authservice.dto;

import com.tutor.authservice.domain.Role;
import com.tutor.authservice.domain.User;
import com.tutor.authservice.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserAdminDto {
    private UUID id;
    private String email;
    private String name;
    private Role role;
    private UserStatus status;

    public static UserAdminDto from(User user) {
        return new UserAdminDto(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getStatus());
    }
}
