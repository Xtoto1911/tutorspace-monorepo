import { useEffect, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { cancelClientApplication, getClientApplicationsPage } from '../../shared/api/client';
import { cn } from '../../shared/lib/cn';
import type { ClientApplicationsPageData } from '../../shared/types';

export function ClientApplicationsPage() {
  const [pageData, setPageData] = useState<ClientApplicationsPageData | null>(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

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

  const handleCancel = async (applicationId: string) => {
    if (!confirm('Отменить заявку? Это действие нельзя отменить.')) return;
    try {
      await cancelClientApplication(applicationId);
      await loadPage();
    } catch {
      setError('Не удалось отменить заявку.');
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
        <Button className="client-applications__new">Новая заявка</Button>
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
                  <Button variant="ghost" onClick={() => void handleCancel(application.id)}>Отменить</Button>
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

          <section className="client-side-card">
            <h2>Подбор репетитора</h2>
            <div className="match-progress">
              {pageData.matchProgress.map((item) => (
                <div key={item.label} className="match-progress__item">
                  <div>
                    <span>{item.label}</span>
                    <strong>{item.value}%</strong>
                  </div>
                  <meter min="0" max="100" value={item.value} />
                </div>
              ))}
            </div>
          </section>

          <section className="client-side-card">
            <h2>Рекомендации</h2>
            <ul className="client-recommendations">
              {pageData.recommendations.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>
        </aside>
      </div>
    </div>
  );
}
