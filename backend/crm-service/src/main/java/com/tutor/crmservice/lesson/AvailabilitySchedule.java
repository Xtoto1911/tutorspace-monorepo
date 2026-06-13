package com.tutor.crmservice.lesson;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "availability_schedules",
    indexes = @Index(name = "idx_availability_schedule_tutor", columnList = "tutorUserId")
)
@Data
public class AvailabilitySchedule {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID tutorUserId;

    private UUID subjectId;

    @Column(nullable = false)
    private int dayOfWeek; // 1 = Monday ... 7 = Sunday

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int slotDurationMinutes;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDate validUntil;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTutorUserId() {
        return tutorUserId;
    }

    public void setTutorUserId(UUID tutorUserId) {
        this.tutorUserId = tutorUserId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }
}
