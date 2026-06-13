import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { getAdminAudit } from '../../shared/api/admin';
import { cn } from '../../shared/lib/cn';
import type { AdminAuditPageData } from '../../shared/types';
import { emptyAdminAuditPage, filterAuditEvents, getReadableError } from './admin-live-helpers';

const severityLabels: Record<AdminAuditPageData['events'][number]['severity'], string> = {
  low: 'низкий',
  medium: 'средний',
  high: 'высокий',
};

export function AdminAuditPage() {
  const [page, setPage] = useState<AdminAuditPageData>(emptyAdminAuditPage);
  const [activeTab, setActiveTab] = useState('Все');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const selected = page.selected;
  const filteredEvents = filterAuditEvents(page.events, activeTab, searchQuery);

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getAdminAudit()
      .then((data) => {
        if (!ignore) {
          setPage(data);
          setError('');
        }
      })
      .catch((error) => {
        if (!ignore) {
          setPage(emptyAdminAuditPage);
          setError(getReadableError(error, 'Не удалось загрузить журнал аудита.'));
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

  return (
    <div className="admin-audit">
      <header className="admin-audit__header">
        <div>
          <p className="admin-audit__eyebrow">Администрирование</p>
          <h1>Журнал аудита</h1>
          <p>Отслеживайте действия администраторов, модераторов и системных сервисов без доступа к приватным данным.</p>
        </div>
        <div className="admin-audit__actions">
          <Button variant="ghost" disabled title="Табы и поиск уже применяют фильтры">Фильтры</Button>
          <Button disabled title="Экспорт появится позже">Экспорт позже</Button>
        </div>
      </header>

      {loading ? <section className="admin-live-state" aria-live="polite">Загружаем журнал аудита...</section> : null}
      {error ? <section className="admin-live-state admin-live-state--error" aria-live="polite">{error}</section> : null}

      <section className="admin-audit-metrics" aria-label="Сводка журнала аудита">
        {page.metrics.map((metric) => (
          <article key={metric.label} className={cn('admin-audit-metric', `admin-audit-metric--${metric.tone}`)}>
            <span>{metric.icon}</span>
            <div>
              <strong>{metric.value}</strong>
              <p>{metric.label}</p>
            </div>
            <b>{metric.hint}</b>
          </article>
        ))}
      </section>

      <section className="admin-audit-toolbar" aria-label="Фильтры аудита">
        <div className="admin-audit-tabs">
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
        <label className="admin-audit-search">
          <span>⌕</span>
          <input
            id="admin-audit-search"
            name="admin-audit-search"
            aria-label="Поиск в журнале аудита"
            placeholder="actor, target, action"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
          />
        </label>
      </section>

      <div className="admin-audit-layout">
        <main className="admin-audit-log" aria-label="События аудита">
          <div className="admin-audit-log__heading">
            <h2>События</h2>
            <p>Последние изменения ролей, модерации, подписок и ограничений.</p>
          </div>

          <div className="admin-audit-log__head" aria-hidden="true">
            <span>Время</span>
            <span>Actor</span>
            <span>Действие</span>
            <span>Сервис</span>
            <span>Риск</span>
          </div>

          <div className="admin-audit-event-list">
            {filteredEvents.length === 0 ? (
              <div className="admin-live-state">
                {page.events.length === 0 ? 'Журнал аудита пуст.' : 'События не найдены по текущему фильтру.'}
              </div>
            ) : filteredEvents.map((event) => (
              <article key={event.id} className={cn('admin-audit-event', event.active && 'is-active')}>
                <time>{event.time}</time>
                <div>
                  <strong>{event.actor}</strong>
                  <p>{event.role}</p>
                </div>
                <div>
                  <b>{event.action}</b>
                  <p>{event.target}</p>
                </div>
                <span>{event.service}</span>
                <em className={`admin-audit-severity admin-audit-severity--${event.tone}`}>{severityLabels[event.severity]}</em>
              </article>
            ))}
          </div>
        </main>

        <aside className="admin-audit-detail" aria-label="Детали события аудита">
          <div className="admin-audit-detail__heading">
            <span>{selected.id}</span>
            <h2>{selected.title}</h2>
            <p>{selected.description}</p>
          </div>

          <dl className="admin-audit-meta">
            {selected.meta.map((item) => (
              <div key={item.label}>
                <dt>{item.label}</dt>
                <dd>{item.value}</dd>
              </div>
            ))}
          </dl>

          <section className="admin-audit-payload">
            <h3>Payload</h3>
            {selected.payload.length === 0 ? (
              <p className="admin-live-state">Payload пуст.</p>
            ) : selected.payload.map((line) => (
              <code key={line}>{line}</code>
            ))}
          </section>
        </aside>
      </div>

      <section className="admin-audit-bottom" aria-label="Политики и экспорт аудита">
        <article className="admin-audit-policy-card">
          <h2>Покрытие политик</h2>
          {page.policies.length === 0 ? (
            <div className="admin-live-state">Политики аудита пока не настроены.</div>
          ) : page.policies.map((policy) => (
            <div key={policy.title} className="admin-audit-policy">
              <div>
                <strong>{policy.title}</strong>
                <span>{policy.percent}%</span>
              </div>
              <p>{policy.text}</p>
              <i>
                <em style={{ width: `${policy.percent}%` }} />
              </i>
            </div>
          ))}
        </article>

        {page.exports.map((item) => (
          <article key={item.title} className="admin-audit-export-card">
            <h2>{item.title}</h2>
            <p>{item.text}</p>
            <Button variant="ghost" disabled>{item.action}</Button>
          </article>
        ))}
      </section>
    </div>
  );
}
