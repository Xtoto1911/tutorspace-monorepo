package com.tutor.crmservice.controller;

import com.tutor.crmservice.lesson.*;
import com.tutor.crmservice.security.JwtPrincipal;
import com.tutor.crmservice.view.SlotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tutor")
public class TutorAvailabilityController {

    private final AvailabilityScheduleRepository scheduleRepository;
    private final SlotOverrideRepository overrideRepository;
    private final AvailabilitySlotService slotService;

    @GetMapping("/availability-schedules")
    public List<AvailabilitySchedule> getSchedules(@AuthenticationPrincipal JwtPrincipal principal) {
        return scheduleRepository.findByTutorUserIdAndActiveTrue(principal.userId());
    }

    @PostMapping("/availability-schedules")
    public AvailabilitySchedule createSchedule(@AuthenticationPrincipal JwtPrincipal principal,
                                                @RequestBody AvailabilitySchedule schedule) {
        schedule.setTutorUserId(principal.userId());
        return scheduleRepository.save(schedule);
    }

    @PatchMapping("/availability-schedules/{id}")
    public AvailabilitySchedule updateSchedule(@AuthenticationPrincipal JwtPrincipal principal,
                                                @PathVariable UUID id,
                                                @RequestBody AvailabilitySchedule patch) {
        AvailabilitySchedule existing = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        if (!existing.getTutorUserId().equals(principal.userId())) {
            throw new IllegalArgumentException("Access denied");
        }
        if (patch.isActive() != existing.isActive()) existing.setActive(patch.isActive());
        if (patch.getStartTime() != null) existing.setStartTime(patch.getStartTime());
        if (patch.getEndTime() != null) existing.setEndTime(patch.getEndTime());
        if (patch.getSlotDurationMinutes() != 0) existing.setSlotDurationMinutes(patch.getSlotDurationMinutes());
        if (patch.getValidUntil() != null) existing.setValidUntil(patch.getValidUntil());
        return scheduleRepository.save(existing);
    }

    @DeleteMapping("/availability-schedules/{id}")
    public void deleteSchedule(@AuthenticationPrincipal JwtPrincipal principal,
                               @PathVariable UUID id) {
        AvailabilitySchedule existing = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        if (!existing.getTutorUserId().equals(principal.userId())) {
            throw new IllegalArgumentException("Access denied");
        }
        scheduleRepository.delete(existing);
    }

    @GetMapping("/slot-overrides")
    public List<SlotOverride> getOverrides(@AuthenticationPrincipal JwtPrincipal principal,
                                            @RequestParam LocalDate from,
                                            @RequestParam LocalDate to) {
        return overrideRepository.findByTutorUserIdAndDateBetween(principal.userId(), from, to);
    }

    @PostMapping("/slot-overrides")
    public SlotOverride createOverride(@AuthenticationPrincipal JwtPrincipal principal,
                                        @RequestBody SlotOverride override) {
        override.setTutorUserId(principal.userId());
        return overrideRepository.save(override);
    }

    @DeleteMapping("/slot-overrides/{id}")
    public void deleteOverride(@AuthenticationPrincipal JwtPrincipal principal,
                               @PathVariable UUID id) {
        SlotOverride existing = overrideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Override not found"));
        if (!existing.getTutorUserId().equals(principal.userId())) {
            throw new IllegalArgumentException("Access denied");
        }
        overrideRepository.delete(existing);
    }

    @GetMapping("/calendar/slots")
    public List<SlotDto> getSlots(@AuthenticationPrincipal JwtPrincipal principal,
                                   @RequestParam LocalDate from,
                                   @RequestParam LocalDate to) {
        return slotService.generateSlots(principal.userId(), from, to, null);
    }
}
