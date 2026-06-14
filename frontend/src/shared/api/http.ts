import {
  clearAuthSession,
  getAuthToken,
  readAuthSession,
  writeAuthSession,
} from '../auth/session';

const env = import.meta.env ?? {};
const baseUrl = env.VITE_API_BASE_URL ?? '/api';

type BackendRole = 'ROLE_CLIENT' | 'ROLE_TUTOR' | 'ROLE_ADMIN' | 'ROLE_MODERATOR';
type RefreshResponse = { token: string; refreshToken?: string };
type JwtPayload = { sub?: string; userId?: string; role?: BackendRole; name?: string };

const roleFromBackend = {
  ROLE_CLIENT: 'parent',
  ROLE_TUTOR: 'tutor',
  ROLE_ADMIN: 'admin',
  ROLE_MODERATOR: 'moderator',
} as const;

const englishErrorMap: Record<string, string> = {
  'Invalid email or password': 'Неверный email или пароль.',
  'Email already registered': 'Этот email уже зарегистрирован.',
  'Invalid or expired password reset token': 'Ссылка для сброса пароля устарела или недействительна.',
  'Public registration is not allowed for role': 'Регистрация с выбранной ролью недоступна.',
  'User not found': 'Пользователь не найден.',
  'User account is blocked': 'Аккаунт заблокирован. Обратитесь в поддержку.',
  'Invalid refresh token': 'Недействительный refresh-токен. Войдите заново.',
  'Access denied': 'Доступ запрещен. Обратитесь в поддержку.',
  'An error occurred': 'Произошла ошибка. Попробуйте позже или обратитесь в поддержку.',
};

function translateErrorMessage(message: string): string {
  for (const [english, russian] of Object.entries(englishErrorMap)) {
    if (message.toLowerCase().startsWith(english.toLowerCase())) {
      return russian;
    }
  }
  return message;
}

async function readErrorMessage(response: Response, path: string) {
  try {
    const payload = (await response.json()) as { message?: string; error?: string };
    const raw = payload.message ?? payload.error ?? `Request failed for ${path}`;
    return translateErrorMessage(String(raw));
  } catch {
    if (path.startsWith('/auth/') && response.status === 401) {
      return 'Неверный email или пароль.';
    }
    if (path.startsWith('/auth/') && response.status === 403) {
      return 'Доступ запрещен. Обратитесь в поддержку.';
    }
    return `Request failed for ${path}: ${response.status} ${response.statusText}`;
  }
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=');
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return new TextDecoder().decode(bytes);
}

function decodeJwtPayload(token: string): JwtPayload {
  const [, payload] = token.split('.');

  if (!payload) {
    return {};
  }

  try {
    return JSON.parse(decodeBase64Url(payload)) as JwtPayload;
  } catch {
    return {};
  }
}

async function attemptSessionRefresh() {
  const session = readAuthSession();

  if (!session?.refreshToken) {
    return null;
  }

  const response = await fetch(`${baseUrl}/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken: session.refreshToken }),
  });

  if (!response.ok) {
    clearAuthSession();
    return null;
  }

  const payload = (await response.json()) as RefreshResponse;
  const tokenPayload = decodeJwtPayload(payload.token);

  writeAuthSession({
    token: payload.token,
    refreshToken: payload.refreshToken ?? session.refreshToken,
    user: {
      id: tokenPayload.userId ?? session.user.id,
      email: tokenPayload.sub ?? session.user.email,
      name: tokenPayload.name?.trim() || session.user.name,
      role: tokenPayload.role ? roleFromBackend[tokenPayload.role] : session.user.role,
    },
  });

  return payload.token;
}

async function request<T>(
  path: string,
  init: RequestInit | undefined,
  retryOnUnauthorized = true,
) {
  const authToken = getAuthToken();

  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    if (response.status === 401 && retryOnUnauthorized && path !== '/auth/refresh') {
      const refreshedToken = await attemptSessionRefresh();

      if (refreshedToken) {
        return request<T>(path, init, false);
      }
    }

    throw new Error(await readErrorMessage(response, path));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export function apiRequest<T>(path: string, init?: RequestInit) {
  return request<T>(path, init);
}

export const liveApiRequest = apiRequest;
export const authRequest = apiRequest;
