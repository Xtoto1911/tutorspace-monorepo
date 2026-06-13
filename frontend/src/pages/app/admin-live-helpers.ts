import type {
  AdminAuditPageData,
  AdminDashboardPageData,
  AdminSettingsPageData,
  AdminSiteStatsPageData,
  AdminSubscriptionsPageData,
  AdminUsersPageData,
  ModeratorComplaintsPageData,
  ModeratorReviewModerationPageData,
} from '../../shared/types';

export const emptyAdminDashboardPage: AdminDashboardPageData = {
  metrics: [
    { icon: 'users', label: 'Пользователи', value: '0', delta: 'live auth-service', tone: 'indigo' },
    { icon: 'shield', label: 'Жалобы', value: '0', delta: 'pending', tone: 'amber' },
    { icon: 'credit-card', label: 'Подписки', value: '0', delta: 'live subscription-service', tone: 'teal' },
    { icon: 'activity', label: 'Аудит', value: '0', delta: 'admin-service', tone: 'rose' },
  ],
  services: [],
  alerts: [],
  moderationQueue: [],
  quickActions: [],
  audit: [],
};

export const emptyAdminUsersPage: AdminUsersPageData = {
  metrics: [
    { icon: 'users', value: '0', title: 'Пользователи', meta: 'live auth-service', tone: 'indigo' },
    { icon: 'shield', value: '0', title: 'Администраторы', meta: 'role count', tone: 'emerald' },
  ],
  tabs: ['Все', 'Админы', 'Модераторы', 'Заблокированные'],
  users: [],
  selected: {
    initials: '--',
    name: 'Нет пользователей',
    email: '',
    role: '',
    status: 'Пусто',
    riskTitle: 'Нет данных',
    signals: [],
    actions: [],
  },
  cards: {
    audit: { title: 'Нет пользователей', text: 'Auth-service вернул пустой список.', action: '' },
    roles: { label: 'Роли', value: '0', text: 'Нет назначенных ролей.', action: '' },
    moderation: { title: 'Модерация', text: 'Нет данных.', action: '' },
    note: { title: 'Заметка', text: 'Демо-данные не используются.', action: '' },
  },
};

export const emptyAdminSiteStatsPage: AdminSiteStatsPageData = {
  metrics: [],
  tabs: ['Обзор', 'Пользователи', 'Модерация'],
  growth: {
    title: 'Рост',
    caption: 'Доступны только текущие live-агрегаты.',
    labels: [],
    users: [],
    requests: [],
    footer: 'История пока не хранится.',
  },
  conversion: { label: 'Конверсия', value: '0%', text: 'Нет текущего источника конверсий.' },
  quality: [],
  funnel: [],
  finance: { title: 'Выручка', value: '0 ₽', caption: 'Нет подтвержденных платежей.', trend: '0%' },
  geo: [],
  events: [],
  note: { title: 'Данные', text: 'Показаны только доступные live-агрегаты.', action: 'Обновить' },
};

export const emptyAdminSubscriptionsPage: AdminSubscriptionsPageData = {
  metrics: [
    { icon: 'credit-card', value: '0', label: 'Подписки', hint: 'live subscription-service', tone: 'emerald' },
    { icon: 'wallet', value: '0', label: 'Платежи, ₽', hint: 'confirmed snapshot', tone: 'indigo' },
  ],
  subscriptions: [],
  revenueByPlan: [],
  webhookQueue: [],
  planSettings: [],
};

export const emptyAdminAuditPage: AdminAuditPageData = {
  metrics: [{ icon: 'activity', value: '0', label: 'События', hint: 'admin-service', tone: 'indigo' }],
  tabs: ['Все', 'Пользователи', 'Модерация', 'Система'],
  events: [],
  selected: {
    id: '',
    title: 'Нет событий',
    actor: '',
    description: 'Журнал аудита пуст.',
    meta: [],
    payload: [],
  },
  policies: [],
  exports: [],
};

export const emptyAdminSettingsPage: AdminSettingsPageData = {
  metrics: [{ icon: 'settings', value: '0', label: 'Пользователи', hint: 'live auth-service', tone: 'indigo' }],
  security: [],
  roles: [],
  limits: [],
  services: [],
  policies: [],
};

export const emptyModeratorComplaintsPage: ModeratorComplaintsPageData = {
  metrics: [
    { icon: '!', value: '0', label: 'в очереди', hint: 'Нет открытых жалоб', trend: '0', tone: 'amber' },
    { icon: 'R', value: '0', label: 'на отзывы', hint: 'Жалобы на отзывы', trend: '0', tone: 'indigo' },
    { icon: '✓', value: '0', label: 'решено', hint: 'Одобрено или отклонено', trend: '0', tone: 'emerald' },
  ],
  tabs: ['Все', 'Отзывы', 'Профили', 'Ожидают', 'Решены'],
  complaints: [],
  selected: {
    id: 'Нет жалоб',
    status: 'Пусто',
    subject: 'Очередь модерации пуста',
    copy: 'Новые жалобы появятся здесь после отправки пользователями.',
    participants: [],
    evidence: [],
    impact: { label: 'Нет активного контекста', hint: 'Модератору не требуется действие.' },
    actions: [],
  },
  sla: [
    { label: 'Отзывы', value: '0', percent: 0 },
    { label: 'Открытые', value: '0', percent: 0 },
  ],
  weekly: {
    bars: [0, 0, 0, 0, 0, 0, 0],
    labels: ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'],
    hint: 'Решений за неделю пока нет.',
  },
  rules: {
    title: 'Регламент жалоб',
    body: 'Проверяйте контекст, snapshot и тип цели перед решением.',
    tags: ['review', 'moderation', 'trust'],
  },
};

