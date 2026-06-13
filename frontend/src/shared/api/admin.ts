import { apiRequest } from './http';
import type {
  AdminAuditPageData,
  AdminDashboardPageData,
  AdminSettingsPageData,
  AdminSiteStatsPageData,
  AdminSubscriptionsPageData,
  AdminUsersPageData,
  ModeratorComplaintsPageData,
  ModeratorDashboardPageData,
  ModeratorReviewModerationPageData,
} from '../types';

type BackendComplaint = {
  id: string;
  reportedUserId: string;
  reporterUserId: string;
  targetType?: 'USER' | 'REVIEW' | 'TUTOR_PROFILE';
  targetId?: string | null;
  targetSnapshot?: string | null;
  category: string;
  reason: string;
  description?: string | null;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  moderatedAt?: string | null;
  moderatorId?: string | null;
  resolutionNotes?: string | null;
};

type ComplaintModerationAction =
  | 'HIDE_REVIEW'
  | 'RESTRICT_REVIEW_PUBLICATION'
  | 'HIDE_TUTOR_PROFILE'
  | 'DISMISS';

export type ModeratorReviewModerationItemType = 'REVIEW' | 'TUTOR_PROFILE';
export type ModeratorReviewModerationDecision = 'approve' | 'reject';

const emptyModeratorComplaintsPage: ModeratorComplaintsPageData = {
  metrics: [
    { icon: '!', value: '0', label: 'в очереди', hint: 'Нет открытых жалоб', trend: '0', tone: 'amber' },
    { icon: 'R', value: '0', label: 'на отзывы', hint: 'Жалобы на отзывы', trend: '0', tone: 'indigo' },
    { icon: '✓', value: '0', label: 'решено', hint: 'Одобрено или отклонено', trend: '0', tone: 'emerald' },
  ],
  tabs: ['Все', 'Отзывы', 'Ожидают', 'Решены'],
  complaints: [],
  selected: {
    id: 'Нет жалоб',
    status: 'Пусто',
    subject: 'Очередь модерации пуста',
    copy: 'Новые жалобы на отзывы появятся здесь после отправки пользователями.',
    participants: [],
    evidence: [],
    impact: {
      label: 'Нет активного контекста',
      hint: 'Модератору не требуется действие.',
    },
    actions: [],
  },
  sla: [
    { label: 'Отзывы', value: '0', percent: 0 },
    { label: 'Профили', value: '0', percent: 0 },
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

function formatComplaintCategory(complaint: BackendComplaint) {
  if (complaint.targetType === 'REVIEW') {
    return 'Отзыв';
  }
  if (complaint.targetType === 'TUTOR_PROFILE') {
    return 'Профиль';
  }

  return {
    SPAM: 'Спам',
    HARASSMENT: 'Оскорбления',
    FAKE_PROFILE: 'Профиль',
    INAPPROPRIATE_CONTENT: 'Контент',
    OTHER: 'Другое',
  }[complaint.category] ?? complaint.category;
}

function formatComplaintDirection(complaint: BackendComplaint) {
  if (complaint.targetType === 'REVIEW') {
    return 'Жалоба на отзыв';
  }
  if (complaint.targetType === 'TUTOR_PROFILE') {
    return 'Жалоба на профиль';
  }
  return 'Жалоба на пользователя';
}

function formatComplaintStatus(status: BackendComplaint['status']) {
  return {
    PENDING: 'Ожидает',
    APPROVED: 'Подтверждена',
    REJECTED: 'Отклонена',
  }[status];
}

function complaintTone(complaint: BackendComplaint): 'rose' | 'amber' | 'indigo' | 'slate' {
  if (complaint.status !== 'PENDING') {
    return 'slate';
  }

  return complaint.priority === 'HIGH' ? 'rose' : complaint.targetType === 'REVIEW' ? 'indigo' : 'amber';
}

function complaintActions(complaint: BackendComplaint) {
  if (complaint.status !== 'PENDING') {
    return [];
  }
  if (complaint.targetType === 'REVIEW') {
    return ['Отклонить', 'Удалить отзыв', 'Запретить публикацию отзывов'];
  }
  if (complaint.targetType === 'TUTOR_PROFILE') {
    return ['Отклонить', 'Скрыть профиль'];
  }
  return ['Отклонить'];
}

function mapComplaintDetail(complaint: BackendComplaint): ModeratorComplaintsPageData['selected'] {
  return {
    id: complaint.id,
    status: formatComplaintStatus(complaint.status),
    subject: complaint.reason,
    copy: complaint.targetSnapshot
      ? `${complaint.description ?? 'Жалоба без описания'} Цель: ${complaint.targetSnapshot}`
      : complaint.description ?? 'Жалоба без описания.',
    participants: [
      { initials: 'RP', name: complaint.reporterUserId, role: 'Автор жалобы' },
      { initials: 'TG', name: complaint.reportedUserId, role: 'На кого пожаловались' },
    ],
    evidence: [
      { label: complaint.targetSnapshot ? 'Snapshot цели' : 'Описание жалобы', ready: Boolean(complaint.targetSnapshot || complaint.description) },
      { label: complaint.targetId ? `Цель ${complaint.targetId}` : 'Цель не указана', ready: Boolean(complaint.targetId) },
    ],
    impact: {
      label: complaint.targetType === 'REVIEW'
        ? 'Отзыв или автор отзыва'
        : complaint.targetType === 'TUTOR_PROFILE'
          ? 'Публичная карточка репетитора'
          : 'Требуется проверка',
      hint: complaint.targetType === 'REVIEW'
        ? 'Можно удалить отзыв или запретить автору публиковать новые отзывы.'
        : complaint.targetType === 'TUTOR_PROFILE'
          ? 'При подтверждении профиль репетитора можно скрыть из публичного каталога.'
          : 'Решение зафиксируется в журнале модерации.',
    },
    actions: complaintActions(complaint),
  };
}

function mapModeratorComplaintsPage(complaints: BackendComplaint[]): ModeratorComplaintsPageData {
  if (complaints.length === 0) {
    return emptyModeratorComplaintsPage;
  }

  const pending = complaints.filter((complaint) => complaint.status === 'PENDING').length;
  const reviewComplaints = complaints.filter((complaint) => complaint.targetType === 'REVIEW').length;
  const resolved = complaints.length - pending;
  const selected = complaints[0];
  const selectedById = Object.fromEntries(complaints.map((complaint) => [complaint.id, mapComplaintDetail(complaint)]));

  return {
    ...emptyModeratorComplaintsPage,
    metrics: [
      { icon: '!', value: String(pending), label: 'в очереди', hint: 'Ждут решения', trend: `${pending}`, tone: 'amber' },
      { icon: 'R', value: String(reviewComplaints), label: 'на отзывы', hint: 'Проверка отзывов', trend: `${reviewComplaints}`, tone: 'indigo' },
      { icon: '✓', value: String(resolved), label: 'решено', hint: 'Закрытые жалобы', trend: `${resolved}`, tone: 'emerald' },
    ],
    complaints: complaints.map((complaint, index) => ({
      id: complaint.id,
      initials: complaint.targetType === 'REVIEW' ? 'ОТ' : 'Ж',
      direction: formatComplaintDirection(complaint),
      category: formatComplaintCategory(complaint),
      status: formatComplaintStatus(complaint.status),
      subject: complaint.reason,
      summary: complaint.description || complaint.targetSnapshot || 'Описание не указано.',
      time: complaint.createdAt.slice(0, 10),
      evidence: complaint.targetSnapshot ? 'Есть snapshot' : 'Без вложений',
      owner: complaint.moderatorId ? 'Назначен модератор' : 'Очередь',
      tone: complaintTone(complaint),
      active: index === 0,
    })),
    selected: mapComplaintDetail(selected),
    selectedById,
    sla: [
      { label: 'Отзывы', value: String(reviewComplaints), percent: complaints.length === 0 ? 0 : Math.round((reviewComplaints / complaints.length) * 100) },
      { label: 'Открытые', value: String(pending), percent: complaints.length === 0 ? 0 : Math.round((pending / complaints.length) * 100) },
    ],
  };
}

export function getAdminDashboard() {
  return apiRequest<AdminDashboardPageData>('/admin/dashboard');
}

export function getAdminAlerts() {
  return apiRequest<AdminDashboardPageData['alerts']>('/admin/alerts');
}

export function getAdminUsers() {
  return apiRequest<AdminUsersPageData>('/admin/users');
}

export function getAdminUsersPage() {
  return apiRequest<AdminUsersPageData>('/admin/users');
}

export function updateAdminUser(userId: string, action: string) {
  return apiRequest<{ userId: string; action: string; updatedAt: string }>(`/admin/users/${encodeURIComponent(userId)}`, {
    method: 'PATCH',
    body: JSON.stringify({ action: mapAdminUserAction(action) }),
  });
}

export function createModeratorAccount(input: { email: string; password: string }) {
  return apiRequest<{ id: string; email: string; role: string; status: string }>('/admin/users/moderators', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

function mapAdminUserAction(action: string) {
  if (action === 'Заблокировать' || action === 'block') return 'block';
  if (action === 'Разблокировать' || action === 'unblock') return 'unblock';
  if (action === 'Сделать админом' || action === 'make_admin') return 'make_admin';
  return action;
}

export function getAdminSiteStats() {
  return apiRequest<AdminSiteStatsPageData>('/admin/site-stats');
}

export function getAdminSiteStatsPage() {
  return apiRequest<AdminSiteStatsPageData>('/admin/site-stats');
}

export function getAdminSubscriptions() {
  return apiRequest<AdminSubscriptionsPageData>('/admin/subscriptions');
}

export function getAdminSubscriptionsPage() {
  return apiRequest<AdminSubscriptionsPageData>('/admin/subscriptions');
}

export function getAdminAudit() {
  return apiRequest<AdminAuditPageData>('/admin/audit');
}

export function getAdminSettingsPage() {
  return apiRequest<AdminSettingsPageData>('/admin/settings');
}

export function getModeratorComplaints() {
  return apiRequest<BackendComplaint[]>('/moderator/complaints');
}

export function getModeratorComplaintsPage() {
  return apiRequest<BackendComplaint[] | ModeratorComplaintsPageData>(
    '/moderator/complaints',
  ).then((data) => (Array.isArray(data) ? mapModeratorComplaintsPage(data) : data));
}

export function getModeratorDashboard() {
  return apiRequest<ModeratorDashboardPageData>('/moderator/dashboard');
}

function resolveComplaintAction(action: string): {
  status: 'APPROVED' | 'REJECTED';
  resolutionNotes: string;
  action: ComplaintModerationAction;
} {
  if (action === 'HIDE_REVIEW' || action === 'Удалить отзыв') {
    return { status: 'APPROVED', resolutionNotes: 'Удалить отзыв', action: 'HIDE_REVIEW' };
  }
  if (action === 'RESTRICT_REVIEW_PUBLICATION' || action === 'Запретить публикацию отзывов') {
    return {
      status: 'APPROVED',
      resolutionNotes: 'Запретить публикацию отзывов',
      action: 'RESTRICT_REVIEW_PUBLICATION',
    };
  }
  if (action === 'HIDE_TUTOR_PROFILE' || action === 'Скрыть профиль') {
    return { status: 'APPROVED', resolutionNotes: 'Скрыть профиль', action: 'HIDE_TUTOR_PROFILE' };
  }
  return { status: 'REJECTED', resolutionNotes: 'Отклонить жалобу', action: 'DISMISS' };
}

export function resolveModeratorComplaint(complaintId: string, action: string) {
  const decision = resolveComplaintAction(action);

  return apiRequest(`/moderator/complaints/${encodeURIComponent(complaintId)}/moderate`, {
    method: 'PATCH',
    body: JSON.stringify(decision),
  });
}

export function getModeratorReviewModeration() {
  return apiRequest<ModeratorReviewModerationPageData>('/moderator/review-moderation');
}

export function getModeratorReviewModerationPage() {
  return apiRequest<ModeratorReviewModerationPageData>('/moderator/review-moderation');
}

export function decideModeratorReviewModerationItem(
  itemType: ModeratorReviewModerationItemType,
  itemId: string,
  decision: ModeratorReviewModerationDecision,
) {
  const area = itemType === 'REVIEW' ? 'review-moderation' : 'profile-moderation';
  const route = `/moderator/${area}/${encodeURIComponent(itemId)}/${decision}`;

  return apiRequest(route, {
    method: 'POST',
    body: JSON.stringify({ itemId, itemType, decision }),
  });
}
