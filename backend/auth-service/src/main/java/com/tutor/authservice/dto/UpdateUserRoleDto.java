package com.tutor.authservice.dto;

import com.tutor.authservice.domain.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleDto {

    @NotNull(message = "Role is required")
    private Role role;
}
