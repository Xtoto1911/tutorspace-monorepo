import { describe, expect, it } from 'vitest';

import {
  getCalendarWorkdayEmptyState,
  getTutorRequestPrimaryAction,
  getTutorRequestTabLabel,
} from './tutor-workday-helpers';
import type { TutorCalendarPageData, TutorRequestsPageData } from '../../shared/types';

const acceptedRequest: TutorRequestsPageData['requests'][number] = {
  id: 'request-1',
  initials: 'ИП',
  studentName: 'Иван Петров',
  gradeGoal: '9 класс · ОГЭ',
  subject: 'Математика',
  status: 'Принята',
  description: 'Хочу начать на этой неделе.',
  price: '1 500 ₽',
  format: 'Онлайн',
  preferredTime: '16:00',
  tone: 'emerald',
  stage: 'ACCEPTED',
  source: 'MARKETPLACE',
  deadline: 'сегодня 18:00',
  overdue: false,
  leadScore: 80,
  nextAction: 'Назначить пробный урок',
  threadId: 'thread-1',
};

describe('tutor workday helpers', () => {
  it('uses workday tab labels for request stages', () => {
    expect(getTutorRequestTabLabel('new')).toBe('Нужно ответить');
    expect(getTutorRequestTabLabel('funnel')).toBe('В работе');
  });

  it('makes accepted requests a scheduling task first', () => {
    expect(getTutorRequestPrimaryAction(acceptedRequest)).toEqual({
      kind: 'schedule',
      label: 'Назначить пробный урок',
      href: '/app/tutor/calendar?applicationId=request-1',
    });
  });

  it('uses message as the primary action after a trial is scheduled', () => {
    expect(
      getTutorRequestPrimaryAction({
        ...acceptedRequest,
        stage: 'TRIAL_SCHEDULED',
        scheduledLessonId: 'lesson-1',
        scheduledLessonAt: '2026-05-08T16:00:00',
      }),
    ).toEqual({
      kind: 'message',
      label: 'Написать клиенту',
      href: '/app/tutor/messages?threadId=thread-1',
    });
  });

  it('explains that accepted requests are waiting for scheduling', () => {
    const page: TutorCalendarPageData = {
      stats: [],
      weekDays: [],
      timeSlots: [],
      events: [],
      nextLesson: {
        time: 'Пока пусто',
        title: 'Назначьте первый урок',
        description: 'После принятия заявки создайте занятие.',
      },
      upcomingEvents: [],
      monthDays: [],
      slots: [],
      pendingRequests: [
        {
          applicationId: 'request-1',
          studentId: 'student-1',
          studentName: 'Иван Петров',
          subject: 'Математика',
          desiredStartTime: '16:00',
          gradeGoal: '9 класс · ОГЭ',
          threadId: 'thread-1',
        },
      ],
    };

    expect(getCalendarWorkdayEmptyState(page)).toEqual({
      title: 'Есть заявки без урока',
      text: 'Назначьте пробный урок для 1 принятой заявки.',
      action: 'Показать заявки',
    });
  });
});
