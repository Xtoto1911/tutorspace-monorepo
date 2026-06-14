export type TutorSlotOption = {
  id: string;
  label: string;
  desiredStartTime: string;
};

export type TutorSlotDayGroup = {
  dayKey: string;
  dayLabel: string;
  slots: TutorSlotOption[];
};

const weekdayIndex: Record<string, number> = {
  Пн: 1,
  Вт: 2,
  Ср: 3,
  Чт: 4,
  Пт: 5,
  Сб: 6,
  Вс: 0,
};

const fullWeekdayLabels: Record<number, string> = {
  0: 'Воскресенье',
  1: 'Понедельник',
  2: 'Вторник',
  3: 'Среда',
  4: 'Четверг',
  5: 'Пятница',
  6: 'Суббота',
};

const monthNames = [
  'января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
  'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря',
];

function pad(value: number) {
  return value.toString().padStart(2, '0');
}

function toLocalDateTimeValue(date: Date) {
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatSlotLabel(slotDate: Date, hours: number, minutes: number) {
  const weekday = fullWeekdayLabels[slotDate.getDay()] ?? '';
  const day = slotDate.getDate();
  const month = monthNames[slotDate.getMonth()] ?? '';
  const time = `${pad(hours)}:${pad(minutes)}`;
  return `${weekday}, ${day} ${month}, ${time}`;
}

function formatDayLabel(slotDate: Date) {
  const weekday = fullWeekdayLabels[slotDate.getDay()] ?? '';
  const day = slotDate.getDate();
  const month = monthNames[slotDate.getMonth()] ?? '';
  return `${weekday}, ${day} ${month}`;
}

function slotDateFromDesiredStartTime(desiredStartTime: string): Date | null {
  const match = desiredStartTime.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/);
  if (!match) return null;
  const [, year, month, day, hours, minutes] = match;
  return new Date(Number(year), Number(month) - 1, Number(day), Number(hours), Number(minutes));
}

export function buildTutorSlotOptions(schedule: string[], now = new Date()): TutorSlotOption[] {
  const seen = new Set<string>();
  const options: TutorSlotOption[] = [];

  for (const rawLabel of schedule) {
    const label = rawLabel.trim();
    const match = label.match(/^(Пн|Вт|Ср|Чт|Пт|Сб|Вс)\s+(\d{1,2}):(\d{2})$/);

    if (!match) {
      continue;
    }

    const [, day, hours, minutes] = match;
    const targetDay = weekdayIndex[day];
    const daysUntil = (targetDay - now.getDay() + 7) % 7;
    const slotDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + daysUntil);
    slotDate.setHours(Number(hours), Number(minutes), 0, 0);

    if (slotDate <= now) {
      slotDate.setDate(slotDate.getDate() + 7);
    }

    const desiredStartTime = toLocalDateTimeValue(slotDate);
    const id = `slot-${desiredStartTime}`;

    if (seen.has(id)) {
      continue;
    }
    seen.add(id);

    options.push({
      id,
      label: formatSlotLabel(slotDate, Number(hours), Number(minutes)),
      desiredStartTime,
    });
  }

  return options;
}

export function groupTutorSlotsByDay(slots: TutorSlotOption[]): TutorSlotDayGroup[] {
  const groups = new Map<string, TutorSlotDayGroup>();

  for (const slot of slots) {
    const slotDate = slotDateFromDesiredStartTime(slot.desiredStartTime);
    if (!slotDate) continue;

    const dayKey = `${slotDate.getFullYear()}-${pad(slotDate.getMonth() + 1)}-${pad(slotDate.getDate())}`;
    const existing = groups.get(dayKey);
    if (existing) {
      existing.slots.push(slot);
    } else {
      groups.set(dayKey, {
        dayKey,
        dayLabel: formatDayLabel(slotDate),
        slots: [slot],
      });
    }
  }

  return Array.from(groups.values());
}
