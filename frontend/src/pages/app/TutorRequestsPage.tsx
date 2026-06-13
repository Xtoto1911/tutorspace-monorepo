import { useEffect, useMemo, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { canAcceptTutorRequest, tutorSidebarRefreshEvent } from '../../components/layout-helpers';
import { acceptTutorRequest, getTutorRequests, rejectTutorRequest, updateTutorRequestStage } from '../../shared/api/tutor';
import { cn } from '../../shared/lib/cn';
import type { TutorRequestsPageData, TutorRequestStage } from '../../shared/types';
import {
  getTutorRequestPrimaryAction,
  getTutorRequestTabLabel,
  type TutorRequestTabKey,
} from './tutor-workday-helpers';

const tabs = [
  { key: 'new' },
  { key: 'funnel' },
  { key: 'trial' },
  { key: 'students' },
  { key: 'closed' },
] as const;

type TabKey = TutorRequestTabKey;

function getRequestCategory(status: string): TabKey {
  const s = status.trim();
  if (s === 'Новая') return 'new';
  if (s === 'Просмотрена' || s === 'Откликнулся' || s === 'Принята' || s === 'Контакт установлен') return 'funnel';
  if (s === 'Пробный урок') return 'trial';
  if (s === 'Стал учеником') return 'students';
  return 'closed';
}

function getRequestCategoryByStage(stage: TutorRequestStage | undefined, status: string): TabKey {
  if (!stage) return getRequestCategory(status);
  if (stage === 'NEW') return 'new';
  if (stage === 'VIEWED' || stage === 'ACCEPTED' || stage === 'CONTACT_ESTABLISHED') return 'funnel';
  if (stage === 'TRIAL_SCHEDULED') return 'trial';
  if (stage === 'STUDENT') return 'students';
  return 'closed';
}

function stageLabel(stage: TutorRequestStage) {
  if (stage === 'NEW') return 'Новая';
  if (stage === 'VIEWED') return 'Просмотрена';
  if (stage === 'ACCEPTED') return 'Принята';
  if (stage === 'CONTACT_ESTABLISHED') return 'Контакт установлен';
  if (stage === 'TRIAL_SCHEDULED') return 'Пробный урок';
  if (stage === 'STUDENT') return 'Стал учеником';
  if (stage === 'REJECTED') return 'Отклонена';
  if (stage === 'CANCELLED') return 'Отменена';
  if (stage === 'ARCHIVED') return 'Архив';
  return stage;
}

function sourceLabel(source?: string) {
  return source === 'MARKETPLACE' || !source ? 'Маркетплейс' : source;
}

function refreshTutorSidebar() {
  window.dispatchEvent(new Event(tutorSidebarRefreshEvent));
}

export function TutorRequestsPage() {
  const [pageData, setPageData] = useState<TutorRequestsPageData | null>(null);
  const [activeTab, setActiveTab] = useState<TabKey>('new');
  const [searchQuery, setSearchQuery] = useState('');
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [loadingId, setLoadingId] = useState<string | null>(null);
  const [loadingAction, setLoadingAction] = useState<'accept' | 'reject' | TutorRequestStage | null>(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const loadRequests = async () => {
    setIsLoading(true);
    setError('');
    try {
      setPageData(await getTutorRequests());
    } catch {
      setPageData(null);
      setError('Не удалось загрузить заявки.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadRequests();
  }, []);

  const requests = pageData?.requests ?? [];

  const filteredRequests = useMemo(() => {
    return requests.filter((request) => {
      const matchesTab = getRequestCategoryByStage(request.stage, request.status) === activeTab;
      if (!matchesTab) return false;
      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase();
      return (
        request.studentName.toLowerCase().includes(q) ||
        request.subject.toLowerCase().includes(q) ||
        request.description.toLowerCase().includes(q) ||
        request.status.toLowerCase().includes(q)
      );
    });
  }, [requests, activeTab, searchQuery]);

  const tabCounts = useMemo(() => {
    const counts: Record<TabKey, number> = { new: 0, funnel: 0, trial: 0, students: 0, closed: 0 };
    for (const request of requests) {
      counts[getRequestCategoryByStage(request.stage, request.status)]++;
    }
    return counts;
  }, [requests]);

  const handleAccept = async (requestId: string) => {
    setLoadingId(requestId);
    setLoadingAction('accept');
    try {
      const data = await acceptTutorRequest(requestId);
      setPageData(data);
      refreshTutorSidebar();
      setActiveTab('funnel');
    } catch {
      setError('Не удалось принять заявку.');
    } finally {
      setLoadingId(null);
      setLoadingAction(null);
    }
  };

  const handleReject = async (requestId: string) => {
    setLoadingId(requestId);
    setLoadingAction('reject');
    try {
      const data = await rejectTutorRequest(requestId, rejectReason || undefined);
      setPageData(data);
      refreshTutorSidebar();
    } catch {
      setError('Не удалось отклонить заявку.');
    } finally {
      setLoadingId(null);
      setLoadingAction(null);
      setRejectingId(null);
      setRejectReason('');
    }
  };

  const handleStage = async (requestId: string, status: TutorRequestStage) => {
    setLoadingId(requestId);
    setLoadingAction(status);
    try {
      const data = await updateTutorRequestStage(requestId, status);
      setPageData(data);
      refreshTutorSidebar();
    } catch {
      setError('Не удалось обновить этап заявки.');
    } finally {
      setLoadingId(null);
      setLoadingAction(null);
    }
  };

  if (isLoading) {
    return <div className="tutor-requests-page">Загрузка заявок...</div>;
  }

  if (error && !pageData) {
    return (
      <div className="tutor-requests-page">
        <p role="alert">{error}</p>
        <Button type="button" onClick={() => void loadRequests()}>Повторить</Button>
      </div>
    );
  }

  if (!pageData) {
    return null;
  }

  return (
    <div className="tutor-requests-page">
      <header className="tutor-requests-page__header">
        <div>
          <p className="tutor-requests-page__eyebrow">Кабинет репетитора</p>
          <h1>Заявки учеников</h1>
          <p>Отвечайте на новые обращения, назначайте пробные занятия и переводите заявки в расписание.</p>
        </div>

        <LinkButton to="/app/tutor/calendar">Добавить свободное время</LinkButton>
      </header>

      <section className="tutor-request-stats" aria-label="Сводка заявок репетитора">
        {error ? <p role="alert">{error}</p> : null}
        {pageData.stats.map((stat) => (
          <article key={stat.label} className={cn('tutor-request-stat', `tutor-request-stat--${stat.tone}`)}>
            <span className="tutor-request-stat__icon">{stat.icon}</span>
            <div>
              <strong>{stat.label}</strong>
              <p>{stat.sublabel}</p>
            </div>
            <b>{stat.value}</b>
          </article>
        ))}
      </section>

      <section className="tutor-request-toolbar" aria-label="Фильтр заявок">
        <div className="tutor-request-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={activeTab === tab.key ? 'is-active' : undefined}
              type="button"
              onClick={() => setActiveTab(tab.key)}
            >
              {getTutorRequestTabLabel(tab.key)}
              <span className="tutor-request-tabs__count">{tabCounts[tab.key]}</span>
            </button>
          ))}
        </div>

        <label className="tutor-request-search">
          <span className="sr-only">Поиск заявки</span>
          <input
            id="tutor-request-search"
            name="tutor-request-search"
            placeholder="Поиск заявки"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </label>

        <Button
          onClick={() => {
            setSearchQuery('');
            setActiveTab('new');
          }}
        >
          Сбросить
        </Button>
      </section>

      <div className="tutor-requests-layout">
        <section className="tutor-request-list" aria-label="Заявки учеников">
          {filteredRequests.length === 0 ? (
            <div className="tutor-request-empty">
              <strong>Заявок не найдено</strong>
              <p>
                {searchQuery
                  ? 'Попробуйте изменить запрос или сбросить фильтры.'
                  : 'В этой категории пока нет заявок.'}
              </p>
              {searchQuery && (
                <Button variant="ghost" onClick={() => setSearchQuery('')}>
                  Очистить поиск
                </Button>
              )}
            </div>
          ) : (
            filteredRequests.map((request) => {
              const primaryAction = getTutorRequestPrimaryAction(request);

              return (
              <article
                key={request.id}
                className={cn(
                  'tutor-request-card',
                  `tutor-request-card--${request.tone}`,
                  request.highlighted && 'is-highlighted',
                )}
              >
                <div className={cn('tutor-request-card__avatar', `tutor-request-card__avatar--${request.tone}`)}>
                  {request.initials}
                </div>

                <div className="tutor-request-card__main">
                  <div className="tutor-request-card__top">
                    <div>
                      <h2>{request.studentName}</h2>
                      <p>{request.gradeGoal}</p>
                    </div>
                    <div className="tutor-request-card__badges">
                      <span>{request.subject}</span>
                      <strong>{request.status}</strong>
                      {request.overdue ? <strong className="is-overdue">Просрочена</strong> : null}
                    </div>
                  </div>

                  <p className="tutor-request-card__description">{request.description}</p>

                  <div className="tutor-request-card__meta">
                    <span>Источник: {sourceLabel(request.source)}</span>
                    <span>Ответить до: {request.deadline ?? '24 часа'}</span>
                    <span>{request.completenessLabel ?? 'Заявка требует уточнения'}</span>
                    <span>{request.nextAction ?? 'Следующее действие'}</span>
                  </div>

                  <div className="tutor-request-card__footer">
                    <div className="tutor-request-card__chips">
                      <span>{request.price}</span>
                      <span>{request.format}</span>
                      <span>{request.preferredTime}</span>
                      {request.rejectionReason ? <span>{request.rejectionReason}</span> : null}
                    </div>

                    <div className="tutor-request-card__actions">
                      {primaryAction.kind === 'schedule' && primaryAction.href ? (
                        <LinkButton to={primaryAction.href}>{primaryAction.label}</LinkButton>
                      ) : null}
                      {primaryAction.kind === 'message' && primaryAction.href ? (
                        <LinkButton to={primaryAction.href}>{primaryAction.label}</LinkButton>
                      ) : null}
                      {request.stage === 'NEW' ? (
                        <Button
                          variant="ghost"
                          onClick={() => void handleStage(request.id, 'VIEWED')}
                          disabled={loadingId === request.id}
                        >
                          {loadingId === request.id && loadingAction === 'VIEWED' ? '...' : 'Просмотрена'}
                        </Button>
                      ) : null}
                      {canAcceptTutorRequest(request.status) ? (
                        <>
                          <Button
                            onClick={() => void handleAccept(request.id)}
                            disabled={loadingId === request.id && loadingAction === 'accept'}
                          >
                            {loadingId === request.id && loadingAction === 'accept' ? '...' : 'Принять'}
                          </Button>
                          <Button
                            variant="ghost"
                            onClick={() => setRejectingId(request.id)}
                            disabled={loadingId === request.id}
                          >
                            Отклонить
                          </Button>
                        </>
                      ) : null}
                      {request.stage === 'ACCEPTED' ? (
                        <Button
                          variant="ghost"
                          onClick={() => void handleStage(request.id, 'CONTACT_ESTABLISHED')}
                          disabled={loadingId === request.id}
                        >
                          {loadingId === request.id && loadingAction === 'CONTACT_ESTABLISHED' ? '...' : 'Контакт есть'}
                        </Button>
                      ) : null}
                      {request.stage === 'TRIAL_SCHEDULED' ? (
                        <Button
                          variant="ghost"
                          onClick={() => void handleStage(request.id, 'STUDENT')}
                          disabled={loadingId === request.id}
                        >
                          {loadingId === request.id && loadingAction === 'STUDENT' ? '...' : 'Стал учеником'}
                        </Button>
                      ) : null}
                      {request.stage && !['REJECTED', 'CANCELLED', 'ARCHIVED', 'STUDENT'].includes(request.stage) ? (
                        <Button
                          variant="ghost"
                          onClick={() => void handleStage(request.id, 'ARCHIVED')}
                          disabled={loadingId === request.id}
                        >
                          Архив
                        </Button>
                      ) : null}
                      {primaryAction.kind !== 'message' ? (
                      <LinkButton to={request.threadId ? `/app/tutor/messages?threadId=${encodeURIComponent(request.threadId)}` : '/app/tutor/messages'}>
                        Написать
                      </LinkButton>
                      ) : null}
                    </div>
                  </div>
                </div>
              </article>
              );
            })
          )}
        </section>

        <aside className="tutor-requests-side" aria-label="Контекст заявок">
          <section className="tutor-today-card">
            <h2>{pageData.today.title}</h2>
            <p>{pageData.today.summary}</p>
            <div className="tutor-today-card__events">
              {pageData.today.events.map((event) => (
                <article key={`${event.time}-${event.text}`} className="tutor-today-event">
                  <span className={cn('tutor-today-event__line', `tutor-today-event__line--${event.tone}`)} />
                  <strong>{event.time}</strong>
                  <p>{event.text}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="tutor-quick-card">
            <h2>Быстрые действия</h2>
            <LinkButton to="/app/tutor/calendar">Открыть время в календаре</LinkButton>
          </section>

          {pageData.funnel ? (
            <section className="tutor-funnel-card">
              <h2>Воронка</h2>
              <div className="tutor-funnel-card__grid">
                <span>Новые <b>{pageData.funnel.newCount}</b></span>
                <span>В работе <b>{pageData.funnel.respondedCount + pageData.funnel.contactCount}</b></span>
                <span>Пробные <b>{pageData.funnel.trialCount}</b></span>
                <span>Ученики <b>{pageData.funnel.studentCount}</b></span>
              </div>
              <div className="tutor-funnel-card__metrics">
                <p>Просрочено: <strong>{pageData.funnel.overdueCount}</strong></p>
                <p>В пробный урок: <strong>{pageData.funnel.trialConversionPercent}%</strong></p>
                <p>В ученика: <strong>{pageData.funnel.studentConversionPercent}%</strong></p>
                <p>Средний ответ: <strong>{pageData.funnel.averageResponseTime}</strong></p>
              </div>
            </section>
          ) : null}

          <section className="tutor-response-card">
            <span>{pageData.quality.label}</span>
            <strong>{pageData.quality.value}</strong>
            <p>{pageData.quality.text}</p>
            <div className="tutor-response-card__track">
              <span style={{ width: `${pageData.quality.percent}%` }} />
            </div>
          </section>
        </aside>
      </div>

      <section className="tutor-request-note" aria-label="Подсказка по заявкам">
        <strong>Подсказка</strong>
        <p>Если заявка не подходит, отклоните её с причиной — это улучшит рекомендации и снизит число нерелевантных обращений.</p>
        <Button disabled title="Фильтр рекомендаций появится позже">Фильтр появится позже</Button>
      </section>

      {rejectingId && (
        <div className="modal-backdrop" onClick={() => setRejectingId(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Отклонить заявку</h3>
            <p className="modal-hint">Укажите причину, чтобы мы могли улучшить подбор заявок.</p>
            <input
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Причина (необязательно)"
              className="modal-input"
            />
            <div className="modal-actions">
              <Button variant="ghost" onClick={() => setRejectingId(null)}>
                Отмена
              </Button>
              <Button
                onClick={() => void handleReject(rejectingId)}
                disabled={loadingId === rejectingId && loadingAction === 'reject'}
              >
                {loadingId === rejectingId && loadingAction === 'reject' ? 'Отклонение...' : 'Отклонить'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
