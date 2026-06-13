package com.tutor.crmservice.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilityScheduleRepository extends JpaRepository<AvailabilitySchedule, UUID> {
    List<AvailabilitySchedule> findByTutorUserIdAndActiveTrue(UUID tutorUserId);
}
