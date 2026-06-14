import { beforeEach, describe, expect, it, vi } from 'vitest';

const { apiRequest } = vi.hoisted(() => ({
  apiRequest: vi.fn(),
}));

vi.mock('./http', () => ({ apiRequest }));

import {
  addClientFavorite,
  cancelClientApplication,
  getClientApplicationDetails,
  getClientApplicationsPage,
  getClientDashboard,
  getClientFavoritesPage,
  getClientHistoryPage,
  getClientMessagesPage,
  getClientSchedulePage,
  getClientSettings,
  requestClientLessonReschedule,
  removeClientFavorite,
  sendClientMessage,
  updateClientSettings,
} from './client';

describe('client live API', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    apiRequest.mockResolvedValue({ ok: true });
  });

  it.each([
    [getClientDashboard, '/client/dashboard'],
    [getClientApplicationsPage, '/client/applications'],
    [getClientSchedulePage, '/client/schedule'],
    [getClientFavoritesPage, '/client/favorites'],
    [getClientHistoryPage, '/client/history'],
    [getClientSettings, '/client/settings'],
  ])('loads %s without fallback data', async (request, path) => {
    await request();

    expect(apiRequest).toHaveBeenCalledWith(path);
  });

  it('loads application details without fallback data', async () => {
    await getClientApplicationDetails('application-1');

    expect(apiRequest).toHaveBeenCalledWith('/client/applications/application-1');
  });

  it('loads a selected client schedule week', async () => {
    await getClientSchedulePage('2026-06-15', '2026-06-21');

    expect(apiRequest).toHaveBeenCalledWith('/client/schedule?from=2026-06-15&to=2026-06-21');
  });

  it('loads a selected message thread without fallback data', async () => {
    await getClientMessagesPage('thread-2');

    expect(apiRequest).toHaveBeenCalledWith('/client/messages?threadId=thread-2');
  });

  it('posts a reschedule request', async () => {
    const payload = {
      message: 'Можно на 19:00?',
      proposedStartAt: '2026-05-07T19:00:00',
    };

    await requestClientLessonReschedule('lesson-1', payload);

    expect(apiRequest).toHaveBeenCalledWith(
      '/client/schedule/lessons/lesson-1/reschedule-request',
      { method: 'POST', body: JSON.stringify(payload) },
    );
  });

  it('sends a message', async () => {
    await sendClientMessage('thread-2', 'Здравствуйте, удобно созвониться?');

    expect(apiRequest).toHaveBeenCalledWith(
      '/client/messages/thread-2',
      {
        method: 'POST',
        body: JSON.stringify({ text: 'Здравствуйте, удобно созвониться?' }),
      },
    );
  });

  it('adds and removes a favorite', async () => {
    await addClientFavorite('tutor-1');
    await removeClientFavorite('tutor-1');

    expect(apiRequest).toHaveBeenNthCalledWith(
      1,
      '/client/favorites/tutor-1',
      { method: 'POST' },
    );
    expect(apiRequest).toHaveBeenNthCalledWith(
      2,
      '/client/favorites/tutor-1',
      { method: 'DELETE' },
    );
  });

  it('cancels an application and updates settings', async () => {
    const settings = {
      metrics: [],
      profile: {
        parentName: 'Иван Петров',
        email: 'parent@example.com',
        phone: '+79990000000',
        city: 'Москва',
        timezone: 'Europe/Moscow',
        language: 'Русский',
      },
      students: [],
      notifications: [],
      privacy: [],
      searchPreferences: [],
    };

    await cancelClientApplication('application-1');
    await updateClientSettings(settings);

    expect(apiRequest).toHaveBeenNthCalledWith(
      1,
      '/client/applications/application-1/cancel',
      { method: 'PATCH' },
    );
    expect(apiRequest).toHaveBeenNthCalledWith(
      2,
      '/client/settings',
      { method: 'PATCH', body: JSON.stringify(settings) },
    );
  });
});
