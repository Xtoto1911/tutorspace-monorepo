import { describe, expect, it } from 'vitest';

import type { AuthSession } from '../shared/types';
import {
  canAcceptTutorRequest,
  getWorkspaceNotificationButtonLabel,
  getWorkspaceNotificationPanelState,
  getPublicHeaderViewModel,
  getTutorNextRequestSidebarCard,
  getTutorSubscriptionSidebarCard,
  tutorSidebarRefreshEvent,
} from './layout-helpers';

describe('getPublicHeaderViewModel', () => {
  it('returns guest actions when there is no session', () => {
    expect(getPublicHeaderViewModel(null)).toEqual({
      isAuthenticated: false,
      dashboardTo: null,
      dashboardLabel: null,
    });
  });

  it('returns dashboard action for an authenticated session', () => {
    const session: AuthSession = {
      token: 'token',
      refreshToken: 'refresh',
      user: {
        id: 'user-1',
        name: 'E2E Client',
        email: 'client@example.com',
        role: 'parent',
      },
    };

    expect(getPublicHeaderViewModel(session)).toEqual({
      isAuthenticated: true,
      dashboardTo: '/app/client',
      dashboardLabel: 'Мой кабинет',
    });
  });
});

describe('canAcceptTutorRequest', () => {
  it('allows accept action for new and viewed requests', () => {
    expect(canAcceptTutorRequest('Новая')).toBe(true);
    expect(canAcceptTutorRequest('Просмотрена')).toBe(true);
    expect(canAcceptTutorRequest('Принята')).toBe(false);
    expect(canAcceptTutorRequest('Отклонена')).toBe(false);
    expect(canAcceptTutorRequest('Отменена')).toBe(false);
  });
});

describe('workspace notifications helpers', () => {
  it('shows unread count in the topbar label only when needed', () => {
    expect(getWorkspaceNotificationButtonLabel(0)).toBe('Уведомления');
    expect(getWorkspaceNotificationButtonLabel(3)).toBe('Уведомления 3');
  });

  it('returns a calm empty state when there are no notifications', () => {
    expect(getWorkspaceNotificationPanelState([], null)).toEqual({
      tone: 'empty',
      title: 'Новых уведомлений нет',
      text: 'Когда появятся заявки, сообщения или переносы, они будут здесь.',
    });
  });

  it('counts unread notifications for live state', () => {
    expect(getWorkspaceNotificationPanelState([
      {
        id: 'n-1',
        eventType: 'REQUEST_CREATED',
        title: 'Новая заявка',
        message: 'Клиент отправил заявку.',
        read: false,
        createdAt: '2026-05-28T12:00:00',
        actionUrl: '/app/tutor/requests',
      },
      {
        id: 'n-2',
        eventType: 'CLIENT_MESSAGE_SENT',
        title: 'Новое сообщение',
        message: 'Клиент написал.',
        read: true,
        createdAt: '2026-05-28T12:05:00',
        actionUrl: '/app/tutor/messages?threadId=thread-1',
      },
    ], null)).toEqual({
      tone: 'live',
      title: 'Последние уведомления',
      text: '1 непрочитанных',
    });
  });
});

describe('tutor workspace sidebar cards', () => {
  it('uses a stable event name for refreshing tutor sidebar live data', () => {
    expect(tutorSidebarRefreshEvent).toBe('tutorspace:tutor-sidebar-refresh');
  });

  it('shows the next live request and routes accepted requests to scheduling', () => {
    const card = getTutorNextRequestSidebarCard({
      stats: [],
      requests: [
        {
          id: 'request-1',
          initials: 'АК',
          studentName: 'Артем Ким',
          gradeGoal: '8 класс',
          subject: 'Математика',
          status: 'Принята',
          description: 'Подготовка к ОГЭ',
          price: '1500 ₽',
          format: 'Онлайн',
          preferredTime: 'Сегодня, 18:00',
          tone: 'emerald',
          stage: 'ACCEPTED',
          nextAction: 'Назначить пробный урок',
        },
      ],
      today: { title: '', summary: '', events: [] },
      quality: { label: '', value: '', text: '', percent: 0 },
    });

    expect(card).toEqual({
      title: 'Следующая заявка',
      meta: 'Сегодня, 18:00',
      text: 'Артем Ким: Математика. Назначить пробный урок.',
      actionLabel: 'Назначить урок',
      to: '/app/tutor/calendar?applicationId=request-1',
    });
  });

  it('shows a calm empty state when there are no tutor requests', () => {
    expect(getTutorNextRequestSidebarCard(null)).toMatchObject({
      meta: 'Очередь пуста',
      actionLabel: 'Открыть заявки',
      to: '/app/tutor/requests',
    });
  });

  it('summarizes the current subscription instead of static copy', () => {
    const card = getTutorSubscriptionSidebarCard({
      plan: {
        name: 'CRM Pro',
        status: 'Активна',
        price: '1990 ₽',
        period: '30 дней',
        expiresAt: '20 июня 2026 г.',
        nextPayment: '20 июня 2026 г.',
        usagePercent: 35,
      },
      metrics: [],
      features: [],
      tariffs: [],
      history: [],
      recommendation: { title: '', text: '', action: '' },
    });

    expect(card).toEqual({
      title: 'Подписка',
      text: 'CRM Pro: Активна. Действует до: 20 июня 2026 г.',
      pill: '1990 ₽ · 30 дней',
      to: '/app/tutor/subscription',
    });
  });

  it('does not show paid period copy for inactive Free subscription', () => {
    const card = getTutorSubscriptionSidebarCard({
      plan: {
        name: 'Free',
        status: 'Не активна',
        price: '0 ₽',
        period: '30 дней',
        expiresAt: 'Не оплачена',
        nextPayment: 'После оплаты тарифа',
        usagePercent: 0,
      },
      metrics: [],
      features: [],
      tariffs: [],
      history: [],
      recommendation: { title: '', text: '', action: '' },
    });

    expect(card).toEqual({
      title: 'Подписка',
      text: 'Free: подписка пока не активна. Выберите тариф, чтобы открыть CRM-период.',
      pill: 'выбрать тариф',
      to: '/app/tutor/subscription',
    });
  });
});
