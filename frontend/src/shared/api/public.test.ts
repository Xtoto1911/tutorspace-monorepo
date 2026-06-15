import { beforeEach, describe, expect, it, vi } from 'vitest';

const { apiRequest, authRequest, liveApiRequest } = vi.hoisted(() => ({
  apiRequest: vi.fn(),
  authRequest: vi.fn(),
  liveApiRequest: vi.fn(),
}));

vi.mock('./http', () => ({
  apiRequest,
  authRequest,
  liveApiRequest,
}));

vi.mock('../auth/session', () => ({
  readAuthSession: vi.fn(),
}));

import { additionalFaq, homeContent } from '../config/site';
import {
  completePasswordReset,
  getHomeContent,
  getTutorProfile,
  getTutorFaq,
  getTutorReviewEligibility,
  listSubjects,
  listTutors,
  registerParent,
  registerTutor,
  reportTutorProfile,
  reportTutorReview,
  requestPasswordReset,
  signIn,
  submitTutorInquiry,
  submitTutorReview,
} from './public';
import { readAuthSession } from '../auth/session';

describe('public static content', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    authRequest.mockReset();
    apiRequest.mockResolvedValue({ mocked: true });
  });

  it('returns home content without hitting the public API', async () => {
    await expect(getHomeContent()).resolves.toEqual(homeContent);
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it('returns faq content without hitting the public API', async () => {
    await expect(getTutorFaq()).resolves.toEqual([...homeContent.faq, ...additionalFaq]);
    expect(apiRequest).not.toHaveBeenCalled();
  });
});

function base64UrlEncode(bytes: Uint8Array): string {
  const binary = String.fromCharCode(...bytes);
  return btoa(binary).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
}

function fakeJwt(payload: Record<string, unknown>) {
  const encoder = new TextEncoder();
  const header = base64UrlEncode(encoder.encode(JSON.stringify({ alg: 'HS256' })));
  const body = base64UrlEncode(encoder.encode(JSON.stringify(payload)));
  return `${header}.${body}.signature`;
}

describe('auth session mapping', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    authRequest.mockReset();
    liveApiRequest.mockReset();
  });

  it('uses the name from the JWT claim on sign in', async () => {
    authRequest.mockResolvedValue({
      token: fakeJwt({ sub: 'login@example.com', userId: 'user-login', role: 'ROLE_CLIENT', name: '  Вошедший Пользователь  ' }),
      refreshToken: 'refresh-login',
    });

    const session = await signIn({ email: 'login@example.com', password: 'secret123' });

    expect(session.user.name).toBe('Вошедший Пользователь');
    expect(session.user.email).toBe('login@example.com');
    expect(session.user.role).toBe('parent');
  });

  it('falls back to the email local part when the JWT has no name', async () => {
    authRequest.mockResolvedValue({
      token: fakeJwt({ sub: 'fallback@example.com', userId: 'user-fallback', role: 'ROLE_TUTOR' }),
      refreshToken: 'refresh-fallback',
    });

    const session = await signIn({ email: 'fallback@example.com', password: 'secret123' });

    expect(session.user.name).toBe('fallback');
  });

  it('sends the name when registering a parent', async () => {
    authRequest.mockResolvedValue({
      token: fakeJwt({ sub: 'parent@example.com', userId: 'user-parent', role: 'ROLE_CLIENT', name: 'Родитель Иванов' }),
      refreshToken: 'refresh-parent',
    });

    await registerParent({ name: 'Родитель Иванов', email: 'parent@example.com', password: 'secret123' });

    expect(authRequest).toHaveBeenCalledWith(
      '/auth/register',
      { method: 'POST', body: JSON.stringify({ email: 'parent@example.com', password: 'secret123', role: 'ROLE_CLIENT', name: 'Родитель Иванов' }) },
    );
  });
});