export const emptyModeratorReviewModerationPage: ModeratorReviewModerationPageData = {
  metrics: [
    { icon: 'shield', value: '0', label: 'в очереди', hint: 'Нет live-заявок', trend: '0', tone: 'amber' },
  ],
  tabs: ['Все', 'Анкеты', 'Отзывы'],
  profiles: [],
  selected: {
    id: 'Нет анкет',
    status: 'Пусто',
    initials: '--',
    name: 'Очередь пуста',
    subject: 'Нет данных',
    experience: '-',
    risk: 'Низкий',
    checklist: [],
    score: '0%',
    note: 'Нет профилей или отзывов для проверки.',
  },
  pipeline: [{ label: 'Всего', value: '0', percent: 0 }],
  rubric: ['Проверить публичное описание', 'Не публиковать спорные отзывы'],
  decision: {
    title: 'Решение модератора',
    body: 'Доступны только действия, поддержанные API.',
    tags: ['live', 'audit'],
  },
};

export function getReadableError(error: unknown, fallback = 'Не удалось загрузить live-данные.') {
  return error instanceof Error && error.message ? error.message : fallback;
}

export function filterAdminUsers(users: AdminUsersPageData['users'], tab: string, query: string) {
  const normalizedQuery = query.trim().toLowerCase();
  return users.filter((user) => {
    const matchesTab =
      tab === 'Все'
      || (tab === 'Админы' && user.role === 'ROLE_ADMIN')
      || (tab === 'Модераторы' && user.role === 'ROLE_MODERATOR')
      || (tab === 'Заблокированные' && ['BLOCKED', 'Блок', 'Заблокирован'].includes(user.status));
    const matchesQuery = !normalizedQuery
      || [user.id, user.name, user.email, user.role, user.status].join(' ').toLowerCase().includes(normalizedQuery);
    return matchesTab && matchesQuery;
  });
}

export function filterAuditEvents(events: AdminAuditPageData['events'], tab: string, query: string) {
  const normalizedQuery = query.trim().toLowerCase();
  return events.filter((event) => {
    const matchesTab =
      tab === 'Все'
      || (tab === 'Пользователи' && event.target.toLowerCase().includes('user'))
      || (tab === 'Модерация' && (event.action.toLowerCase().includes('moderation') || event.target.toLowerCase().includes('review')))
      || (tab === 'Система' && event.service.toLowerCase().includes('admin-service'));
    const matchesQuery = !normalizedQuery
      || [event.id, event.actor, event.action, event.target, event.service, event.status].join(' ').toLowerCase().includes(normalizedQuery);
    return matchesTab && matchesQuery;
  });
}

export function filterModeratorProfiles(
  profiles: ModeratorReviewModerationPageData['profiles'],
  tab: string,
  query: string,
) {
  const normalizedQuery = query.trim().toLowerCase();
  return profiles.filter((profile) => {
    const matchesTab =
      tab === 'Все'
      || (tab === 'Анкеты' && profile.type === 'Анкета')
      || (tab === 'Отзывы' && profile.type === 'Отзыв');
    const matchesQuery = !normalizedQuery
      || [profile.name, profile.subject, profile.type, profile.status, profile.summary, profile.owner].join(' ').toLowerCase().includes(normalizedQuery);
    return matchesTab && matchesQuery;
  });
}

export function filterModeratorComplaints(
  complaints: ModeratorComplaintsPageData['complaints'],
  tab: string,
  query: string,
) {
  const normalizedQuery = query.trim().toLowerCase();
  return complaints.filter((complaint) => {
    const matchesTab =
      tab === 'Все'
      || (tab === 'Отзывы' && complaint.direction.toLowerCase().includes('отзыв'))
      || (tab === 'Профили' && complaint.direction.toLowerCase().includes('профиль'))
      || (tab === 'Ожидают' && complaint.status === 'Ожидает')
      || (tab === 'Решены' && complaint.status !== 'Ожидает');
    const matchesQuery = !normalizedQuery
      || [
        complaint.id,
        complaint.direction,
        complaint.category,
        complaint.status,
        complaint.subject,
        complaint.summary,
        complaint.owner,
      ].join(' ').toLowerCase().includes(normalizedQuery);
    return matchesTab && matchesQuery;
  });
}

export function getMaxGrowthValue(page: AdminSiteStatsPageData) {
  return Math.max(1, ...page.growth.users, ...page.growth.requests);
}
