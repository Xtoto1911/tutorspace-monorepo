package com.tutor.crmservice.student;

import com.tutor.crmservice.service.CreateProgressReportRequest;
import com.tutor.crmservice.service.CreateStudentRequest;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCardServiceTest {

    @Mock StudentCardRepository studentCardRepository;
    @Mock StudentProgressReportRepository progressReportRepository;

    @InjectMocks
    StudentCardService service;

    @Test
    void createManualStudent_persistsTutorOwnedCard() {
        UUID tutorId = UUID.randomUUID();
        when(studentCardRepository.save(any(StudentCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentCard card = service.createManualStudent(
                tutorId,
                new CreateStudentRequest(
                        "Мария Петрова",
                        "9 класс · подготовка к ОГЭ",
                        "Математика",
                        "Нужно укрепить геометрию"
                )
        );

        assertEquals(tutorId, card.getTutorUserId());
        assertEquals("Мария Петрова", card.getName());
        assertEquals("Активен", card.getStatus());
        assertEquals(0, card.getProgress());
    }

    @Test
    void createProgressReport_persistsReportAndUpdatesStudentProgress() {
        UUID tutorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentCard card = new StudentCard();
        card.setId(studentId);
        card.setTutorUserId(tutorId);
        card.setProgress(20);

        when(studentCardRepository.findByIdAndTutorUserId(studentId, tutorId)).thenReturn(Optional.of(card));
        when(progressReportRepository.save(any(StudentProgressReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProgressReport report = service.createProgressReport(
                tutorId,
                studentId,
                new CreateProgressReportRequest(
                        "Итоги мая",
                        "Уверенно решает линейные уравнения, продолжаем геометрию.",
                        65
                )
        );

        assertEquals(studentId, report.getStudentId());
        assertEquals(tutorId, report.getTutorUserId());
        assertEquals(65, report.getProgress());
        assertEquals(65, card.getProgress());
        verify(studentCardRepository).save(card);
    }

    @Test
    void createProgressReport_rejectsAnotherTutorsStudent() {
        UUID tutorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(studentCardRepository.findByIdAndTutorUserId(studentId, tutorId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.createProgressReport(
                        tutorId,
                        studentId,
                        new CreateProgressReportRequest("Итоги мая", "Прогресс ученика.", 50)
                )
        );

        verify(progressReportRepository, never()).save(any());
    }
}
