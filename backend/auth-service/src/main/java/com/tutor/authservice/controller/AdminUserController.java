package com.tutor.authservice.controller;

import com.tutor.authservice.dto.CreateModeratorRequestDto;
import com.tutor.authservice.dto.UpdateUserRoleDto;
import com.tutor.authservice.dto.UpdateUserStatusDto;
import com.tutor.authservice.dto.UserAdminDto;
import com.tutor.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserAdminDto> listUsers() {
        return userService.listUsersForAdmin().stream()
                .map(UserAdminDto::from)
                .toList();
    }

    @PostMapping("/moderators")
    public UserAdminDto createModerator(@Valid @RequestBody CreateModeratorRequestDto dto) {
        return UserAdminDto.from(userService.createModeratorAccount(dto.getEmail(), dto.getPassword(), dto.getName()));
    }

    @GetMapping("/{userId}")
    public UserAdminDto getUser(@PathVariable UUID userId) {
        return UserAdminDto.from(userService.getUserById(userId));
    }

    @PatchMapping("/{userId}/role")
    public UserAdminDto updateRole(@PathVariable UUID userId,
                                   @Valid @RequestBody UpdateUserRoleDto dto) {
        return UserAdminDto.from(userService.updateRole(userId, dto.getRole()));
    }

    @PatchMapping("/{userId}/status")
    public UserAdminDto updateStatus(@PathVariable UUID userId,
                                     @Valid @RequestBody UpdateUserStatusDto dto) {
        return UserAdminDto.from(userService.updateStatus(userId, dto.getStatus()));
    }
}
