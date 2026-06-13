import { additionalFaq, homeContent } from '../config/site';
import { apiRequest, authRequest, liveApiRequest } from './http';
import { readAuthSession } from '../auth/session';
import type {
  AuthSession,
  FaqItem,
  HomeContent,
  Role,
  Review,
  ReviewEligibility,
  SignInInput,
  SignUpParentInput,
  SignUpTutorInput,
  Subject,
  TutorFilters,
  TutorProfile,
  TutorSummary,
} from '../types';

function matchesQuery(value: string, query?: string) {
  return !query || value.toLowerCase().includes(query.toLowerCase());
}

type BackendAuthResponse = {
  token: string;
  refreshToken?: string;
  name?: string;
};

type BackendSubject = {
  id: string;
  name: string;
  slug: string;
  description: string;
};

type BackendTutorProfile = {
  id: string;
  userId: string;
  slug: string;
  fullName: string;
  description: string;
  city?: string | null;
  education: string[];
  highlights: string[];
  tags: string[];
  responseTimeMinutes?: number | null;
  activeStudents?: number | null;
  lessonsCount?: number | null;
  repeatRatePercent?: number | null;
  experienceYears: number;
  pricePerHour: number;
  photoUrl?: string | null;
  rating: number;
  reviewsCount: number;
  status: string;
  subjects: BackendSubject[];
};

type BackendReview = {
  id: string;
  tutorProfileId: string;
  clientUserId: string;
  rating: number;
  text: string;
  status: string;
  createdAt: string;
  updatedAt?: string;
  authorName?: string;
};

type BackendReviewEligibility = {
  eligible: boolean;
  reason: string;
  existingReview: BackendReview | null;
};

type BackendReviewComplaint = {
  id: string;
  status: string;
};

type BackendPublicSlot = {
  date: string;
  startTime: string;
  endTime: string;
  available: boolean;
  source: 'RULE' | 'EXTRA';
};

type BackendRole = 'ROLE_CLIENT' | 'ROLE_TUTOR' | 'ROLE_ADMIN' | 'ROLE_MODERATOR';

type JwtPayload = {
  sub?: string;
  userId?: string;
  role?: BackendRole;
  name?: string;
};

const roleFromBackend: Record<BackendRole, Role> = {
  ROLE_CLIENT: 'parent',
  ROLE_TUTOR: 'tutor',
  ROLE_ADMIN: 'admin',
  ROLE_MODERATOR: 'moderator',
};

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=');
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return new TextDecoder().decode(bytes);
}

function decodeJwtPayload(token: string): JwtPayload {
  const [, payload] = token.split('.');

  if (!payload) {
    return {};
  }

  try {
    return JSON.parse(decodeBase64Url(payload)) as JwtPayload;
  } catch {
    return {};
  }
}

function getNameFromEmail(email: string) {
  return email.split('@')[0] || email;
}

function mapTutorSummary(profile: BackendTutorProfile): TutorSummary {
  const subjectNames = profile.subjects.map((subject) => subject.name);
  const primarySubjectSlug = profile.subjects[0]?.slug ?? '';

  return {
    id: profile.id,
    slug: profile.slug,
    name: profile.fullName,
    city: profile.city?.trim() || 'Город не указан',
    experienceYears: profile.experienceYears,
    rating: profile.rating,
    reviewCount: profile.reviewsCount,
    hourlyRate: profile.pricePerHour,
    subjects: subjectNames,
    tags: profile.tags,
    verified: profile.status === 'ACTIVE',
    featured: profile.status === 'ACTIVE',
    nextAvailable: 'Смотрите доступные слоты в профиле',
    about: profile.description,
    avatarTone: 'avatar-indigo',
    primarySubjectSlug,
  };
}

const weekdayLabels = ['Вс', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];

function formatPublicSlot(slot: BackendPublicSlot) {
  const date = new Date(`${slot.date}T00:00:00`);
  const day = weekdayLabels[date.getDay()] ?? slot.date;
  return `${day} ${slot.startTime.slice(0, 5)}`;
}

function mapTutorProfile(
  profile: BackendTutorProfile,
  reviews: BackendReview[],
  slots: BackendPublicSlot[],
): TutorProfile {
  const summary = mapTutorSummary(profile);
  const slotSchedule = slots.filter((slot) => slot.available).map(formatPublicSlot);

  return {
    ...summary,
    education: profile.education,
    highlights: profile.highlights,
    schedule: slotSchedule,
    reviews: reviews.map((review) => mapReview(review, summary.subjects[0] ?? 'Занятие')),
    stats: {
      responseTime: profile.responseTimeMinutes == null ? 'Нет данных' : `${profile.responseTimeMinutes} мин`,
      reviews: profile.reviewsCount,
      successRate: 'Нет данных',
      activeStudents: profile.activeStudents ?? null,
      averageRating: profile.rating,
      lessons: profile.lessonsCount ?? null,
      repeatRate: profile.repeatRatePercent == null ? 'Нет данных' : `${profile.repeatRatePercent}%`,
    },
  };
}