describe('tutor registration live onboarding', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    authRequest.mockReset();
    liveApiRequest.mockReset();
    authRequest.mockResolvedValue({
      token: 'registered-token',
      refreshToken: 'registered-refresh',
    });
    liveApiRequest.mockResolvedValue({ ok: true });
  });

  it('stores the tutor registration profile in CRM after auth registration', async () => {
    await registerTutor({
      name: 'Новый Репетитор',
      email: 'new-tutor@example.com',
      password: 'secret123',
      subjects: ['Математика', 'Физика'],
      experienceYears: '4',
      city: 'Казань',
      about: 'Готовлю к экзаменам спокойно и системно.',
    });

    expect(authRequest).toHaveBeenCalledWith(
      '/auth/register',
      { method: 'POST', body: JSON.stringify({ email: 'new-tutor@example.com', password: 'secret123', role: 'ROLE_TUTOR', name: 'Новый Репетитор' }) },
    );
    expect(liveApiRequest).toHaveBeenCalledWith('/tutor/profile', {
      method: 'PATCH',
      headers: { Authorization: 'Bearer registered-token' },
      body: JSON.stringify({
        isHidden: true,
        identity: {
          name: 'Новый Репетитор',
          city: 'Казань',
          experienceYears: 4,
          pricePerHour: 1500,
        },
        fields: [
          { id: 'headline', label: 'Заголовок', value: 'Репетитор TutorSpace', type: 'input' },
          { id: 'about', label: 'О себе', value: 'Готовлю к экзаменам спокойно и системно.', type: 'textarea' },
        ],
        subjects: [
          { title: 'Математика', meta: '4 года опыта', price: '1500 ₽/час', tone: 'indigo' },
          { title: 'Физика', meta: '4 года опыта', price: '1500 ₽/час', tone: 'emerald' },
        ],
      }),
    });
  });

  it('does not invent a subject when the registration payload is empty', async () => {
    await registerTutor({
      name: 'Новый Репетитор',
      email: 'empty-subjects@example.com',
      password: 'secret123',
      subjects: [],
      experienceYears: '1',
      city: 'Тула',
      about: 'Индивидуальные занятия.',
    });

    expect(JSON.parse(liveApiRequest.mock.calls[0][1].body)).toMatchObject({
      subjects: [],
    });
  });
});

describe('public catalog live mapping', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    authRequest.mockReset();
    liveApiRequest.mockReset();
  });

  it('maps subject descriptions from the backend response', async () => {
    apiRequest.mockResolvedValueOnce([
      {
        id: 'subject-1',
        name: 'Математика',
        slug: 'matematika',
        description: 'Алгебра, геометрия и подготовка к экзаменам.',
      },
    ]);

    const result = await listSubjects();

    expect(apiRequest).toHaveBeenCalledWith('/marketplace/subjects');
    expect(result[0]).toMatchObject({
      title: 'Математика',
      description: 'Алгебра, геометрия и подготовка к экзаменам.',
    });
  });

  it('maps tutor metadata only from the backend response', async () => {
    apiRequest.mockResolvedValueOnce([
      {
        id: 'profile-1',
        userId: 'user-1',
        slug: 'live-tutor',
        fullName: 'Лев Орлов',
        description: 'Готовлю к экзаменам.',
        city: 'Тула',
        education: ['ТГПУ'],
        highlights: ['Средний балл 85'],
        tags: ['Онлайн', 'ЕГЭ'],
        responseTimeMinutes: 35,
        experienceYears: 5,
        pricePerHour: 1700,
        photoUrl: null,
        rating: 4.8,
        reviewsCount: 12,
        status: 'ACTIVE',
        subjects: [{ id: 'subject-1', name: 'Математика', slug: 'matematika', description: 'Алгебра' }],
      },
    ]);

    const result = await listTutors();

    expect(apiRequest).toHaveBeenCalledWith('/marketplace/tutors');
    expect(result[0]).toMatchObject({
      city: 'Тула',
      subjects: ['Математика'],
      tags: ['Онлайн', 'ЕГЭ'],
    });
  });

  it('forwards rating and price range filters as query params to the backend', async () => {
    apiRequest.mockResolvedValueOnce([]);

    await listTutors({
      minRating: 4.5,
      maxRating: 4.9,
      minPrice: 1500,
      maxPrice: 3000,
    });

    const calledPath = vi.mocked(apiRequest).mock.calls[0]?.[0] as string;
    expect(calledPath).toContain('minRating=4.5');
    expect(calledPath).toContain('maxRating=4.9');
    expect(calledPath).toContain('minPricePerHour=1500');
    expect(calledPath).toContain('maxPricePerHour=3000');
  });

  it('drops 0 values from query params (zero means "no filter")', async () => {
    apiRequest.mockResolvedValueOnce([]);

    await listTutors({
      minRating: 0,
      maxRating: 0,
      minPrice: 0,
      maxPrice: 0,
    });

    const calledPath = vi.mocked(apiRequest).mock.calls[0]?.[0] as string;
    expect(calledPath).toBe('/marketplace/tutors');
  });
});

