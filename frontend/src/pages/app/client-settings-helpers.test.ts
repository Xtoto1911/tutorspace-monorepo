import { describe, expect, it } from 'vitest';
import {
  getClientInitials,
  setClientProfileField,
  validateClientProfile,
} from './client-settings-helpers';
import type { ClientSettings } from '../../shared/types';

const baseSettings: ClientSettings = {
  metrics: [],
  profile: {
    parentName: 'Анна Родионова',
    phone: '+79990000000',
    email: 'anna@example.com',
    city: 'Москва',
    timezone: 'Europe/Moscow',
    language: 'Русский',
  },
  students: [],
  notifications: [],
  privacy: [],
  searchPreferences: [],
};

describe('client settings helpers', () => {
  it('updates parentName and email immutably', () => {
    const renamed = setClientProfileField(baseSettings, 'parentName', 'Мария Иванова');
    const reEmaild = setClientProfileField(renamed, 'email', 'maria@example.com');

    expect(renamed.profile.parentName).toBe('Мария Иванова');
    expect(renamed.profile.email).toBe('anna@example.com');
    expect(renamed).not.toBe(baseSettings);

    expect(reEmaild.profile.email).toBe('maria@example.com');
    expect(reEmaild.profile.parentName).toBe('Мария Иванова');
    expect(reEmaild).not.toBe(renamed);
    expect(baseSettings.profile.parentName).toBe('Анна Родионова');
  });

  it('returns no errors for a valid profile', () => {
    expect(validateClientProfile(baseSettings)).toEqual({});
  });

  it('rejects an empty or single-character name', () => {
    const empty = validateClientProfile({
      ...baseSettings,
      profile: { ...baseSettings.profile, parentName: '   ' },
    });
    const single = validateClientProfile({
      ...baseSettings,
      profile: { ...baseSettings.profile, parentName: 'A' },
    });

    expect(empty.parentName).toBeDefined();
    expect(single.parentName).toBeDefined();
  });

  it('rejects a name longer than 100 characters', () => {
    const tooLong = validateClientProfile({
      ...baseSettings,
      profile: { ...baseSettings.profile, parentName: 'А'.repeat(101) },
    });

    expect(tooLong.parentName).toMatch(/100/);
  });

  it('rejects an email without @', () => {
    const errors = validateClientProfile({
      ...baseSettings,
      profile: { ...baseSettings.profile, email: 'annaexample.com' },
    });

    expect(errors.email).toBeDefined();
  });

  it('rejects an email without a dot after @', () => {
    const errors = validateClientProfile({
      ...baseSettings,
      profile: { ...baseSettings.profile, email: 'anna@example' },
    });

    expect(errors.email).toBeDefined();
  });

  it('rejects an empty email', () => {
    const errors = validateClientProfile({
      ...baseSettings,
      profile: { ...baseSettings.profile, email: '   ' },
    });

    expect(errors.email).toBeDefined();
  });

  it('derives initials from a name and falls back to TS', () => {
    expect(getClientInitials('Анна Родионова')).toBe('АР');
    expect(getClientInitials('мария')).toBe('М');
    expect(getClientInitials('   ')).toBe('TS');
    expect(getClientInitials('')).toBe('TS');
  });
});
