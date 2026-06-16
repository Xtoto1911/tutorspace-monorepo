import { apiRequest } from './http';

export type UpdateAuthProfilePayload = {
  name: string;
};

export type AuthPublicUser = {
  id: string;
  email?: string;
  name: string;
  role?: string;
  status?: string;
};

export function updateAuthProfile(payload: UpdateAuthProfilePayload) {
  return apiRequest<AuthPublicUser>('/auth/me', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}
