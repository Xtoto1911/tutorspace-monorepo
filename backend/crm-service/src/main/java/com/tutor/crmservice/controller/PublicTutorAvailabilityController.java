package com.tutor.crmservice.controller;

import com.tutor.crmservice.lesson.AvailabilitySlotService;
import com.tutor.crmservice.view.SlotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/tutors")
public class PublicTutorAvailabilityController {

    private final AvailabilitySlotService slotService;

    @GetMapping("/{tutorUserId}/slots")
    public List<SlotDto> getPublicSlots(@PathVariable UUID tutorUserId,
                                        @RequestParam LocalDate from,
                                        @RequestParam LocalDate to) {
        return slotService.generateSlots(tutorUserId, from, to, null).stream()
                .filter(SlotDto::available)
                .toList();
    }
}
