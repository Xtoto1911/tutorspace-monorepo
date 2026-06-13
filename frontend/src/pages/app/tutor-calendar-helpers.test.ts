import { describe, expect, it } from 'vitest';

import {
  buildCalendarWeekDays,
  buildCellLessonPayload,
  buildLessonReschedulePayload,
  buildSlotLessonPayload,
  formatLessonRequestStartTime,
  getLessonModalDefaults,
  getTutorCalendarDisplayStats,
  isCalendarCellInPast,
} from './tutor-calendar-helpers';

describe('tutor calendar helpers', () => {
  it('builds a full Monday to Sunday week for the tutor calendar', () => {
    const week = buildCalendarWeekDays(new Date('2026-05-11T00:00:00'), new Date('2026-05-14T12:00:00'));

    expect(week).toEqual([
      { day: 'Пн', date: '11', isoDate: '2026-05-11', active: false },
      { day: 'Вт', date: '12', isoDate: '2026-05-12', active: false },
      { day: 'Ср', date: '13', isoDate: '2026-05-13', active: false },
      { day: 'Чт', date: '14', isoDate: '2026-05-14', active: true },
      { day: 'Пт', date: '15', isoDate: '2026-05-15', active: false },
      { day: 'Сб', date: '16', isoDate: '2026-05-16', active: false },
      { day: 'Вс', date: '17', isoDate: '2026-05-17', active: false },
    ]);
  });

  it('marks only past calendar cells as unavailable for booking', () => {
    const now = new Date('2026-05-14T12:30:00');

    expect(isCalendarCellInPast('2026-05-13', '20:00', now)).toBe(true);
    expect(isCalendarCellInPast('2026-05-14', '12:00', now)).toBe(true);
    expect(isCalendarCellInPast('2026-05-14', '14:00', now)).toBe(false);
    expect(isCalendarCellInPast('2026-05-15', '09:00', now)).toBe(false);
  });

  it('builds the platform cell lesson payload exactly', () => {
    expect(
      buildCellLessonPayload({
        mode: 'platform',
        applicationId: 'app-1',
        date: '2026-05-07',
        time: '09:30',
        durationMinutes: 60,
      }),
    ).toEqual({
      applicationId: 'app-1',
      startsAt: '2026-05-07T09:30:00',
      durationMinutes: 60,
      slot: null,
    });
  });

  it('builds the manual cell lesson payload exactly', () => {
    expect(
      buildCellLessonPayload({
        mode: 'manual',
        date: '2026-05-07',
        time: '14:00',
        durationMinutes: 45,
        studentName: 'Иван Иванов',
        subject: 'Математика',
        format: 'Онлайн',
      }),
    ).toEqual({
      applicationId: null,
      startsAt: '2026-05-07T14:00:00',
      durationMinutes: 45,
      studentName: 'Иван Иванов',
      subject: 'Математика',
      format: 'Онлайн',
    });
  });

  it('builds a recurring lesson payload for an existing student', () => {
    expect(
      buildCellLessonPayload({
        mode: 'manual',
        studentId: 'student-1',
        date: '2026-05-07',
        time: '14:00',
        durationMinutes: 45,
        studentName: '',
        subject: 'Физика',
        format: 'Онлайн',
        recurrence: { type: 'WEEKLY', occurrences: 8 },
      }),
    ).toEqual({
      applicationId: null,
      studentId: 'student-1',
      startsAt: '2026-05-07T14:00:00',
      durationMinutes: 45,
      studentName: undefined,
      subject: 'Физика',
      format: 'Онлайн',
      recurrence: { type: 'WEEKLY', occurrences: 8 },
    });
  });

  it('builds lesson reschedule payload for single lesson and series', () => {
    expect(buildLessonReschedulePayload('2026-05-08', '15:30', 'SINGLE')).toEqual({
      startsAt: '2026-05-08T15:30:00',
      scope: 'SINGLE',
    });
    expect(buildLessonReschedulePayload('2026-05-08', '15:30', 'SERIES')).toEqual({
      startsAt: '2026-05-08T15:30:00',
      scope: 'SERIES',
    });
  });

  it('builds the platform slot lesson payload exactly', () => {
    expect(
      buildSlotLessonPayload({
        mode: 'platform',
        applicationId: 'app-2',
        slotDate: '2026-05-08',
        slotStartTime: '10:00',
        slotEndTime: '11:30',
      }),
    ).toEqual({
      applicationId: 'app-2',
      startsAt: '2026-05-08T10:00:00',
      durationMinutes: 90,
      slot: {
        date: '2026-05-08',
        startTime: '10:00',
        durationMinutes: 90,
      },
    });
  });

  it('builds the manual slot lesson payload exactly', () => {
    expect(
      buildSlotLessonPayload({
        mode: 'manual',
        slotDate: '2026-05-08',
        slotStartTime: '15:15',
        slotEndTime: '16:00',
        studentName: 'Мария Смирнова',
        subject: 'Физика',
        format: 'Офлайн',
      }),
    ).toEqual({
      applicationId: null,
      startsAt: '2026-05-08T15:15:00',
      durationMinutes: 45,
      studentName: 'Мария Смирнова',
      subject: 'Физика',
      format: 'Офлайн',
    });
  });

  it('shows the live available slot count instead of stale backend stats', () => {
    const stats = getTutorCalendarDisplayStats({
      stats: [
        { icon: 'С', value: '6', label: 'Окна', sublabel: 'окон', tone: 'emerald' },
        { icon: 'Н', value: '2', label: 'Неделя', sublabel: 'урока', tone: 'cyan' },
      ],
      slots: [
        { date: '2026-05-28', startTime: '19:00', endTime: '20:00', available: true, source: 'EXTRA' },
        { date: '2026-05-29', startTime: '19:00', endTime: '20:00', available: true, source: 'RULE' },
        { date: '2026-05-30', startTime: '19:00', endTime: '20:00', available: false, source: 'RULE' },
      ],
    });

    expect(stats[0]).toMatchObject({ label: 'Окна', value: '2' });
  });

  it('prefills lesson modal date and time from the selected accepted request', () => {
    expect(getLessonModalDefaults({
      fallbackDate: '2026-05-27',
      selectedCell: null,
      selectedLessonRequest: {
        desiredStartTime: '2026-05-29T18:00',
        durationMinutes: 90,
      },
    })).toEqual({
      date: '2026-05-29',
      time: '18:00',
      durationMinutes: 90,
    });
  });

  it('keeps calendar cell selection above request defaults', () => {
    expect(getLessonModalDefaults({
      fallbackDate: '2026-05-27',
      selectedCell: { date: '2026-05-28', time: '14:00' },
      selectedLessonRequest: {
        desiredStartTime: '2026-05-29T18:00',
      },
    })).toEqual({
      date: '2026-05-28',
      time: '14:00',
      durationMinutes: 60,
    });
  });

  it('formats request start time without exposing raw ISO text', () => {
    expect(formatLessonRequestStartTime('2026-05-29T18:00')).toBe('29.05, 18:00');
    expect(formatLessonRequestStartTime('18:00')).toBe('18:00');
  });
});
