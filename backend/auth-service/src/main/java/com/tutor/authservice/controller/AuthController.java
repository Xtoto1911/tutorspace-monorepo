package com.tutor.authservice.controller;

import com.tutor.authservice.dto.JwtAuthenticationDto;
import com.tutor.authservice.dto.PasswordResetConfirmDto;
import com.tutor.authservice.dto.PasswordResetRequestDto;
import com.tutor.authservice.dto.PasswordResetResponseDto;
import com.tutor.authservice.dto.RefreshTokenDto;
import com.tutor.authservice.dto.RegisterRequestDto;
import com.tutor.authservice.dto.UserCredentialsDto;
import com.tutor.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<JwtAuthenticationDto> register(@Valid @RequestBody RegisterRequestDto dto) {
        JwtAuthenticationDto response = userService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationDto> login(@Valid @RequestBody UserCredentialsDto dto){
        JwtAuthenticationDto response = userService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationDto> refresh(@Valid @RequestBody RefreshTokenDto dto) {
        JwtAuthenticationDto response = userService.refreshToken(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenDto dto) {
        userService.logout(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset")
    public ResponseEntity<PasswordResetResponseDto> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto dto
    ) {
        return ResponseEntity.ok(userService.requestPasswordReset(dto));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<PasswordResetResponseDto> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDto dto
    ) {
        return ResponseEntity.ok(userService.confirmPasswordReset(dto));
    }
}
