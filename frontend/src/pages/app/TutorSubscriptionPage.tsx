import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { createTutorSubscriptionCheckout, getTutorSubscription } from '../../shared/api/tutor';
import { cn } from '../../shared/lib/cn';
import type { TutorSubscriptionPageData } from '../../shared/types';

const checkoutStorageKey = 'tutorspace.subscription.checkout';

export function TutorSubscriptionPage() {
  const [subscription, setSubscription] = useState<TutorSubscriptionPageData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pendingTariff, setPendingTariff] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    getTutorSubscription()
      .then((data) => {
        if (mounted) {
          setSubscription(data);
          setStatusMessage(null);
        }
      })
      .catch((error) => {
        if (mounted) {
          setStatusMessage(error instanceof Error ? error.message : 'Не удалось загрузить подписку.');
        }
      })
      .finally(() => {
        if (mounted) {
          setIsLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  async function handleCheckout(tariffCode: string) {
    setPendingTariff(tariffCode);
    setStatusMessage(null);

    try {
      const checkout = await createTutorSubscriptionCheckout(tariffCode);
      window.sessionStorage.setItem(checkoutStorageKey, JSON.stringify({
        paymentId: checkout.paymentId,
      }));
      window.location.assign('/app/tutor/subscription/checkout/result');
    } catch (error) {
      setStatusMessage(error instanceof Error ? error.message : 'Не удалось создать платёж ЮKassa.');
      setPendingTariff(null);
    }
  }

  if (isLoading) {
    return <div className="tutor-subscription">Загрузка подписки...</div>;
  }

  if (!subscription) {
    return (
      <div className="tutor-subscription">
        <p role="alert">{statusMessage || 'Подписка недоступна.'}</p>
        <Button type="button" onClick={() => window.location.reload()}>Повторить</Button>
      </div>
    );
  }

  return (
    <div className="tutor-subscription">
      <header className="tutor-subscription__header">
        <div>
          <p className="tutor-subscription__eyebrow">Кабинет репетитора</p>
          <h1>Подписка</h1>
          <p>Тариф, лимиты заявок, продвижение профиля и история активности подписки.</p>
        </div>

        <div className="tutor-subscription__actions">
          <span className="tutor-subscription-status">
            <i />
            {subscription.plan.status}
          </span>
          <Button type="button" onClick={() => handleCheckout('CRM_PRO')} disabled={pendingTariff !== null}>
            Улучшить тариф
          </Button>
        </div>
      </header>

      {statusMessage ? <p className="tutor-subscription-alert">{statusMessage}</p> : null}

      <section className="tutor-subscription-stats" aria-label="Эффект подписки">
        {subscription.metrics.map((metric) => (
          <article key={metric.label} className={cn('tutor-subscription-stat', `tutor-subscription-stat--${metric.tone}`)}>
            <span className="tutor-subscription-stat__icon">{metric.icon}</span>
            <div>
              <strong>{metric.label}</strong>
              <p>{metric.sublabel}</p>
            </div>
            <b>{metric.value}</b>
          </article>
        ))}
      </section>

      <section className="tutor-subscription-card tutor-subscription-tariffs">
        <div className="tutor-subscription-card__heading">
          <h2>Тарифные планы</h2>
          <p>Выберите пакет для нужного объёма заявок и продвижения в каталоге.</p>
        </div>

        <div className="tutor-tariff-grid">
          {subscription.tariffs.map((tariff) => (
            <article key={tariff.id} className={cn('tutor-tariff-card', tariff.current && 'is-current')}>
              <div className="tutor-tariff-card__top">
                <h3>{tariff.name}</h3>
                {tariff.badge ? <span>{tariff.badge}</span> : null}
              </div>
              <p>{tariff.description}</p>
              <div className="tutor-tariff-card__price">
                <strong>{tariff.price}</strong>
                <small>{tariff.period}</small>
              </div>
              <ul>
                {tariff.features.map((feature) => (
                  <li key={feature}>{feature}</li>
                ))}
              </ul>
              <Button
                type="button"
                variant={tariff.current ? 'ghost' : 'primary'}
                disabled={pendingTariff !== null}
                onClick={() => handleCheckout(tariff.id)}
              >
                {pendingTariff === tariff.id ? 'Создаём платёж' : tariff.current ? 'Продлить тариф' : 'Выбрать'}
              </Button>
            </article>
          ))}
        </div>
      </section>

      <div className="tutor-subscription-layout">
        <main className="tutor-subscription-main" aria-label="Активность подписки">

          <section className="tutor-subscription-activity">
            <div className="tutor-subscription-card__heading">
              <h2>Активность подписки</h2>
              <p>Что тариф дал профилю за текущий период.</p>
            </div>

            <div className="tutor-subscription-activity-grid">
              {subscription.features.map((feature) => (
                <article key={feature.title} className={cn('tutor-subscription-feature', feature.enabled && 'is-enabled')}>
                  <span>{feature.enabled ? '✓' : '!'}</span>
                  <div>
                    <strong>{feature.title}</strong>
                    <p>{feature.text}</p>
                  </div>
                </article>
              ))}
            </div>
          </section>
        </main>

        <aside className="tutor-subscription-side" aria-label="История и рекомендации">
          <section className="tutor-plan-limits">
            <div>
              <span>Текущий план</span>
              <h2>{subscription.plan.name}</h2>
              <p>
                {subscription.plan.price} · {subscription.plan.period}
              </p>
            </div>

            <div className="tutor-plan-limits__bar">
              <span>Лимит заявок</span>
              <strong>{subscription.plan.usagePercent}% использовано</strong>
              <i>
                <b style={{ width: `${subscription.plan.usagePercent}%` }} />
              </i>
            </div>

            <dl className="tutor-plan-limits__list">
              <div>
                <dt>Следующее списание</dt>
                <dd>{subscription.plan.nextPayment}</dd>
              </div>
              <div>
                <dt>Действует до</dt>
                <dd>{subscription.plan.expiresAt}</dd>
              </div>
              <div>
                <dt>Аналитика профиля</dt>
                <dd>Включена</dd>
              </div>
              <div>
                <dt>Бейдж доверия</dt>
                <dd>Активен</dd>
              </div>
            </dl>
          </section>

          <section className="tutor-subscription-history">
            <h2>История списаний</h2>
            <div>
              {subscription.history.map((item) => (
                <article key={`${item.date}-${item.title}`}>
                  <span>{item.date}</span>
                  <div>
                    <strong>{item.title}</strong>
                    <p>{item.status}</p>
                  </div>
                  <b>{item.amount}</b>
                </article>
              ))}
            </div>
          </section>
        </aside>
      </div>

      <section className="tutor-subscription-faq" aria-label="Вопросы по подписке">
        <div>
          <strong>Вопросы по подписке</strong>
          <p>{subscription.recommendation.text}</p>
        </div>
        <Button type="button" onClick={() => handleCheckout('CRM_PRO')} disabled={pendingTariff !== null}>
          {subscription.recommendation.action}
        </Button>
        <Button variant="ghost">Написать поддержке</Button>
      </section>
    </div>
  );
}
