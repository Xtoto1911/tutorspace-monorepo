import { useEffect, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { cancelClientApplication, getClientApplicationsPage } from '../../shared/api/client';
import { cn } from '../../shared/lib/cn';
import type { ClientApplicationsPageData } from '../../shared/types';

export function ClientApplicationsPage() {
  const [pageData, setPageData] = useState<ClientApplicationsPageData | null>(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [cancelModalId, setCancelModalId] = useState<string | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);

  const loadPage = async () => {
    setIsLoading(true);
    setError('');
    try {
      setPageData(await getClientApplicationsPage());
    } catch {
      setPageData(null);
      setError('Не удалось загрузить заявки.');
    } finally {
      setIsLoading(false);
    }
  };

  const openCancelModal = (applicationId: string) => {
    setCancelModalId(applicationId);
    setError('');
  };

  const closeCancelModal = () => {
    setCancelModalId(null);
    setIsCancelling(false);
  };

  const handleCancel = async () => {
    if (!cancelModalId) return;
    setIsCancelling(true);
    try {
      await cancelClientApplication(cancelModalId);
      closeCancelModal();
      await loadPage();
    } catch {
      setError('Не удалось отменить заявку.');
      setIsCancelling(false);
    }
  };

  useEffect(() => {
    void loadPage();
  }, []);

  if (isLoading) {
    return <div className="client-applications">Загрузка заявок...</div>;
  }

  if (error || !pageData) {
    return (
      <div className="client-applications">
        <p role="alert">{error || 'Заявки недоступны.'}</p>
        <Button type="button" onClick={() => void loadPage()}>Повторить</Button>
      </div>
    );
  }

  return (
    <div className="client-applications">
      <header className="client-applications__header">
        <div>
          <h1>Мои заявки</h1>
          <p>Отслеживайте ответы репетиторов, ближайшие занятия и историю обращений.</p>
        </div>
      </header>

      <section className="client-applications__metrics" aria-label="Сводка по заявкам">
        {pageData.metrics.map((metric) => (
          <article key={metric.label} className="client-metric">
            <strong className={`client-metric__value client-metric__value--${metric.tone}`}>{metric.value}</strong>
            <span>{metric.label}</span>
          </article>
        ))}
      </section>

      <nav className="client-tabs" aria-label="Фильтр заявок">
        {pageData.tabs.map((tab, index) => (
          <button key={tab} className={cn('client-tabs__item', index === 0 && 'is-active')} type="button">
            {tab}
          </button>
        ))}
      </nav>

      <div className="client-applications__grid">
        <section className="applications-list" aria-label="Список заявок">
          {pageData.applications.length === 0 ? (
            <p>Заявок пока нет. Выберите репетитора в каталоге и отправьте первую заявку.</p>
          ) : pageData.applications.map((application) => (
            <article key={application.id} className="application-card">
              <div className="application-card__avatar">{application.initials}</div>

              <div className="application-card__body">
                <h2>{application.name}</h2>
                <p className="application-card__subject">{application.subject}</p>
                <div className="application-card__chips">
                  {application.chips.map((chip) => (
                    <span key={chip}>{chip}</span>
                  ))}
                </div>
                <p className="application-card__note">{application.note}</p>
              </div>

              <aside className="application-card__side">
                <div className={`application-status application-status--${application.statusTone}`}>
                  {application.status}
                </div>
                <strong>{application.price}</strong>
                <span>за урок</span>
              </aside>

              <div className="application-card__actions">
                <LinkButton to={`/app/client/applications/${application.id}`} variant="ghost">
                  Открыть
                </LinkButton>
                {(application.status === 'Ждет ответа' || application.status === 'Заявка принята' || application.status === 'Занятие назначено') && (
                  <Button variant="ghost" onClick={() => openCancelModal(application.id)}>Отменить</Button>
                )}
                <Button>{application.action}</Button>
              </div>
            </article>
          ))}
        </section>

        <aside className="client-aside" aria-label="Дополнительная информация">
          <section className="client-side-card client-side-card--lesson">
            <h2>{pageData.nextLesson.title}</h2>
            <span className="client-side-card__pill">{pageData.nextLesson.pill}</span>
            <strong>{pageData.nextLesson.teacher}</strong>
            <p>{pageData.nextLesson.text}</p>
            <Button variant="ghost">Открыть урок</Button>
          </section>
        </aside>
      </div>

      {cancelModalId && (
        <div className="modal-backdrop" onClick={closeCancelModal}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <h3>Отменить заявку?</h3>
            <p className="modal-hint">Это действие нельзя отменить. Заявка будет закрыта, и репетитор увидит статус «Отменена».</p>
            <div className="modal-actions">
              <Button variant="ghost" onClick={closeCancelModal} disabled={isCancelling}>
                Вернуться
              </Button>
              <Button onClick={() => void handleCancel()} disabled={isCancelling}>
                {isCancelling ? 'Отмена...' : 'Отменить заявку'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
