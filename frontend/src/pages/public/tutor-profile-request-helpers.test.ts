import { describe, expect, it } from 'vitest';
import {
  buildTutorSlotOptions,
  groupTutorSlotsByDay,
} from './tutor-profile-request-helpers';

describe('buildTutorSlotOptions', () => {
  it('turns tutor schedule labels into upcoming local date-time values', () => {
    const now = new Date(2026, 4, 14, 12, 0);

    expect(buildTutorSlotOptions(['Пн 18:30', 'Чт 18:00', 'Сб 11:00'], now)).toEqual([
      { id: 'slot-2026-05-18T18:30', label: 'Понедельник, 18 мая, 18:30', desiredStartTime: '2026-05-18T18:30' },
      { id: 'slot-2026-05-14T18:00', label: 'Четверг, 14 мая, 18:00', desiredStartTime: '2026-05-14T18:00' },
      { id: 'slot-2026-05-16T11:00', label: 'Суббота, 16 мая, 11:00', desiredStartTime: '2026-05-16T11:00' },
    ]);
  });

  it('moves today slots to the next week when their time has already passed', () => {
    const now = new Date(2026, 4, 14, 19, 0);

    expect(buildTutorSlotOptions(['Чт 18:00'], now)).toEqual([
      { id: 'slot-2026-05-21T18:00', label: 'Четверг, 21 мая, 18:00', desiredStartTime: '2026-05-21T18:00' },
    ]);
  });

  it('deduplicates slots that resolve to the same date-time', () => {
    const now = new Date(2026, 4, 14, 12, 0);

    const result = buildTutorSlotOptions(
      ['Пн 09:00', 'Пн 09:00', 'Пн 10:00', 'Ср 09:00', 'Ср 09:00'],
      now,
    );

    expect(result).toEqual([
      { id: 'slot-2026-05-18T09:00', label: 'Понедельник, 18 мая, 09:00', desiredStartTime: '2026-05-18T09:00' },
      { id: 'slot-2026-05-18T10:00', label: 'Понедельник, 18 мая, 10:00', desiredStartTime: '2026-05-18T10:00' },
      { id: 'slot-2026-05-20T09:00', label: 'Среда, 20 мая, 09:00', desiredStartTime: '2026-05-20T09:00' },
    ]);
  });
});

describe('groupTutorSlotsByDay', () => {
  it('groups slots by calendar day and keeps time order', () => {
    const now = new Date(2026, 4, 14, 12, 0);
    const slots = buildTutorSlotOptions(
      ['Пн 09:00', 'Пн 10:00', 'Ср 09:00', 'Пн 09:00'],
      now,
    );

    const groups = groupTutorSlotsByDay(slots);

    expect(groups.map((g) => g.dayKey)).toEqual(['2026-05-18', '2026-05-20']);
    expect(groups[0].dayLabel).toBe('Понедельник, 18 мая');
    expect(groups[0].slots.map((s) => s.id)).toEqual([
      'slot-2026-05-18T09:00',
      'slot-2026-05-18T10:00',
    ]);
    expect(groups[1].slots.map((s) => s.id)).toEqual(['slot-2026-05-20T09:00']);
  });
});
