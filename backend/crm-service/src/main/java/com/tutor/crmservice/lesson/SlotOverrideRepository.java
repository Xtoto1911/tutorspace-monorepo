package com.tutor.crmservice.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SlotOverrideRepository extends JpaRepository<SlotOverride, UUID> {
    List<SlotOverride> findByTutorUserIdAndDate(UUID tutorUserId, LocalDate date);
    List<SlotOverride> findByTutorUserIdAndDateBetween(UUID tutorUserId, LocalDate from, LocalDate to);
}
