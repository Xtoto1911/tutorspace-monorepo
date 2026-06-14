import { apiRequest } from './http';
import type {
  ClientApplicationDetailsData,
  ClientApplicationsPageData,
  ClientFavoritesPageData,
  ClientHistoryPageData,
  ClientMessagesPageData,
  ClientOverviewPageData,
  ClientSchedulePageData,
  ClientSettings,
} from '../types';

export function getClientDashboard() {
  return apiRequest<ClientOverviewPageData>('/client/dashboard');
}

export function getClientApplications() {
  return apiRequest<ClientApplicationsPageData>('/client/applications');
}

export function getClientApplicationsPage() {
  return apiRequest<ClientApplicationsPageData>('/client/applications');
}

export function getClientApplicationDetails(applicationId: string) {
  return apiRequest<ClientApplicationDetailsData>(`/client/applications/${applicationId}`);
}

export function getClientSchedule() {
  return apiRequest<ClientSchedulePageData>('/client/schedule');
}

export function getClientSchedulePage(from?: string, to?: string) {
  const path = from && to
    ? `/client/schedule?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    : '/client/schedule';
  return apiRequest<ClientSchedulePageData>(path);
}

export function requestClientLessonReschedule(
  lessonId: string,
  payload: { message: string; proposedStartAt?: string },
) {
  return apiRequest<{ id: string; status: string }>(
    `/client/schedule/lessons/${lessonId}/reschedule-request`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
  );
}

export function getClientMessages() {
  return apiRequest<ClientMessagesPageData>('/client/messages');
}

export function getClientMessagesPage(threadId?: string | null) {
  const path = threadId ? `/client/messages?threadId=${encodeURIComponent(threadId)}` : '/client/messages';
  return apiRequest<ClientMessagesPageData>(path);
}

export function getClientFavorites() {
  return apiRequest<ClientFavoritesPageData>('/client/favorites');
}

export function getClientFavoritesPage() {
  return apiRequest<ClientFavoritesPageData>('/client/favorites');
}

export function addClientFavorite(tutorId: string) {
  return apiRequest<{ tutorId: string; favorite: boolean }>(
    `/client/favorites/${tutorId}`,
    { method: 'POST' },
  );
}

export function removeClientFavorite(tutorId: string) {
  return apiRequest<{ tutorId: string; favorite: boolean }>(
    `/client/favorites/${tutorId}`,
    { method: 'DELETE' },
  );
}

export function getClientHistory() {
  return apiRequest<ClientHistoryPageData>('/client/history');
}

export function getClientHistoryPage() {
  return apiRequest<ClientHistoryPageData>('/client/history');
}

export function getClientSettings() {
  return apiRequest<ClientSettings>('/client/settings');
}

export function cancelClientApplication(applicationId: string) {
  return apiRequest<ClientApplicationDetailsData>(`/client/applications/${applicationId}/cancel`, {
    method: 'PATCH',
  });
}

export function sendClientMessage(threadId: string, text: string) {
  return apiRequest<{ id: string; threadId: string; text: string; status: 'sent' }>(
    `/client/messages/${threadId}`,
    {
      method: 'POST',
      body: JSON.stringify({ text }),
    },
  );
}

export function updateClientSettings(payload: ClientSettings) {
  return apiRequest<ClientSettings>('/client/settings', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}
