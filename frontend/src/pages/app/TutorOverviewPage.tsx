import { useCallback, useEffect, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { getTutorDashboard } from '../../shared/api/tutor';
import type { TutorOverviewPageData } from '../../shared/types';

export function TutorOverviewPage() {
  const [dashboard, setDashboard] = useState<TutorOverviewPageData | null>(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const loadDashboard = useCallback(() => {
    let ignore = false;
    setIsLoading(true);
    setError('');
    getTutorDashboard()
      .then((data) => {
        if (!ignore) {
          setDashboard(data);
        }
      })
      .catch((error) => {
        if (!ignore) {
          const message = error instanceof Error ? error.message : 'Не удалось загрузить обзор. Проверьте подключение и повторите попытку.';
          console.error('Tutor dashboard load failed:', error);
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
    <div className="tutor-overview">
      <header className="tutor-overview__header">
        <h1>Обзор</h1>
        <p>Сводка по заявкам, календарю и ученикам.</p>
      </header>

      <div className="tutor-overview__grid">
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
          <h2>Новые заявки</h2>
          <div className="overview-metric">
            <strong>{dashboard?.newRequests ?? 0}</strong>
            <span>нужно ответить</span>
          </div>
          <LinkButton to="/app/tutor/requests">Перейти к заявкам</LinkButton>
        </section>

        <section className="overview-card">
          <h2>Следующий урок</h2>
          {dashboard?.nextLesson ? (
            <div className="overview-lesson">
              <strong>{new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(dashboard.nextLesson.startsAt))}</strong>
              <span>{dashboard.nextLesson.subject}</span>
              <span>{dashboard.nextLesson.studentName} · {dashboard.nextLesson.format.toLowerCase()}</span>
            </div>
          ) : (
            <p>Ближайших уроков нет.</p>
          )}
          <LinkButton to="/app/tutor/calendar">Календарь</LinkButton>
        </section>

        <section className="overview-card">
          <h2>Ученики</h2>
          <div className="overview-metric">
            <strong>{dashboard?.activeStudents ?? 0}</strong>
            <span>активных учеников</span>
          </div>
          <LinkButton to="/app/tutor/students">Список учеников</LinkButton>
        </section>
          </>
        ) : null}
      </div>
    </div>
  );
}
