import { useEffect, useMemo, useState } from 'react';
import { LinkButton } from '../../components/primitives';
import { getClientHistoryPage } from '../../shared/api/client';
import { cn } from '../../shared/lib/cn';
import type { ClientHistoryPageData } from '../../shared/types';

function eventMatchesFilter(event: ClientHistoryPageData['events'][number], filter: string) {
  if (filter === 'Занятия') {
    return event.type === 'lesson';
  }

  if (filter === 'Заявки') {
    return event.type === 'application';
  }

  return true;
}

export function ClientHistoryPage() {
  const [pageData, setPageData] = useState<ClientHistoryPageData | null>(null);
  const [activeFilter, setActiveFilter] = useState('Все записи');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;

    async function loadHistory() {
      setIsLoading(true);
      setError(null);

      try {
        const data = await getClientHistoryPage();
        if (!ignore) {
          setPageData(data);
          setActiveFilter(data.filters[0] ?? 'Все записи');
        }
      } catch (loadError) {
        if (!ignore) {
          setPageData(null);
          setActiveFilter('Все записи');
          setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить историю.');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    void loadHistory();

    return () => {
      ignore = true;
    };
  }, []);

  const filteredEvents = useMemo(
    () => pageData?.events.filter((event) => eventMatchesFilter(event, activeFilter)) ?? [],
    [activeFilter, pageData?.events],
  );

  if (isLoading) {
    return (
      <div className="client-history">
        <section className="schedule-loading-state" aria-live="polite">
          <h2>Загружаем историю</h2>
          <p>Собираем прошедшие уроки и статусы заявок.</p>
        </section>
      </div>
    );
  }

  if (error || !pageData) {
    return (
      <div className="client-history">
        <p role="alert">{error || 'История недоступна.'}</p>
        <LinkButton to="/app/client">Вернуться в обзор</LinkButton>
      </div>
    );
  }

  return (
    <div className="client-history">
      <header className="client-history__header">
        <div>
          <h1>История занятий и заявок</h1>
          <p>Прошедшие уроки, статусы заявок и сводка за месяц из live CRM-данных.</p>
        </div>
      </header>

      {isLoading ? (
        <section className="schedule-loading-state" aria-live="polite">
          <h2>Загружаем историю</h2>
          <p>Собираем прошедшие уроки и статусы заявок.</p>
        </section>
      ) : (
        <>
          <section className="history-stats" aria-label="Сводка истории">
            {pageData.stats.map((stat) => (
              <article key={stat.label} className={cn('history-stat', `history-stat--${stat.tone}`)}>
                <span className="history-stat__icon">{stat.icon}</span>
                <strong>{stat.value}</strong>
                <span>{stat.label}</span>
                <small>{stat.sublabel}</small>
              </article>
            ))}
          </section>

          <section className="history-filter-bar" aria-label="Фильтр истории">
            <strong>Фильтр истории</strong>
            <div className="history-filter-bar__chips">
              {pageData.filters.map((filter) => (
                <button
                  key={filter}
                  className={filter === activeFilter ? 'is-active' : undefined}
                  type="button"
                  onClick={() => setActiveFilter(filter)}
                >
                  {filter}
                </button>
              ))}
            </div>
            <span>{pageData.activeMonth || 'Текущий месяц'}</span>
          </section>

          <div className="history-layout">
            <section className="history-feed-card" aria-label="Лента событий">
              <div className="history-card-heading">
                <h2>Лента событий</h2>
                <p>Фильтр применяется сразу, без отдельной кнопки.</p>
              </div>

              {filteredEvents.length > 0 ? (
                <div className="history-event-list">
                  {filteredEvents.map((event) => (
                    <article key={`${event.type}-${event.id}`} className="history-event">
                      <span className="history-event__date">{event.date}</span>
                      <div>
                        <h3>{event.title}</h3>
                        <p>{event.text}</p>
                      </div>
                      <span className={cn('history-status', `history-status--${event.tone}`)}>{event.status}</span>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="history-empty-state">
                  <h3>{pageData.emptyState.title}</h3>
                  <p>{activeFilter === 'Все записи' ? pageData.emptyState.text : 'По этому фильтру пока нет записей.'}</p>
                </div>
              )}
            </section>

            <aside className="history-summary-card" aria-label={`Сводка за ${pageData.activeMonth || 'месяц'}`}>
              <h2>Сводка за месяц</h2>
              <div className="history-summary-list">
                {pageData.monthSummary.map((item) => (
                  <article key={item.title} className="history-summary-row">
                    <span className={cn('history-summary-row__dot', `history-summary-row__dot--${item.tone}`)} />
                    <div>
                      <strong>{item.title}</strong>
                      <p>{item.caption}</p>
                    </div>
                    <b>{item.value}</b>
                  </article>
                ))}
              </div>
              <p className="history-summary-card__hint">
                Сводка строится по занятиям и заявкам CRM без клиентских платежей.
              </p>
            </aside>
          </div>

          <div className="history-secondary-layout history-secondary-layout--single">
            <section className="history-lessons-card" aria-label="Прошедшие занятия">
              <h2>Прошедшие занятия</h2>
              {pageData.completedLessons.length > 0 ? (
                <div className="history-lessons-list">
                  {pageData.completedLessons.map((lesson) => (
                    <article key={lesson.lessonId} className="history-lesson-row">
                      <span>{lesson.date}</span>
                      <div>
                        <strong>{lesson.tutor}</strong>
                        <p>{lesson.subject}</p>
                      </div>
                      <p>{lesson.result}</p>
                      <LinkButton to={lesson.href} variant="ghost">
                        Открыть
                      </LinkButton>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="history-empty-state">
                  <h3>Прошедших занятий пока нет</h3>
                  <p>Когда урок завершится, он появится в этом списке.</p>
                </div>
              )}
            </section>
          </div>
        </>
      )}
    </div>
  );
}
