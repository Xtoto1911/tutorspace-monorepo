package com.tutor.authservice.dto;

import com.tutor.authservice.domain.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusDto {

    @NotNull(message = "Status is required")
    private UserStatus status;
}
