package com.tutor.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCredentialsDto {

    @NotBlank(message = "Укажите email.")
    @Email(message = "Некорректный формат email.")
    private String email;

    @NotBlank(message = "Укажите пароль.")
    private String password;
}
