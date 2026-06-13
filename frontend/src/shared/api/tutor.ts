import { apiRequest, liveApiRequest } from './http';
import { readAuthSession } from '../auth/session';
import type {
  TutorCalendarPageData,
  TutorRequestsPageData,
  TutorRequestStage,
  TutorMessagesPageData,
  TutorLessonCandidate,
  TutorOverviewPageData,
  TutorSettingsPageData,
  TutorSubscriptionPageData,
  TutorStudentsPageData,
  TutorWorkspaceProfilePageData,
  AvailabilitySchedule,
  SlotOverride,
  TutorCalendarSlot,
} from '../types';

type BackendPaymentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'FAILED';

type SubscriptionCurrentDto = {
  tutorUserId: string;
  active: boolean;
  tariffCode: string | null;
  activeUntil: string | null;
};

type SubscriptionTariffDto = {
  id: string;
  code: string;
  name: string;
  priceRub: number;
  durationDays: number;
  active: boolean;
};

export type TutorSubscriptionCheckoutResponse = {
  paymentId: string;
  confirmationUrl: string;
  status: BackendPaymentStatus;
};

export type TutorSubscriptionActivationResponse = {
  subscriptionId?: string;
  paymentId?: string;
  periodId?: string;
  tutorUserId?: string;
  tariffCode?: string;
  activeUntil?: string;
} | null;

export type TutorCheckoutPageResponse = {
  paymentId: string;
  tariffCode: string;
  tariffName: string;
  amountRub: number;
  currency: string;
  durationDays: number;
  status: BackendPaymentStatus;
  providerStatus: string | null;
  confirmationUrl: string;
  testMode: boolean;
};

function requireTutorUserId() {
  const session = readAuthSession();

  if (!session?.user.id) {
    throw new Error('Не удалось определить репетитора для подписки.');
  }

  return session.user.id;
}

function formatRub(amount: number) {
  return new Intl.NumberFormat('ru-RU').format(amount).replace(/\u00a0/g, ' ') + ' ₽';
}

function formatSubscriptionDate(value: string | null) {
  if (!value) {
    return 'Не оплачена';
  }

  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(new Date(value));
}

function tariffFeatures(tariffCode: string) {
  if (tariffCode === 'CRM_PREMIUM') {
    return ['Максимум заявок', 'Расширенная аналитика профиля', 'Приоритет в каталоге'];
  }

  if (tariffCode === 'CRM_PRO') {
    return ['Больше заявок', 'Аналитика профиля', 'Выше видимость в каталоге'];
  }

  return ['Базовая CRM', 'Заявки из каталога', 'Календарь занятий'];
}

