import type { ClientSettings } from '../../shared/types';

export type ClientProfileField = 'parentName' | 'email';
export type ClientProfileErrors = Partial<Record<ClientProfileField, string>>;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function setClientProfileField(
  settings: ClientSettings,
  field: ClientProfileField,
  value: string,
): ClientSettings {
  return {
    ...settings,
    profile: {
      ...settings.profile,
      [field]: value,
    },
  };
}

export function validateClientProfile(settings: ClientSettings): ClientProfileErrors {
  const errors: ClientProfileErrors = {};
  const name = settings.profile.parentName.trim();
  const email = settings.profile.email.trim();

  if (name.length < 2) {
    errors.parentName = 'Имя должно содержать минимум 2 символа';
  } else if (name.length > 100) {
    errors.parentName = 'Имя не должно превышать 100 символов';
  }

  if (!email) {
    errors.email = 'Введите корректный email';
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Введите корректный email';
  }

  return errors;
}

export function getClientInitials(name: string): string {
  const parts = name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0]?.toUpperCase() ?? '');

  const initials = parts.join('').slice(0, 2);
  return initials || 'TS';
}
