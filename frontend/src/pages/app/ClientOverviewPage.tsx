import { useCallback, useEffect, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { getClientDashboard } from '../../shared/api/client';
import type { ClientOverviewPageData } from '../../shared/types';

export function ClientOverviewPage() {
  const [dashboard, setDashboard] = useState<ClientOverviewPageData | null>(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const loadDashboard = useCallback(() => {
    let ignore = false;
    setIsLoading(true);
    setError('');
    getClientDashboard()
      .then((data) => {
        if (!ignore) {
          setDashboard(data);
        }
      })
      .catch((error) => {
        if (!ignore) {
          const message = error instanceof Error ? error.message : 'Не удалось загрузить обзор. Проверьте подключение и повторите попытку.';
          console.error('Client dashboard load failed:', error);
          setError(message);
        }
      })
      .finally(() => {
        if (!ignore) {
          setIsLoading(false);
        }
      });
    return () => { ignore = true; };
  }, []);

  useEffect(() => loadDashboard(), [loadDashboard]);

  return (
    <div className="client-overview">
      <header className="client-overview__header">
        <div>
          <h1>Обзор</h1>
          <p>Сводка по заявкам, занятиям и сообщениям.</p>
        </div>
        <LinkButton to="/catalog">Перейти в каталог</LinkButton>
      </header>

      <div className="client-overview__grid">
        {isLoading ? <section className="overview-card"><p>Загрузка обзора...</p></section> : null}
        {error ? (
          <section className="overview-card">
            <p>{error}</p>
            <Button type="button" onClick={loadDashboard}>Повторить</Button>
          </section>
        ) : null}
        {!isLoading && !error ? (
          <>
        <section className="overview-card">
          <h2>Последние заявки</h2>
          {dashboard?.recentApplications.length === 0 ? (
            <p>У вас пока нет заявок.</p>
          ) : (
            <ul className="overview-list">
              {dashboard?.recentApplications.map((app) => (
                <li key={app.id} className="overview-item">
                  <strong>{app.tutorName}</strong>
                  <span>{app.subject}</span>
                  <span className={`overview-status overview-status--${app.statusTone}`}>{app.status}</span>
                </li>
              ))}
            </ul>
          )}
          <LinkButton to="/app/client/applications">Все заявки</LinkButton>
        </section>

        <section className="overview-card">
          <h2>Ближайшее занятие</h2>
          {dashboard?.nextLesson ? (
            <div className="overview-lesson">
              <strong>{new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(dashboard.nextLesson.startsAt))}</strong>
              <span>{dashboard.nextLesson.subject}</span>
              <span>{dashboard.nextLesson.tutorName} · {dashboard.nextLesson.format.toLowerCase()}</span>
            </div>
          ) : (
            <p>Ближайших занятий нет.</p>
          )}
          <LinkButton to="/app/client/schedule">Расписание</LinkButton>
        </section>

        <section className="overview-card">
          <h2>Последнее сообщение</h2>
          {dashboard?.recentConversation ? (
            <div className="overview-message">
              <strong>{dashboard.recentConversation.participantName}</strong>
              <p>{dashboard.recentConversation.preview}</p>
            </div>
          ) : (
            <p>Сообщений пока нет.</p>
          )}
          <LinkButton to="/app/client/messages">Все сообщения</LinkButton>
        </section>
          </>
        ) : null}
      </div>
    </div>
  );
}
