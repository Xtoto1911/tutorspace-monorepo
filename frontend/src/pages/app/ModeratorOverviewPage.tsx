import { useCallback, useEffect, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { getModeratorDashboard } from '../../shared/api/admin';
import type { ModeratorDashboardPageData } from '../../shared/types';
import { getReadableError } from './admin-live-helpers';

export function ModeratorOverviewPage() {
  const [dashboard, setDashboard] = useState<ModeratorDashboardPageData | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setDashboard(await getModeratorDashboard());
    } catch (loadError) {
      setDashboard(null);
      setError(getReadableError(loadError, 'Не удалось загрузить обзор модерации.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  if (loading) {
    return <section className="admin-live-state" aria-live="polite">Загружаем обзор модерации...</section>;
  }

  if (!dashboard) {
    return (
      <section className="admin-live-state admin-live-state--error" role="alert">
        <p>{error}</p>
        <Button type="button" onClick={() => void loadDashboard()}>Повторить</Button>
      </section>
    );
  }

  const metrics = [
    { label: 'Всего в работе', value: dashboard.totalPending, tone: 'amber' },
    { label: 'Жалобы', value: dashboard.pendingComplaints, tone: 'rose' },
    { label: 'Отзывы', value: dashboard.pendingReviews, tone: 'indigo' },
    { label: 'Анкеты', value: dashboard.pendingProfiles, tone: 'teal' },
  ];

  return (
    <div className="admin-dashboard">
      <header className="admin-dashboard__header">
        <div>
          <span>Moderator panel</span>
          <h1>Обзор модерации</h1>
          <p>Живые очереди жалоб, отзывов и анкет преподавателей.</p>
        </div>
        <div className="admin-dashboard__actions">
          <Button type="button" variant="ghost" onClick={() => void loadDashboard()}>Обновить</Button>
        </div>
      </header>

      <section className="admin-dashboard-metrics" aria-label="Очереди модерации">
        {metrics.map((metric) => (
          <article key={metric.label} className={`admin-dashboard-metric admin-dashboard-metric--${metric.tone}`}>
            <strong>{metric.value}</strong>
            <p>{metric.label}</p>
            <b>live</b>
          </article>
        ))}
      </section>

      <div className="admin-dashboard-layout">
        <main className="admin-dashboard-main">
          <section className="admin-dashboard-card admin-dashboard-actions">
            <div className="admin-dashboard-card__heading">
              <div><h2>Рабочие очереди</h2><p>Решения сохраняются в журнале аудита.</p></div>
            </div>
            <div className="admin-dashboard-action-grid">
              <LinkButton to="/moderator/complaints" variant="ghost">Открыть жалобы</LinkButton>
              <LinkButton to="/moderator/review-moderation" variant="ghost">Проверить контент</LinkButton>
            </div>
          </section>
        </main>

        <aside className="admin-dashboard-side">
          <section className="admin-dashboard-card admin-dashboard-audit">
            <div className="admin-dashboard-card__heading"><div><h2>Последние решения</h2></div></div>
            <div className="admin-dashboard-audit-list">
              {dashboard.recentDecisions.length === 0 ? (
                <div className="admin-live-state">Решений пока нет.</div>
              ) : dashboard.recentDecisions.map((decision) => (
                <article key={decision.id}>
                  <strong>{decision.action}</strong>
                  <p>{decision.targetType}: {decision.targetId}</p>
                  <span>{decision.decidedAt}</span>
                </article>
              ))}
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}
