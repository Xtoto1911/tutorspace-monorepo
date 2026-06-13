package com.tutor.crmservice.controller;

import com.tutor.crmservice.application.ApplicationLifecycleService;
import com.tutor.crmservice.lesson.AvailabilitySlotService;
import com.tutor.crmservice.lesson.Lesson;
import com.tutor.crmservice.lesson.LessonService;
import com.tutor.crmservice.message.Message;
import com.tutor.crmservice.message.MessageService;
import com.tutor.crmservice.security.JwtPrincipal;
import com.tutor.crmservice.service.CreateLessonRequest;
import com.tutor.crmservice.service.CreateProgressReportRequest;
import com.tutor.crmservice.service.CreateStudentRequest;
import com.tutor.crmservice.service.RescheduleLessonRequest;
import com.tutor.crmservice.service.SendMessageRequest;
import com.tutor.crmservice.service.SendMessageResponse;
import com.tutor.crmservice.service.UpdateRequestStageRequest;
import com.tutor.crmservice.view.PageViewService;
import com.tutor.crmservice.view.SlotDto;
import com.tutor.crmservice.view.TutorCalendarPageDto;
import com.tutor.crmservice.view.TutorLessonCandidateDto;
import com.tutor.crmservice.view.TutorMessagesPageDto;
import com.tutor.crmservice.view.TutorOverviewPageDto;
import com.tutor.crmservice.view.TutorRequestsPageDto;
import com.tutor.crmservice.view.TutorSettingsPageDto;
import com.tutor.crmservice.view.TutorStudentsPageDto;
import com.tutor.crmservice.view.TutorWorkspaceProfilePageDto;
import com.tutor.crmservice.settings.SettingsService;
import com.tutor.crmservice.student.StudentCard;
import com.tutor.crmservice.student.StudentCardService;
import com.tutor.crmservice.student.StudentProgressReport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tutor")
public class TutorCrmController {

    private final ApplicationLifecycleService applicationService;
    private final LessonService lessonService;
    private final MessageService messageService;
    private final PageViewService pageViewService;
    private final AvailabilitySlotService slotService;
    private final SettingsService settingsService;
    private final StudentCardService studentCardService;

    @GetMapping("/dashboard")
    public TutorOverviewPageDto getDashboard(@AuthenticationPrincipal JwtPrincipal principal) {
        return pageViewService.getTutorOverviewPage(principal.userId());
    }

    @GetMapping("/requests")
    public TutorRequestsPageDto getRequests(@AuthenticationPrincipal JwtPrincipal principal) {
        return pageViewService.getTutorRequestsPage(principal.userId());
    }

    @PatchMapping("/requests/{requestId}/accept")
    public TutorRequestsPageDto acceptRequest(@AuthenticationPrincipal JwtPrincipal principal,
                                              @PathVariable UUID requestId) {
        applicationService.acceptTutorRequest(principal.userId(), requestId);
        return pageViewService.getTutorRequestsPage(principal.userId());
    }

    @PatchMapping("/requests/{requestId}/reject")
    public TutorRequestsPageDto rejectRequest(@AuthenticationPrincipal JwtPrincipal principal,
                                              @PathVariable UUID requestId,
                                              @RequestBody(required = false) Map<String, String> payload) {
        applicationService.rejectTutorRequest(principal.userId(), requestId, payload == null ? null : payload.get("reason"));
        return pageViewService.getTutorRequestsPage(principal.userId());
    }

    @PatchMapping("/requests/{requestId}/stage")
    public TutorRequestsPageDto updateRequestStage(@AuthenticationPrincipal JwtPrincipal principal,
                                                   @PathVariable UUID requestId,
                                                   @RequestBody UpdateRequestStageRequest payload) {
        if (payload == null || payload.status() == null) {
            throw new IllegalArgumentException("Request status is required.");
        }
        applicationService.updateTutorRequestStage(principal.userId(), requestId, payload.status(), payload.reason());
        return pageViewService.getTutorRequestsPage(principal.userId());
    }

    @GetMapping("/students")
    public TutorStudentsPageDto getStudents(@AuthenticationPrincipal JwtPrincipal principal) {
        return pageViewService.getTutorStudentsPage(principal.userId());
    }

