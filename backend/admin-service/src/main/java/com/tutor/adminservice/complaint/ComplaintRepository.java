package com.tutor.adminservice.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);
    List<Complaint> findByPriorityOrderByCreatedAtDesc(ComplaintPriority priority);
    List<Complaint> findByStatusAndPriorityOrderByCreatedAtDesc(ComplaintStatus status, ComplaintPriority priority);
}
