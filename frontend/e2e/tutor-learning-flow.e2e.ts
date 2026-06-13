import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

const sessionKey = 'tutorspace.auth.session';

type AuthSnapshot = string;

type MarketplaceTutor = {
  id: string;
  slug: string;
  fullName: string;
  status: string;
};

function uniqueEmail(prefix: string, suffix: string) {
  return `${prefix}.${suffix}@e2e.tutorspace.local`;
}

function yesterdayIso() {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return formatDateIso(date);
}

function formatDateIso(date: Date) {
  const now = date;
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

async function clearSession(page: Page) {
  await page.goto('/');
  await page.evaluate((key) => window.localStorage.removeItem(key), sessionKey);
}

async function readSession(page: Page): Promise<AuthSnapshot> {
  const session = await page.evaluate((key) => window.localStorage.getItem(key), sessionKey);

  if (!session) {
    throw new Error('Auth session was not written to localStorage.');
  }

  return session;
}

async function restoreSession(page: Page, session: AuthSnapshot) {
  await page.goto('/');
  await page.evaluate(
    ({ key, value }) => window.localStorage.setItem(key, value),
    { key: sessionKey, value: session },
  );
}

async function signIn(page: Page, input: {
  email: string;
  password: string;
}) {
  await page.goto('/auth/sign-in');
  await page.getByLabel('Email').fill(input.email);
  await page.getByLabel('Пароль').fill(input.password);
  await page.getByRole('button', { name: 'Войти' }).click();
}

async function registerTutor(page: Page, input: {
  name: string;
  email: string;
  password: string;
}) {
  await page.goto('/auth/sign-up/tutor');

  await page.getByLabel('ФИО').fill(input.name);
  await page.getByLabel('Email').fill(input.email);
  await page.getByLabel('Пароль').fill(input.password);
  await page.locator('.subject-picker__options button').filter({ hasText: 'Математика' }).click();
  await page.getByLabel('Опыт, лет').fill('7');
  await page.getByLabel('Город').fill('Москва');
  await page.getByLabel('О себе').fill('E2E профиль: готовлю к экзаменам спокойно и по понятному плану.');
  await page.getByRole('button', { name: 'Создать профиль' }).click();

  await expect(page).toHaveURL(/\/app\/tutor\/profile\?welcome=1/);
  await expect(page.getByText('Карточка скрыта из каталога')).toBeVisible();

  const onboarding = page.getByRole('region', { name: 'Первый запуск профиля репетитора' });
  await onboarding.getByRole('button', { name: 'Открыть карточку' }).click();
  await expect(page.getByText('Профиль снова виден в каталоге.')).toBeVisible();

  return readSession(page);
}

async function registerClient(page: Page, input: {
  name: string;
  email: string;
  password: string;
}) {
  await page.goto('/auth/sign-up/parent');

  await page.getByLabel('Имя').fill(input.name);
  await page.getByLabel('Email').fill(input.email);
  await page.getByLabel('Пароль').fill(input.password);
  await page.getByRole('button', { name: 'Создать аккаунт' }).click();

  await expect(page).toHaveURL(/\/app\/client/);
  return readSession(page);
}

async function findPublishedTutor(request: APIRequestContext, baseURL: string, fullName: string) {
  for (let attempt = 0; attempt < 12; attempt += 1) {
    const response = await request.get(`${baseURL}/api/marketplace/tutors`);
    expect(response.ok()).toBeTruthy();

    const tutors = await response.json() as MarketplaceTutor[];
    const tutor = tutors.find((item) => item.fullName === fullName && item.status === 'ACTIVE');

    if (tutor) {
      return tutor;
    }

    await new Promise((resolve) => setTimeout(resolve, 500));
  }

  throw new Error(`Published tutor was not found in marketplace: ${fullName}`);
}

test('client registers, books a tutor lesson, and leaves a review after tutor registration', async ({
  page,
  request,
  baseURL,
}) => {
  const suffix = Date.now().toString(36);
  const password = 'e2ePass123';
  const tutorName = `Е2Е Репетитор ${suffix}`;
  const clientName = `Е2Е Клиент ${suffix}`;
  const reviewText = `E2E отзыв ${suffix}: занятие прошло понятно, спокойно и с хорошим планом.`;

  await clearSession(page);
  const tutorSession = await registerTutor(page, {
    name: tutorName,
    email: uniqueEmail('tutor', suffix),
    password,
  });
  const tutor = await findPublishedTutor(request, String(baseURL), tutorName);

  await clearSession(page);
  const clientSession = await registerClient(page, {
    name: clientName,
    email: uniqueEmail('client', suffix),
    password,
  });

  await page.goto(`/tutors/${tutor.slug}`);
  await expect(page.getByRole('heading', { name: tutorName })).toBeVisible();
  await page.getByRole('button', { name: 'Выбрать время' }).click();
  await page.getByRole('button', { name: 'Отправить быструю заявку' }).click();
  await expect(page.getByText('Заявка отправлена.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Открыть заявку' })).toBeVisible();

  await restoreSession(page, tutorSession);
  await page.goto('/app/tutor/requests');
  const requestCard = page.locator('.tutor-request-card').filter({ hasText: clientName });
  await expect(requestCard).toBeVisible();
  await requestCard.getByRole('button', { name: 'Принять' }).click();
  await expect(page.locator('.tutor-request-tabs button.is-active')).toContainText('В работе');
  const acceptedRequestCard = page.locator('.tutor-request-card').filter({ hasText: clientName });
  await expect(acceptedRequestCard.getByText('Принята')).toBeVisible();
  await acceptedRequestCard.getByRole('link', { name: 'Написать' }).click();
  await expect(page).toHaveURL(/\/app\/tutor\/messages\?threadId=/);
  await expect(page.getByText(clientName).first()).toBeVisible();

  await page.goto('/app/tutor/calendar');
  await page.getByRole('button', { name: 'Добавить занятие' }).click();
  await page.getByLabel('Из принятых заявок').check();
  await page.locator('select[name="applicationId"]').selectOption({ index: 1 });
  // The review form is available only after a finished lesson; use a completed timestamp without waiting in real time.
  await page.locator('input[name="date"]').evaluate((input) => input.removeAttribute('min'));
  await page.locator('input[name="date"]').fill(yesterdayIso());
  await page.locator('input[name="time"]').fill('00:00');
  await page.locator('select[name="duration"]').selectOption('30');
  await page.getByRole('button', { name: 'Создать', exact: true }).click();
  await expect(page.getByRole('dialog', { name: 'Новое занятие' })).toBeHidden();
  await expect(page.locator('.tutor-week-event').filter({ hasText: clientName })).toBeVisible();

  await restoreSession(page, clientSession);
  await page.goto('/app/client/schedule');
  await expect(page.getByText('Математика').first()).toBeVisible();
  await expect(page.getByText('Уроков пока нет')).toHaveCount(0);

  await page.goto('/app/client/settings');
  await expect(page.getByLabel('Email')).toHaveValue(uniqueEmail('client', suffix));
  await expect(page.getByText('Миша Смирнов')).toHaveCount(0);
  await expect(page.getByText('Катя Смирнова')).toHaveCount(0);
  await expect(page.getByText('92%')).toHaveCount(0);

  await page.goto(`/tutors/${tutor.slug}#reviews`);
  await expect(page.getByRole('button', { name: 'Оставить отзыв' })).toBeVisible();
  await page.getByPlaceholder('Что было полезно на занятии?').fill(reviewText);
  await page.getByRole('button', { name: 'Оставить отзыв' }).click();
  await expect(page.getByText('Отзыв отправлен на модерацию.')).toBeVisible();

  await signIn(page, {
    email: 'admin@tutorspace.local',
    password: 'admin12345',
  });
  await expect(page).toHaveURL(/\/admin|\/moderator/);
  await page.goto('/moderator/review-moderation');
  await expect(page.getByText(tutorName).first()).toBeVisible();
  await expect(page.getByText(reviewText).first()).toBeVisible();
});
