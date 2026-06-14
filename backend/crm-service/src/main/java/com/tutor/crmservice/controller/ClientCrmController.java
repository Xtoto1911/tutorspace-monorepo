package com.tutor.crmservice.controller;

import com.tutor.crmservice.application.ApplicationLifecycleService;
import com.tutor.crmservice.favorite.ClientFavoritePageService;
import com.tutor.crmservice.favorite.ClientFavoriteService;
import com.tutor.crmservice.favorite.FavoriteToggleResponse;
import com.tutor.crmservice.lesson.LessonRescheduleRequest;
import com.tutor.crmservice.lesson.LessonRescheduleService;
import com.tutor.crmservice.message.Message;
import com.tutor.crmservice.message.MessageService;
import com.tutor.crmservice.security.JwtPrincipal;
import com.tutor.crmservice.service.RequestLessonRescheduleDto;
import com.tutor.crmservice.service.SendMessageRequest;
import com.tutor.crmservice.service.SendMessageResponse;
import com.tutor.crmservice.view.PageViewService;
import com.tutor.crmservice.view.ClientApplicationDetailsDto;
import com.tutor.crmservice.view.ClientApplicationsPageDto;
import com.tutor.crmservice.view.ClientFavoritesPageDto;
import com.tutor.crmservice.view.ClientHistoryPageDto;
import com.tutor.crmservice.view.ClientMessagesPageDto;
import com.tutor.crmservice.view.ClientOverviewPageDto;
import com.tutor.crmservice.view.ClientSchedulePageDto;
import com.tutor.crmservice.view.ClientSettingsPageDto;
import com.tutor.crmservice.settings.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client")
public class ClientCrmController {

    private final ApplicationLifecycleService applicationService;
    private final MessageService messageService;
    private final PageViewService pageViewService;
    private final LessonRescheduleService lessonRescheduleService;
    private final SettingsService settingsService;
    private final ClientFavoriteService clientFavoriteService;
    private final ClientFavoritePageService clientFavoritePageService;

    @GetMapping("/dashboard")
    public ClientOverviewPageDto getDashboard(@AuthenticationPrincipal JwtPrincipal principal) {
        return pageViewService.getClientOverviewPage(principal.userId());
    }

    @GetMapping("/applications")
    public ClientApplicationsPageDto getApplications(@AuthenticationPrincipal JwtPrincipal principal) {
        return pageViewService.getClientApplicationsPage(principal.userId());
    }

    @GetMapping("/applications/{applicationId}")
    public ClientApplicationDetailsDto getApplication(@AuthenticationPrincipal JwtPrincipal principal,
                                                      @PathVariable UUID applicationId) {
        return pageViewService.getClientApplication(principal.userId(), applicationId);
    }

    @PatchMapping("/applications/{applicationId}/cancel")
    public ClientApplicationDetailsDto cancelApplication(@AuthenticationPrincipal JwtPrincipal principal,
                                                         @PathVariable UUID applicationId) {
        applicationService.cancelClientApplication(principal.userId(), applicationId);
        return pageViewService.getClientApplication(principal.userId(), applicationId);
    }

    @GetMapping("/schedule")
    public ClientSchedulePageDto getSchedule(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from == null && to == null) {
            return pageViewService.getClientSchedulePage(principal.userId());
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Параметры from и to нужно передавать вместе.");
        }
        if (from.getDayOfWeek() != DayOfWeek.MONDAY || !to.equals(from.plusDays(6))) {
            throw new IllegalArgumentException("Диапазон расписания должен идти с понедельника по воскресенье.");
        }
        return pageViewService.getClientSchedulePage(principal.userId(), from, to);
    }

    @GetMapping("/history")
    public ClientHistoryPageDto getHistory(@AuthenticationPrincipal JwtPrincipal principal) {
        return pageViewService.getClientHistoryPage(principal.userId());
    }

    @GetMapping("/messages")
    public ClientMessagesPageDto getMessages(@AuthenticationPrincipal JwtPrincipal principal,
                                             @RequestParam(required = false) UUID threadId) {
        return pageViewService.getClientMessagesPage(principal.userId(), threadId);
    }

    @GetMapping("/favorites")
    public ClientFavoritesPageDto getFavorites(@AuthenticationPrincipal JwtPrincipal principal) {
        return clientFavoritePageService.getFavoritesPage(principal.userId());
    }

    @PostMapping("/favorites/{tutorId}")
    public FavoriteToggleResponse addFavorite(@AuthenticationPrincipal JwtPrincipal principal,
                                              @PathVariable UUID tutorId) {
        return clientFavoriteService.addFavorite(principal.userId(), tutorId);
    }

    @DeleteMapping("/favorites/{tutorId}")
    public FavoriteToggleResponse removeFavorite(@AuthenticationPrincipal JwtPrincipal principal,
                                                 @PathVariable UUID tutorId) {
        return clientFavoriteService.removeFavorite(principal.userId(), tutorId);
    }

    @PostMapping("/schedule/lessons/{lessonId}/reschedule-request")
    public Map<String, String> requestReschedule(@AuthenticationPrincipal JwtPrincipal principal,
                                                 @PathVariable UUID lessonId,
                                                 @Valid @RequestBody RequestLessonRescheduleDto payload) {
        LessonRescheduleRequest request = lessonRescheduleService.createClientRequest(principal.userId(), lessonId, payload);
        return Map.of(
                "id", request.getId().toString(),
                "status", request.getStatus().name()
        );
    }

    @PostMapping("/messages/{threadId}")
    public SendMessageResponse sendMessage(@AuthenticationPrincipal JwtPrincipal principal,
                                           @PathVariable UUID threadId,
                                           @Valid @RequestBody SendMessageRequest request) {
        Message message = messageService.sendClientMessage(principal.userId(), threadId, request.text());
        return new SendMessageResponse(message.getId().toString(), message.getThreadId().toString(), message.getText(), "sent");
    }

    @GetMapping("/settings")
    public ClientSettingsPageDto getSettings(@AuthenticationPrincipal JwtPrincipal principal) {
        return settingsService.getClientSettings(principal.userId(), principal.email());
    }

    @PatchMapping("/settings")
    public ClientSettingsPageDto updateSettings(@AuthenticationPrincipal JwtPrincipal principal,
                                                @RequestBody ClientSettingsPageDto dto) {
        return settingsService.updateClientSettings(principal.userId(), dto);
    }
}
