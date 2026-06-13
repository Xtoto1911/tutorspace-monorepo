import { useEffect, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { getAdminDashboard } from '../../shared/api/admin';
import { cn } from '../../shared/lib/cn';
import type { AdminDashboardPageData } from '../../shared/types';
import { emptyAdminDashboardPage, getReadableError } from './admin-live-helpers';

const severityLabelMap: Record<AdminDashboardPageData['alerts'][number]['severity'], string> = {
  high: 'Высокий',
  medium: 'Средний',
  low: 'Низкий',
};

export function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState<AdminDashboardPageData>(emptyAdminDashboardPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getAdminDashboard()
      .then((data) => {
        if (!ignore) {
          setDashboard(data);
          setError('');
        }
      })
      .catch((error) => {
        if (!ignore) {
          setDashboard(emptyAdminDashboardPage);
          setError(getReadableError(error, 'Не удалось загрузить обзор администратора.'));
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
    <div className="admin-dashboard">
      <header className="admin-dashboard__header">
        <div>
          <span>Admin Control Center</span>
          <h1>Обзор администратора</h1>
          <p>Ключевые показатели, здоровье сервисов, очередь модерации и быстрые переходы к управлению платформой.</p>
        </div>

        <div className="admin-dashboard__actions">
          <LinkButton to="/admin/audit" variant="ghost">
            Смотреть аудит
          </LinkButton>
          <Button disabled title="Экспорт появится позже">Экспорт позже</Button>
        </div>
      </header>

      {loading ? <section className="admin-live-state" aria-live="polite">Загружаем live-данные...</section> : null}
      {error ? <section className="admin-live-state admin-live-state--error" aria-live="polite">{error}</section> : null}

      <section className="admin-dashboard-metrics" aria-label="Ключевые показатели платформы">
        {dashboard.metrics.map((metric) => (
          <article key={metric.label} className={cn('admin-dashboard-metric', `admin-dashboard-metric--${metric.tone}`)}>
            <span>{metric.icon}</span>
            <div>
              <strong>{metric.value}</strong>
              <p>{metric.label}</p>
            </div>
            <b>{metric.delta}</b>
          </article>
        ))}
      </section>

      <div className="admin-dashboard-layout">
        <main className="admin-dashboard-main" aria-label="Операционная сводка">
          <section className="admin-dashboard-card admin-dashboard-services">
            <div className="admin-dashboard-card__heading">
              <div>
                <h2>Здоровье сервисов</h2>
                <p>Статусы auth, marketplace, CRM и notification слоя.</p>
              </div>
              <span>Live</span>
            </div>

            <div className="admin-dashboard-service-grid">
              {dashboard.services.length === 0 ? (
                <div className="admin-live-state">Нет live-статусов сервисов.</div>
              ) : dashboard.services.map((service) => (
                <article key={service.name} className="admin-dashboard-service">
                  <div>
                    <i className={`admin-dashboard-service__dot admin-dashboard-service__dot--${service.tone}`} />
                    <strong>{service.name}</strong>
                  </div>
                  <span>{service.status}</span>
                  <dl>
                    <div>
                      <dt>uptime</dt>
                      <dd>{service.uptime}</dd>
                    </div>
                    <div>
                      <dt>latency</dt>
                      <dd>{service.latency}</dd>
                    </div>
                  </dl>
                </article>
              ))}
            </div>
          </section>

          <section className="admin-dashboard-card">
            <div className="admin-dashboard-card__heading">
              <div>
                <h2>Очередь модерации</h2>
                <p>Проверки, которые влияют на публичный каталог и доверие пользователей.</p>
              </div>
              <LinkButton to="/moderator/complaints" variant="ghost">
                В модерацию
              </LinkButton>
            </div>

            <div className="admin-dashboard-table" role="table" aria-label="Очередь модерации">
              {dashboard.moderationQueue.length === 0 ? (
                <div className="admin-live-state">Нет открытых жалоб.</div>
              ) : dashboard.moderationQueue.map((item) => (
                <article key={item.id} className="admin-dashboard-row" role="row">
                  <span>{item.type}</span>
                  <strong>{item.target}</strong>
                  <p>{item.status}</p>
                  <b>{item.owner}</b>
                </article>
              ))}
            </div>
          </section>

          <section className="admin-dashboard-card admin-dashboard-actions">
            <div className="admin-dashboard-card__heading">
              <div>
                <h2>Быстрые действия</h2>
                <p>Основные admin-маршруты уже подготовлены под будущие backend API.</p>
              </div>
            </div>

            <div className="admin-dashboard-action-grid">
              {dashboard.quickActions.length === 0 ? (
                <div className="admin-live-state">Нет быстрых действий.</div>
              ) : dashboard.quickActions.map((action) => (
                <LinkButton key={action.to} to={action.to} variant="ghost" className="admin-dashboard-action">
                  <strong>{action.title}</strong>
                  <span>{action.text}</span>
                </LinkButton>
              ))}
            </div>
          </section>
        </main>

        <aside className="admin-dashboard-side" aria-label="Алерты и аудит">
          <section className="admin-dashboard-card admin-dashboard-alerts">
            <div className="admin-dashboard-card__heading">
              <div>
                <h2>Системные алерты</h2>
                <p>События из `/api/admin/alerts`.</p>
              </div>
            </div>

            <div className="admin-dashboard-alert-list">
              {dashboard.alerts.length === 0 ? (
                <div className="admin-live-state">Системных алертов нет.</div>
              ) : dashboard.alerts.map((alert) => (
                <article key={alert.id} className={cn('admin-dashboard-alert', `admin-dashboard-alert--${alert.severity}`)}>
                  <div>
                    <span>{severityLabelMap[alert.severity]}</span>
                    <time>{alert.time}</time>
                  </div>
                  <strong>{alert.title}</strong>
                  <p>{alert.description}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="admin-dashboard-card admin-dashboard-audit">
            <div className="admin-dashboard-card__heading admin-dashboard-card__heading--row">
              <h2>Последний аудит</h2>
              <LinkButton to="/admin/audit" variant="ghost">
                Все
              </LinkButton>
            </div>

            <div className="admin-dashboard-audit-list">
              {dashboard.audit.length === 0 ? (
                <div className="admin-live-state">Нет событий аудита.</div>
              ) : dashboard.audit.map((item) => (
                <article key={item.id}>
                  <span>{item.actor}</span>
                  <strong>{item.action}</strong>
                  <time>{item.date}</time>
                </article>
              ))}
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}
