package com.tutor.crmservice.lesson;

import com.tutor.crmservice.view.SlotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilitySlotService {

    private final AvailabilityScheduleRepository scheduleRepository;
    private final SlotOverrideRepository overrideRepository;
    private final LessonRepository lessonRepository;

    public List<SlotDto> generateSlots(UUID tutorUserId, LocalDate from, LocalDate to, UUID subjectId) {
        List<SlotDto> result = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            result.addAll(generateForDate(tutorUserId, date, subjectId));
        }

        return result.stream()
                .sorted(Comparator.comparing(SlotDto::date)
                        .thenComparing(SlotDto::startTime))
                .toList();
    }

    private List<SlotDto> generateForDate(UUID tutorUserId, LocalDate date, UUID subjectId) {
        List<SlotDto> slots = new ArrayList<>();
        int dayOfWeek = date.getDayOfWeek().getValue();

        List<AvailabilitySchedule> rules = scheduleRepository.findByTutorUserIdAndActiveTrue(tutorUserId);
        for (AvailabilitySchedule rule : rules) {
            if (!rule.isActive()) continue;
            if (rule.getValidUntil() != null && date.isAfter(rule.getValidUntil())) continue;
            if (rule.getDayOfWeek() != dayOfWeek) continue;
            if (subjectId != null && rule.getSubjectId() != null && !subjectId.equals(rule.getSubjectId())) continue;

            LocalTime t = rule.getStartTime();
            while (t.plusMinutes(rule.getSlotDurationMinutes()).compareTo(rule.getEndTime()) <= 0) {
                slots.add(new SlotDto(
                        date,
                        t,
                        t.plusMinutes(rule.getSlotDurationMinutes()),
                        true,
                        "RULE"
                ));
                t = t.plusMinutes(rule.getSlotDurationMinutes());
            }
        }

        List<SlotOverride> overrides = overrideRepository.findByTutorUserIdAndDate(tutorUserId, date);
        for (SlotOverride override : overrides) {
            if (override.getType() == SlotOverrideType.BLOCKED) {
                LocalTime blockEnd = override.getStartTime().plusMinutes(override.getDurationMinutes());
                slots.removeIf(s ->
                        s.startTime().compareTo(blockEnd) < 0 &&
                                s.endTime().compareTo(override.getStartTime()) > 0
                );
            } else if (override.getType() == SlotOverrideType.EXTRA) {
                slots.add(new SlotDto(
                        date,
                        override.getStartTime(),
                        override.getStartTime().plusMinutes(override.getDurationMinutes()),
                        true,
                        "EXTRA"
                ));
            }
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Lesson> lessons = lessonRepository.findByTutorUserIdAndStartsAtBetween(tutorUserId, dayStart, dayEnd);

        for (Lesson lesson : lessons) {
            LocalTime lessonStart = lesson.getStartsAt().toLocalTime();
            LocalTime lessonEnd = lessonStart.plusMinutes(lesson.getDurationMinutes());
            for (int i = 0; i < slots.size(); i++) {
                SlotDto slot = slots.get(i);
                if (slot.startTime().compareTo(lessonEnd) < 0 && slot.endTime().compareTo(lessonStart) > 0) {
                    slots.set(i, new SlotDto(slot.date(), slot.startTime(), slot.endTime(), false, slot.source()));
                }
            }
        }

        return slots;
    }

    public boolean isSlotAvailable(UUID tutorUserId, LocalDate date, LocalTime startTime, int durationMinutes) {
        List<SlotDto> slots = generateForDate(tutorUserId, date, null);
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        return slots.stream()
                .filter(SlotDto::available)
                .anyMatch(s -> s.startTime().equals(startTime) && s.endTime().equals(endTime));
    }
}
