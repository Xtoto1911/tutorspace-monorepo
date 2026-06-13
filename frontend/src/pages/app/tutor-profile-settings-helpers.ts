import type { TutorSettingsPageData, TutorWorkspaceProfilePageData } from '../../shared/types';

type ToggleSection = 'visibility' | 'notifications';

export function buildTutorSettingsFromWorkspaceProfile(
  profile: TutorWorkspaceProfilePageData,
  settings: TutorSettingsPageData,
): TutorSettingsPageData {
  return {
    ...settings,
    profile: {
      ...settings.profile,
      name: profile.identity.name,
      city: profile.identity.city,
      headline: getWorkspaceProfileField(profile, 'headline'),
      about: getWorkspaceProfileField(profile, 'about'),
      initials: profile.identity.avatarInitials,
      experienceYears: profile.identity.experienceYears,
      pricePerHour: profile.identity.pricePerHour,
    },
  };
}

export function setWorkspaceProfileField(
  profile: TutorWorkspaceProfilePageData,
  fieldId: string,
  value: string,
): TutorWorkspaceProfilePageData {
  if (fieldId === 'name') {
    return {
      ...profile,
      identity: {
        ...profile.identity,
        name: value,
        avatarInitials: getInitials(value),
      },
      fields: profile.fields.map((field) => (field.id === fieldId ? { ...field, value } : field)),
    };
  }

  if (fieldId === 'city') {
    return {
      ...profile,
      identity: {
        ...profile.identity,
        city: value,
      },
      fields: profile.fields.map((field) => (field.id === fieldId ? { ...field, value } : field)),
    };
  }

  return {
    ...profile,
    fields: profile.fields.map((field) => (field.id === fieldId ? { ...field, value } : field)),
  };
}

export function toggleTutorSettingItem(
  settings: TutorSettingsPageData,
  section: ToggleSection,
  index: number,
): TutorSettingsPageData {
  return {
    ...settings,
    [section]: settings[section].map((item, itemIndex) => (
      itemIndex === index ? { ...item, enabled: !item.enabled } : item
    )),
  };
}

export function toggleTutorSubject(settings: TutorSettingsPageData, subject: string): TutorSettingsPageData {
  const selectedSubjects = settings.subjects.includes(subject)
    ? settings.subjects.filter((item) => item !== subject)
    : [...settings.subjects, subject];

  return {
    ...settings,
    subjects: selectedSubjects,
  };
}

export function toggleTutorHidden(settings: TutorSettingsPageData): TutorSettingsPageData {
  return setTutorHidden(settings, !settings.isHidden);
}

export function setTutorHidden(settings: TutorSettingsPageData, isHidden: boolean): TutorSettingsPageData {
  return {
    ...settings,
    isHidden,
  };
}

export function shouldShowTutorProfileOnboarding(
  profile: TutorWorkspaceProfilePageData,
  settings: TutorSettingsPageData,
  welcome: boolean,
) {
  return welcome || settings.isHidden || profile.identity.completion < 100;
}

function getWorkspaceProfileField(profile: TutorWorkspaceProfilePageData, fieldId: string) {
  return profile.fields.find((field) => field.id === fieldId)?.value ?? '';
}

function getInitials(name: string) {
  return name
    .split(' ')
    .map((part) => part[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase();
}
