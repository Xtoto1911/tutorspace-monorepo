package com.tutor.crmservice.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_threads")
@Getter
@Setter
@NoArgsConstructor
public class MessageThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID applicationId;

    @Column(nullable = false)
    private UUID clientUserId;

    @Column(nullable = false)
    private UUID tutorUserId;

    @Column(nullable = false)
    private String participantName;

    @Column(nullable = false)
    private String participantInitials;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String subtitle;

    @Column(nullable = false, length = 500)
    private String preview;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantInitials() {
        return participantInitials;
    }

    public void setParticipantInitials(String participantInitials) {
        this.participantInitials = participantInitials;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