describe('public tutor inquiry', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    authRequest.mockReset();
    liveApiRequest.mockReset();
    vi.mocked(readAuthSession).mockReturnValue({
      token: 'parent-token',
      user: {
        id: '11111111-1111-1111-1111-111111111111',
        name: 'Родитель',
        email: 'parent@example.com',
        role: 'parent',
      },
    });
  });

  it('sends the selected slot time when the client chooses a concrete tutor slot', async () => {
    apiRequest
      .mockResolvedValueOnce({
        id: '22222222-2222-2222-2222-222222222222',
        userId: '33333333-3333-3333-3333-333333333333',
        slug: 'anna-petrova',
        fullName: 'Анна Смирнова',
        description: 'Репетитор',
        experienceYears: 7,
        pricePerHour: 1800,
        photoUrl: null,
        rating: 4.9,
        reviewsCount: 128,
        status: 'ACTIVE',
        subjects: [{ id: '44444444-4444-4444-4444-444444444444', name: 'Математика', slug: 'matematika' }],
      })
      .mockResolvedValueOnce({ requestId: 'request-slot', applicationId: 'crm-application-slot' });

    const response = await submitTutorInquiry('anna-petrova', {
      message: 'Хочу записаться на слот.',
      desiredStartTime: '2026-05-18T18:30',
    });

    expect(apiRequest).toHaveBeenLastCalledWith(
      '/marketplace/tutors/22222222-2222-2222-2222-222222222222/requests',
      {
        method: 'POST',
        body: JSON.stringify({
          clientUserId: '11111111-1111-1111-1111-111111111111',
          subjectId: '44444444-4444-4444-4444-444444444444',
          desiredStartTime: '2026-05-18T18:30',
          studentName: 'Родитель',
          studentGrade: 'класс уточняется',
          comment: 'Хочу записаться на слот.',
        }),
      },
    );
    expect(response.applicationId).toBe('crm-application-slot');
  });
});

describe('public tutor profile live mapping', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    authRequest.mockReset();
    liveApiRequest.mockReset();
  });

  it('does not reuse demo success metrics for a new live tutor profile', async () => {
    apiRequest
      .mockResolvedValueOnce({
        id: '22222222-2222-2222-2222-222222222222',
        userId: '33333333-3333-3333-3333-333333333333',
        slug: 'e2e-tutor',
        fullName: 'E2E Репетитор',
        description: 'Спокойная подготовка к экзаменам.',
        city: 'Омск',
        education: [],
        highlights: [],
        tags: ['Онлайн'],
        responseTimeMinutes: null,
        experienceYears: 2,
        pricePerHour: 1500,
        photoUrl: null,
        rating: 0,
        reviewsCount: 0,
        status: 'ACTIVE',
        subjects: [{ id: '44444444-4444-4444-4444-444444444444', name: 'Математика', slug: 'matematika' }],
      })
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([]);

    const profile = await getTutorProfile('e2e-tutor');

    expect(profile?.stats.successRate).toBe('Нет данных');
    expect(profile?.stats.activeStudents).toBeNull();
    expect(profile?.schedule).toEqual([]);
  });

  it('loads public CRM slots for the live tutor user id', async () => {
    apiRequest
      .mockResolvedValueOnce({
        id: '22222222-2222-2222-2222-222222222222',
        userId: '33333333-3333-3333-3333-333333333333',
        slug: 'e2e-tutor',
        fullName: 'E2E Репетитор',
        description: 'Спокойная подготовка к экзаменам.',
        city: 'Омск',
        education: [],
        highlights: [],
        tags: ['Онлайн'],
        responseTimeMinutes: null,
        experienceYears: 2,
        pricePerHour: 1500,
        photoUrl: null,
        rating: 0,
        reviewsCount: 0,
        status: 'ACTIVE',
        subjects: [{ id: '44444444-4444-4444-4444-444444444444', name: 'Математика', slug: 'matematika' }],
      })
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        { date: '2026-05-28', startTime: '19:00', endTime: '20:00', available: true, source: 'EXTRA' },
      ]);

    const profile = await getTutorProfile('e2e-tutor');

    expect(apiRequest).toHaveBeenLastCalledWith(
      expect.stringMatching(/^\/public\/tutors\/33333333-3333-3333-3333-333333333333\/slots\?from=\d{4}-\d{2}-\d{2}&to=\d{4}-\d{2}-\d{2}$/),
    );
    expect(profile?.schedule).toEqual(['Чт 19:00']);
  });

  it('propagates slot API failures instead of rendering an invented empty schedule', async () => {
    apiRequest
      .mockResolvedValueOnce({
        id: 'profile-1',
        userId: 'user-1',
        slug: 'live-tutor',
        fullName: 'Лев Орлов',
        description: 'Готовлю к экзаменам.',
        city: 'Тула',
        education: [],
        highlights: [],
        tags: ['Онлайн'],
        responseTimeMinutes: 35,
        experienceYears: 5,
        pricePerHour: 1700,
        photoUrl: null,
        rating: 4.8,
        reviewsCount: 12,
        status: 'ACTIVE',
        subjects: [{ id: 'subject-1', name: 'Математика', slug: 'matematika', description: 'Алгебра' }],
      })
      .mockResolvedValueOnce([])
      .mockRejectedValueOnce(new Error('crm unavailable'));

    await expect(getTutorProfile('live-tutor')).rejects.toThrow('crm unavailable');
  });
});

