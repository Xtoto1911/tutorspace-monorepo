import { describe, expect, it } from 'vitest';
import { buildTutorSlotOptions } from './tutor-profile-request-helpers';

describe('buildTutorSlotOptions', () => {
  it('turns tutor schedule labels into upcoming local date-time values', () => {
    const now = new Date(2026, 4, 14, 12, 0);

    expect(buildTutorSlotOptions(['Пн 18:30', 'Чт 18:00', 'Сб 11:00'], now)).toEqual([
      { id: 'slot-2026-05-18T18:30', label: 'Пн 18:30', desiredStartTime: '2026-05-18T18:30' },
      { id: 'slot-2026-05-14T18:00', label: 'Чт 18:00', desiredStartTime: '2026-05-14T18:00' },
      { id: 'slot-2026-05-16T11:00', label: 'Сб 11:00', desiredStartTime: '2026-05-16T11:00' },
    ]);
  });

  it('moves today slots to the next week when their time has already passed', () => {
    const now = new Date(2026, 4, 14, 19, 0);

    expect(buildTutorSlotOptions(['Чт 18:00'], now)).toEqual([
      { id: 'slot-2026-05-21T18:00', label: 'Чт 18:00', desiredStartTime: '2026-05-21T18:00' },
    ]);
  });
});
