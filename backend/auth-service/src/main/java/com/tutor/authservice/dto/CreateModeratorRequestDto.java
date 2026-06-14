package com.tutor.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateModeratorRequestDto {

    @NotBlank(message = "Укажите email.")
    @Email(message = "Некорректный формат email.")
    private String email;

    @NotBlank(message = "Укажите пароль.")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов.")
    private String password;

    @Size(max = 255, message = "Имя не должно превышать 255 символов.")
    private String name;
}
