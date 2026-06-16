type LessonCellPlatformInput = {
  mode: 'platform';
  applicationId: string;
  date: string;
  time: string;
  durationMinutes: number;
};

type LessonCellManualInput = {
  mode: 'manual';
  studentId?: string;
  date: string;
  time: string;
  durationMinutes: number;
  studentName: string;
  subject: string;
  format: string;
  recurrence?: LessonRecurrenceInput | null;
};

type LessonSlotPlatformInput = {
  mode: 'platform';
  applicationId: string;
  slotDate: string;
  slotStartTime: string;
  slotEndTime: string;
};

type LessonSlotManualInput = {
  mode: 'manual';
  studentId?: string;
  slotDate: string;
  slotStartTime: string;
  slotEndTime: string;
  studentName: string;
  subject: string;
  format: string;
  recurrence?: LessonRecurrenceInput | null;
};

type LessonRecurrenceInput = {
  type: 'NONE' | 'WEEKLY';
  occurrences: number;
};

export type LessonRescheduleScope = 'SINGLE' | 'SERIES';

type CalendarDisplayStat = {
  icon: string;
  value: string;
  label: string;
  sublabel: string;
  tone: 'indigo' | 'cyan' | 'emerald' | 'amber';
};

type CalendarDisplaySlot = {
  available: boolean;
  [key: string]: unknown;
};

type LessonModalRequest = {
  desiredStartTime: string;
  durationMinutes?: number;
};

type LessonModalCell = {
  date: string;
  time: string;
};

function buildStartsAt(date: string, time: string) {
  return `${date}T${time}:00`;
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  result.setHours(0, 0, 0, 0);
  return result;
}

