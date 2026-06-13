package com.tutor.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(@Value("${app.auth-service-url}") String authServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }

    public List<AdminUserDto> listUsers(String adminToken) {
        return restClient.get()
                .uri("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public AdminUserDto createModerator(String email, String password, String adminToken) {
        var created = restClient.post()
                .uri("/api/admin/users/moderators")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .body(new CreateModeratorRequest(email, password))
                .retrieve()
                .body(AdminUserDto.class);
        log.info("Created moderator account {}", email);
        return created;
    }

    public void updateUserRole(UUID userId, String role, String adminToken) {
        restClient.patch()
                .uri("/api/admin/users/{userId}/role", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .body(new UpdateRoleRequest(role))
                .retrieve()
                .toBodilessEntity();
        log.info("Updated user {} role to {}", userId, role);
    }

    public void updateUserStatus(UUID userId, String status, String adminToken) {
        restClient.patch()
                .uri("/api/admin/users/{userId}/status", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .body(new UpdateStatusRequest(status))
                .retrieve()
                .toBodilessEntity();
        log.info("Updated user {} status to {}", userId, status);
    }

    public record AdminUserDto(UUID id, String email, String role, String status) {}

    public record CreateModeratorRequest(String email, String password) {}

    public record UpdateRoleRequest(String role) {}

    public record UpdateStatusRequest(String status) {}
}
