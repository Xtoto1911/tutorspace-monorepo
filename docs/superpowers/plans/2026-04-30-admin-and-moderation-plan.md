# Admin And Moderation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create admin-service with complaints, review/certificate moderation, audit log, and user blocking integration.

**Architecture:** New Spring Boot microservice (admin-service) with H2 database. Communicates with auth-service (user blocking) and marketplace-service (review/certificate moderation) via RestClient. Stores complaints and audit log locally.

**Tech Stack:** Spring Boot 3.5.10, Java 21, H2, JPA, Maven, RestClient, JWT, Docker Compose.

---

## File Structure

**New Files (admin-service):**
- `backend/admin-service/pom.xml` — Maven project config
- `backend/admin-service/src/main/java/com/tutor/adminservice/AdminServiceApplication.java` — Main class
- `backend/admin-service/src/main/resources/application.yaml` — App config
- `backend/admin-service/src/main/java/com/tutor/adminservice/config/SecurityConfig.java` — JWT security
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/Complaint.java` — Entity
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintCategory.java` — Enum
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintPriority.java` — Enum
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintStatus.java` — Enum
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintRepository.java` — Repository
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintService.java` — Service
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintController.java` — Controller
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/CreateComplaintRequest.java` — DTO
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ModerateComplaintRequest.java` — DTO
- `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintDto.java` — DTO
- `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLog.java` — Entity
- `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogRepository.java` — Repository
- `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogService.java` — Service
- `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogController.java` — Controller
- `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogDto.java` — DTO
- `backend/admin-service/src/main/java/com/tutor/adminservice/client/AuthServiceClient.java` — RestClient
- `backend/admin-service/src/main/java/com/tutor/adminservice/client/MarketplaceServiceClient.java` — RestClient
- `backend/admin-service/src/main/java/com/tutor/adminservice/moderation/ModerationController.java` — Controller
- `backend/admin-service/src/test/java/com/tutor/adminservice/complaint/ComplaintServiceTest.java` — Unit test

**Modified Files:**
- `docker-compose.yml` — Add admin-service
- `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewService.java` — Change status to PENDING
- `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewModerationController.java` — New internal controller
- `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/certificate/CertificateModerationController.java` — New internal controller

---

## Task 1: Scaffold admin-service project

**Files:**
- Create: `backend/admin-service/pom.xml`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/AdminServiceApplication.java`
- Create: `backend/admin-service/src/main/resources/application.yaml`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.10</version>
        <relativePath/>
    </parent>

    <groupId>com.tutor</groupId>
    <artifactId>admin-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>admin-service</name>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create AdminServiceApplication.java**

```java
package com.tutor.adminservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yaml**

```yaml
server:
  port: ${SERVER_PORT:8084}

spring:
  application:
    name: admin-service
  datasource:
    url: ${ADMIN_DB_URL:jdbc:h2:mem:admindb}
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect

jwt:
  secret: ${JWT_SECRET}

