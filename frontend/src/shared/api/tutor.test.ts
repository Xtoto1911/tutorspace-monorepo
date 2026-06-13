import { beforeEach, describe, expect, it, vi } from 'vitest';

const { apiRequest } = vi.hoisted(() => ({
  apiRequest: vi.fn(),
}));

vi.mock('./http', () => ({ apiRequest, liveApiRequest: apiRequest }));
vi.mock('../auth/session', () => ({
  readAuthSession: vi.fn(),
}));

import {
  acceptTutorRequest,
  createParentReport,
  createTutorAvailabilitySchedule,
  createTutorStudent,
  createTutorSubscriptionCheckout,
  getTutorAvailabilitySchedules,
  getTutorCalendar,
  getTutorCheckoutPage,
  getTutorDashboard,
  getTutorMessages,
  getTutorRequests,
  getTutorStudents,
  getTutorSubscription,
  refreshTutorSubscriptionPayment,
  sendTutorMessage,
  updateTutorRequestStage,
} from './tutor';
import { readAuthSession } from '../auth/session';

describe('tutor live API', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    apiRequest.mockResolvedValue({ ok: true });
    vi.mocked(readAuthSession).mockReturnValue({
      token: 'tutor-token',
      user: {
        id: '11111111-1111-1111-1111-111111111111',
        name: 'Репетитор',
        email: 'tutor@example.com',
        role: 'tutor',
      },
    });
  });

  it.each([
    [getTutorDashboard, '/tutor/dashboard'],
    [getTutorRequests, '/tutor/requests'],
    [getTutorStudents, '/tutor/students'],
    [getTutorAvailabilitySchedules, '/tutor/availability-schedules'],
  ])('loads %s without fallback data', async (request, path) => {
    await request();

    expect(apiRequest).toHaveBeenCalledWith(path);
  });

  it('loads calendar and selected message thread without fallback data', async () => {
    await getTutorCalendar('2026-06-15', '2026-06-21');
    await getTutorMessages('thread-1');

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/tutor/calendar?from=2026-06-15&to=2026-06-21');
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/tutor/messages?threadId=thread-1');
  });

  it('updates request state through CRM', async () => {
    await acceptTutorRequest('request-1');
    await updateTutorRequestStage('request-1', 'CONTACT_ESTABLISHED', 'Созвонились');

    expect(apiRequest).toHaveBeenNthCalledWith(
      1,
      '/tutor/requests/request-1/accept',
      { method: 'PATCH' },
    );
    expect(apiRequest).toHaveBeenNthCalledWith(
      2,
      '/tutor/requests/request-1/stage',
      {
        method: 'PATCH',
        body: JSON.stringify({ status: 'CONTACT_ESTABLISHED', reason: 'Созвонились' }),
      },
    );
  });

  it('creates a manual student and progress report with persisted payloads', async () => {
    const student = {
      name: 'Мария',
      details: '9 класс',
      subject: 'Математика',
      note: 'Подготовка к ОГЭ',
    };
    const report = {
      title: 'Итоги мая',
      summary: 'Освоены линейные уравнения.',
      progress: 65,
    };

    await createTutorStudent(student);
    await createParentReport('student-1', report);

    expect(apiRequest).toHaveBeenNthCalledWith(
      1,
      '/tutor/students',
      { method: 'POST', body: JSON.stringify(student) },
    );
    expect(apiRequest).toHaveBeenNthCalledWith(
      2,
      '/tutor/students/student-1/reports',
      { method: 'POST', body: JSON.stringify(report) },
    );
  });

  it('sends a message without a fabricated response', async () => {
    await sendTutorMessage('thread-1', 'Здравствуйте');

    expect(apiRequest).toHaveBeenCalledWith(
      '/tutor/messages/thread-1',
      { method: 'POST', body: JSON.stringify({ text: 'Здравствуйте' }) },
    );
  });

  it('creates availability with the backend payload', async () => {
    const schedule = {
      dayOfWeek: 1,
      startTime: '09:00',
      endTime: '12:00',
      slotDurationMinutes: 60,
      subjectId: null,
      active: true,
      validUntil: null,
    };

    await createTutorAvailabilitySchedule(schedule);

    expect(apiRequest).toHaveBeenCalledWith(
      '/tutor/availability-schedules',
      { method: 'POST', body: JSON.stringify(schedule) },
    );
  });

  it('builds subscription page data from live current state and tariffs', async () => {
    apiRequest
      .mockResolvedValueOnce({
        tutorUserId: '11111111-1111-1111-1111-111111111111',
        active: true,
        tariffCode: 'CRM_PRO',
        activeUntil: '2026-06-18T12:00:00Z',
      })
      .mockResolvedValueOnce([
        { id: 'tariff-pro', code: 'CRM_PRO', name: 'CRM Pro', priceRub: 1990, durationDays: 30, active: true },
      ]);

    const page = await getTutorSubscription();

    expect(apiRequest).toHaveBeenNthCalledWith(
      1,
      '/subscriptions/current?tutorUserId=11111111-1111-1111-1111-111111111111',
    );
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/subscriptions/tariffs');
    expect(page.plan).toMatchObject({ name: 'CRM Pro', status: 'Активна', price: '1 990 ₽' });
  });

  it('creates and refreshes a subscription payment without fallback data', async () => {
    await createTutorSubscriptionCheckout('CRM_PRO');
    await refreshTutorSubscriptionPayment('payment-1');

    expect(apiRequest).toHaveBeenNthCalledWith(
      1,
      '/subscriptions/checkout',
      {
        method: 'POST',
        body: JSON.stringify({
          tutorUserId: '11111111-1111-1111-1111-111111111111',
          tariffCode: 'CRM_PRO',
        }),
      },
    );
    expect(apiRequest).toHaveBeenNthCalledWith(
      2,
      '/subscriptions/payments/payment-1/refresh',
      { method: 'POST' },
    );
  });

  it('loads persisted checkout data without fallback content', async () => {
    await getTutorCheckoutPage('payment-1');

    expect(apiRequest).toHaveBeenCalledWith('/subscriptions/checkout/payment-1');
  });
});
