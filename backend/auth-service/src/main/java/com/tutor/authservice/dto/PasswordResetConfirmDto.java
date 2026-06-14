package com.tutor.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmDto {

    @NotBlank(message = "Укажите токен сброса пароля.")
    private String token;

    @NotBlank(message = "Укажите пароль.")
    @Size(min = 8, message = "Пароль должен содержать минимум 8 символов.")
    private String password;
}
