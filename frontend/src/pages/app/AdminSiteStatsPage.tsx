import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { getAdminSiteStatsPage } from '../../shared/api/admin';
import { cn } from '../../shared/lib/cn';
import type { AdminSiteStatsPageData } from '../../shared/types';
import { emptyAdminSiteStatsPage, getMaxGrowthValue, getReadableError } from './admin-live-helpers';

export function AdminSiteStatsPage() {
  const [page, setPage] = useState<AdminSiteStatsPageData>(emptyAdminSiteStatsPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getAdminSiteStatsPage()
      .then((data) => {
        if (!ignore) {
          setPage(data);
          setError('');
        }
      })
      .catch((error) => {
        if (!ignore) {
          setPage(emptyAdminSiteStatsPage);
          setError(getReadableError(error, 'Не удалось загрузить статистику платформы.'));
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

  const maxGrowth = getMaxGrowthValue(page);
  const hasGrowthHistory = page.growth.labels.length > 0;

  return (
    <div className="admin-stats">
      <header className="admin-stats__header">
        <div>
          <p className="admin-stats__eyebrow">Администрирование</p>
          <h1>Статистика платформы</h1>
          <p>Отслеживайте рост пользователей, заявки, конверсию, подписки и стабильность сервиса.</p>
        </div>
        <Button disabled title="История периодов пока не хранится">Текущие агрегаты</Button>
      </header>

      {loading ? <section className="admin-live-state" aria-live="polite">Загружаем live-агрегаты...</section> : null}
      {error ? <section className="admin-live-state admin-live-state--error" aria-live="polite">{error}</section> : null}

      <section className="admin-stats-metrics" aria-label="Ключевые показатели платформы">
        {page.metrics.map((metric) => (
          <article key={metric.title} className={cn('admin-stats-metric', `admin-stats-metric--${metric.tone}`)}>
            <span>{metric.icon}</span>
            <div>
              <strong>{metric.title}</strong>
              <p>{metric.meta}</p>
            </div>
            <b>{metric.value}</b>
          </article>
        ))}
      </section>

      <section className="admin-stats-toolbar" aria-label="Фильтры статистики">
        <div className="admin-stats-tabs">
          {page.tabs.map((tab, index) => (
            <button key={tab} className={cn(index === 0 && 'is-active')} type="button">
              {tab}
            </button>
          ))}
        </div>
        <div className="admin-stats-toolbar__actions">
          <Button disabled title="Исторические периоды пока не сохраняются">Сравнить позже</Button>
          <Button variant="ghost" disabled title="Экспорт появится позже">Экспорт позже</Button>
        </div>
      </section>

      <section className="admin-stats-top-grid" aria-label="Динамика и качество">
        <article className="admin-stats-chart">
          <div className="admin-stats-card__heading admin-stats-card__heading--row">
            <div>
              <h2>{page.growth.title}</h2>
              <p>{page.growth.caption}</p>
            </div>
            <div className="admin-stats-chart__legend" aria-hidden="true">
              <span className="is-users">Пользователи</span>
              <span className="is-requests">Заявки</span>
            </div>
          </div>

          {hasGrowthHistory ? (
            <div className="admin-stats-chart__plot">
              {page.growth.labels.map((label, index) => (
              <div key={label} className="admin-stats-chart__week">
                <div className="admin-stats-chart__bars">
                  <i style={{ height: `${(page.growth.users[index] / maxGrowth) * 230}px` }} />
                  <b style={{ height: `${(page.growth.requests[index] / maxGrowth) * 230}px` }} />
                </div>
                <span>{label}</span>
              </div>
              ))}
            </div>
          ) : (
            <div className="admin-live-state">История пока не хранится.</div>
          )}

          <p className="admin-stats-chart__footer">{page.growth.footer}</p>
        </article>

        <aside className="admin-stats-side">
          <article className="admin-stats-conversion">
            <span>{page.conversion.label}</span>
            <strong>{page.conversion.value}</strong>
            <p>{page.conversion.text}</p>
          </article>

          <article className="admin-stats-quality">
            <h2>Качество сервиса</h2>
            {page.quality.length === 0 ? (
              <div className="admin-live-state">Нет live-метрик качества.</div>
            ) : page.quality.map((item) => (
              <div key={item.label} className="admin-stats-progress-row">
                <div>
                  <span>{item.label}</span>
                  <b>{item.value}</b>
                </div>
                <i>
                  <em style={{ width: `${item.percent}%` }} />
                </i>
              </div>
            ))}
          </article>
        </aside>
      </section>

      <section className="admin-stats-info-grid" aria-label="Воронка, финансы и география">
        <article className="admin-stats-card">
          <h2>Воронка заявки</h2>
          {page.funnel.length === 0 ? (
            <div className="admin-live-state">Нет live-источника воронки.</div>
          ) : page.funnel.map((step) => (
            <div key={step.label} className="admin-stats-progress-row admin-stats-progress-row--funnel">
              <div>
                <span>{step.label}</span>
                <b>{step.value}</b>
              </div>
              <i>
                <em style={{ width: `${step.percent}%` }} />
              </i>
            </div>
          ))}
        </article>

        <article className="admin-stats-finance">
          <h2>{page.finance.title}</h2>
          <strong>{page.finance.value}</strong>
          <p>{page.finance.caption}</p>
          <span>{page.finance.trend}</span>
        </article>

        <article className="admin-stats-card">
          <h2>География спроса</h2>
          {page.geo.length === 0 ? (
            <div className="admin-live-state">География пока не собирается.</div>
          ) : page.geo.map((item) => (
            <div key={item.city} className="admin-stats-progress-row admin-stats-progress-row--geo">
              <div>
                <span>{item.city}</span>
                <b>{item.value}</b>
              </div>
              <i>
                <em style={{ width: `${item.percent}%` }} />
              </i>
            </div>
          ))}
        </article>
      </section>

      <section className="admin-stats-bottom-grid" aria-label="Операционные события и вывод">
        <article className="admin-stats-events">
          <h2>Операционные события</h2>
          {page.events.length === 0 ? (
            <div className="admin-live-state">Операционных событий пока нет.</div>
          ) : page.events.map((event) => (
            <div key={`${event.time}-${event.title}`} className="admin-stats-event">
              <i className={`admin-stats-event__line admin-stats-event__line--${event.tone}`} />
              <time>{event.time}</time>
              <strong>{event.title}</strong>
              <span>{event.meta}</span>
            </div>
          ))}
        </article>

        <article className="admin-stats-note">
          <h2>{page.note.title}</h2>
          <p>{page.note.text}</p>
          <Button>{page.note.action}</Button>
        </article>
      </section>
    </div>
  );
}
