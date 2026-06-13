import { expect, test, type Page } from '@playwright/test';

const sessionKey = 'tutorspace.auth.session';

type Role = 'parent' | 'tutor' | 'admin' | 'moderator';

const publicRoutes = ['/', '/catalog', '/auth/sign-in'];
const protectedRoutes: Array<{ role: Role; routes: string[] }> = [
  { role: 'parent', routes: ['/app/client', '/app/client/messages'] },
  { role: 'tutor', routes: ['/app/tutor', '/app/tutor/calendar'] },
  { role: 'admin', routes: ['/admin', '/admin/users'] },
  { role: 'moderator', routes: ['/moderator', '/moderator/review-moderation'] },
];

const viewports = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'laptop', width: 1024, height: 768 },
  { name: 'tablet', width: 768, height: 1024 },
  { name: 'mobile', width: 390, height: 844 },
];

async function writeSession(page: Page, role: Role) {
  await page.goto('/');
  await page.evaluate(
    ({ key, sessionRole }) => {
      window.localStorage.setItem(
        key,
        JSON.stringify({
          token: 'responsive-layout-token',
          user: {
            id: `responsive-${sessionRole}`,
            name: 'Responsive Tester',
            email: `${sessionRole}@responsive.local`,
            role: sessionRole,
          },
        }),
      );
    },
    { key: sessionKey, sessionRole: role },
  );
}

async function expectNoDocumentOverflow(page: Page, route: string) {
  await page.goto(route);
  await page.waitForLoadState('domcontentloaded');

  const dimensions = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));

  expect(
    dimensions.scrollWidth,
    `${route} overflows by ${dimensions.scrollWidth - dimensions.clientWidth}px`,
  ).toBeLessThanOrEqual(dimensions.clientWidth);
}

for (const viewport of viewports) {
  test.describe(`${viewport.name} ${viewport.width}px`, () => {
    test.use({ viewport });

    test('public pages stay inside the viewport', async ({ page }) => {
      for (const route of publicRoutes) {
        await expectNoDocumentOverflow(page, route);
      }
    });

    test('workspace pages stay inside the viewport', async ({ page }) => {
      for (const group of protectedRoutes) {
        await writeSession(page, group.role);
        for (const route of group.routes) {
          await expectNoDocumentOverflow(page, route);
        }
      }
    });
  });
}

test.describe('mobile workspace navigation', () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test('can be opened and closes after navigation', async ({ page }) => {
    await writeSession(page, 'parent');
    await page.goto('/app/client');

    const toggle = page.getByRole('button', { name: /навигацию кабинета/ });
    const navigation = page.getByRole('navigation', { name: 'Кабинет клиента' });

    await expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await expect(navigation).toBeHidden();

    await toggle.click();
    await expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await expect(navigation).toBeVisible();

    await navigation.getByRole('link', { name: 'Сообщения' }).click();
    await expect(page).toHaveURL(/\/app\/client\/messages$/);
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await expect(navigation).toBeHidden();
  });
});
