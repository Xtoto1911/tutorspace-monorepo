import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  readAuthSession,
  updateSessionUserName,
  writeAuthSession,
} from './session';
import type { AuthSession } from '../types';

const baseSession: AuthSession = {
  token: 'token-1',
  refreshToken: 'refresh-1',
  user: {
    id: 'user-1',
    email: 'tutor@example.com',
    name: 'Иван',
    role: 'tutor',
  },
};

describe('updateSessionUserName', () => {
  beforeEach(() => {
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

  it('updates the name in the persisted session', () => {
    writeAuthSession(baseSession);
    updateSessionUserName('Пётр');
    const session = readAuthSession();
    expect(session?.user.name).toBe('Пётр');
    expect(session?.user.id).toBe('user-1');
    expect(session?.user.email).toBe('tutor@example.com');
  });

  it('is a no-op when no session is stored', () => {
    updateSessionUserName('Пётр');
    expect(readAuthSession()).toBeNull();
  });
});
