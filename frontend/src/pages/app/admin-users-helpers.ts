import type { AdminUsersPageData } from '../../shared/types';

type AdminUser = AdminUsersPageData['users'][number];

const blockedStatuses = new Set(['BLOCKED', 'Блок', 'Заблокирован']);

export function getDefaultAdminUserId(users: AdminUser[]) {
  return users.find((user) => user.role !== 'ROLE_ADMIN')?.id ?? users[0]?.id ?? '';
}

export function getAdminUserActions(user: AdminUser | undefined) {
  if (!user || user.role === 'ROLE_ADMIN') {
    return [];
  }

  const restrictionAction = blockedStatuses.has(user.status) ? 'Разблокировать' : 'Заблокировать';

  return [restrictionAction, 'Сделать админом'];
}

export function toAdminUserDetail(user: AdminUser | undefined, fallback: AdminUsersPageData['selected']) {
  if (!user) {
    return fallback;
  }

  return {
    initials: user.initials,
    name: user.name,
    email: user.email,
    role: user.role,
    status: user.status,
    riskTitle: `${user.risk} risk`,
    signals: [
      { title: 'Источник', meta: 'auth-service' },
      { title: 'ID', meta: user.id },
    ],
    actions: getAdminUserActions(user),
  };
}