describe('public tutor reviews', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    authRequest.mockReset();
    liveApiRequest.mockReset();
  });

  it('fetches review eligibility for the current client', async () => {
    apiRequest.mockResolvedValueOnce({ eligible: true, reason: 'FINISHED_LESSON_FOUND', existingReview: null });

    await getTutorReviewEligibility('profile-1');

    expect(apiRequest).toHaveBeenCalledWith(
      '/marketplace/reviews/eligibility?tutorProfileId=profile-1',
    );
  });

  it('submits a tutor review without clientUserId in the payload', async () => {
    apiRequest.mockResolvedValueOnce({ id: 'review-1', rating: 5, text: 'Отличный урок' });

    await submitTutorReview('profile-1', { rating: 5, text: 'Отличный урок' });

    expect(apiRequest).toHaveBeenCalledWith(
      '/marketplace/reviews',
      {
        method: 'POST',
        body: JSON.stringify({
          tutorProfileId: 'profile-1',
          rating: 5,
          text: 'Отличный урок',
        }),
      },
    );
  });

  it('reports a review to moderation', async () => {
    apiRequest.mockResolvedValueOnce({ id: 'complaint-1', status: 'PENDING' });

    await reportTutorReview('review-1', {
      reason: 'Некорректный отзыв',
      description: 'Похоже на спам',
    });

    expect(apiRequest).toHaveBeenCalledWith(
      '/marketplace/reviews/review-1/complaints',
      {
        method: 'POST',
        body: JSON.stringify({
          reason: 'Некорректный отзыв',
          description: 'Похоже на спам',
        }),
      },
    );
  });

  it('reports a tutor profile to moderation', async () => {
    apiRequest.mockResolvedValueOnce({ id: 'complaint-2', status: 'PENDING' });

    await reportTutorProfile('anna-petrova', {
      reason: 'Некорректный профиль',
      description: 'В карточке указаны неподтвержденные данные.',
    });

    expect(apiRequest).toHaveBeenCalledWith(
      '/marketplace/tutors/anna-petrova/complaints',
      {
        method: 'POST',
        body: JSON.stringify({
          reason: 'Некорректный профиль',
          description: 'В карточке указаны неподтвержденные данные.',
        }),
      },
    );
  });
});

describe('password reset live API', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    apiRequest.mockResolvedValue({ success: true, message: 'Готово' });
  });

  it('requests a password reset through the auth endpoint', async () => {
    await requestPasswordReset('client@example.com');

    expect(apiRequest).toHaveBeenCalledWith('/auth/password-reset', {
      method: 'POST',
      body: JSON.stringify({ email: 'client@example.com' }),
    });
  });

  it('confirms a password reset through the auth endpoint', async () => {
    await completePasswordReset('reset-token', 'new-password');

    expect(apiRequest).toHaveBeenCalledWith('/auth/password-reset/confirm', {
      method: 'POST',
      body: JSON.stringify({ token: 'reset-token', password: 'new-password' }),
    });
  });
});