function buildSubscriptionPage(
  current: SubscriptionCurrentDto,
  tariffs: SubscriptionTariffDto[],
): TutorSubscriptionPageData {
  const activeTariffs = tariffs.filter((tariff) => tariff.active);
  const currentTariff =
    activeTariffs.find((tariff) => tariff.code === current.tariffCode) ??
    activeTariffs[0] ??
    null;
  const planName = current.active && currentTariff ? currentTariff.name : 'Free';
  const planPrice = current.active && currentTariff ? formatRub(currentTariff.priceRub) : '0 ₽';

  return {
    plan: {
      name: planName,
      status: current.active ? 'Активна' : 'Не активна',
      price: planPrice,
      period: currentTariff ? `${currentTariff.durationDays} дней` : 'без оплаты',
      expiresAt: formatSubscriptionDate(current.activeUntil),
      nextPayment: current.activeUntil ? formatSubscriptionDate(current.activeUntil) : 'После оплаты тарифа',
      usagePercent: current.active ? 35 : 0,
    },
    metrics: [
      {
        icon: '₽',
        value: current.active ? 'Оплачено' : 'Sandbox',
        label: 'ЮKassa',
        sublabel: current.active ? 'Платёж подтверждён' : 'Готово к тестовой оплате',
        tone: current.active ? 'emerald' : 'indigo',
      },
      {
        icon: 'CRM',
        value: current.active ? 'Включена' : 'Ожидает',
        label: 'Подписка CRM',
        sublabel: 'Оплата открывает период тарифа',
        tone: current.active ? 'cyan' : 'amber',
      },
    ],
    features: [
      {
        title: 'Платёж через ЮKassa',
        text: 'Checkout создаётся в тестовом магазине, а статус приходит через webhook или проверку оплаты.',
        enabled: true,
      },
      {
        title: 'Период подписки',
        text: current.active ? `Действует до ${formatSubscriptionDate(current.activeUntil)}.` : 'Активируется после успешной оплаты.',
        enabled: current.active,
      },
    ],
    tariffs: activeTariffs.map((tariff) => ({
      id: tariff.code,
      name: tariff.name,
      price: formatRub(tariff.priceRub),
      period: `${tariff.durationDays} дней`,
      description: 'CRM-подписка TutorSpace для работы с заявками и учениками.',
      badge: tariff.code === 'CRM_PRO' ? 'Оптимальный' : undefined,
      current: current.active && current.tariffCode === tariff.code,
      features: tariffFeatures(tariff.code),
    })),
    history: current.active && currentTariff ? [
      {
        date: formatSubscriptionDate(current.activeUntil),
        title: currentTariff.name,
        amount: formatRub(currentTariff.priceRub),
        status: 'Активирована через ЮKassa sandbox',
      },
    ] : [],
    recommendation: {
      title: 'Тестовый платёж',
      text: 'Для дипломной демонстрации используйте тестовый магазин ЮKassa: API и checkout настоящие, деньги не списываются.',
      action: current.active ? 'Продлить тариф' : 'Выбрать тариф',
    },
  };
}

export function getTutorDashboard() {
  return apiRequest<TutorOverviewPageData>('/tutor/dashboard');
}

export function getTutorRequests() {
  return apiRequest<TutorRequestsPageData>('/tutor/requests');
}

export function getTutorLessonCandidates() {
  return apiRequest<TutorLessonCandidate[]>('/tutor/lesson-candidates');
}

export function acceptTutorRequest(requestId: string) {
  return apiRequest<TutorRequestsPageData>(`/tutor/requests/${requestId}/accept`, { method: 'PATCH' });
}

export function rejectTutorRequest(requestId: string, reason = 'Не подходит по параметрам') {
  return apiRequest<TutorRequestsPageData>(`/tutor/requests/${requestId}/reject`, {
    method: 'PATCH',
    body: JSON.stringify({ reason }),
  });
}

export function updateTutorRequestStage(requestId: string, status: TutorRequestStage, reason?: string) {
  return apiRequest<TutorRequestsPageData>(`/tutor/requests/${requestId}/stage`, {
    method: 'PATCH',
    body: JSON.stringify({ status, reason }),
  });
}

export function getTutorCalendar(from?: string, to?: string) {
  const qs = from && to ? `?from=${from}&to=${to}` : '';
  return apiRequest<TutorCalendarPageData>(`/tutor/calendar${qs}`);
}

export function createTutorLesson(payload?: {
  applicationId?: string | null;
  studentId?: string;
  startsAt?: string;
  durationMinutes?: number;
  slot?: { date: string; startTime: string; durationMinutes: number } | null;
  studentName?: string;
  subject?: string;
  format?: string;
  recurrence?: { type: 'NONE' | 'WEEKLY'; occurrences: number };
}) {
  return apiRequest<{ id: string; status: string }>(
    '/tutor/lessons',
    {
      method: 'POST',
      body: JSON.stringify(payload || {}),
    },
  );
}

export function rescheduleTutorLesson(
  lessonId: string,
  payload: { startsAt: string; scope: 'SINGLE' | 'SERIES' },
) {
  return apiRequest<{ id: string; status: string }>(
    `/tutor/lessons/${lessonId}/reschedule`,
    {
      method: 'PATCH',
      body: JSON.stringify(payload),
    },
  );
}

export function getTutorStudents() {
  return apiRequest<TutorStudentsPageData>('/tutor/students');
}

