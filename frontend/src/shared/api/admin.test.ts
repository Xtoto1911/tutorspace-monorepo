import { beforeEach, describe, expect, it, vi } from 'vitest';

const { apiRequest, liveApiRequest } = vi.hoisted(() => ({
  apiRequest: vi.fn(),
  liveApiRequest: vi.fn(),
}));

vi.mock('./http', () => ({
  apiRequest,
  liveApiRequest,
}));

import {
  createModeratorAccount,
  decideModeratorReviewModerationItem,
  getAdminAudit,
  getAdminDashboard,
  getAdminSettingsPage,
  getAdminSiteStatsPage,
  getAdminSubscriptionsPage,
  getAdminUsersPage,
  getModeratorDashboard,
  getModeratorComplaintsPage,
  getModeratorReviewModerationPage,
  resolveModeratorComplaint,
  updateAdminUser,
} from './admin';

describe('moderator complaints API', () => {
  beforeEach(() => {
    apiRequest.mockReset();
    liveApiRequest.mockReset();
  });

  it.each([
    ['dashboard', getAdminDashboard, '/admin/dashboard'],
    ['users', getAdminUsersPage, '/admin/users'],
    ['site stats', getAdminSiteStatsPage, '/admin/site-stats'],
    ['subscriptions', getAdminSubscriptionsPage, '/admin/subscriptions'],
    ['audit', getAdminAudit, '/admin/audit'],
    ['settings', getAdminSettingsPage, '/admin/settings'],
  ])('loads admin %s from the live endpoint', async (_label, loader, route) => {
    apiRequest.mockResolvedValueOnce({});

    await loader();

    expect(apiRequest).toHaveBeenCalledWith(route);
  });

  it('propagates admin dashboard failures', async () => {
    apiRequest.mockRejectedValueOnce(new Error('admin-service unavailable'));

    await expect(getAdminDashboard()).rejects.toThrow('admin-service unavailable');
  });

  it('loads the moderator dashboard from the live endpoint', async () => {
    apiRequest.mockResolvedValueOnce({
      pendingComplaints: 2,
      pendingReviews: 1,
      pendingProfiles: 3,
      totalPending: 6,
      recentDecisions: [],
    });

    await getModeratorDashboard();

    expect(apiRequest).toHaveBeenCalledWith('/moderator/dashboard');
  });

  it('maps review complaints from the live endpoint', async () => {
    apiRequest.mockResolvedValueOnce([
      {
        id: 'complaint-1',
        reportedUserId: 'tutor-1',
        reporterUserId: 'client-1',
        targetType: 'REVIEW',
        targetId: 'review-1',
        targetSnapshot: 'Некорректный отзыв',
        category: 'INAPPROPRIATE_CONTENT',
        reason: 'Спам',
        description: 'Похоже на накрутку',
        priority: 'MEDIUM',
        status: 'PENDING',
        createdAt: '2026-05-14T10:00:00Z',
        moderatedAt: null,
        moderatorId: null,
        resolutionNotes: null,
      },
    ]);

    const page = await getModeratorComplaintsPage();

    expect(apiRequest).toHaveBeenCalledWith('/moderator/complaints');
    expect(liveApiRequest).not.toHaveBeenCalled();
    expect(page.complaints).toHaveLength(1);
    expect(page.complaints[0]).toMatchObject({
      id: 'complaint-1',
      category: 'Отзыв',
      subject: 'Спам',
      summary: 'Похоже на накрутку',
    });
    expect(page.selected.copy).toContain('Некорректный отзыв');
  });

  it('posts moderator complaint decisions to the selected complaint route', async () => {
    apiRequest.mockResolvedValueOnce({ id: 'complaint-1', status: 'APPROVED' });

    await resolveModeratorComplaint('complaint-1', 'HIDE_REVIEW');

    expect(apiRequest).toHaveBeenCalledWith(
      '/moderator/complaints/complaint-1/moderate',
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'APPROVED',
          resolutionNotes: 'Удалить отзыв',
          action: 'HIDE_REVIEW',
        }),
      },
    );
  });

  it('posts review publication restrictions as an approved moderation action', async () => {
    apiRequest.mockResolvedValueOnce({ id: 'complaint-1', status: 'APPROVED' });

    await resolveModeratorComplaint('complaint-1', 'RESTRICT_REVIEW_PUBLICATION');

    expect(apiRequest).toHaveBeenCalledWith(
      '/moderator/complaints/complaint-1/moderate',
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'APPROVED',
          resolutionNotes: 'Запретить публикацию отзывов',
          action: 'RESTRICT_REVIEW_PUBLICATION',
        }),
      },
    );
  });

  it('maps tutor profile complaints for moderator queue', async () => {
    apiRequest.mockResolvedValueOnce([
      {
        id: 'complaint-2',
        reportedUserId: 'tutor-2',
        reporterUserId: 'client-1',
        targetType: 'TUTOR_PROFILE',
        targetId: 'profile-1',
        targetSnapshot: 'Профиль репетитора: Анна Иванова',
        category: 'FAKE_PROFILE',
        reason: 'Некорректный профиль',
        description: 'Нет подтверждения опыта',
        priority: 'MEDIUM',
        status: 'PENDING',
        createdAt: '2026-05-14T10:00:00Z',
        moderatedAt: null,
        moderatorId: null,
        resolutionNotes: null,
      },
    ]);

    const page = await getModeratorComplaintsPage();

    expect(page.complaints[0]).toMatchObject({
      direction: 'Жалоба на профиль',
      category: 'Профиль',
      subject: 'Некорректный профиль',
    });
    expect(page.selected.actions).toContain('Скрыть профиль');
  });

  it('patches admin user actions to the concrete user route', async () => {
    apiRequest.mockResolvedValueOnce({ userId: 'user-1', action: 'block' });

    await updateAdminUser('user-1', 'Заблокировать');

    expect(apiRequest).toHaveBeenCalledWith(
      '/admin/users/user-1',
      {
        method: 'PATCH',
        body: JSON.stringify({ action: 'block' }),
      },
    );
    expect(apiRequest.mock.calls[0][0]).not.toContain(':id');
  });

  it('creates moderator accounts through the dedicated admin route', async () => {
    apiRequest.mockResolvedValueOnce({ id: 'moderator-1', role: 'ROLE_MODERATOR' });

    await createModeratorAccount({
      email: 'moderator@example.com',
      password: 'moderator-password',
    });

    expect(apiRequest).toHaveBeenCalledWith(
      '/admin/users/moderators',
      {
        method: 'POST',
        body: JSON.stringify({
          email: 'moderator@example.com',
          password: 'moderator-password',
        }),
      },
    );
  });

  it('loads moderator review queue from the live endpoint', async () => {
    apiRequest.mockResolvedValueOnce({ profiles: [], metrics: [] });

    await getModeratorReviewModerationPage();

    expect(apiRequest).toHaveBeenCalledWith('/moderator/review-moderation');
    expect(liveApiRequest).not.toHaveBeenCalled();
  });

  it('posts moderator review approval to the concrete live review route', async () => {
    apiRequest.mockResolvedValueOnce({ profileId: 'review-1', decision: 'approve' });

    await decideModeratorReviewModerationItem('REVIEW', 'review-1', 'approve');

    expect(apiRequest).toHaveBeenCalledWith(
      '/moderator/review-moderation/review-1/approve',
      {
        method: 'POST',
        body: JSON.stringify({ itemId: 'review-1', itemType: 'REVIEW', decision: 'approve' }),
      },
    );
    expect(apiRequest.mock.calls[0][0]).not.toContain(':id');
  });

  it('posts moderator review rejection to the concrete live review route', async () => {
    apiRequest.mockResolvedValueOnce({ profileId: 'review-1', decision: 'reject' });

    await decideModeratorReviewModerationItem('REVIEW', 'review-1', 'reject');

    expect(apiRequest).toHaveBeenCalledWith(
      '/moderator/review-moderation/review-1/reject',
      {
        method: 'POST',
        body: JSON.stringify({ itemId: 'review-1', itemType: 'REVIEW', decision: 'reject' }),
      },
    );
    expect(apiRequest.mock.calls[0][0]).not.toContain(':id');
  });

  it('routes tutor profile decisions to the profile moderation endpoint', async () => {
    apiRequest.mockResolvedValueOnce({ profileId: 'profile-1', decision: 'approve' });

    await decideModeratorReviewModerationItem('TUTOR_PROFILE', 'profile-1', 'approve');

    expect(apiRequest).toHaveBeenCalledWith(
      '/moderator/profile-moderation/profile-1/approve',
      {
        method: 'POST',
        body: JSON.stringify({ itemId: 'profile-1', itemType: 'TUTOR_PROFILE', decision: 'approve' }),
      },
    );
  });

});
