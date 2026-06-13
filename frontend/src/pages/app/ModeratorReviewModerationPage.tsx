import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { decideModeratorReviewModerationItem, getModeratorReviewModerationPage } from '../../shared/api/admin';
import { cn } from '../../shared/lib/cn';
import type { ModeratorReviewModerationPageData } from '../../shared/types';
import { emptyModeratorReviewModerationPage, filterModeratorProfiles, getReadableError } from './admin-live-helpers';
import { getModeratorReviewActions, toModeratorSelectedProfile } from './moderator-review-helpers';

export function ModeratorReviewModerationPage() {
  const [page, setPage] = useState<ModeratorReviewModerationPageData>(emptyModeratorReviewModerationPage);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [activeTab, setActiveTab] = useState('Все');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionStatus, setActionStatus] = useState('');
  const filteredProfiles = filterModeratorProfiles(page.profiles, activeTab, searchQuery);
  const selectedProfile = selectedProfileId ? page.profiles.find((profile) => profile.id === selectedProfileId) : undefined;
  const selected = toModeratorSelectedProfile(selectedProfile, page.selected);
  const selectedActions = getModeratorReviewActions(selected);
  const canModerateSelected = selectedActions.length > 0;

  const loadReviewModeration = () => {
    return getModeratorReviewModerationPage()
      .then((data) => {
        setPage(data);
        setSelectedProfileId((current) => {
          if (data.profiles.some((profile) => profile.id === current)) {
            return current;
          }
          return data.profiles[0]?.id ?? '';
        });
        setError('');
      })
      .catch((error) => {
        setPage(emptyModeratorReviewModerationPage);
        setSelectedProfileId('');
        setError(getReadableError(error, 'Не удалось загрузить очередь модерации.'));
      });
  };

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getModeratorReviewModerationPage()
      .then((data) => {
        if (!ignore) {
          setPage(data);
          setSelectedProfileId((current) => (data.profiles.some((profile) => profile.id === current) ? current : (data.profiles[0]?.id ?? '')));
          setError('');
        }
      })
      .catch((error) => {
        if (!ignore) {
          setPage(emptyModeratorReviewModerationPage);
          setSelectedProfileId('');
          setError(getReadableError(error, 'Не удалось загрузить очередь модерации.'));
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, []);

  const handleDecision = async (action: (typeof selectedActions)[number]) => {
    if (!canModerateSelected || !action) {
      setActionStatus('Для выбранной записи нет доступного действия.');
      return;
    }

    setActionStatus('Сохраняем решение...');
    try {
      await decideModeratorReviewModerationItem(action.itemType, selected.id, action.decision);
      await loadReviewModeration();
      setActionStatus(action.decision === 'approve' ? 'Решение: одобрено.' : 'Решение: отклонено.');
    } catch (error) {
      setActionStatus(error instanceof Error ? error.message : 'Не удалось сохранить решение.');
    }
  };

  return (
    <div className="moderator-review">
      <header className="moderator-review__header">
        <div>
          <p className="moderator-review__eyebrow">Moderator panel</p>
          <h1>Очередь модерации</h1>
          <p>Профили и отзывы из live marketplace-service с понятным контекстом для решения.</p>
        </div>

      </header>

      {loading ? <section className="admin-live-state" aria-live="polite">Загружаем live-очередь...</section> : null}
      {error ? <section className="admin-live-state admin-live-state--error" aria-live="polite">{error}</section> : null}

      <section className="moderator-review-kpis" aria-label="Сводка по очереди модерации">
        {page.metrics.map((metric) => (
          <article key={metric.label} className={cn('moderator-review-kpi', `moderator-review-kpi--${metric.tone}`)}>
            <span>{metric.icon}</span>
            <strong>{metric.value}</strong>
            <p>{metric.label}</p>
            <small>{metric.hint}</small>
            <b>{metric.trend}</b>
          </article>
        ))}
      </section>

      <section className="moderator-review-toolbar" aria-label="Фильтры очереди модерации">
        <div className="moderator-review-tabs">
          {page.tabs.map((tab) => (
            <button
              key={tab}
              className={cn(activeTab === tab && 'is-active')}
              type="button"
              onClick={() => setActiveTab(tab)}
            >
              {tab}
            </button>
          ))}
        </div>
        <label className="moderator-review-search">
          <span>⌕</span>
          <input
            id="moderator-review-search"
            name="profile-review-search"
            aria-label="Поиск по ФИО"
            placeholder="Поиск по ФИО"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
          />
        </label>
      </section>

      <div className="moderator-review-layout">
        <main className="moderator-review-list" aria-label="Очередь модерации">
          <div className="moderator-review-card__heading moderator-review-card__heading--row">
            <div>
              <h2>Очередь</h2>
              <p>Профили и отзывы с live-статусами</p>
            </div>
            <span>Live</span>
          </div>

          <div className="moderator-review-items">
            {filteredProfiles.length === 0 ? (
              <div className="moderator-review-empty">
                <strong>Ничего не найдено</strong>
                <p>Попробуйте искать по имени преподавателя, автору или тексту отзыва.</p>
              </div>
            ) : filteredProfiles.map((profile) => (
              <article
                key={profile.id}
                className={cn('moderator-profile-row', `moderator-profile-row--${profile.tone}`, selected.id === profile.id && 'is-active')}
              >
                <i />
                <div className="moderator-profile-row__avatar">{profile.initials}</div>
                <div className="moderator-profile-row__body">
                  <div className="moderator-profile-row__top">
                    <strong>{profile.name}</strong>
                    <span>{profile.subject}</span>
                    <b>{profile.type}</b>
                    <em>{profile.status}</em>
                  </div>
                  <p>{profile.summary}</p>
                  <div className="moderator-profile-row__meta">
                    <span>{profile.score}</span>
                    <span>{profile.owner}</span>
                  </div>
                </div>
                <Button
                  variant="ghost"
                  onClick={() => {
                    setSelectedProfileId(profile.id);
                    setActionStatus('');
                  }}
                >
                  Проверить
                </Button>
              </article>
            ))}
          </div>
        </main>

        <aside className="moderator-review-detail" aria-label="Выбранная анкета">
          <div className="moderator-review-detail__accent" />
          <div className="moderator-review-detail__title">
            <div>
              <span>Выбранная анкета</span>
              <h2>{selected.id}</h2>
            </div>
            <b>{selected.status}</b>
          </div>

          <section className="moderator-profile-card">
            <span>{selected.initials}</span>
            <div>
              <h3>{selected.name}</h3>
              <p>{selected.subject}</p>
              <small>
                {selected.experience} · {selected.risk}
              </small>
            </div>
          </section>

          <section className="moderator-review-detail-card">
            <h4>Проверка профиля</h4>
            {selected.checklist.map((item) => (
              <div key={item.label} className="moderator-checkitem">
                <span>{item.done ? '✓' : '!'}</span>
                <strong>{item.label}</strong>
              </div>
            ))}
          </section>

          <section className="moderator-readiness-card">
            <div>
              <h4>Индекс готовности</h4>
              <strong>{selected.score}</strong>
            </div>
            <i>
              <b style={{ width: selected.score }} />
            </i>
            <p>{selected.note}</p>
          </section>

          <div className="moderator-review-detail__actions">
            {selectedActions.length > 0 ? selectedActions.map((action, index) => (
              <Button
                key={`${action.itemType}-${action.decision}`}
                disabled={!canModerateSelected}
                variant={index === 0 ? 'primary' : 'ghost'}
                onClick={() => void handleDecision(action)}
              >
                {action.label}
              </Button>
            )) : <p>Для выбранной записи нет действия в API.</p>}
          </div>
          {actionStatus ? <p className="moderator-review-detail__status">{actionStatus}</p> : null}
        </aside>
      </div>

      <section className="moderator-review-insights" aria-label="Аналитика проверки анкет">
        <article className="moderator-review-card">
          <h2>Воронка проверки</h2>
          <div className="moderator-pipeline-list">
            {page.pipeline.map((item) => (
              <div key={item.label} className="moderator-pipeline">
                <div>
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                </div>
                <i>
                  <b style={{ width: `${item.percent}%` }} />
                </i>
              </div>
            ))}
          </div>
        </article>

        <article className="moderator-review-card moderator-rubric">
          <h2>Критерии качества</h2>
          <div>
            {page.rubric.map((item) => (
              <span key={item}>{item}</span>
            ))}
          </div>
          <p>Красный критерий блокирует публикацию.</p>
        </article>

        <article className="moderator-review-card moderator-decision">
          <h2>{page.decision.title}</h2>
          <p>{page.decision.body}</p>
          <div>
            {page.decision.tags.map((tag) => (
              <span key={tag}>{tag}</span>
            ))}
          </div>
        </article>
      </section>
    </div>
  );
}
