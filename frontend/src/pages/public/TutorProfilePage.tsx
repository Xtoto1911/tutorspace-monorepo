import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Badge, Button, Card, EmptyState, LinkButton, ReviewCard, SectionHeading } from '../../components/primitives';
import { addClientFavorite, getClientFavoritesPage, removeClientFavorite } from '../../shared/api/client';
import {
  getTutorProfile,
  getTutorReviewEligibility,
  listTutors,
  reportTutorProfile,
  reportTutorReview,
  submitTutorInquiry,
  submitTutorReview,
} from '../../shared/api/public';
import { readAuthSession } from '../../shared/auth/session';
import { formatCurrency, formatRating } from '../../shared/lib/format';
import type { ReviewEligibility, TutorProfile, TutorSummary } from '../../shared/types';
import { buildTutorSlotOptions } from './tutor-profile-request-helpers';

type RequestMode = 'slot' | 'quick';

type QuickRequest = {
  mode: RequestMode;
  title: string;
  text: string;
};

function getInitials(name: string) {
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2);
}

function ProfilePill({ children }: { children: string }) {
  return <span className="tutor-profile__pill">{children}</span>;
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="tutor-profile__info-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function MiniTutorCard({
  slug,
  name,
  subject,
  price,
  note,
}: {
  slug: string;
  name: string;
  subject: string;
  price: string;
  note: string;
}) {
  return (
    <Link className="tutor-profile__mini-tutor" to={`/tutors/${slug}`} aria-label={`Открыть профиль ${name}`}>
      <div className="tutor-profile__mini-tutor-avatar">{getInitials(name)}</div>
      <div className="tutor-profile__mini-tutor-copy">
        <strong>{name}</strong>
        <span>{subject}</span>
        <p>{note}</p>
      </div>
      <strong className="tutor-profile__mini-tutor-price">{price}</strong>
    </Link>
  );
}

