import { describe, expect, it } from 'vitest';
import {
  buildTutorSettingsFromWorkspaceProfile,
  setTutorHidden,
  setWorkspaceProfileField,
  shouldShowTutorProfileOnboarding,
  toggleTutorSettingItem,
} from './tutor-profile-settings-helpers';
import type { TutorSettingsPageData, TutorWorkspaceProfilePageData } from '../../shared/types';

const workspaceProfile: TutorWorkspaceProfilePageData = {
  stats: [],
  identity: {
    name: 'Анна Петрова',
    role: 'Репетитор по математике',
    city: 'Москва',
    avatarInitials: 'АП',
    verified: true,
    status: 'Опубликован',
    completion: 82,
  },
  publicCard: {
    title: 'Карточка в каталоге',
    subtitle: 'Витрина',
    rating: '4.9',
    price: '1500 ₽/час',
    response: '15 минут',
    views: '128 просмотров',
    chips: ['Математика'],
  },
  fields: [
    { id: 'headline', label: 'Заголовок', value: 'Готовлю к ЕГЭ', type: 'input' },
    { id: 'about', label: 'О себе', value: 'Работаю с понятной системой прогресса.', type: 'textarea' },
  ],
  subjects: [],
  checklist: [],
};

const tutorSettings: TutorSettingsPageData = {
  profile: {
    name: 'Старое имя',
    city: 'Старый город',
    headline: 'Старый заголовок',
    about: 'Старое описание',
    initials: 'СИ',
    moderationStatus: 'На проверке',
  },
  subjects: ['Математика'],
  availableSubjects: ['Математика', 'Физика'],
  isHidden: true,
  visibility: [
    { title: 'Показывать телефон', description: 'После заявки', enabled: false },
  ],
  notifications: [
    { title: 'Новые заявки', description: 'Email и push', enabled: true },
  ],
  moderation: {
    status: 'Профиль скрыт',
    description: 'Карточка не показывается в каталоге.',
  },
};

describe('tutor profile/settings consolidation helpers', () => {
  it('builds tutor settings payload from workspace profile without losing visibility controls', () => {
    const payload = buildTutorSettingsFromWorkspaceProfile(workspaceProfile, tutorSettings);

    expect(payload.profile).toEqual({
      ...tutorSettings.profile,
      name: 'Анна Петрова',
      city: 'Москва',
      headline: 'Готовлю к ЕГЭ',
      about: 'Работаю с понятной системой прогресса.',
      initials: 'АП',
    });
    expect(payload.subjects).toEqual(['Математика']);
    expect(payload.isHidden).toBe(true);
    expect(payload.visibility).toEqual(tutorSettings.visibility);
    expect(payload.notifications).toEqual(tutorSettings.notifications);
  });

  it('keeps identity and editable fields in sync when name or city changes in profile', () => {
    const renamed = setWorkspaceProfileField(workspaceProfile, 'name', 'Мария Иванова');
    const moved = setWorkspaceProfileField(renamed, 'city', 'Казань');

    expect(moved.identity.name).toBe('Мария Иванова');
    expect(moved.identity.avatarInitials).toBe('МИ');
    expect(moved.identity.city).toBe('Казань');
    expect(moved.fields.find((field) => field.id === 'name')?.value).toBeUndefined();
  });

  it('toggles visibility and notification rows immutably inside profile page state', () => {
    const hiddenPhone = toggleTutorSettingItem(tutorSettings, 'visibility', 0);
    const disabledNotifications = toggleTutorSettingItem(tutorSettings, 'notifications', 0);

    expect(hiddenPhone.visibility[0]?.enabled).toBe(true);
    expect(tutorSettings.visibility[0]?.enabled).toBe(false);
    expect(disabledNotifications.notifications[0]?.enabled).toBe(false);
    expect(tutorSettings.notifications[0]?.enabled).toBe(true);
  });

  it('sets the catalog hidden flag explicitly for immediate switch persistence', () => {
    const visibleSettings = setTutorHidden(tutorSettings, false);
    const hiddenSettings = setTutorHidden(visibleSettings, true);

    expect(visibleSettings.isHidden).toBe(false);
    expect(hiddenSettings.isHidden).toBe(true);
    expect(tutorSettings.isHidden).toBe(true);
  });

  it('shows onboarding for hidden, incomplete, or welcome profile states only', () => {
    expect(shouldShowTutorProfileOnboarding(workspaceProfile, tutorSettings, false)).toBe(true);

    const visibleSettings = setTutorHidden(tutorSettings, false);
    expect(shouldShowTutorProfileOnboarding(workspaceProfile, visibleSettings, true)).toBe(true);
    expect(shouldShowTutorProfileOnboarding({
      ...workspaceProfile,
      identity: {
        ...workspaceProfile.identity,
        completion: 100,
      },
    }, visibleSettings, false)).toBe(false);
  });
});
