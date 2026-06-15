import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import {
  getTutorCheckoutPage,
  refreshTutorSubscriptionPayment,
  type TutorCheckoutPageResponse,
} from '../../shared/api/tutor';

const checkoutStorageKey = 'tutorspace.subscription.checkout';

function readStoredPaymentId(): string | null {
  try {
    const raw = window.sessionStorage.getItem(checkoutStorageKey);
    if (!raw) return null;
    const stored = JSON.parse(raw) as { paymentId?: string };
    return stored.paymentId ?? null;
  } catch {
    return null;
  }
}

function formatRub(amount: number) {
  return `${new Intl.NumberFormat('ru-RU').format(amount).replace(/\u00a0/g, ' ')} ₽`;
}

export function TutorTariffCheckoutPage() {
  const paymentId = useMemo(() => readStoredPaymentId(), []);
  const [checkout, setCheckout] = useState<TutorCheckoutPageResponse | null>(null);
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const hasAutoRefreshed = useRef(false);

  const loadCheckout = useCallback(async () => {
    if (!paymentId) {
      setCheckout(null);
      setMessage('Нет активного платежа. Сначала выберите тариф.');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setMessage('');
    try {
      setCheckout(await getTutorCheckoutPage(paymentId));
    } catch (error) {
      setCheckout(null);
      setMessage(error instanceof Error ? error.message : 'Не удалось загрузить платеж.');
    } finally {
      setIsLoading(false);
    }
  }, [paymentId]);

  const refreshPayment = useCallback(async (mode: 'auto' | 'manual' = 'manual') => {
    if (!paymentId) return;

    setIsRefreshing(true);
    setMessage(mode === 'auto' ? 'Проверяем статус платежа в ЮKassa...' : '');
    try {
      const result = await refreshTutorSubscriptionPayment(paymentId);
      const updated = await getTutorCheckoutPage(paymentId);
      setCheckout(updated);

      if (result?.activeUntil || updated.status === 'CONFIRMED') {
        setMessage('Оплата подтверждена. CRM-подписка активирована.');
        window.sessionStorage.removeItem(checkoutStorageKey);
      } else if (updated.status === 'CANCELLED' || updated.status === 'FAILED') {
        setMessage('Платеж не завершен. Создайте новый checkout на странице подписки.');
      } else {
        setMessage('Платеж еще не подтвержден. После оплаты повторите проверку.');
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Не удалось проверить оплату.');
    } finally {
      setIsRefreshing(false);
    }
  }, [paymentId]);

  useEffect(() => {
    void loadCheckout();
  }, [loadCheckout]);

  useEffect(() => {
    if (!checkout || checkout.status !== 'PENDING' || hasAutoRefreshed.current) return;
    hasAutoRefreshed.current = true;
    void refreshPayment('auto');
  }, [checkout, refreshPayment]);

  if (isLoading) {
    return <div className="tutor-checkout">Загрузка платежа...</div>;
  }

  if (!checkout) {
    return (
      <div className="tutor-checkout">
        <p role="alert">{message}</p>
        <LinkButton to="/app/tutor/subscription">Выбрать тариф</LinkButton>
        {paymentId ? <Button type="button" variant="ghost" onClick={() => void loadCheckout()}>Повторить</Button> : null}
      </div>
    );
  }

  const checkoutPage = checkout;
  const tariffPrice = formatRub(checkoutPage.amountRub);
  const qrImageUrl = checkoutPage.confirmationUrl
    ? `https://api.qrserver.com/v1/create-qr-code/?size=260x260&margin=14&data=${encodeURIComponent(checkoutPage.confirmationUrl)}`
    : null;
  function openYooKassa() {
    if (!checkoutPage.confirmationUrl) {
      setMessage('Ссылка оплаты отсутствует. Создайте новый checkout.');
      return;
    }
    window.location.assign(checkoutPage.confirmationUrl);
  }

  return (
    <div className="tutor-checkout">
      <header className="tutor-checkout__header">
        <div>
          <h1>Оплата тарифа</h1>
          <p>Оплата относится только к CRM-подписке TutorSpace и выполняется в тестовом магазине ЮKassa.</p>
        </div>
        <span className="tutor-checkout-status"><i />{checkout.status}</span>
      </header>

      {message ? <p className="tutor-checkout-alert" role="status">{message}</p> : null}

      <div className="tutor-checkout-layout">
        <main className="tutor-checkout-main" aria-label="Оплата">
          <section className="tutor-checkout-card tutor-qr-card">
            <div className="tutor-qr-card__heading">
              <div><h2>ЮKassa sandbox</h2><p>Откройте тестовую страницу оплаты или отсканируйте QR-код.</p></div>
              <span>{checkout.providerStatus ?? checkout.status}</span>
            </div>
            <div className="tutor-qr-card__body">
              <div className="tutor-qr-preview" aria-label="QR код для оплаты">
                {qrImageUrl ? <img src={qrImageUrl} alt="QR-код со ссылкой тестовой оплаты ЮKassa" /> : <span>Ссылка оплаты отсутствует</span>}
              </div>
              <div className="tutor-qr-card__amount">
                <span>К оплате</span>
                <strong>{tariffPrice}</strong>
                <Button type="button" disabled={!checkout.confirmationUrl} onClick={openYooKassa}>Перейти в ЮKassa</Button>
                <Button type="button" variant="ghost" disabled={isRefreshing} onClick={() => void refreshPayment('manual')}>
                  {isRefreshing ? 'Проверяем' : 'Проверить оплату'}
                </Button>
              </div>
            </div>
            <p className="tutor-qr-card__footnote">Тестовый режим: реальные деньги не списываются.</p>
          </section>
        </main>

        <aside className="tutor-checkout-side" aria-label="Детали заказа">
          <section className="tutor-checkout-card tutor-order-card">
            <div className="tutor-order-card__heading"><h2>Заказ</h2><span>{checkout.paymentId}</span></div>
            <dl>
              <div><dt>Тариф</dt><dd>{checkout.tariffName}</dd></div>
              <div><dt>Код тарифа</dt><dd>{checkout.tariffCode}</dd></div>
              <div><dt>Период</dt><dd>{checkout.durationDays} дней</dd></div>
              <div><dt>Сумма</dt><dd>{tariffPrice} {checkout.currency}</dd></div>
            </dl>
          </section>
        </aside>
      </div>

      <section className="tutor-activation-card">
        <div><h2>После подтверждения</h2><p>Subscription-service активирует CRM-тариф и обновит срок подписки.</p></div>
        <LinkButton to="/app/tutor/subscription" variant="ghost">Вернуться к подписке</LinkButton>
      </section>
    </div>
  );
}
