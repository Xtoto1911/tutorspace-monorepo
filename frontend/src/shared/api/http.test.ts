import { beforeEach, describe, expect, it, vi } from 'vitest';
import { readAuthSession, writeAuthSession } from '../auth/session';
import { apiRequest, authRequest, liveApiRequest } from './http';

const session = {
  token: 'expired-token',
  refreshToken: 'refresh-token',
  user: {
    id: 'user-1',
    email: 'user@example.com',
    name: 'User',
    role: 'parent' as const,
  },
};

function response(
  status: number,
  payload?: unknown,
  statusText = '',
) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText,
    json: vi.fn().mockResolvedValue(payload),
  } as unknown as Response;
}

describe('apiRequest', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    const storage = new Map<string, string>();
    vi.stubGlobal('window', {
      localStorage: {
        clear: () => storage.clear(),
        getItem: (key: string) => storage.get(key) ?? null,
        removeItem: (key: string) => storage.delete(key),
        setItem: (key: string, value: string) => storage.set(key, value),
      },
    });
    window.localStorage.clear();
  });

  it('always calls fetch through its two-argument live-only public API', async () => {
    const fetchMock = vi.fn().mockResolvedValue(response(200, { ok: true }));
    vi.stubGlobal('fetch', fetchMock);

    const result = await apiRequest<{ ok: boolean }>('/subjects', {
      method: 'GET',
    });

    expect(apiRequest).toHaveLength(2);
    expect(liveApiRequest).toBe(apiRequest);
    expect(authRequest).toBe(apiRequest);
    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledOnce();
    expect(fetchMock).toHaveBeenCalledWith('/api/subjects', expect.objectContaining({
      method: 'GET',
    }));
  });

  it('keeps JSON, bearer, and request-specific headers', async () => {
    writeAuthSession(session);
    const fetchMock = vi.fn().mockResolvedValue(response(200, { ok: true }));
    vi.stubGlobal('fetch', fetchMock);

    await apiRequest('/tutor/profile', {
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer registered-token',
        'X-Request-Id': 'request-1',
      },
      body: JSON.stringify({ identity: { name: 'Анна' } }),
    });

    expect(fetchMock).toHaveBeenCalledWith('/api/tutor/profile', expect.objectContaining({
      method: 'PATCH',
      body: JSON.stringify({ identity: { name: 'Анна' } }),
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        Authorization: 'Bearer registered-token',
        'X-Request-Id': 'request-1',
      }),
    }));
  });

  it('returns undefined for a 204 response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response(204)));

    await expect(apiRequest('/notifications/read', {
      method: 'POST',
    })).resolves.toBeUndefined();
  });

  it.each([
    [{ message: 'Profile is incomplete' }, 'Profile is incomplete'],
    [{ error: 'Access denied' }, 'Access denied'],
  ])('parses structured backend errors from %j', async (payload, expectedMessage) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response(400, payload)));

    await expect(apiRequest('/profile')).rejects.toThrow(expectedMessage);
  });

  it('refreshes once after a 401 and retries with the new bearer token', async () => {
    writeAuthSession(session);
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response(401, { message: 'Expired' }))
      .mockResolvedValueOnce(response(200, {
        token: 'header.eyJzdWIiOiJuZXdlckBleGFtcGxlLmNvbSIsInVzZXJJZCI6InVzZXItMiIsInJvbGUiOiJST0xFX1RVVE9SIn0.signature',
        refreshToken: 'new-refresh-token',
      }))
      .mockResolvedValueOnce(response(200, { id: 'profile-1' }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiRequest('/tutor/profile')).resolves.toEqual({ id: 'profile-1' });

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/auth/refresh', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ refreshToken: 'refresh-token' }),
    }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, '/api/tutor/profile', expect.objectContaining({
      headers: expect.objectContaining({
        Authorization: expect.stringContaining('Bearer header.'),
      }),
    }));
    expect(readAuthSession()).toMatchObject({
      token: expect.stringContaining('header.'),
      refreshToken: 'new-refresh-token',
      user: {
        id: 'user-2',
        email: 'newer@example.com',
        role: 'tutor',
      },
    });
  });

  it('does not retry again and clears the session when refresh fails', async () => {
    writeAuthSession(session);
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response(401, { message: 'Expired' }))
      .mockResolvedValueOnce(response(401, { message: 'Refresh expired' }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiRequest('/tutor/profile')).rejects.toThrow('Expired');

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(readAuthSession()).toBeNull();
  });
});
