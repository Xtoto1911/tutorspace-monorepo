import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { getModeratorComplaintsPage, resolveModeratorComplaint } from '../../shared/api/admin';
import { cn } from '../../shared/lib/cn';
import type { ModeratorComplaintsPageData } from '../../shared/types';
import { emptyModeratorComplaintsPage, filterModeratorComplaints, getReadableError } from './admin-live-helpers';

export function ModeratorComplaintsPage() {
  const [page, setPage] = useState<ModeratorComplaintsPageData>(emptyModeratorComplaintsPage);
  const [selectedComplaintId, setSelectedComplaintId] = useState('');
  const [activeTab, setActiveTab] = useState('Все');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionStatus, setActionStatus] = useState('');
  const selected = selectedComplaintId ? (page.selectedById?.[selectedComplaintId] ?? page.selected) : page.selected;
  const filteredComplaints = filterModeratorComplaints(page.complaints, activeTab, searchQuery);

  const loadComplaints = () => {
    return getModeratorComplaintsPage()
      .then((data) => {
        setPage(data);
        setSelectedComplaintId((current) => (data.complaints.some((complaint) => complaint.id === current) ? current : data.selected.id));
        setError('');
      })
      .catch((error) => {
        setPage(emptyModeratorComplaintsPage);
        setSelectedComplaintId('');
        setError(getReadableError(error, 'Не удалось загрузить жалобы.'));
      });
  };

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getModeratorComplaintsPage()
      .then((data) => {
        if (!ignore) {
          setPage(data);
          setSelectedComplaintId(data.selected.id);
          setError('');
        }
      })
      .catch((error) => {
        if (!ignore) {
          setPage(emptyModeratorComplaintsPage);
          setSelectedComplaintId('');
          setError(getReadableError(error, 'Не удалось загрузить жалобы.'));
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

  const handleResolveComplaint = async (action: string) => {
    setActionStatus('Сохраняем решение...');
    try {
      await resolveModeratorComplaint(selected.id, action);
      await loadComplaints();
      setActionStatus('Решение сохранено.');
    } catch (error) {
      setActionStatus(error instanceof Error ? error.message : 'Не удалось сохранить решение.');
    }
  };

  return (
    <div className="moderator-complaints">
      <header className="moderator-complaints__header">
        <div>
          <p className="moderator-complaints__eyebrow">Moderator panel</p>
          <h1>Жалобы пользователей</h1>
          <p>Отслеживай жалобы на комментарии, профили и сообщения преподавателей, а также обращения преподавателей на клиентов.</p>
        </div>

      </header>

      {loading ? <section className="admin-live-state" aria-live="polite">Загружаем жалобы...</section> : null}
      {error ? <section className="admin-live-state admin-live-state--error" aria-live="polite">{error}</section> : null}

      <section className="moderator-complaints-kpis" aria-label="Сводка по жалобам">
        {page.metrics.map((metric) => (
          <article key={metric.label} className={cn('moderator-complaints-kpi', `moderator-complaints-kpi--${metric.tone}`)}>
            <span>{metric.icon}</span>
            <strong>{metric.value}</strong>
            <p>{metric.label}</p>
            <small>{metric.hint}</small>
            <b>{metric.trend}</b>
          </article>
        ))}
      </section>

      <section className="moderator-complaints-toolbar" aria-label="Фильтры жалоб">
        <div className="moderator-complaints-tabs">
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
        <label className="moderator-complaints-search">
          <span>⌕</span>
          <input
            id="moderator-complaints-search"
            name="complaint-search"
            aria-label="Поиск по жалобам"
            placeholder="ID, причина, контекст"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
          />
        </label>
      </section>

      <div className="moderator-complaints-layout">
        <main className="moderator-complaints-list" aria-label="Очередь жалоб">
          <div className="moderator-complaints-card__heading moderator-complaints-card__heading--row">
            <div>
              <h2>Очередь жалоб</h2>
              <p>Сортировка: срочность, тип жалобы и контекст общения</p>
            </div>
            <span>Live</span>
          </div>

          <div className="moderator-complaints-items">
            {filteredComplaints.length === 0 ? (
              <div className="admin-live-state">
                {page.complaints.length === 0 ? 'Открытых жалоб нет.' : 'Жалобы не найдены по текущему фильтру.'}
              </div>
            ) : filteredComplaints.map((complaint) => (
              <article
                key={complaint.id}
                className={cn(
                  'moderator-complaint-row',
                  `moderator-complaint-row--${complaint.tone}`,
                  selected.id === complaint.id && 'is-active',
                )}
              >
                <i />
                <div className="moderator-complaint-row__avatar">{complaint.initials}</div>
                <div className="moderator-complaint-row__body">
                  <div className="moderator-complaint-row__top">
                    <strong>{complaint.id}</strong>
                    <span>{complaint.direction}</span>
                    <b>{complaint.category}</b>
                    <em>{complaint.status}</em>
                  </div>
                  <h3>{complaint.subject}</h3>
                  <p>{complaint.summary}</p>
                  <div className="moderator-complaint-row__meta">
                    <span>{complaint.time}</span>
                    <span>{complaint.evidence}</span>
                    <span>{complaint.owner}</span>
                  </div>
                </div>
                <Button type="button" variant="ghost" onClick={() => setSelectedComplaintId(complaint.id)}>
                  Открыть
                </Button>
              </article>
            ))}
          </div>
        </main>

        <aside className="moderator-complaint-detail" aria-label="Выбранная жалоба">
          <div className="moderator-complaint-detail__accent" />
          <div className="moderator-complaint-detail__title">
            <div>
              <span>Выбранная жалоба</span>
              <h2>{selected.id}</h2>
            </div>
            <b>{selected.status}</b>
          </div>
          <h3>{selected.subject}</h3>
          <p>{selected.copy}</p>

          <section className="moderator-detail-card">
            <h4>Участники</h4>
            {selected.participants.map((participant) => (
              <article key={participant.name} className="moderator-participant">
                <span>{participant.initials}</span>
                <div>
                  <strong>{participant.name}</strong>
                  <p>{participant.role}</p>
                </div>
              </article>
            ))}
          </section>

          <section className="moderator-detail-card">
            <h4>Доказательства</h4>
            {selected.evidence.map((evidence) => (
              <div key={evidence.label} className="moderator-evidence">
                <span>{evidence.ready ? '✓' : '!'}</span>
                <strong>{evidence.label}</strong>
              </div>
            ))}
          </section>

          <section className="moderator-impact-card">
            <h4>Контекст жалобы</h4>
            <strong>{selected.impact.label}</strong>
            <p>{selected.impact.hint}</p>
          </section>

          <div className="moderator-complaint-detail__actions">
            {selected.actions.length === 0 ? (
              <p>Для выбранной жалобы нет доступных действий.</p>
            ) : selected.actions.map((action, index) => (
              <Button
                key={action}
                variant={index === 0 ? 'ghost' : 'primary'}
                onClick={() => void handleResolveComplaint(action)}
              >
                {action}
              </Button>
            ))}
          </div>
          {actionStatus ? <p className="moderator-complaint-detail__status">{actionStatus}</p> : null}
        </aside>
      </div>

      <section className="moderator-complaints-insights" aria-label="Аналитика жалоб">
        <article className="moderator-complaints-card">
          <h2>SLA по категориям</h2>
          <div className="moderator-sla-list">
            {page.sla.map((item) => (
              <div key={item.label} className="moderator-sla">
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

        <article className="moderator-complaints-card moderator-weekly">
          <h2>Решения за неделю</h2>
          <div className="moderator-weekly__chart">
            {page.weekly.bars.map((bar, index) => (
              <div key={`${page.weekly.labels[index]}-${bar}`}>
                <span style={{ height: `${bar}%` }} />
                <b>{page.weekly.labels[index]}</b>
              </div>
            ))}
          </div>
          <p>{page.weekly.hint}</p>
        </article>

        <article className="moderator-complaints-card moderator-rules">
          <h2>{page.rules.title}</h2>
          <p>{page.rules.body}</p>
          <div>
            {page.rules.tags.map((tag) => (
              <span key={tag}>{tag}</span>
            ))}
          </div>
        </article>
      </section>
    </div>
  );
}