export function TutorProfilePage() {
  const { slug } = useParams();
  const [profile, setProfile] = useState<TutorProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [status, setStatus] = useState('');
  const [statusTone, setStatusTone] = useState<'success' | 'error'>('success');
  const [submittedApplicationId, setSubmittedApplicationId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<RequestMode>('slot');
  const [selectedSlotId, setSelectedSlotId] = useState('');
  const [similarTutors, setSimilarTutors] = useState<TutorSummary[]>([]);
  const [isFavorite, setIsFavorite] = useState(false);
  const [favoriteBusy, setFavoriteBusy] = useState(false);
  const session = readAuthSession();
  const canUseFavorites = session?.user.role === 'parent';
  const canSubmitReview = session?.user.role === 'parent';
  const canReportReview = session?.user.role === 'parent' || session?.user.role === 'tutor';
  const canReportProfile = session?.user.role === 'parent' || session?.user.role === 'tutor';
  const [reviewEligibility, setReviewEligibility] = useState<ReviewEligibility | null>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewText, setReviewText] = useState('');
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [reviewReportStatuses, setReviewReportStatuses] = useState<Record<string, string>>({});
  const [profileReportStatus, setProfileReportStatus] = useState('');
  const [profileReportTone, setProfileReportTone] = useState<'success' | 'error'>('success');
  const [profileReportBusy, setProfileReportBusy] = useState(false);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setLoadError('');
    setSimilarTutors([]);

    if (!slug) {
      setProfile(null);
      setLoading(false);
      return () => {
        alive = false;
      };
    }

    getTutorProfile(slug)
      .then((result) => {
        if (alive) {
          setProfile(result);
        }
      })
      .catch((error) => {
        if (alive) {
          setProfile(null);
          setLoadError(error instanceof Error ? error.message : 'Не удалось загрузить профиль.');
        }
      })
      .finally(() => {
        if (alive) {
          setLoading(false);
        }
      });

    return () => {
      alive = false;
    };
  }, [slug, reloadKey]);

  useEffect(() => {
    if (!canUseFavorites || !profile) {
      setIsFavorite(false);
      return;
    }

    let alive = true;
    getClientFavoritesPage()
      .then((page) => {
        if (alive) {
          setIsFavorite(page.tutors.some((tutor) => tutor.id === profile.id));
        }
      })
      .catch(() => {});

    return () => {
      alive = false;
    };
  }, [canUseFavorites, profile]);

  useEffect(() => {
    if (!canSubmitReview || !profile) {
      setReviewEligibility(null);
      setReviewText('');
      return;
    }

    let alive = true;
    getTutorReviewEligibility(profile.id)
      .then((eligibility) => {
        if (!alive) {
          return;
        }

        setReviewEligibility(eligibility);
        if (eligibility.existingReview) {
          setReviewRating(eligibility.existingReview.rating);
          setReviewText(eligibility.existingReview.text);
        }
      })
      .catch(() => {
        if (alive) {
          setReviewEligibility(null);
        }
      });

    return () => {
      alive = false;
    };
  }, [canSubmitReview, profile]);

  useEffect(() => {
    if (!profile) {
      return;
    }

    let alive = true;
    const primarySubject = profile.primarySubjectSlug;

    Promise.all([
      listTutors({ subject: primarySubject, city: profile.city }),
      listTutors({ subject: primarySubject }),
      listTutors({ city: profile.city }),
      listTutors(),
    ])
      .then(([sameSubjectCity, sameSubject, sameCity, allTutors]) => {
        if (!alive) {
          return;
        }

        const merged: TutorSummary[] = [];
        const seen = new Set<string>([profile.slug]);

        for (const pool of [sameSubjectCity, sameSubject, sameCity, allTutors]) {
          for (const tutor of pool) {
            if (seen.has(tutor.slug)) {
              continue;
            }

            seen.add(tutor.slug);
            merged.push(tutor);

            if (merged.length === 3) {
              setSimilarTutors(merged);
              return;
            }
          }
        }

        setSimilarTutors(merged.slice(0, 3));
      })
      .catch(() => {
        if (alive) {
          setSimilarTutors([]);
        }
      });

    return () => {
      alive = false;
    };
  }, [profile]);

  const slotOptions = useMemo(() => (profile ? buildTutorSlotOptions(profile.schedule) : []), [profile]);
  const selectedSlot = slotOptions.find((slot) => slot.id === selectedSlotId) ?? slotOptions[0] ?? null;

  useEffect(() => {
    if (profile && slotOptions.length === 0) {
      setSelectedRequest('quick');
    }
  }, [profile, slotOptions.length]);

  const handleQuickSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!slug) {
      return;
    }

    setIsSubmitting(true);
    setSubmittedApplicationId(null);
    try {
      const requestCopy: Record<RequestMode, QuickRequest> = {
        slot: {
          mode: 'slot',
          title: 'Запись на выбранное время',
          text: selectedSlot
            ? `Хочу записаться на занятие ${selectedSlot.label}. После одобрения прошу подтвердить этот слот.`
            : 'Хочу записаться на ближайшее свободное время.',
        },
        quick: {
          mode: 'quick',
          title: 'Быстрая заявка',
          text: 'Хочу оставить заявку без бронирования календаря. После одобрения преподаватель сам назначит урок.',
        },
      };

      const requestMode = selectedRequest === 'slot' && selectedSlot ? 'slot' : 'quick';
      const request = requestCopy[requestMode];
      const response = await submitTutorInquiry(slug, {
        message: `${request.title}. ${request.text}`,
        desiredStartTime: requestMode === 'slot' ? selectedSlot?.desiredStartTime : undefined,
      });

      setStatusTone('success');
      setSubmittedApplicationId(response.applicationId);
      setStatus(
        requestMode === 'slot'
          ? 'Заявка отправлена. Репетитор увидит выбранное время в кабинете.'
          : 'Заявка отправлена. После одобрения преподаватель сам добавит урок в календарь.',
      );
    } catch (error) {
      setStatusTone('error');
      setSubmittedApplicationId(null);
      setStatus(error instanceof Error ? error.message : 'Не удалось отправить заявку. Попробуйте позже.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleScrollToRequest = () => {
    document.getElementById('quick-request')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });

    window.history.replaceState(null, '', '#quick-request');
  };

  const handleToggleFavorite = async () => {
    if (!profile || !canUseFavorites || favoriteBusy) {
      return;
    }

    const nextFavorite = !isFavorite;
    setFavoriteBusy(true);

    try {
      if (nextFavorite) {
        await addClientFavorite(profile.id);
      } else {
        await removeClientFavorite(profile.id);
      }
      setIsFavorite(nextFavorite);
    } catch (error) {
      setStatusTone('error');
      setStatus(error instanceof Error ? error.message : 'Не удалось обновить избранное.');
    } finally {
      setFavoriteBusy(false);
    }
  };

  const refreshReviewState = async () => {
    if (!profile) {
      return;
    }

    const [freshProfile, eligibility] = await Promise.all([
      getTutorProfile(profile.slug),
      getTutorReviewEligibility(profile.id),
    ]);

    if (freshProfile) {
      setProfile(freshProfile);
    }
    setReviewEligibility(eligibility);
    if (eligibility.existingReview) {
      setReviewRating(eligibility.existingReview.rating);
      setReviewText(eligibility.existingReview.text);
    }
  };

  const handleReviewSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!profile || !reviewEligibility?.eligible) {
      return;
    }

    const trimmedText = reviewText.trim();
    if (trimmedText.length < 10) {
      setStatusTone('error');
      setStatus('Отзыв должен быть чуть подробнее: минимум 10 символов.');
      return;
    }

    setReviewSubmitting(true);
    try {
      const savedReview = await submitTutorReview(profile.id, { rating: reviewRating, text: trimmedText });
      await refreshReviewState();
      setStatusTone('success');
      if (savedReview.status === 'PENDING_MODERATION') {
        setStatus('Отзыв отправлен на модерацию.');
      } else {
        setStatus(reviewEligibility.existingReview ? 'Отзыв обновлен.' : 'Отзыв опубликован.');
      }
    } catch (error) {
      setStatusTone('error');
      setStatus(error instanceof Error ? error.message : 'Не удалось сохранить отзыв.');
    } finally {
      setReviewSubmitting(false);
    }
  };

  const handleReportReview = async (reviewId: string) => {
    if (!canReportReview) {
      return;
    }

    setReviewReportStatuses((current) => ({ ...current, [reviewId]: 'Отправляем жалобу...' }));
    try {
      await reportTutorReview(reviewId, {
        reason: 'Некорректный отзыв',
        description: 'Пользователь отметил отзыв для проверки модератором.',
      });
      setReviewReportStatuses((current) => ({ ...current, [reviewId]: 'Жалоба отправлена модератору.' }));
    } catch (error) {
      setReviewReportStatuses((current) => ({
        ...current,
        [reviewId]: error instanceof Error ? error.message : 'Не удалось отправить жалобу.',
      }));
    }
  };

  const handleReportProfile = async () => {
    if (!profile || !canReportProfile || profileReportBusy) {
      return;
    }

    setProfileReportBusy(true);
    setProfileReportTone('success');
    setProfileReportStatus('Отправляем жалобу...');

    try {
      await reportTutorProfile(profile.slug, {
        reason: 'Жалоба на профиль репетитора',
        description: 'Пользователь отметил карточку репетитора для проверки администрацией сайта.',
      });
      setProfileReportStatus('Жалоба отправлена администрации.');
    } catch (error) {
      setProfileReportTone('error');
      setProfileReportStatus(error instanceof Error ? error.message : 'Не удалось отправить жалобу.');
    } finally {
      setProfileReportBusy(false);
    }
  };

  if (loading) {
    return <Card className="loading-panel">Загружаем профиль репетитора...</Card>;
  }

  if (loadError) {
    return (
      <EmptyState
        title="Не удалось загрузить профиль"
        text={loadError}
        action={<Button type="button" variant="secondary" onClick={() => setReloadKey((value) => value + 1)}>Повторить</Button>}
      />
    );
  }

  if (!profile) {
    return (
      <EmptyState
        title="Профиль не найден"
        text="Проверьте ссылку или вернитесь в каталог, чтобы выбрать другого репетитора."
        action={<LinkButton to="/catalog">Вернуться в каталог</LinkButton>}
      />
    );
  }

  const mainFacts = [
    `${formatRating(profile.rating)} · ${profile.reviewCount} отзывов`,
    `${profile.experienceYears} лет опыта`,
    profile.city,
    profile.stats.responseTime === 'Нет данных' ? 'Время ответа не указано' : `Отвечает за ${profile.stats.responseTime}`,
  ];

  const primarySubjectTitle = profile.subjects[0] ?? 'Каталог';
  const primarySubjectUrl = profile.primarySubjectSlug ? `/catalog/subject/${profile.primarySubjectSlug}` : '/catalog';

  const statTiles = [
    { label: 'отзывов', value: `${profile.stats.reviews}` },
    { label: 'средняя оценка', value: formatRating(profile.stats.averageRating) },
  ];

  const subjectPills = [...new Set([...profile.subjects, ...profile.tags])];

  const quickFacts = [
    { label: 'Формат', value: profile.tags.length > 0 ? profile.tags.join(', ') : 'Не указан' },
    { label: 'Свободных окон', value: slotOptions.length > 0 ? `${slotOptions.length}` : 'Уточняется' },
    { label: 'Город', value: profile.city },
  ];

  const quickRequests: QuickRequest[] = [
    { mode: 'slot', title: 'Записаться на слот', text: 'Выберите свободный день и время для первого занятия.' },
    { mode: 'quick', title: 'Быстрая заявка', text: 'Отправьте запрос без записи в календарь, время появится после одобрения.' },
  ];

  return (
    <div className="page-stack tutor-profile">
      <nav className="tutor-profile__breadcrumbs" aria-label="Хлебные крошки">
        <Link to="/catalog">Каталог</Link>
        <span>/</span>
        <Link to={primarySubjectUrl}>{primarySubjectTitle}</Link>
        <span>/</span>
        <span>{profile.name}</span>
      </nav>

      <section className="tutor-profile__hero">
        <Card className="tutor-profile__hero-main">
          <div className="tutor-profile__identity">
            <div className="tutor-profile__avatar">
              <span>{getInitials(profile.name)}</span>
            </div>

            <div className="tutor-profile__identity-copy">
              <Badge tone="emerald">Проверенный профиль</Badge>
              <h1>{profile.name}</h1>
              <p className="tutor-profile__subtitle">{profile.subjects.join(' · ') || 'Предметы пока не указаны'}</p>

              <div className="tutor-profile__facts">
                {mainFacts.map((fact) => (
                  <ProfilePill key={fact}>{fact}</ProfilePill>
                ))}
              </div>
            </div>
          </div>

          <p className="tutor-profile__lead">{profile.about}</p>

          <div className="tutor-profile__stats">
            {statTiles.map((stat) => (
              <Card key={stat.label} className="tutor-profile__stat">
                <strong>{stat.value}</strong>
                <span>{stat.label}</span>
              </Card>
            ))}
          </div>
        </Card>

        <Card className="tutor-profile__booking">
          <p className="section-eyebrow">Стоимость занятия</p>
          <h2>{formatCurrency(profile.hourlyRate)} / 60 минут</h2>

          <div className="profile-hero__action-row tutor-profile__booking-actions">
            <Button type="button" onClick={handleScrollToRequest}>
              Выбрать время
            </Button>
            {canUseFavorites ? (
              <Button
                type="button"
                variant={isFavorite ? 'secondary' : 'ghost'}
                className="favorite-toggle favorite-toggle--profile"
                aria-pressed={isFavorite}
                disabled={favoriteBusy}
                onClick={handleToggleFavorite}
              >
                {isFavorite ? '♥ В избранном' : '♡ В избранное'}
              </Button>
            ) : null}
            {canReportProfile ? (
              <Button type="button" variant="ghost" disabled={profileReportBusy} onClick={handleReportProfile}>
                Пожаловаться
              </Button>
            ) : (
              <LinkButton to="/auth/sign-in" variant="ghost">
                Пожаловаться
              </LinkButton>
            )}
          </div>

          {profileReportStatus ? (
            <div className={profileReportTone === 'error' ? 'error-banner' : 'success-banner'}>{profileReportStatus}</div>
          ) : null}

          <div className="tutor-profile__booking-meta">
            {quickFacts.map((fact) => (
              <InfoRow key={fact.label} label={fact.label} value={fact.value} />
            ))}
          </div>
        </Card>
      </section>

      <section className="tutor-profile__content">
        <div className="tutor-profile__main">
          <Card className="tutor-profile__section tutor-profile__section--text">
            <SectionHeading eyebrow="О репетиторе" title="О репетиторе" />
            <div className="tutor-profile__text">
              <p>{profile.about}</p>
              {profile.education.length > 0 ? <p><strong>Образование:</strong> {profile.education.join('; ')}</p> : null}
            </div>
          </Card>

          <Card className="tutor-profile__section">
            <SectionHeading eyebrow="Предметы и форматы" title="Предметы и форматы" />
            <div className="tutor-profile__chip-row">
              {subjectPills.map((pill) => (
                <Badge key={pill} tone={pill === 'Математика' ? 'indigo' : 'slate'}>
                  {pill}
                </Badge>
              ))}
            </div>

            {profile.highlights.length > 0 ? (
              <div className="tutor-profile__mini-grid">
                {profile.highlights.map((highlight) => (
                  <article key={highlight} className="tutor-profile__mini-card">
                    <span>{highlight}</span>
                  </article>
                ))}
              </div>
            ) : null}
          </Card>

          <Card className="tutor-profile__section tutor-profile__application" id="quick-request">
            <SectionHeading eyebrow="Заявка на занятие" title="Выберите способ связи с репетитором" />
            <form className="tutor-profile__request" onSubmit={handleQuickSubmit}>
              <div className="tutor-profile__request-options tutor-profile__request-options--modes">
                {quickRequests.map((option) => {
                  const disabled = option.mode === 'slot' && slotOptions.length === 0;

                  return (
                    <button
                      key={option.mode}
                      type="button"
                      className={`tutor-profile__request-option${selectedRequest === option.mode ? ' is-active' : ''}`}
                      disabled={disabled}
                      onClick={() => {
                        if (!disabled) {
                          setSelectedRequest(option.mode);
                        }
                      }}
                    >
                      <span className="tutor-profile__request-dot" aria-hidden="true" />
                      <span className="tutor-profile__request-copy">
                        <strong>{option.title}</strong>
                        <span>{disabled ? 'У репетитора пока нет открытых слотов, можно оставить быструю заявку.' : option.text}</span>
                      </span>
                    </button>
                  );
                })}
              </div>

              {selectedRequest === 'slot' && slotOptions.length > 0 ? (
                slotOptions.length > 8 ? (
                  <label className="tutor-profile__slot-select">
                    <span>Свободное время</span>
                    <select
                      value={selectedSlotId}
                      onChange={(event) => setSelectedSlotId(event.target.value)}
                      aria-label="Свободные слоты репетитора"
                    >
                      {slotOptions.map((slot) => (
                        <option key={slot.id} value={slot.id}>
                          {slot.label}
                        </option>
                      ))}
                    </select>
                    <small>Запрос попадет репетитору с выбранным временем.</small>
                  </label>
                ) : (
                  <div className="tutor-profile__slot-picker" aria-label="Свободные слоты репетитора">
                    {slotOptions.map((slot) => {
                      const active = selectedSlot?.id === slot.id;

                      return (
                        <button
                          key={slot.id}
                          type="button"
                          className={`tutor-profile__schedule-item tutor-profile__slot-option${active ? ' is-active' : ''}`}
                          onClick={() => setSelectedSlotId(slot.id)}
                        >
                          <strong>{slot.label}</strong>
                          <span>Запрос попадет репетитору с этим временем</span>
                        </button>
                      );
                    })}
                  </div>
                )
              ) : (
                <p className="tutor-profile__request-note">
                  Быстрая заявка не бронирует календарь. Если репетитор ее примет, ученик появится в списке кандидатов, и преподаватель сам назначит урок.
                </p>
              )}

              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Отправляем...' : selectedRequest === 'slot' ? 'Отправить заявку на слот' : 'Отправить быструю заявку'}
              </Button>
            </form>

            {status ? (
              <div className={statusTone === 'error' ? 'error-banner' : 'success-banner'}>
                <p>{status}</p>
                {statusTone === 'success' && submittedApplicationId ? (
                  <div className="tutor-profile__request-success-actions">
                    <LinkButton to={`/app/client/applications/${submittedApplicationId}`}>Открыть заявку</LinkButton>
                    <LinkButton to="/app/client/applications" variant="ghost">
                      Мои заявки
                    </LinkButton>
                  </div>
                ) : null}
              </div>
            ) : null}
          </Card>

          <Card className="tutor-profile__section" id="reviews">
            <SectionHeading
              eyebrow="Отзывы учеников"
              title="Отзывы учеников"
              actions={<LinkButton to="#reviews" variant="ghost">{`Все ${profile.stats.reviews}`}</LinkButton>}
            />

            {reviewEligibility?.eligible ? (
              <form className="tutor-profile__review-form" onSubmit={handleReviewSubmit}>
                <label>
                  <span>Оценка</span>
                  <select value={reviewRating} onChange={(event) => setReviewRating(Number(event.target.value))}>
                    {[5, 4, 3, 2, 1].map((value) => (
                      <option key={value} value={value}>
                        {value}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  <span>Отзыв</span>
                  <textarea
                    value={reviewText}
                    onChange={(event) => setReviewText(event.target.value)}
                    maxLength={2000}
                    placeholder="Что было полезно на занятии?"
                  />
                </label>
                <Button type="submit" disabled={reviewSubmitting}>
                  {reviewSubmitting ? 'Сохраняем...' : reviewEligibility.existingReview ? 'Обновить отзыв' : 'Оставить отзыв'}
                </Button>
              </form>
            ) : null}

            <div className="tutor-profile__reviews">
              {profile.reviews.map((review) => (
                <ReviewCard
                  key={review.id}
                  {...review}
                  onReport={canReportReview ? handleReportReview : undefined}
                  reportStatus={reviewReportStatuses[review.id]}
                />
              ))}
            </div>
          </Card>
        </div>

        <aside className="tutor-profile__sidebar">
          <Card className="tutor-profile__section">
            <SectionHeading eyebrow="Кратко" title="Кратко" />
            <div className="tutor-profile__fact-list">
              <InfoRow label="Рейтинг" value={`${formatRating(profile.rating)} / 5`} />
              <InfoRow label="Отзывов" value={`${profile.reviewCount}`} />
              <InfoRow label="Опыт" value={`${profile.experienceYears} лет`} />
              <InfoRow label="Ответ" value={profile.stats.responseTime} />
            </div>
          </Card>

          <Card className="tutor-profile__section">
            <SectionHeading eyebrow="Похожие репетиторы" title="Похожие репетиторы" />
            <div className="tutor-profile__similar">
              {similarTutors.map((tutor) => (
                <MiniTutorCard
                  key={tutor.id}
                  slug={tutor.slug}
                  name={tutor.name}
                  subject={tutor.subjects.slice(0, 2).join(' · ')}
                  price={formatCurrency(tutor.hourlyRate)}
                  note={`${tutor.city} · ${tutor.experienceYears} лет опыта`}
                />
              ))}
            </div>
          </Card>
        </aside>
      </section>

      <section className="tutor-profile__cta">
        <div>
          <p className="section-eyebrow">Не уверены, что репетитор подходит?</p>
          <h2>Подберем альтернативу по предмету, цели и бюджету.</h2>
        </div>
        <LinkButton to="/catalog" variant="secondary">
          Подобрать репетитора
        </LinkButton>
      </section>
    </div>
  );
}
