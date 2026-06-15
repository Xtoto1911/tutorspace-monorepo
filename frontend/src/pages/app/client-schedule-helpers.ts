function formatLocalIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseLocalIsoDate(value: string) {
  return new Date(`${value}T12:00:00`);
}

export function getClientScheduleWeekRange(date: Date) {
  const weekStart = new Date(date);
  const day = weekStart.getDay();
  const daysSinceMonday = day === 0 ? 6 : day - 1;
  weekStart.setDate(weekStart.getDate() - daysSinceMonday);

  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekEnd.getDate() + 6);

  return {
    from: formatLocalIsoDate(weekStart),
    to: formatLocalIsoDate(weekEnd),
  };
}

export function shiftClientScheduleWeekStart(weekStart: string, weekOffset: number) {
  const shifted = parseLocalIsoDate(weekStart);
  shifted.setDate(shifted.getDate() + weekOffset * 7);
  return formatLocalIsoDate(shifted);
}

export function getClientSchedulePrimaryLink(input: { applicationId?: string | null; threadId?: string | null }) {
  if (input.applicationId) {
    return `/app/client/applications/${input.applicationId}`;
  }

  if (input.threadId) {
    return `/app/client/messages?threadId=${encodeURIComponent(input.threadId)}`;
  }

  return '/app/client/applications';
}

export function getClientScheduleMessageLink(input: {
  applicationId?: string | null;
  lessonId: string;
  threadId?: string | null;
}) {
  if (!input.threadId) {
    return null;
  }

  const searchParams = new URLSearchParams({
    threadId: input.threadId,
  });

  if (input.applicationId) {
    searchParams.set('applicationId', input.applicationId);
  }

  searchParams.set('lessonId', input.lessonId);

  return `/app/client/messages?${searchParams.toString()}`;
}

export function getClientRescheduleSuccessMessage(lessonTime: string) {
  return `Запрос на перенос отправлен для урока ${lessonTime}. Статус появится в блоке переносов после синхронизации.`;
}

export function shouldShowClientScheduleEmptyState(input: {
  events: Array<{ id: string }>;
  upcomingLessons: Array<{ id: string }>;
  rescheduleRequests: Array<{ id: string }>;
  emptyState?: { show: boolean };
}) {
  if (input.emptyState) {
    return input.emptyState.show;
  }
  return input.events.length === 0 && input.upcomingLessons.length === 0 && input.rescheduleRequests.length === 0;
}

export function getClientScheduleUpcomingLessons(input: {
  upcomingLessons: Array<{
    id: string;
    lessonId: string;
    applicationId?: string | null;
    threadId?: string | null;
    time: string;
    title: string;
    details: string;
    canJoin: boolean;
    canReschedule: boolean;
    canMessageTutor: boolean;
  }>;
  events: Array<{
    id: string;
    lessonId: string;
    applicationId?: string | null;
    threadId?: string | null;
    title: string;
    meta: string;
    time: string;
    startsAt: string;
    canJoin: boolean;
    canReschedule: boolean;
    canMessageTutor: boolean;
  }>;
}) {
  if (input.upcomingLessons.length > 0) {
    return input.upcomingLessons;
  }

  return [...input.events]
    .filter((event) => event.canJoin || event.canReschedule || event.canMessageTutor)
    .sort((left, right) => new Date(left.startsAt).getTime() - new Date(right.startsAt).getTime())
    .slice(0, 2)
    .map((event) => ({
      id: event.id,
      lessonId: event.lessonId,
      applicationId: event.applicationId,
      threadId: event.threadId,
      time: event.time,
      title: event.title,
      details: event.meta,
      canJoin: event.canJoin,
      canReschedule: event.canReschedule,
      canMessageTutor: event.canMessageTutor,
    }));
}
