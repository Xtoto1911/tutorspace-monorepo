package com.tutor.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequestDto {

    @NotBlank(message = "Укажите email.")
    @Email(message = "Некорректный формат email.")
    private String email;
}