app:
  auth-service-url: ${AUTH_SERVICE_URL:http://auth-service:8080}
  marketplace-service-url: ${MARKETPLACE_SERVICE_URL:http://marketplace-service:8081}
```

- [ ] **Step 4: Verify build**

Run: `cd backend/admin-service && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/admin-service/pom.xml backend/admin-service/src/main/java/com/tutor/adminservice/AdminServiceApplication.java backend/admin-service/src/main/resources/application.yaml
git commit -m "feat(admin-service): scaffold project"
```

---

## Task 2: Add JWT security config to admin-service

**Files:**
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/config/SecurityConfig.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/security/JwtFilter.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/security/JwtService.java`

- [ ] **Step 1: Create JwtService.java**

```java
package com.tutor.adminservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public UUID extractUserId(String token) {
        var claims = Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public String extractRole(String token) {
        var claims = Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
        return claims.get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Create JwtFilter.java**

```java
package com.tutor.adminservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            var token = header.substring(7);
            if (jwtService.isTokenValid(token)) {
                var role = jwtService.extractRole(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        jwtService.extractUserId(token), null,
                        List.of(new SimpleGrantedAuthority(role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Create SecurityConfig.java**

```java
package com.tutor.adminservice.config;

import com.tutor.adminservice.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/h2-console/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
```

- [ ] **Step 4: Add jjwt dependency to pom.xml**

Add to `backend/admin-service/pom.xml`:
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 5: Verify build**

Run: `cd backend/admin-service && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/admin-service/pom.xml backend/admin-service/src/main/java/com/tutor/adminservice/config/ backend/admin-service/src/main/java/com/tutor/adminservice/security/
git commit -m "feat(admin-service): add JWT security config"
```

---

## Task 3: Create Complaint entity and enums

**Files:**
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/Complaint.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintCategory.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintPriority.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintStatus.java`

- [ ] **Step 1: Create enums**

```java
// ComplaintCategory.java
package com.tutor.adminservice.complaint;

public enum ComplaintCategory {
    SPAM, HARASSMENT, FAKE_PROFILE, INAPPROPRIATE_CONTENT, OTHER
}
```

```java
// ComplaintPriority.java
package com.tutor.adminservice.complaint;

public enum ComplaintPriority {
    LOW, MEDIUM, HIGH
}
```

```java
// ComplaintStatus.java
package com.tutor.adminservice.complaint;

public enum ComplaintStatus {
    PENDING, APPROVED, REJECTED
}
```

- [ ] **Step 2: Create Complaint entity**

```java
package com.tutor.adminservice.complaint;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID reportedUserId;

    @Column(nullable = false)
    private UUID reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory category;

    @Column(nullable = false)
    private String reason;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant moderatedAt;

    private UUID moderatorId;

    @Column(length = 1000)
    private String resolutionNotes;
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/admin-service/src/main/java/com/tutor/adminservice/complaint/
git commit -m "feat(admin-service): add Complaint entity and enums"
```

---

## Task 4: Create Complaint repository, service, DTOs, controller

**Files:**
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintRepository.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintService.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintController.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/CreateComplaintRequest.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ModerateComplaintRequest.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintDto.java`

- [ ] **Step 1: Create ComplaintRepository.java**

```java
package com.tutor.adminservice.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);
    List<Complaint> findByPriorityOrderByCreatedAtDesc(ComplaintPriority priority);
    List<Complaint> findByStatusAndPriorityOrderByCreatedAtDesc(ComplaintStatus status, ComplaintPriority priority);
}
```

- [ ] **Step 2: Create DTOs**

```java
// CreateComplaintRequest.java
package com.tutor.adminservice.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateComplaintRequest(
    @NotNull UUID reportedUserId,
    @NotNull ComplaintCategory category,
    @NotBlank String reason,
    String description,
    ComplaintPriority priority
) {}
```

```java
// ModerateComplaintRequest.java
package com.tutor.adminservice.complaint;

import jakarta.validation.constraints.NotNull;

public record ModerateComplaintRequest(
    @NotNull ComplaintStatus status,
    String resolutionNotes
) {}
```

```java
// ComplaintDto.java
package com.tutor.adminservice.complaint;

import java.time.Instant;
import java.util.UUID;

public record ComplaintDto(
    UUID id,
    UUID reportedUserId,
    UUID reporterUserId,
    ComplaintCategory category,
    String reason,
    String description,
    ComplaintPriority priority,
    ComplaintStatus status,
    Instant createdAt,
    Instant moderatedAt,
    UUID moderatorId,
    String resolutionNotes
) {
    public static ComplaintDto from(Complaint c) {
        return new ComplaintDto(
            c.getId(), c.getReportedUserId(), c.getReporterUserId(),
            c.getCategory(), c.getReason(), c.getDescription(),
            c.getPriority(), c.getStatus(), c.getCreatedAt(),
            c.getModeratedAt(), c.getModeratorId(), c.getResolutionNotes()
        );
    }
}
```

- [ ] **Step 3: Create ComplaintService.java**

```java
package com.tutor.adminservice.complaint;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;

    public Complaint create(CreateComplaintRequest request, UUID reporterUserId) {
        var complaint = Complaint.builder()
                .reportedUserId(request.reportedUserId())
                .reporterUserId(reporterUserId)
                .category(request.category())
                .reason(request.reason())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : ComplaintPriority.MEDIUM)
                .build();
        return complaintRepository.save(complaint);
    }

    public Complaint moderate(UUID id, ModerateComplaintRequest request, UUID moderatorId) {
        var complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        complaint.setStatus(request.status());
        complaint.setResolutionNotes(request.resolutionNotes());
        complaint.setModeratorId(moderatorId);
        complaint.setModeratedAt(Instant.now());
        return complaintRepository.save(complaint);
    }

    public List<Complaint> listAll() {
        return complaintRepository.findAll();
    }

    public List<Complaint> listByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Complaint getById(UUID id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
    }
}
```

- [ ] **Step 4: Create ComplaintController.java**

```java
package com.tutor.adminservice.complaint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/complaints")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public List<ComplaintDto> list(@RequestParam(required = false) ComplaintStatus status) {
        var complaints = status != null
                ? complaintService.listByStatus(status)
                : complaintService.listAll();
        return complaints.stream().map(ComplaintDto::from).toList();
    }

    @GetMapping("/{id}")
    public ComplaintDto get(@PathVariable UUID id) {
        return ComplaintDto.from(complaintService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintDto create(@Valid @RequestBody CreateComplaintRequest request,
                               @AuthenticationPrincipal UUID reporterUserId) {
        return ComplaintDto.from(complaintService.create(request, reporterUserId));
    }

    @PatchMapping("/{id}/moderate")
    public ComplaintDto moderate(@PathVariable UUID id,
                                 @Valid @RequestBody ModerateComplaintRequest request,
                                 @AuthenticationPrincipal UUID moderatorId) {
        return ComplaintDto.from(complaintService.moderate(id, request, moderatorId));
    }
}
```

- [ ] **Step 5: Verify build**

Run: `cd backend/admin-service && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/admin-service/src/main/java/com/tutor/adminservice/complaint/
git commit -m "feat(admin-service): add Complaint repository, service, controller, DTOs"
```

---

## Task 5: Create AuditLog entity, repository, service, controller

**Files:**
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLog.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogRepository.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogService.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogController.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/audit/AuditLogDto.java`

- [ ] **Step 1: Create AuditLog entity**

```java
package com.tutor.adminservice.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID adminUserId;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 2: Create AuditLogRepository.java**

```java
package com.tutor.adminservice.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTargetTypeOrderByCreatedAtDesc(String targetType);
    List<AuditLog> findByAdminUserIdOrderByCreatedAtDesc(UUID adminUserId);
}
```

- [ ] **Step 3: Create AuditLogDto.java**

```java
package com.tutor.adminservice.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
    UUID id,
    UUID adminUserId,
    String actionType,
    String targetType,
    UUID targetId,
    String details,
    Instant createdAt
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
            log.getId(), log.getAdminUserId(), log.getActionType(),
            log.getTargetType(), log.getTargetId(), log.getDetails(),
            log.getCreatedAt()
        );
    }
}
```

- [ ] **Step 4: Create AuditLogService.java**

```java
package com.tutor.adminservice.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog log(UUID adminUserId, String actionType, String targetType, UUID targetId, String details) {
        var log = AuditLog.builder()
                .adminUserId(adminUserId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();
        return auditLogRepository.save(log);
    }

    public List<AuditLog> listAll() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> listByTargetType(String targetType) {
        return auditLogRepository.findByTargetTypeOrderByCreatedAtDesc(targetType);
    }
}
```

- [ ] **Step 5: Create AuditLogController.java**

```java
package com.tutor.adminservice.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-log")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public List<AuditLogDto> list(@RequestParam(required = false) String targetType) {
        var logs = targetType != null
                ? auditLogService.listByTargetType(targetType)
                : auditLogService.listAll();
        return logs.stream().map(AuditLogDto::from).toList();
    }
}
```

- [ ] **Step 6: Verify build**

Run: `cd backend/admin-service && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/admin-service/src/main/java/com/tutor/adminservice/audit/
git commit -m "feat(admin-service): add AuditLog entity, repository, service, controller"
```

---

## Task 6: Create service clients (auth-service and marketplace-service)

**Files:**
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/client/AuthServiceClient.java`
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/client/MarketplaceServiceClient.java`

- [ ] **Step 1: Create AuthServiceClient.java**

```java
package com.tutor.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public void updateUserStatus(UUID userId, String status, String adminToken) {
        try {
            restClient.patch()
                    .uri("/api/admin/users/{userId}/status", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .body(new UpdateStatusRequest(status))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Updated user {} status to {}", userId, status);
        } catch (Exception e) {
            log.error("Failed to update user {} status: {}", userId, e.getMessage());
        }
    }

    public record UpdateStatusRequest(String status) {}
}
```

- [ ] **Step 2: Create MarketplaceServiceClient.java**

```java
package com.tutor.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class MarketplaceServiceClient {

    private final RestClient restClient;

    public MarketplaceServiceClient(@Value("${app.marketplace-service-url}") String marketplaceServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(marketplaceServiceUrl)
                .build();
    }

    public void updateReviewStatus(UUID reviewId, String status) {
        try {
            restClient.patch()
                    .uri("/internal/reviews/{reviewId}/status", reviewId)
                    .body(new UpdateStatusRequest(status))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Updated review {} status to {}", reviewId, status);
        } catch (Exception e) {
            log.error("Failed to update review {} status: {}", reviewId, e.getMessage());
        }
    }

    public void updateCertificateStatus(UUID certificateId, String status) {
        try {
            restClient.patch()
                    .uri("/internal/certificates/{certificateId}/status", certificateId)
                    .body(new UpdateStatusRequest(status))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Updated certificate {} status to {}", certificateId, status);
        } catch (Exception e) {
            log.error("Failed to update certificate {} status: {}", certificateId, e.getMessage());
        }
    }

    public record UpdateStatusRequest(String status) {}
}
```

- [ ] **Step 3: Verify build**

Run: `cd backend/admin-service && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/admin-service/src/main/java/com/tutor/adminservice/client/
git commit -m "feat(admin-service): add auth and marketplace service clients"
```

---

## Task 7: Create ModerationController and User Management endpoints

**Files:**
- Create: `backend/admin-service/src/main/java/com/tutor/adminservice/moderation/ModerationController.java`

- [ ] **Step 1: Create ModerationController.java**

```java
package com.tutor.adminservice.moderation;

import com.tutor.adminservice.audit.AuditLogService;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ModerationController {

    private final MarketplaceServiceClient marketplaceClient;
    private final AuthServiceClient authClient;
    private final AuditLogService auditLogService;

    @PostMapping("/moderation/reviews/{id}/approve")
    public void approveReview(@PathVariable UUID id,
                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                              @AuthenticationPrincipal UUID adminUserId) {
        marketplaceClient.updateReviewStatus(id, "PUBLISHED");
        auditLogService.log(adminUserId, "REVIEW_APPROVED", "REVIEW", id, null);
    }

    @PostMapping("/moderation/reviews/{id}/reject")
    public void rejectReview(@PathVariable UUID id,
                             @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                             @AuthenticationPrincipal UUID adminUserId) {
        marketplaceClient.updateReviewStatus(id, "REJECTED");
        auditLogService.log(adminUserId, "REVIEW_REJECTED", "REVIEW", id, null);
    }

    @PostMapping("/moderation/certificates/{id}/approve")
    public void approveCertificate(@PathVariable UUID id,
                                   @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                   @AuthenticationPrincipal UUID adminUserId) {
        marketplaceClient.updateCertificateStatus(id, "APPROVED");
        auditLogService.log(adminUserId, "CERTIFICATE_APPROVED", "CERTIFICATE", id, null);
    }

    @PostMapping("/moderation/certificates/{id}/reject")
    public void rejectCertificate(@PathVariable UUID id,
                                  @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                  @AuthenticationPrincipal UUID adminUserId) {
        marketplaceClient.updateCertificateStatus(id, "REJECTED");
        auditLogService.log(adminUserId, "CERTIFICATE_REJECTED", "CERTIFICATE", id, null);
    }

    @PostMapping("/users/{id}/block")
    public void blockUser(@PathVariable UUID id,
                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                          @AuthenticationPrincipal UUID adminUserId) {
        var token = authHeader.substring(7);
        authClient.updateUserStatus(id, "BLOCKED", token);
        auditLogService.log(adminUserId, "USER_BLOCKED", "USER", id, null);
    }

    @PostMapping("/users/{id}/unblock")
    public void unblockUser(@PathVariable UUID id,
                            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                            @AuthenticationPrincipal UUID adminUserId) {
        var token = authHeader.substring(7);
        authClient.updateUserStatus(id, "ACTIVE", token);
        auditLogService.log(adminUserId, "USER_UNBLOCKED", "USER", id, null);
    }
}
```

- [ ] **Step 2: Verify build**

Run: `cd backend/admin-service && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/admin-service/src/main/java/com/tutor/adminservice/moderation/
git commit -m "feat(admin-service): add ModerationController with review, certificate, user endpoints"
```

---

## Task 8: Add complaint moderation audit logging

**Files:**
- Modify: `backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintController.java`

- [ ] **Step 1: Add AuditLogService to ComplaintController**

Modify `ComplaintController.java` to inject `AuditLogService` and log moderation actions:

```java
private final AuditLogService auditLogService; // Add field

// In moderate method:
auditLogService.log(moderatorId, 
    request.status() == ComplaintStatus.APPROVED ? "COMPLAINT_APPROVED" : "COMPLAINT_REJECTED",
    "COMPLAINT", id, request.resolutionNotes());
```

- [ ] **Step 2: Verify build**

Run: `cd backend/admin-service && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/admin-service/src/main/java/com/tutor/adminservice/complaint/ComplaintController.java
git commit -m "feat(admin-service): add audit logging to complaint moderation"
```

---

## Task 9: Add tests for admin-service

**Files:**
- Create: `backend/admin-service/src/test/java/com/tutor/adminservice/complaint/ComplaintServiceTest.java`

- [ ] **Step 1: Create ComplaintServiceTest.java**

```java
package com.tutor.adminservice.complaint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private ComplaintService complaintService;

    @Test
    void shouldCreateComplaint() {
        var request = new CreateComplaintRequest(
                UUID.randomUUID(), ComplaintCategory.SPAM, "Spam content", "Details", ComplaintPriority.HIGH);
        var reporterId = UUID.randomUUID();

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = complaintService.create(request, reporterId);

        assertEquals(request.reportedUserId(), result.getReportedUserId());
        assertEquals(reporterId, result.getReporterUserId());
        assertEquals(ComplaintStatus.PENDING, result.getStatus());
        assertEquals(ComplaintPriority.HIGH, result.getPriority());
    }

    @Test
    void shouldModerateComplaint() {
        var complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .status(ComplaintStatus.PENDING)
                .build();
        var moderatorId = UUID.randomUUID();

        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new ModerateComplaintRequest(ComplaintStatus.APPROVED, "Confirmed spam");
        var result = complaintService.moderate(complaint.getId(), request, moderatorId);

        assertEquals(ComplaintStatus.APPROVED, result.getStatus());
        assertEquals("Confirmed spam", result.getResolutionNotes());
        assertEquals(moderatorId, result.getModeratorId());
        assertNotNull(result.getModeratedAt());
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd backend/admin-service && mvn test`
Expected: Tests PASS, BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/admin-service/src/test/java/com/tutor/adminservice/complaint/ComplaintServiceTest.java
git commit -m "test(admin-service): add ComplaintService unit tests"
```

---

## Task 10: Modify marketplace-service for moderation

**Files:**
- Modify: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewService.java`
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ReviewModerationController.java`
- Create: `backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/certificate/CertificateModerationController.java`

- [ ] **Step 1: Change review creation to PENDING status**

In `ReviewService.java`, change:
```java
review.setStatus(ReviewStatus.PUBLISHED);
```
to:
```java
review.setStatus(ReviewStatus.PENDING);
```

- [ ] **Step 2: Create ReviewModerationController.java**

```java
package com.tutor.marketplaceservice.review;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/reviews")
public class ReviewModerationController {

    private final ReviewRepository reviewRepository;

    @GetMapping
    public List<ReviewDto> listByStatus(@RequestParam ReviewStatus status) {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(ReviewDto::from).toList();
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        var review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
        review.setStatus(request.status());
        reviewRepository.save(review);
    }

    public record UpdateStatusRequest(ReviewStatus status) {}
}
```

- [ ] **Step 3: Create CertificateModerationController.java**

```java
package com.tutor.marketplaceservice.certificate;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/certificates")
public class CertificateModerationController {

    private final CertificateRepository certificateRepository;

    @GetMapping
    public List<CertificateDto> listByStatus(@RequestParam CertificateStatus status) {
        return certificateRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(CertificateDto::from).toList();
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        var certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + id));
        certificate.setStatus(request.status());
        certificateRepository.save(certificate);
    }

    public record UpdateStatusRequest(CertificateStatus status) {}
}
```

- [ ] **Step 4: Add findByStatus methods to repositories**

Add to `ReviewRepository.java`:
```java
List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
```

Add to `CertificateRepository.java`:
```java
List<Certificate> findByStatusOrderByCreatedAtDesc(CertificateStatus status);
```

- [ ] **Step 5: Update security config for internal endpoints**

In `SecurityConfig.java` of marketplace-service, add:
```java
.requestMatchers("/internal/**").permitAll()
```

- [ ] **Step 6: Run marketplace-service tests**

Run: `cd backend/marketplace-service && mvn test`
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/review/ backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/certificate/ backend/marketplace-service/src/main/java/com/tutor/marketplaceservice/config/SecurityConfig.java
git commit -m "feat(marketplace-service): add review and certificate moderation endpoints"
```

---

## Task 11: Add admin-service to docker-compose

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add admin-service to docker-compose**

```yaml
  admin-service:
    build:
      context: ./backend/admin-service
    container_name: tutorspace-admin-service
    environment:
      SERVER_PORT: 8084
      JWT_SECRET: ${JWT_SECRET:-MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}
      AUTH_SERVICE_URL: http://auth-service:8080
      MARKETPLACE_SERVICE_URL: http://marketplace-service:8081
      KAFKA_BOOTSTRAP: kafka:9092
    ports:
      - "18084:8084"
    depends_on:
      - auth-service
      - marketplace-service
```

- [ ] **Step 2: Add depends_on admin-service to frontend**

Add `admin-service` to frontend's `depends_on` list.

- [ ] **Step 3: Verify docker-compose syntax**

Run: `docker compose config`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml
git commit -m "infra: add admin-service to docker-compose"
```

---

## Task 12: Final verification and documentation

- [ ] **Step 1: Run all service tests**

Run each:
```bash
cd backend/admin-service && mvn test
cd backend/marketplace-service && mvn test
```
Expected: All BUILD SUCCESS

- [ ] **Step 2: Update Iteration E status in doc**

Modify: `doc/TutorSpace_E2E_MVP_Iterations.md`
- Change `Status: [ ]` to `Status: [x]` for Iteration E
- Mark all plan items as `[x]`
- Add summary of what was accomplished

- [ ] **Step 3: Final commit**

```bash
git add doc/TutorSpace_E2E_MVP_Iterations.md
git commit -m "docs: mark Iteration E as complete"
```

---

## Self-Review

### Spec coverage check
- ✅ Create admin-service → Tasks 1-3
- ✅ Complaints with categories/priorities → Tasks 3-4
- ✅ Review moderation → Tasks 7, 10
- ✅ Certificate moderation → Tasks 7, 10
- ✅ Audit log → Tasks 5, 8
- ✅ User blocking through auth-service → Tasks 6, 7
- ✅ JWT security with ROLE_ADMIN → Tasks 2-3
- ✅ Docker compose update → Task 11

### Placeholder scan
- No TBD/TODO found
- All code snippets are complete
- All file paths are exact

### Type consistency
- `ComplaintStatus` used consistently (PENDING, APPROVED, REJECTED)
- `ReviewStatus` and `CertificateStatus` match marketplace-service enums
- UUID types consistent across all services

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-04-30-admin-and-moderation-plan.md`.**

Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
