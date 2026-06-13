import { beforeEach, describe, expect, it, vi } from 'vitest';

const { apiRequest } = vi.hoisted(() => ({
  apiRequest: vi.fn(),
}));

vi.mock('./http', () => ({
  apiRequest,
}));

import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from './notifications';

describe('notification api helpers', () => {
  beforeEach(() => {
    apiRequest.mockReset();
  });

  it('loads current user notifications', async () => {
    apiRequest.mockResolvedValueOnce([]);

    await getNotifications();

    expect(apiRequest).toHaveBeenCalledWith('/notifications');
  });

  it('loads unread notification count', async () => {
    apiRequest.mockResolvedValueOnce({ unreadCount: 2 });

    await expect(getUnreadNotificationCount()).resolves.toEqual({ unreadCount: 2 });
    expect(apiRequest).toHaveBeenCalledWith('/notifications/unread-count');
  });

  it('marks one notification read', async () => {
    apiRequest.mockResolvedValueOnce({ id: 'n-1', read: true });

    await markNotificationRead('n-1');

    expect(apiRequest).toHaveBeenCalledWith('/notifications/n-1/read', { method: 'PATCH' });
  });

  it('marks all notifications read', async () => {
    apiRequest.mockResolvedValueOnce({ unreadCount: 0 });

    await markAllNotificationsRead();

    expect(apiRequest).toHaveBeenCalledWith('/notifications/read-all', { method: 'PATCH' });
  });
});