export function createTutorStudent(payload: {
  name: string;
  details: string;
  subject: string;
  note?: string;
}) {
  return apiRequest('/tutor/students', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function createParentReport(
  studentId: string,
  payload: { title: string; summary: string; progress: number },
) {
  return apiRequest(`/tutor/students/${studentId}/reports`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getTutorWorkspaceProfile() {
  return liveApiRequest<TutorWorkspaceProfilePageData>('/tutor/profile');
}

export function updateTutorWorkspaceProfile(payload: Partial<TutorWorkspaceProfilePageData>) {
  return liveApiRequest<TutorWorkspaceProfilePageData>('/tutor/profile', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function getTutorMessages(threadId?: string | null) {
  const path = threadId ? `/tutor/messages?threadId=${encodeURIComponent(threadId)}` : '/tutor/messages';
  return apiRequest<TutorMessagesPageData>(path);
}

export function sendTutorMessage(threadId: string, text: string) {
  return apiRequest<{ id: string; threadId: string; text: string; status: 'sent' }>(`/tutor/messages/${threadId}`, {
    method: 'POST',
    body: JSON.stringify({ text }),
  });
}

export function getTutorSubscription() {
  const tutorUserId = requireTutorUserId();

  return Promise.all([
    apiRequest<SubscriptionCurrentDto>(
      `/subscriptions/current?tutorUserId=${encodeURIComponent(tutorUserId)}`,
    ),
    apiRequest<SubscriptionTariffDto[]>('/subscriptions/tariffs'),
  ]).then(([current, tariffs]) => buildSubscriptionPage(current, tariffs));
}

export function createTutorSubscriptionCheckout(tariffCode = 'CRM_PRO') {
  const tutorUserId = requireTutorUserId();

  return apiRequest<TutorSubscriptionCheckoutResponse>('/subscriptions/checkout', {
    method: 'POST',
    body: JSON.stringify({ tutorUserId, tariffCode }),
  });
}

export function getTutorCheckoutPage(paymentId: string) {
  return apiRequest<TutorCheckoutPageResponse>(`/subscriptions/checkout/${paymentId}`);
}

export function confirmTutorTariffPayment(checkoutId = 'checkout-demo') {
  return refreshTutorSubscriptionPayment(checkoutId);
}

export function refreshTutorSubscriptionPayment(paymentId: string) {
  return apiRequest<TutorSubscriptionActivationResponse>(
    `/subscriptions/payments/${paymentId}/refresh`,
    { method: 'POST' },
  );
}

export function getTutorSettings() {
  return liveApiRequest<TutorSettingsPageData>('/tutor/settings');
}

export function updateTutorSettings(payload: Partial<TutorSettingsPageData>) {
  return liveApiRequest<TutorSettingsPageData>('/tutor/settings', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function getTutorAvailabilitySchedules() {
  return apiRequest<AvailabilitySchedule[]>('/tutor/availability-schedules');
}

export function createTutorAvailabilitySchedule(payload: Omit<AvailabilitySchedule, 'id'>) {
  return apiRequest<AvailabilitySchedule>('/tutor/availability-schedules', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateTutorAvailabilitySchedule(id: string, payload: Partial<AvailabilitySchedule>) {
  return apiRequest<AvailabilitySchedule>(`/tutor/availability-schedules/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function deleteTutorAvailabilitySchedule(id: string) {
  return apiRequest<void>(`/tutor/availability-schedules/${id}`, { method: 'DELETE' });
}

export function getTutorSlotOverrides(from: string, to: string) {
  return apiRequest<SlotOverride[]>(`/tutor/slot-overrides?from=${from}&to=${to}`);
}

export function createTutorSlotOverride(payload: Omit<SlotOverride, 'id'>) {
  return apiRequest<SlotOverride>('/tutor/slot-overrides', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function deleteTutorSlotOverride(id: string) {
  return apiRequest<void>(`/tutor/slot-overrides/${id}`, { method: 'DELETE' });
}

export function getTutorCalendarSlots(from: string, to: string) {
  return apiRequest<TutorCalendarSlot[]>(`/tutor/calendar/slots?from=${from}&to=${to}`);
}