function getPublicTutorSlots(tutorUserId: string): Promise<BackendPublicSlot[]> {
  const from = new Date();
  from.setHours(0, 0, 0, 0);
  const to = new Date(from);
  to.setDate(to.getDate() + 14);
  const fromIso = from.toISOString().slice(0, 10);
  const toIso = to.toISOString().slice(0, 10);

  return apiRequest<BackendPublicSlot[]>(
    `/public/tutors/${encodeURIComponent(tutorUserId)}/slots?from=${fromIso}&to=${toIso}`,
  );
}

function mapReview(review: BackendReview, lesson: string): Review {
  return {
    id: review.id,
    author: review.authorName ?? 'Клиент TutorSpace',
    date: review.createdAt.slice(0, 10),
    rating: review.rating,
    text: review.text,
    lesson,
  };
}

function toAuthSession(response: BackendAuthResponse, fallback: { email: string; name?: string; role?: Role }): AuthSession {
  const payload = decodeJwtPayload(response.token);
  const email = payload.sub ?? fallback.email;
  const role = payload.role ? roleFromBackend[payload.role] : (fallback.role ?? 'parent');
  const name = payload.name?.trim()
    || response.name?.trim()
    || fallback.name?.trim()
    || getNameFromEmail(email);

  return {
    token: response.token,
    refreshToken: response.refreshToken,
    user: {
      id: payload.userId ?? email,
      name,
      email,
      role,
    },
  };
}

export function getHomeContent(): Promise<HomeContent> {
  return Promise.resolve(homeContent);
}

export function listSubjects(): Promise<Subject[]> {
  const subjectAccents = [
    'oklch(0.92 0.035 250)',
    'oklch(0.93 0.04 150)',
    'oklch(0.94 0.055 85)',
    'oklch(0.93 0.035 350)',
  ];

  return apiRequest<BackendSubject[]>('/marketplace/subjects').then((backendSubjects) =>
    backendSubjects.map((subject, index) => ({
      slug: subject.slug,
      title: subject.name,
      description: subject.description,
      tutorCount: null,
      accent: subjectAccents[index % subjectAccents.length],
    })),
  );
}

export function listTutors(filters: TutorFilters = {}): Promise<TutorSummary[]> {
  const params = new URLSearchParams();
  const subjectSlug = filters.subject?.trim();

  if (subjectSlug) {
    params.set('subject', subjectSlug);
  }
  if (filters.maxPrice) {
    params.set('maxPricePerHour', String(filters.maxPrice));
  }

  return apiRequest<BackendTutorProfile[]>(`/marketplace/tutors${params.size ? `?${params.toString()}` : ''}`).then((profiles) =>
    profiles
      .map(mapTutorSummary)
      .filter((tutor) => {
        const query = filters.query?.trim();
        const cityMatches = !filters.city || matchesQuery(tutor.city, filters.city);
        const queryMatches =
          !query ||
          matchesQuery(tutor.name, query) ||
          matchesQuery(tutor.city, query) ||
          tutor.subjects.some((value) => matchesQuery(value, query)) ||
          matchesQuery(tutor.about, query);
        const ratingMatches = !filters.minRating || tutor.rating >= filters.minRating;
        const onlineMatches = !filters.onlineOnly || tutor.tags.some((tag) => tag.toLowerCase().includes('онлайн'));
        return queryMatches && cityMatches && ratingMatches && onlineMatches;
      }),
  );
}

export function getTutorProfile(slug: string): Promise<TutorProfile | null> {
  return apiRequest<BackendTutorProfile | null>(`/marketplace/tutors/${slug}`).then(async (profile) => {
    if (!profile) {
      return null;
    }

    const [reviews, slots] = await Promise.all([
      apiRequest<BackendReview[]>(`/marketplace/reviews?tutorProfileId=${profile.id}`),
      getPublicTutorSlots(profile.userId),
    ]);

    return mapTutorProfile(profile, reviews, slots);
  });
}

export function getTutorReviewEligibility(tutorProfileId: string) {
  return apiRequest<BackendReviewEligibility>(
    `/marketplace/reviews/eligibility?tutorProfileId=${encodeURIComponent(tutorProfileId)}`,
  ).then((eligibility): ReviewEligibility => ({
    eligible: eligibility.eligible,
    reason: eligibility.reason,
    existingReview: eligibility.existingReview ? mapReview(eligibility.existingReview, 'Занятие') : null,
  }));
}

export function submitTutorReview(tutorProfileId: string, input: { rating: number; text: string }) {
  return apiRequest<BackendReview>(
    '/marketplace/reviews',
    {
      method: 'POST',
      body: JSON.stringify({
        tutorProfileId,
        rating: input.rating,
        text: input.text,
      }),
    },
  );
}

export function reportTutorReview(reviewId: string, input: { reason: string; description?: string }) {
  return apiRequest<BackendReviewComplaint>(
    `/marketplace/reviews/${encodeURIComponent(reviewId)}/complaints`,
    {
      method: 'POST',
      body: JSON.stringify({
        reason: input.reason,
        description: input.description,
      }),
    },
  );
}

