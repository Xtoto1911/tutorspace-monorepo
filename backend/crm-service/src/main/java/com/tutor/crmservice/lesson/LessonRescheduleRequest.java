package com.tutor.crmservice.lesson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lesson_reschedule_requests")
@Getter
@Setter
@NoArgsConstructor
public class LessonRescheduleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID lessonId;

    @Column(nullable = false)
    private UUID applicationId;

    @Column(nullable = false)
    private UUID clientUserId;

    @Column(nullable = false)
    private UUID tutorUserId;

    @Column(nullable = false)
    private String lessonTitle;

    @Column(nullable = false)
    private LocalDateTime lessonStartsAt;

    @Column(nullable = false, length = 1000)
    private String message;

    private LocalDateTime proposedStartAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LessonRescheduleStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getClientUserId() {
        return clientUserId;
    }

    public void setClientUserId(UUID clientUserId) {
        this.clientUserId = clientUserId;
    }

    public UUID getTutorUserId() {
        return tutorUserId;
    }

    public void setTutorUserId(UUID tutorUserId) {
        this.tutorUserId = tutorUserId;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }

    public LocalDateTime getLessonStartsAt() {
        return lessonStartsAt;
    }

    public void setLessonStartsAt(LocalDateTime lessonStartsAt) {
        this.lessonStartsAt = lessonStartsAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getProposedStartAt() {
        return proposedStartAt;
    }

    public void setProposedStartAt(LocalDateTime proposedStartAt) {
        this.proposedStartAt = proposedStartAt;
    }

    public LessonRescheduleStatus getStatus() {
        return status;
    }

    public void setStatus(LessonRescheduleStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
