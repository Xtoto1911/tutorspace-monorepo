package com.tutor.crmservice.view;

import java.util.List;

public record TutorStudentsPageDto(
        List<StatDto> stats,
        List<StudentDto> students,
        SelectedStudentDto selectedStudent,
        CardsDto cards
) {
    public record StatDto(String icon, String value, String label, String sublabel, String tone) {
    }

    public record StudentDto(String id,
                             String initials,
                             String name,
                             String details,
                             String subject,
                             String status,
                             String note,
                             int progress,
                             String tone,
                             boolean highlighted) {
    }

    public record SelectedStudentDto(String initials,
                                     String name,
                                     String details,
                                     List<String> badges,
                                     int progress,
                                     List<TaskDto> tasks) {
    }

    public record TaskDto(String title, String meta, String tone) {
    }

    public record CardsDto(HomeworkCardDto homework, PaymentsCardDto payments, ReportCardDto reports) {
    }

    public record HomeworkCardDto(String title, String text) {
    }

    public record PaymentsCardDto(String label, String value, String text) {
    }

    public record ReportCardDto(String title, String text) {
    }
}
