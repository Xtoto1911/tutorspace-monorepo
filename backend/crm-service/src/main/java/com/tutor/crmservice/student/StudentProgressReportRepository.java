package com.tutor.crmservice.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentProgressReportRepository extends JpaRepository<StudentProgressReport, UUID> {
    List<StudentProgressReport> findByStudentIdAndTutorUserIdOrderByCreatedAtDesc(UUID studentId, UUID tutorUserId);
}
