import { getAppRouteForRole } from '../shared/auth/session';
import type { AuthSession, TutorRequestsPageData, TutorSubscriptionPageData } from '../shared/types';
import type { WorkspaceNotification } from '../shared/api/notifications';

export type WorkspaceSidebarCardViewModel = {
  title: string;
  meta?: string;
  text: string;
  actionLabel?: string;
  to?: string;
  pill?: string;
};

export const tutorSidebarRefreshEvent = 'tutorspace:tutor-sidebar-refresh';
export const profileChangedEvent = 'tutorspace:profile-changed';

export function getWorkspaceNotificationButtonLabel(unreadCount: number) {
  return unreadCount > 0 ? `Уведомления ${unreadCount}` : 'Уведомления';
}

export function getWorkspaceNotificationPanelState(notifications: WorkspaceNotification[], error: string | null) {
  if (error) {
    return {
      tone: 'error',
      title: 'Не удалось загрузить уведомления',
      text: error,
    };
  }

  if (notifications.length === 0) {
    return {
      tone: 'empty',
      title: 'Новых уведомлений нет',
      text: 'Когда появятся заявки, сообщения или переносы, они будут здесь.',
    };
  }

  return {
    tone: 'live',
    title: 'Последние уведомления',
    text: `${notifications.filter((notification) => !notification.read).length} непрочитанных`,
  };
}

export function getPublicHeaderViewModel(session: AuthSession | null) {
  if (!session) {
    return {
      isAuthenticated: false,
      dashboardTo: null,
      dashboardLabel: null,
    };
  }

  return {
    isAuthenticated: true,
    dashboardTo: getAppRouteForRole(session.user.role),
    dashboardLabel: 'Мой кабинет',
  };
}

export function canAcceptTutorRequest(status: string) {
  const normalized = status.trim();
  return normalized === 'Новая' || normalized === 'Просмотрена';
}

export function getTutorNextRequestSidebarCard(
  page: TutorRequestsPageData | null | undefined,
): WorkspaceSidebarCardViewModel {
  const requests = page?.requests ?? [];
  const nextRequest =
    requests.find((request) => request.stage === 'NEW' || request.stage === 'VIEWED') ??
    requests.find((request) => (
      request.stage === 'ACCEPTED' || request.stage === 'CONTACT_ESTABLISHED'
    ) && !request.scheduledLessonId) ??
    requests[0];

  if (!nextRequest) {
    return {
      title: 'Следующая заявка',
      meta: 'Очередь пуста',
      text: 'Новых заявок пока нет. Когда клиент отправит запрос, он появится здесь первым.',
      actionLabel: 'Открыть заявки',
      to: '/app/tutor/requests',
    };
  }

  const shouldSchedule =
    (nextRequest.stage === 'ACCEPTED' || nextRequest.stage === 'CONTACT_ESTABLISHED') &&
    !nextRequest.scheduledLessonId;
  const meta = nextRequest.deadline || nextRequest.preferredTime || nextRequest.status;
  const subject = nextRequest.subject ? `${nextRequest.subject}. ` : '';
  const actionHint = nextRequest.nextAction ? `${nextRequest.nextAction}.` : 'Откройте карточку и выберите следующий шаг.';

  return {
    title: 'Следующая заявка',
    meta,
    text: `${nextRequest.studentName}: ${subject}${actionHint}`,
    actionLabel: shouldSchedule ? 'Назначить урок' : 'Открыть заявку',
    to: shouldSchedule
      ? `/app/tutor/calendar?applicationId=${encodeURIComponent(nextRequest.id)}`
      : '/app/tutor/requests',
  };
}

export function getTutorSubscriptionSidebarCard(
  subscription: TutorSubscriptionPageData | null | undefined,
): WorkspaceSidebarCardViewModel {
  if (!subscription) {
    return {
      title: 'Подписка',
      text: 'Загрузка текущего тарифа. Если сервис подписок недоступен, откройте раздел подписки.',
      pill: 'проверить тариф',
      to: '/app/tutor/subscription',
    };
  }

  const expiresAt = subscription.plan.expiresAt.replace(/[.。]+$/, '');
  const isFreePlan = subscription.plan.name === 'Free' || subscription.plan.status === 'Не активна';

  return {
    title: 'Подписка',
    text: isFreePlan
      ? 'Free: подписка пока не активна. Выберите тариф, чтобы открыть CRM-период.'
      : `${subscription.plan.name}: ${subscription.plan.status}. Действует до: ${expiresAt}.`,
    pill: isFreePlan ? 'выбрать тариф' : `${subscription.plan.price} · ${subscription.plan.period}`,
    to: '/app/tutor/subscription',
  };
}
