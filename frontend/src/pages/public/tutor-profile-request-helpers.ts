export type TutorSlotOption = {
  id: string;
  label: string;
  desiredStartTime: string;
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

export function buildTutorSlotOptions(schedule: string[], now = new Date()): TutorSlotOption[] {
  return schedule.flatMap((label) => {
    const match = label.trim().match(/^(Пн|Вт|Ср|Чт|Пт|Сб|Вс)\s+(\d{1,2}):(\d{2})$/);

    if (!match) {
      return [];
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

    return [{
      id: `slot-${desiredStartTime}`,
      label,
      desiredStartTime,
    }];
  });
}
