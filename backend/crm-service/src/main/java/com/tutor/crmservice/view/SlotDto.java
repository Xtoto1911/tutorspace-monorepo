package com.tutor.crmservice.view;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotDto(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean available,
        String source // "RULE" or "EXTRA"
) {}
