package com.tutor.crmservice.request;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crm_applications")
@Getter
@Setter
@NoArgsConstructor
public class CrmApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID marketplaceRequestId;

    @Column(nullable = false)
    private UUID clientUserId;

    @Column(nullable = false)
    private UUID tutorUserId;

    @Column(nullable = false)
    private UUID tutorProfileId;

    @Column(nullable = false)
    private String tutorSlug;

    @Column(nullable = false)
    private String tutorName;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String studentGrade;

    @Column(nullable = false)
    private UUID subjectId;

    @Column(nullable = false)
    private String subjectName;

    @Column(nullable = false)
    private String format;

    @Column(nullable = false)
    private String goal;

    @Column(nullable = false, length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime desiredStartTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.NEW;

    @Column(nullable = false)
    private String source = "MARKETPLACE";

    @Column(nullable = false)
    private Instant responseDeadline;

    private Instant viewedAt;

    private Instant respondedAt;

    private Instant contactEstablishedAt;

    private Instant trialScheduledAt;

    private Instant convertedAt;

    private Instant archivedAt;

    @Column(nullable = false)
    private Integer leadScore = 0;

    private String rejectionReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (responseDeadline == null) {
            responseDeadline = now.plusSeconds(24 * 60 * 60);
        }
        if (source == null || source.isBlank()) {
            source = "MARKETPLACE";
        }
        if (leadScore == null) {
            leadScore = 0;
        }
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

    public UUID getMarketplaceRequestId() {
        return marketplaceRequestId;
    }

    public void setMarketplaceRequestId(UUID marketplaceRequestId) {
        this.marketplaceRequestId = marketplaceRequestId;
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

    public UUID getTutorProfileId() {
        return tutorProfileId;
    }

    public void setTutorProfileId(UUID tutorProfileId) {
        this.tutorProfileId = tutorProfileId;
    }

    public String getTutorSlug() {
        return tutorSlug;
    }

    public void setTutorSlug(String tutorSlug) {
        this.tutorSlug = tutorSlug;
    }

    public String getTutorName() {
        return tutorName;
    }

    public void setTutorName(String tutorName) {
        this.tutorName = tutorName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentGrade() {
        return studentGrade;
    }

    public void setStudentGrade(String studentGrade) {
        this.studentGrade = studentGrade;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getDesiredStartTime() {
        return desiredStartTime;
    }

    public void setDesiredStartTime(LocalDateTime desiredStartTime) {
        this.desiredStartTime = desiredStartTime;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getResponseDeadline() {
        return responseDeadline;
    }

    public void setResponseDeadline(Instant responseDeadline) {
        this.responseDeadline = responseDeadline;
    }

    public Instant getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(Instant viewedAt) {
        this.viewedAt = viewedAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Instant getContactEstablishedAt() {
        return contactEstablishedAt;
    }

    public void setContactEstablishedAt(Instant contactEstablishedAt) {
        this.contactEstablishedAt = contactEstablishedAt;
    }

    public Instant getTrialScheduledAt() {
        return trialScheduledAt;
    }

    public void setTrialScheduledAt(Instant trialScheduledAt) {
        this.trialScheduledAt = trialScheduledAt;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(Instant convertedAt) {
        this.convertedAt = convertedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Integer getLeadScore() {
        return leadScore;
    }

    public void setLeadScore(Integer leadScore) {
        this.leadScore = leadScore;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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