export function reportTutorProfile(slug: string, input: { reason: string; description?: string }) {
  return apiRequest<BackendReviewComplaint>(
    `/marketplace/tutors/${encodeURIComponent(slug)}/complaints`,
    {
      method: 'POST',
      body: JSON.stringify({
        reason: input.reason,
        description: input.description,
      }),
    },
  );
}

export function getTutorFaq(): Promise<FaqItem[]> {
  return Promise.resolve([...homeContent.faq, ...additionalFaq]);
}

export function signIn(input: SignInInput): Promise<AuthSession> {
  return authRequest<BackendAuthResponse>(
    '/auth/login',
    { method: 'POST', body: JSON.stringify(input) },
  ).then((response) => toAuthSession(response, { email: input.email }));
}

export function logout(refreshToken?: string): Promise<void> {
  if (!refreshToken) {
    return Promise.resolve();
  }

  return authRequest<void>('/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) });
}

export function registerParent(
  input: SignUpParentInput,
): Promise<AuthSession> {
  const role: Role = 'parent';

  return authRequest<BackendAuthResponse>(
    '/auth/register',
    { method: 'POST', body: JSON.stringify({ email: input.email, password: input.password, role: 'ROLE_CLIENT', name: input.name }) },
  ).then((response) => toAuthSession(response, { email: input.email, name: input.name, role }));
}

export function registerTutor(
  input: SignUpTutorInput,
): Promise<AuthSession> {
  const role: Role = 'tutor';

  return authRequest<BackendAuthResponse>(
    '/auth/register',
    { method: 'POST', body: JSON.stringify({ email: input.email, password: input.password, role: 'ROLE_TUTOR', name: input.name }) },
  ).then(async (response) => {
    const session = toAuthSession(response, { email: input.email, name: input.name, role });
    const experienceYears = Number.parseInt(input.experienceYears, 10);
    const normalizedExperience = Number.isFinite(experienceYears) && experienceYears >= 0 ? experienceYears : 0;
    const selectedSubjects = input.subjects.map((subject) => subject.trim()).filter(Boolean);
    const subjectTones = ['indigo', 'emerald', 'amber'] as const;

    await liveApiRequest('/tutor/profile', {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${response.token}` },
      body: JSON.stringify({
        isHidden: true,
        identity: {
          name: input.name,
          city: input.city,
          experienceYears: normalizedExperience,
          pricePerHour: 1500,
        },
        fields: [
          { id: 'headline', label: 'Заголовок', value: 'Репетитор TutorSpace', type: 'input' },
          { id: 'about', label: 'О себе', value: input.about, type: 'textarea' },
        ],
        subjects: selectedSubjects.map((subject, index) => ({
          title: subject,
          meta: `${normalizedExperience} года опыта`,
          price: '1500 ₽/час',
          tone: subjectTones[index % subjectTones.length],
        })),
      }),
    });

    return session;
  });
}

export function requestPasswordReset(
  email: string,
): Promise<{ message: string; testToken?: string }> {
  return apiRequest<{ message: string; testToken?: string }>(
    '/auth/password-reset',
    { method: 'POST', body: JSON.stringify({ email }) },
  );
}

export function completePasswordReset(
  token: string,
  password: string,
): Promise<{ message: string }> {
  return apiRequest<{ message: string }>(
    '/auth/password-reset/confirm',
    { method: 'POST', body: JSON.stringify({ token, password }) },
  );
}

export function submitTutorInquiry(
  tutorSlug: string,
  payload: { message: string; desiredStartTime?: string },
): Promise<{ success: true; applicationId: string }> {
  return apiRequest<BackendTutorProfile | null>(`/marketplace/tutors/${tutorSlug}`).then((tutor) => {
    if (!tutor) {
      throw new Error('Профиль репетитора не найден.');
    }

    const session = readAuthSession();

    if (!session || session.user.role !== 'parent') {
      throw new Error('Чтобы отправить заявку, войдите как клиент.');
    }

    const primarySubject = tutor.subjects[0];
    const studentName = session.user.name.trim() || 'Ученик';

    if (!primarySubject) {
      throw new Error('У репетитора пока не настроены предметы.');
    }

    return apiRequest<{ requestId?: string; applicationId?: string; message?: string }>(
      `/marketplace/tutors/${tutor.id}/requests`,
      {
        method: 'POST',
        body: JSON.stringify({
          clientUserId: session.user.id,
          subjectId: primarySubject.id,
          desiredStartTime: payload.desiredStartTime ?? new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
          studentName,
          studentGrade: 'класс уточняется',
          comment: payload.message,
        }),
      },
    ).then((response) => {
      const applicationId = response.applicationId ?? response.requestId;
      if (!applicationId) {
        throw new Error(response.message || 'Backend не вернул идентификатор заявки.');
      }
      return {
        success: true as const,
        applicationId,
      };
    });
  });
}
