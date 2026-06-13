package com.tutor.crmservice.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_cards")
@Getter
@Setter
@NoArgsConstructor
public class StudentCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private UUID applicationId;

    @Column
    private UUID clientUserId;

    @Column(nullable = false)
    private UUID tutorUserId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String details;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, length = 500)
    private String note;

    @Column(nullable = false)
    private Integer progress;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
