import { apiRequest } from './http';

export type WorkspaceNotification = {
  id: string;
  eventType: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  metadata?: Record<string, string>;
  actionUrl?: string | null;
};

export type UnreadCountResponse = {
  unreadCount: number;
};

export function getNotifications() {
  return apiRequest<WorkspaceNotification[]>('/notifications');
}

export function getUnreadNotificationCount() {
  return apiRequest<UnreadCountResponse>('/notifications/unread-count');
}

export function markNotificationRead(id: string) {
  return apiRequest<WorkspaceNotification>(
    `/notifications/${encodeURIComponent(id)}/read`,
    { method: 'PATCH' },
  );
}

export function markAllNotificationsRead() {
  return apiRequest<UnreadCountResponse>('/notifications/read-all', { method: 'PATCH' });
}
