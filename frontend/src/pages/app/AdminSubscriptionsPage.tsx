import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { getAdminSubscriptionsPage } from '../../shared/api/admin';
import { cn } from '../../shared/lib/cn';
import type { AdminSubscriptionsPageData } from '../../shared/types';
import { emptyAdminSubscriptionsPage, getReadableError } from './admin-live-helpers';

export function AdminSubscriptionsPage() {
  const [page, setPage] = useState<AdminSubscriptionsPageData>(emptyAdminSubscriptionsPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getAdminSubscriptionsPage()
      .then((data) => {
        if (!ignore) {
          setPage(data);
          setError('');
        }
      })
      .catch((error) => {
        if (!ignore) {
          setPage(emptyAdminSubscriptionsPage);
          setError(getReadableError(error, 'Не удалось загрузить подписки.'));
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
    <div className="admin-subscriptions">
      <header className="admin-subscriptions__header">
        <div>
          <p className="admin-subscriptions__eyebrow">Администрирование</p>
          <h1>Подписки и выручка</h1>
          <p>Администратор видит активные тарифы, платежи провайдера и проблемные webhook-события.</p>
        </div>
        <Button disabled title="Статусы читаются из live snapshot">Live snapshot</Button>
      </header>

      {loading ? <section className="admin-live-state" aria-live="polite">Загружаем подписки...</section> : null}
      {error ? <section className="admin-live-state admin-live-state--error" aria-live="polite">{error}</section> : null}

      <section className="admin-subscription-kpis" aria-label="Сводка по подпискам">
        {page.metrics.map((metric) => (
          <article key={metric.label} className={cn('admin-subscription-kpi', `admin-subscription-kpi--${metric.tone}`)}>
            <span>{metric.icon}</span>
            <div>
              <strong>{metric.value}</strong>
              <p>{metric.label}</p>
            </div>
            <b>{metric.hint}</b>
          </article>
        ))}
      </section>

      <section className="admin-subscription-table" aria-label="Последние подписки">
        <h2>Последние подписки</h2>
        <div className="admin-subscription-table__head" aria-hidden="true">
          <span>Репетитор</span>
          <span>Тариф</span>
          <span>Статус</span>
          <span>Активна до</span>
          <span>Платёж</span>
        </div>

        <div className="admin-subscription-list">
          {page.subscriptions.length === 0 ? (
            <div className="admin-live-state">Подписок пока нет или subscription-service недоступен.</div>
          ) : page.subscriptions.map((subscription) => (
            <article key={subscription.id} className="admin-subscription-row">
              <strong>{subscription.tutor}</strong>
              <span className="admin-subscription-plan">{subscription.plan}</span>
              <span className={cn('admin-subscription-status', `admin-subscription-status--${subscription.tone}`)}>
                {subscription.status}
              </span>
              <span>{subscription.expiresAt}</span>
              <b>{subscription.payment}</b>
            </article>
          ))}
        </div>
      </section>

      <section className="admin-subscription-insights" aria-label="Аналитика подписок">
        <article className="admin-subscription-card admin-subscription-revenue">
          <h2>Выручка по тарифам</h2>
          <div className="admin-subscription-chart">
            {page.revenueByPlan.length === 0 ? (
              <div className="admin-live-state">Подтвержденных платежей пока нет.</div>
            ) : page.revenueByPlan.map((item) => (
              <div key={item.plan} className="admin-subscription-chart__item">
                <div>
                  <i className={`admin-subscription-chart__bar admin-subscription-chart__bar--${item.tone}`} style={{ height: `${item.percent * 1.84}px` }} />
                </div>
                <strong>{item.plan}</strong>
                <span>{item.value}</span>
              </div>
            ))}
          </div>
          <p>Показаны только подтвержденные платежи из subscription-service.</p>
        </article>

        <article className="admin-subscription-card admin-subscription-webhooks">
          <h2>Очередь платежных событий</h2>
          <div className="admin-webhook-list">
            {page.webhookQueue.length === 0 ? (
              <div className="admin-live-state">Проблемных платежных событий нет.</div>
            ) : page.webhookQueue.map((event) => (
              <article key={event.id} className="admin-webhook-row">
                <strong>{event.id}</strong>
                <span className={`admin-webhook-row__event admin-webhook-row__event--${event.tone}`}>{event.event}</span>
                <p>{event.action}</p>
              </article>
            ))}
          </div>
        </article>
      </section>

      <section className="admin-plan-settings" aria-label="Настройки тарифов">
        <div className="admin-plan-settings__heading">
          <h2>Настройки тарифов</h2>
          <Button variant="ghost" disabled title="Редактирование тарифов не входит в live API">Только просмотр</Button>
        </div>
        <div className="admin-plan-grid">
          {page.planSettings.length === 0 ? (
            <div className="admin-live-state">Тарифы не получены из subscription-service.</div>
          ) : page.planSettings.map((plan) => (
            <article key={plan.name} className="admin-plan-card">
              <div>
                <h3>{plan.name}</h3>
                <strong>{plan.price}</strong>
              </div>
              <p>{plan.body}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
