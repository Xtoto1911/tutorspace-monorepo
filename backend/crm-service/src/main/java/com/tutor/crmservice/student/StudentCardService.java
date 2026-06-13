package com.tutor.crmservice.student;

import com.tutor.crmservice.request.CrmApplication;
import com.tutor.crmservice.service.CreateProgressReportRequest;
import com.tutor.crmservice.service.CreateStudentRequest;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentCardService {

    private final StudentCardRepository studentCardRepository;
    private final StudentProgressReportRepository progressReportRepository;

    @Transactional
    public StudentCard createFromApplication(CrmApplication application) {
        StudentCard card = new StudentCard();
        card.setApplicationId(application.getId());
        card.setClientUserId(application.getClientUserId());
        card.setTutorUserId(application.getTutorUserId());
        card.setName(application.getStudentName());
        card.setDetails(application.getStudentGrade() + " · " + application.getGoal());
        card.setSubject(application.getSubjectName());
        card.setStatus("Активен");
        card.setNote("Карточка создана после принятия заявки.");
        card.setProgress(25);
        return studentCardRepository.save(card);
    }

    @Transactional
    public StudentCard createManualStudent(UUID tutorUserId, CreateStudentRequest request) {
        StudentCard card = new StudentCard();
        card.setTutorUserId(tutorUserId);
        card.setName(request.name().trim());
        card.setDetails(request.details().trim());
        card.setSubject(request.subject().trim());
        card.setStatus("Активен");
        card.setNote(request.note() == null || request.note().isBlank()
                ? "Ученик добавлен вручную."
                : request.note().trim());
        card.setProgress(0);
        return studentCardRepository.save(card);
    }

    @Transactional
    public StudentProgressReport createProgressReport(
            UUID tutorUserId,
            UUID studentId,
            CreateProgressReportRequest request
    ) {
        StudentCard card = studentCardRepository.findByIdAndTutorUserId(studentId, tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Ученик не найден."));

        StudentProgressReport report = new StudentProgressReport();
        report.setStudentId(studentId);
        report.setTutorUserId(tutorUserId);
        report.setTitle(request.title().trim());
        report.setSummary(request.summary().trim());
        report.setProgress(request.progress());

        card.setProgress(request.progress());
        studentCardRepository.save(card);
        return progressReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public Optional<StudentCard> findByApplicationId(UUID applicationId) {
        return studentCardRepository.findByApplicationId(applicationId);
    }
}