function formatDateISO(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function toMinutes(time: string) {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

function getDurationMinutes(startTime: string, endTime: string) {
  return toMinutes(endTime) - toMinutes(startTime);
}

const calendarWeekdayLabels = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
const tutorCalendarStartHour = 8;
const tutorCalendarLastStartHour = 22;
const tutorCalendarEndHour = tutorCalendarLastStartHour + 1;

export type CalendarWeekDay = {
  day: string;
  date: string;
  isoDate: string;
  active: boolean;
};

export function buildTutorCalendarTimeSlots(): string[] {
  return Array.from(
    { length: tutorCalendarLastStartHour - tutorCalendarStartHour + 1 },
    (_, index) => `${String(tutorCalendarStartHour + index).padStart(2, '0')}:00`,
  );
}

export function getTutorCalendarGridPosition(startTime: string, endTime: string) {
  const visibleStart = tutorCalendarStartHour * 60;
  const visibleEnd = tutorCalendarEndHour * 60;
  const clampedStart = Math.min(Math.max(toMinutes(startTime), visibleStart), visibleEnd - 1);
  const clampedEnd = Math.min(Math.max(toMinutes(endTime), clampedStart + 1), visibleEnd);

  return {
    row: Math.floor((clampedStart - visibleStart) / 60),
    rowSpan: Math.max(1, Math.ceil((clampedEnd - clampedStart) / 60)),
  };
}

export function buildCalendarWeekDays(weekStart: Date, now = new Date()): CalendarWeekDay[] {
  const todayIso = formatDateISO(now);

  return calendarWeekdayLabels.map((day, index) => {
    const date = addDays(weekStart, index);
    const isoDate = formatDateISO(date);

    return {
      day,
      date: String(date.getDate()),
      isoDate,
      active: isoDate === todayIso,
    };
  });
}

export function isCalendarCellInPast(date: string, time: string, now = new Date()): boolean {
  return new Date(`${date}T${time}:00`).getTime() < now.getTime();
}

function normalizeOptionalText(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? undefined : trimmed;
}

function normalizeRecurrence(recurrence?: LessonRecurrenceInput | null) {
  if (!recurrence || recurrence.type === 'NONE') {
    return undefined;
  }

  return recurrence;
}

export function buildCellLessonPayload(input: LessonCellPlatformInput | LessonCellManualInput) {
  if (input.mode === 'platform') {
    return {
      applicationId: input.applicationId,
      startsAt: buildStartsAt(input.date, input.time),
      durationMinutes: input.durationMinutes,
      slot: null,
    };
  }

  return {
    applicationId: null,
    studentId: input.studentId,
    startsAt: buildStartsAt(input.date, input.time),
    durationMinutes: input.durationMinutes,
    studentName: normalizeOptionalText(input.studentName),
    subject: input.subject,
    format: input.format,
    recurrence: normalizeRecurrence(input.recurrence),
  };
}

export function buildSlotLessonPayload(input: LessonSlotPlatformInput | LessonSlotManualInput) {
  const basePayload = {
    applicationId: input.mode === 'platform' ? input.applicationId : null,
    startsAt: buildStartsAt(input.slotDate, input.slotStartTime),
    durationMinutes: getDurationMinutes(input.slotStartTime, input.slotEndTime),
  };

  if (input.mode === 'platform') {
    return {
      ...basePayload,
      slot: {
        date: input.slotDate,
        startTime: input.slotStartTime,
        durationMinutes: basePayload.durationMinutes,
      },
    };
  }

  return {
    ...basePayload,
    studentId: input.studentId,
    studentName: input.studentName,
    subject: input.subject,
    format: input.format,
    recurrence: normalizeRecurrence(input.recurrence),
  };
}

export function buildLessonReschedulePayload(date: string, time: string, scope: LessonRescheduleScope) {
  return {
    startsAt: buildStartsAt(date, time),
    scope,
  };
}

export function getTutorCalendarDisplayStats(input: {
  stats: CalendarDisplayStat[];
  slots: CalendarDisplaySlot[];
}) {
  const availableSlots = input.slots.filter((slot) => slot.available).length;

  return input.stats.map((stat) => (
    stat.label === 'Свободно' || stat.label === 'Окна'
      ? { ...stat, value: String(availableSlots) }
      : stat
  ));
}

function parseLessonRequestStartTime(value?: string | null) {
  if (!value) {
    return null;
  }

  const match = value.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/);
  if (match) {
    return {
      date: match[1],
      time: match[2],
    };
  }

  const timeOnlyMatch = value.match(/^(\d{2}:\d{2})/);
  if (timeOnlyMatch) {
    return {
      date: null,
      time: timeOnlyMatch[1],
    };
  }

  return null;
}

export function getLessonModalDefaults(input: {
  fallbackDate: string;
  selectedCell: LessonModalCell | null;
  selectedLessonRequest: LessonModalRequest | null;
}) {
  if (input.selectedCell) {
    return {
      date: input.selectedCell.date,
      time: input.selectedCell.time,
      durationMinutes: 60,
    };
  }

  const requestStart = parseLessonRequestStartTime(input.selectedLessonRequest?.desiredStartTime);

  return {
    date: requestStart?.date ?? input.fallbackDate,
    time: requestStart?.time ?? '09:00',
    durationMinutes: input.selectedLessonRequest?.durationMinutes ?? 60,
  };
}

export function formatLessonRequestStartTime(value: string) {
  const parsed = parseLessonRequestStartTime(value);
  if (!parsed?.date) {
    return parsed?.time ?? value;
  }

  const [, month, day] = parsed.date.split('-');
  return `${day}.${month}, ${parsed.time}`;
}

export type LessonStartKind = 'create' | 'recurrence' | 'reschedule';

export function getLessonStartValidationError(
  date: string,
  time: string,
  kind: LessonStartKind,
  now: Date = new Date(),
): string | null {
  if (!isCalendarCellInPast(date, time, now)) {
    return null;
  }
  switch (kind) {
    case 'recurrence':
      return 'Нельзя назначить серию уроков в прошлое. Выберите будущую дату и время.';
    case 'reschedule':
      return 'Нельзя перенести урок в прошлое. Выберите будущую дату и время.';
    case 'create':
      return 'Нельзя назначить урок в прошлое. Выберите будущую дату и время.';
  }
}
