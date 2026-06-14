package com.tutor.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDto {

    @NotBlank(message = "Укажите refresh-токен.")
    private String refreshToken;
}
