import type { ModeratorReviewModerationPageData } from '../../shared/types';

type ModeratorProfile = ModeratorReviewModerationPageData['profiles'][number];
type ModeratorSelectedProfile = ModeratorReviewModerationPageData['selected'];
export type ModeratorReviewAction = {
  label: string;
  itemType: 'REVIEW' | 'TUTOR_PROFILE';
  decision: 'approve' | 'reject';
};

export function canModerateProfile(selected: ModeratorSelectedProfile | undefined) {
  return getModeratorReviewActions(selected).length > 0;
}

export function getModeratorReviewActions(selected: ModeratorSelectedProfile | undefined): ModeratorReviewAction[] {
  if (!selected?.id || selected.id === 'Нет анкет') {
    return [];
  }

  if (selected.itemType === 'REVIEW') {
    return [
      { label: 'Опубликовать отзыв', itemType: 'REVIEW', decision: 'approve' },
      { label: 'Отклонить отзыв', itemType: 'REVIEW', decision: 'reject' },
    ];
  }

  if (selected.itemType === 'TUTOR_PROFILE') {
    return [
      { label: 'Опубликовать анкету', itemType: 'TUTOR_PROFILE', decision: 'approve' },
      { label: 'Отклонить анкету', itemType: 'TUTOR_PROFILE', decision: 'reject' },
    ];
  }

  return [];
}

export function toModeratorSelectedProfile(
  profile: ModeratorProfile | undefined,
  fallback: ModeratorSelectedProfile,
): ModeratorSelectedProfile {
  if (!profile) {
    return fallback;
  }

  return {
    id: profile.id,
    itemType: profile.itemType,
    status: profile.status,
    initials: profile.initials,
    name: profile.name,
    subject: profile.subject,
    experience: profile.type,
    risk: profile.tone === 'rose' ? 'Высокий' : profile.tone === 'amber' ? 'Средний' : 'Низкий',
    checklist: [
      { label: 'Публичное описание', done: true },
      { label: 'Текст отзыва или анкеты', done: profile.summary.trim().length > 0 },
    ],
    score: profile.score,
    note: profile.summary,
  };
}
