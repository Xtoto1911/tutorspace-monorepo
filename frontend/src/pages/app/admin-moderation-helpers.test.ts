import { describe, expect, it } from 'vitest';
import {
  emptyAdminDashboardPage,
  emptyAdminSettingsPage,
  emptyAdminSiteStatsPage,
  emptyAdminSubscriptionsPage,
  filterAdminUsers,
  filterAuditEvents,
  filterModeratorComplaints,
  filterModeratorProfiles,
  getMaxGrowthValue,
} from './admin-live-helpers';
import { getAdminUserActions, getDefaultAdminUserId } from './admin-users-helpers';
import { canModerateProfile, getModeratorReviewActions, toModeratorSelectedProfile } from './moderator-review-helpers';

describe('admin users helpers', () => {
  it('selects a non-admin user by default so real actions are available', () => {
    expect(getDefaultAdminUserId([
      {
        id: 'admin-1',
        initials: 'AD',
        name: 'Admin',
        email: 'admin@example.com',
        role: 'ROLE_ADMIN',
        status: 'ACTIVE',
        risk: 'Low',
        tone: 'emerald',
      },
      {
        id: 'tutor-1',
        initials: 'TU',
        name: 'Tutor',
        email: 'tutor@example.com',
        role: 'ROLE_TUTOR',
        status: 'ACTIVE',
        risk: 'Low',
        tone: 'emerald',
      },
    ])).toBe('tutor-1');
  });

  it('exposes supported live actions for a regular user', () => {
    expect(getAdminUserActions({
      id: 'tutor-1',
      initials: 'TU',
      name: 'Tutor',
      email: 'tutor@example.com',
      role: 'ROLE_TUTOR',
      status: 'ACTIVE',
      risk: 'Low',
      tone: 'emerald',
    })).toEqual(['Заблокировать', 'Сделать админом']);
  });

  it('does not expose self-destructive admin actions for admin accounts', () => {
    expect(getAdminUserActions({
      id: 'admin-1',
      initials: 'AD',
      name: 'Admin',
      email: 'admin@example.com',
      role: 'ROLE_ADMIN',
      status: 'ACTIVE',
      risk: 'Low',
      tone: 'emerald',
    })).toEqual([]);
  });
});

describe('admin live helpers', () => {
  it('keeps page defaults empty instead of importing demo rows', () => {
    expect(emptyAdminDashboardPage.audit).toEqual([]);
    expect(emptyAdminSiteStatsPage.growth.labels).toEqual([]);
    expect(emptyAdminSubscriptionsPage.subscriptions).toEqual([]);
    expect(emptyAdminSettingsPage.limits).toEqual([]);
  });

  it('filters admin users by tab and search query', () => {
    const users = [
      {
        id: 'admin-1',
        initials: 'AD',
        name: 'Admin',
        email: 'admin@example.com',
        role: 'ROLE_ADMIN',
        status: 'ACTIVE',
        risk: 'Low' as const,
        tone: 'emerald' as const,
      },
      {
        id: 'moderator-1',
        initials: 'MO',
        name: 'Moderator',
        email: 'moderator@example.com',
        role: 'ROLE_MODERATOR',
        status: 'BLOCKED',
        risk: 'Low' as const,
        tone: 'rose' as const,
      },
    ];

    expect(filterAdminUsers(users, 'Модераторы', '').map((user) => user.id)).toEqual(['moderator-1']);
    expect(filterAdminUsers(users, 'Заблокированные', 'moderator').map((user) => user.id)).toEqual(['moderator-1']);
  });

  it('filters audit, review moderation, and complaint live rows', () => {
    expect(filterAuditEvents([
      {
        id: 'audit-1',
        time: '2026-05-20T08:00:00Z',
        actor: 'admin',
        role: 'admin',
        action: 'USER_BLOCKED',
        target: 'USER:user-1',
        service: 'admin-service',
        severity: 'low',
        status: 'recorded',
        tone: 'indigo',
      },
    ], 'Пользователи', 'blocked')).toHaveLength(1);

    expect(filterModeratorProfiles([
      {
        id: 'review-1',
        itemType: 'REVIEW',
        initials: 'TR',
        name: 'Отзыв о Tutor',
        subject: 'Отзыв',
        type: 'Отзыв',
        status: 'PENDING_MODERATION',
        summary: 'Клиент: отличный урок',
        score: '100%',
        owner: 'profile-1',
        tone: 'amber',
      },
    ], 'Отзывы', 'отличный')).toHaveLength(1);

    expect(filterModeratorComplaints([
      {
        id: 'complaint-1',
        initials: 'ОТ',
        direction: 'Жалоба на отзыв',
        category: 'Отзыв',
        status: 'Ожидает',
        subject: 'Спам',
        summary: 'Некорректный отзыв',
        time: '2026-05-20',
        evidence: 'Есть snapshot',
        owner: 'Очередь',
        tone: 'indigo',
      },
    ], 'Отзывы', 'спам')).toHaveLength(1);
  });

  it('uses a non-zero chart denominator for pages without history', () => {
    expect(getMaxGrowthValue(emptyAdminSiteStatsPage)).toBe(1);
  });
});

describe('moderator review helpers', () => {
  it('disables moderation decisions for the empty live queue', () => {
    expect(canModerateProfile({
      id: 'Нет анкет',
      status: 'Пусто',
      initials: '--',
      name: 'Очередь пуста',
      subject: 'Нет данных',
      experience: '-',
      risk: 'Низкий',
      checklist: [],
      score: '0%',
      note: 'Нет профилей для проверки.',
    })).toBe(false);
  });

  it('builds a selected profile from a clicked queue row', () => {
    const selected = toModeratorSelectedProfile({
      id: 'profile-1',
      itemType: 'TUTOR_PROFILE',
      initials: 'TU',
      name: 'Tutor',
      subject: 'Math',
      type: 'Анкета',
      status: 'Новая',
      summary: 'Проверить описание.',
      score: '80%',
      owner: 'Модератор',
      tone: 'amber',
    }, {
      id: 'Нет анкет',
      status: 'Пусто',
      initials: '--',
      name: 'Очередь пуста',
      subject: 'Нет данных',
      experience: '-',
      risk: 'Низкий',
      checklist: [],
      score: '0%',
      note: 'Нет профилей для проверки.',
    });

    expect(selected).toMatchObject({
      id: 'profile-1',
      itemType: 'TUTOR_PROFILE',
      name: 'Tutor',
      risk: 'Средний',
      score: '80%',
    });
  });

  it('exposes only API-backed actions for reviews', () => {
    const baseSelected = {
      id: 'item-1',
      status: 'PENDING_MODERATION',
      initials: 'IT',
      name: 'Item',
      subject: 'Moderation',
      risk: 'Средний',
      checklist: [],
      score: '0%',
      note: 'Проверить.',
    };

    expect(getModeratorReviewActions({ ...baseSelected, experience: 'Отзыв', itemType: 'REVIEW' })).toEqual([
      { label: 'Опубликовать отзыв', itemType: 'REVIEW', decision: 'approve' },
      { label: 'Отклонить отзыв', itemType: 'REVIEW', decision: 'reject' },
    ]);
    expect(getModeratorReviewActions({ ...baseSelected, experience: 'Анкета', itemType: 'TUTOR_PROFILE' })).toEqual([
      { label: 'Опубликовать анкету', itemType: 'TUTOR_PROFILE', decision: 'approve' },
      { label: 'Отклонить анкету', itemType: 'TUTOR_PROFILE', decision: 'reject' },
    ]);
  });
});
