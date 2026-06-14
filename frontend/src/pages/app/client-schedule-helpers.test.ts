import { describe, expect, it } from 'vitest';

import {
  getClientScheduleWeekRange,
  getClientRescheduleSuccessMessage,
  getClientScheduleMessageLink,
  getClientSchedulePrimaryLink,
  getClientScheduleUpcomingLessons,
  shiftClientScheduleWeekStart,
  shouldShowClientScheduleEmptyState,
} from './client-schedule-helpers';

describe('client schedule helpers', () => {
  it('normalizes any date to a Monday-to-Sunday range', () => {
    expect(getClientScheduleWeekRange(new Date('2026-06-17T12:00:00'))).toEqual({
      from: '2026-06-15',
      to: '2026-06-21',
    });
  });

  it('moves a selected week by exact seven-day increments', () => {
    expect(shiftClientScheduleWeekStart('2026-06-15', -1)).toBe('2026-06-08');
    expect(shiftClientScheduleWeekStart('2026-06-15', 1)).toBe('2026-06-22');
  });

  it('prefers application details when applicationId is present', () => {
    expect(getClientSchedulePrimaryLink({ applicationId: 'app-1', threadId: 'thread-1' })).toBe('/app/client/applications/app-1');
  });

  it('falls back to messages when only threadId is present', () => {
    expect(getClientSchedulePrimaryLink({ threadId: 'thread-1' })).toBe('/app/client/messages?threadId=thread-1');
  });

  it('builds a lesson-aware message link only when the thread is available', () => {
    expect(getClientScheduleMessageLink({ applicationId: 'app-1', lessonId: 'lesson-1', threadId: 'thread-1' })).toBe(
      '/app/client/messages?threadId=thread-1&applicationId=app-1&lessonId=lesson-1',
    );
    expect(getClientScheduleMessageLink({ applicationId: 'app-1', lessonId: 'lesson-1', threadId: null })).toBeNull();
  });

  it('keeps the message CTA data-driven when applicationId is absent', () => {
    expect(getClientScheduleMessageLink({ lessonId: 'lesson-1', threadId: 'thread-1' })).toBe(
      '/app/client/messages?threadId=thread-1&lessonId=lesson-1',
    );
  });

  it('shows the empty state only when there are no events, lessons, or reschedule requests', () => {
    expect(shouldShowClientScheduleEmptyState({ events: [], upcomingLessons: [], rescheduleRequests: [] })).toBe(true);
    expect(shouldShowClientScheduleEmptyState({ events: [{ id: 'evt-1' }], upcomingLessons: [], rescheduleRequests: [] })).toBe(false);
  });

  it('trusts backend empty state when the lesson exists outside the visible week', () => {
    expect(shouldShowClientScheduleEmptyState({
      events: [],
      upcomingLessons: [],
      rescheduleRequests: [],
      emptyState: { show: false },
    })).toBe(false);
  });

  it('derives the next lesson from live events when backend omits upcomingLessons', () => {
    expect(getClientScheduleUpcomingLessons({
      upcomingLessons: [],
      events: [
        {
          id: 'event-1',
          lessonId: 'lesson-1',
          applicationId: 'app-1',
          threadId: 'thread-1',
          title: 'Математика',
          meta: 'E2E Репетитор',
          time: '19:00',
          startsAt: '2026-05-27T19:00:00',
          canJoin: false,
          canReschedule: true,
          canMessageTutor: true,
        },
      ],
    })).toEqual([
      {
        id: 'event-1',
        lessonId: 'lesson-1',
        applicationId: 'app-1',
        threadId: 'thread-1',
        time: '19:00',
        title: 'Математика',
        details: 'E2E Репетитор',
        canJoin: false,
        canReschedule: true,
        canMessageTutor: true,
      },
    ]);
  });

  it('does not treat past non-actionable events as upcoming lessons', () => {
    expect(getClientScheduleUpcomingLessons({
      upcomingLessons: [],
      events: [
        {
          id: 'event-past',
          lessonId: 'lesson-past',
          applicationId: 'app-past',
          threadId: 'thread-past',
          title: 'Математика',
          meta: 'E2E Репетитор',
          time: '09:00',
          startsAt: '2026-05-27T09:00:00',
          canJoin: false,
          canReschedule: false,
          canMessageTutor: false,
        },
        {
          id: 'event-next',
          lessonId: 'lesson-next',
          applicationId: 'app-next',
          threadId: 'thread-next',
          title: 'Физика',
          meta: 'E2E Репетитор',
          time: '19:00',
          startsAt: '2026-05-27T19:00:00',
          canJoin: false,
          canReschedule: true,
          canMessageTutor: true,
        },
      ],
    }).map((lesson) => lesson.id)).toEqual(['event-next']);
  });

  it('builds visible feedback after a reschedule request is sent', () => {
    expect(getClientRescheduleSuccessMessage('19:00')).toBe('Запрос на перенос отправлен для урока 19:00. Статус появится в блоке переносов после синхронизации.');
  });
});
