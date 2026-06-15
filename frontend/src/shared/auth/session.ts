import type { AuthSession, Role } from '../types';

const AUTH_SESSION_KEY = 'tutorspace.auth.session';

export function readAuthSession(): AuthSession | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const raw = window.localStorage.getItem(AUTH_SESSION_KEY);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    window.localStorage.removeItem(AUTH_SESSION_KEY);
    return null;
  }
}

export function getAuthToken() {
  return readAuthSession()?.token ?? null;
}

export function writeAuthSession(session: AuthSession) {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session));
}

export function clearAuthSession() {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.removeItem(AUTH_SESSION_KEY);
}

export function hasAuthSession() {
  return readAuthSession() !== null;
}

export function getAppRouteForRole(role: Role) {
  switch (role) {
    case 'tutor':
      return '/app/tutor';
    case 'admin':
      return '/admin';
    case 'moderator':
      return '/moderator';
    default:
      return '/app/client';
  }
}

export function updateSessionUserName(name: string) {
  const current = readAuthSession();
  if (!current) {
    return;
  }
  writeAuthSession({
    ...current,
    user: { ...current.user, name },
  });
}
