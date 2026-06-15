import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiRequest } from './http';
import { updateAuthProfile } from './auth';

vi.mock('./http', () => ({
  apiRequest: vi.fn(),
}));

describe('updateAuthProfile', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('PATCHes the new name to /auth/me', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ id: 'user-1', name: 'Пётр' });

    const result = await updateAuthProfile({ name: 'Пётр' });

    expect(apiRequest).toHaveBeenCalledWith('/auth/me', {
      method: 'PATCH',
      body: JSON.stringify({ name: 'Пётр' }),
    });
    expect(result).toEqual({ id: 'user-1', name: 'Пётр' });
  });
});
