package com.tutor.crmservice.lesson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilitySlotServiceTest {

    @Mock AvailabilityScheduleRepository scheduleRepository;
    @Mock SlotOverrideRepository overrideRepository;
    @Mock LessonRepository lessonRepository;

    @InjectMocks
    AvailabilitySlotService service;

    @Test
    void generateSlots_withOneRule_producesCorrectSlots() {
        UUID tutorId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 7); // Thursday

        AvailabilitySchedule rule = new AvailabilitySchedule();
        rule.setTutorUserId(tutorId);
        rule.setDayOfWeek(4); // Thursday
        rule.setStartTime(LocalTime.of(15, 0));
        rule.setEndTime(LocalTime.of(17, 0));
        rule.setSlotDurationMinutes(60);
        rule.setActive(true);

        when(scheduleRepository.findByTutorUserIdAndActiveTrue(tutorId))
                .thenReturn(List.of(rule));
        when(overrideRepository.findByTutorUserIdAndDate(tutorId, date))
                .thenReturn(List.of());
        when(lessonRepository.findByTutorUserIdAndStartsAtBetween(tutorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        var slots = service.generateSlots(tutorId, date, date, null);

        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(15, 0), slots.get(0).startTime());
        assertEquals(LocalTime.of(16, 0), slots.get(0).endTime());
        assertEquals(LocalTime.of(16, 0), slots.get(1).startTime());
        assertEquals(LocalTime.of(17, 0), slots.get(1).endTime());
    }

    @Test
    void generateSlots_withBlockedOverride_removesSlot() {
        UUID tutorId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 7);

        AvailabilitySchedule rule = new AvailabilitySchedule();
        rule.setTutorUserId(tutorId);
        rule.setDayOfWeek(4);
        rule.setStartTime(LocalTime.of(15, 0));
        rule.setEndTime(LocalTime.of(17, 0));
        rule.setSlotDurationMinutes(60);
        rule.setActive(true);

        SlotOverride blocked = new SlotOverride();
        blocked.setTutorUserId(tutorId);
        blocked.setDate(date);
        blocked.setStartTime(LocalTime.of(15, 0));
        blocked.setDurationMinutes(60);
        blocked.setType(SlotOverrideType.BLOCKED);

        when(scheduleRepository.findByTutorUserIdAndActiveTrue(tutorId))
                .thenReturn(List.of(rule));
        when(overrideRepository.findByTutorUserIdAndDate(tutorId, date))
                .thenReturn(List.of(blocked));
        when(lessonRepository.findByTutorUserIdAndStartsAtBetween(tutorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        var slots = service.generateSlots(tutorId, date, date, null);

        assertEquals(1, slots.size());
        assertEquals(LocalTime.of(16, 0), slots.get(0).startTime());
    }
}