    @PostMapping("/students")
    public StudentCard createStudent(@AuthenticationPrincipal JwtPrincipal principal,
                                     @Valid @RequestBody CreateStudentRequest request) {
        return studentCardService.createManualStudent(principal.userId(), request);
    }

    @PostMapping("/students/{studentId}/reports")
    public StudentProgressReport createProgressReport(@AuthenticationPrincipal JwtPrincipal principal,
                                                      @PathVariable UUID studentId,
                                                      @Valid @RequestBody CreateProgressReportRequest request) {
        return studentCardService.createProgressReport(principal.userId(), studentId, request);
    }

    @GetMapping("/lesson-candidates")
    public List<TutorLessonCandidateDto> getLessonCandidates(@AuthenticationPrincipal JwtPrincipal principal) {
        return pageViewService.getTutorLessonCandidates(principal.userId());
    }

    @GetMapping("/calendar")
    public TutorCalendarPageDto getCalendar(
        @AuthenticationPrincipal JwtPrincipal principal,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to
    ) {
        return pageViewService.getTutorCalendarPage(principal.userId(), from, to);
    }

    @PostMapping("/lessons")
    public Lesson createLesson(@AuthenticationPrincipal JwtPrincipal principal,
                               @Valid @RequestBody CreateLessonRequest request) {
        validateCreateLessonRequest(request);
        return lessonService.createTutorLesson(principal.userId(), request);
    }

    @PatchMapping("/lessons/{lessonId}/reschedule")
    public Lesson rescheduleLesson(@AuthenticationPrincipal JwtPrincipal principal,
                                   @PathVariable UUID lessonId,
                                   @Valid @RequestBody RescheduleLessonRequest request) {
        return lessonService.rescheduleTutorLesson(principal.userId(), lessonId, request);
    }

    @GetMapping("/lessons/slots")
    public List<SlotDto> getPublicSlots(@AuthenticationPrincipal JwtPrincipal principal,
                                         @RequestParam LocalDate from,
                                         @RequestParam LocalDate to) {
        return slotService.generateSlots(principal.userId(), from, to, null);
    }

    @GetMapping("/profile")
    public TutorWorkspaceProfilePageDto getProfile(@AuthenticationPrincipal JwtPrincipal principal) {
        return settingsService.getTutorWorkspaceProfile(principal.userId());
    }

    @PatchMapping("/profile")
    public TutorWorkspaceProfilePageDto updateProfile(@AuthenticationPrincipal JwtPrincipal principal,
                                                      @RequestBody TutorWorkspaceProfilePageDto dto) {
        return settingsService.updateTutorWorkspaceProfile(principal.userId(), dto);
    }

    @GetMapping("/messages")
    public TutorMessagesPageDto getMessages(@AuthenticationPrincipal JwtPrincipal principal,
                                            @RequestParam(required = false) UUID threadId) {
        return pageViewService.getTutorMessagesPage(principal.userId(), threadId);
    }

    @PostMapping("/messages/{threadId}")
    public SendMessageResponse sendMessage(@AuthenticationPrincipal JwtPrincipal principal,
                                           @PathVariable UUID threadId,
                                           @Valid @RequestBody SendMessageRequest request) {
        Message message = messageService.sendTutorMessage(principal.userId(), threadId, request.text());
        return new SendMessageResponse(message.getId().toString(), message.getThreadId().toString(), message.getText(), "sent");
    }

    @GetMapping("/settings")
    public TutorSettingsPageDto getSettings(@AuthenticationPrincipal JwtPrincipal principal) {
        return settingsService.getTutorSettings(principal.userId());
    }

    @PatchMapping("/settings")
    public TutorSettingsPageDto updateSettings(@AuthenticationPrincipal JwtPrincipal principal,
                                               @RequestBody TutorSettingsPageDto dto) {
        return settingsService.updateTutorSettings(principal.userId(), dto);
    }

    private void validateCreateLessonRequest(CreateLessonRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Lesson request body is required.");
        }

        CreateLessonRequest.SlotRequest slot = request.slot();
        if (slot != null && (slot.date() == null || slot.startTime() == null || slot.durationMinutes() == null)) {
            throw new IllegalArgumentException("Slot date, startTime, and durationMinutes are required when slot is provided.");
        }
    }
}
