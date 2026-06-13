import type { TutorCalendarPageData, TutorRequestsPageData } from '../../shared/types';

type TutorRequest = TutorRequestsPageData['requests'][number];

export type TutorRequestTabKey = 'new' | 'funnel' | 'trial' | 'students' | 'closed';

export type TutorRequestPrimaryAction = {
  kind: 'accept' | 'schedule' | 'message' | 'none';
  label: string;
  href?: string;
};

const tabLabels: Record<TutorRequestTabKey, string> = {
  new: 'Нужно ответить',
  funnel: 'В работе',
  trial: 'Пробные',
  students: 'Ученики',
  closed: 'Закрытые',
};

export function getTutorRequestTabLabel(tab: TutorRequestTabKey) {
  return tabLabels[tab];
}

export function getTutorRequestPrimaryAction(request: TutorRequest): TutorRequestPrimaryAction {
  if (request.stage === 'NEW' || request.stage === 'VIEWED') {
    return { kind: 'accept', label: 'Принять заявку' };
  }

  if ((request.stage === 'ACCEPTED' || request.stage === 'CONTACT_ESTABLISHED') && !request.scheduledLessonId) {
    return {
      kind: 'schedule',
      label: 'Назначить пробный урок',
      href: `/app/tutor/calendar?applicationId=${encodeURIComponent(request.id)}`,
    };
  }

  if (request.threadId && !['REJECTED', 'CANCELLED', 'ARCHIVED'].includes(request.stage ?? '')) {
    return {
      kind: 'message',
      label: 'Написать клиенту',
      href: `/app/tutor/messages?threadId=${encodeURIComponent(request.threadId)}`,
    };
  }

  return { kind: 'none', label: 'Нет действия' };
}

export function getCalendarWorkdayEmptyState(pageData: TutorCalendarPageData) {
  const pendingCount = pageData.pendingRequests?.length ?? 0;
  if (pageData.events.length > 0 || pageData.slots.length > 0) {
    return null;
  }

  if (pendingCount > 0) {
    return {
      title: 'Есть заявки без урока',
      text: `Назначьте пробный урок для ${pendingCount} принятой заявки.`,
      action: 'Показать заявки',
    };
  }

  return {
    title: 'Неделя пока пустая',
    text: 'Добавьте урок вручную или откройте свободные окна для новых заявок.',
    action: 'Добавить занятие',
  };
}
