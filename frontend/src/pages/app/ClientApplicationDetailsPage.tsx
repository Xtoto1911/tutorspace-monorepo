import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Button, LinkButton } from '../../components/primitives';
import { getClientApplicationDetails } from '../../shared/api/client';
import type { ClientApplicationDetailsData } from '../../shared/types';

const checklist = [
  { label: 'Проверьте интернет', checked: true },
  { label: 'Подготовьте тетрадь', checked: true },
  { label: 'Добавьте вопросы ученика', checked: false },
];

export function ClientApplicationDetailsPage() {
  const { applicationId } = useParams();
  const [details, setDetails] = useState<ClientApplicationDetailsData | null>(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const timeline = useMemo(() => {
    if (!details) {
      return [];
    }
    const isWaiting = details.statusTone === 'amber';
    const isConfirmed = details.statusTone === 'green';
    const isScheduled = details.statusTone === 'indigo';

    return [
      {
        step: '1',
        title: 'Заявка создана',
        text: 'Вы выбрали цель, класс ученика и удобный формат.',
        active: false,
      },
      {
        step: '2',
        title: isWaiting ? 'Ожидает ответа репетитора' : 'Репетитор подтвердил',
        text: isWaiting
          ? 'Заявка отправлена. Ожидайте подтверждения от репетитора.'
          : 'Заявка принята и готова к переводу в расписание.',
        active: isWaiting || isConfirmed,
      },
      {
        step: '3',
        title: 'Ожидается занятие',
        text: 'После создания урока время и ссылка появятся в расписании.',
        active: isScheduled,
      },
    ];
  }, [details?.statusTone]);

  const loadDetails = () => {
    let ignore = false;

    if (!applicationId) {
      setIsLoading(false);
      setError('Не указан идентификатор заявки.');
      return () => {
        ignore = true;
      };
    }

    setIsLoading(true);
    setError('');
    getClientApplicationDetails(applicationId)
      .then((data) => {
        if (!ignore) {
          setDetails(data);
        }
      })
      .catch(() => {
        if (!ignore) {
          setDetails(null);
          setError('Не удалось загрузить заявку.');
        }
      })
      .finally(() => {
        if (!ignore) {
          setIsLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  };

  useEffect(() => {
    return loadDetails();
  }, [applicationId]);

  if (isLoading) {
    return <div className="application-details">Загрузка заявки...</div>;
  }

  if (error || !details) {
    return (
      <div className="application-details">
        <p role="alert">{error || 'Заявка недоступна.'}</p>
        <Button type="button" onClick={loadDetails}>Повторить</Button>
      </div>
    );
  }

  return (
    <div className="application-details">
      <nav className="application-details__breadcrumbs" aria-label="Хлебные крошки">
        <LinkButton to="/app/client/applications" variant="ghost">
          Мои заявки
        </LinkButton>
        <span>/</span>
        <strong>Заявка №{details.id}</strong>
      </nav>

      <header className="application-details__header">
        <div>
          <h1>Детали заявки</h1>
          <p>{details.subject} · пробное занятие с {details.tutorName}</p>
        </div>
        <div className="application-details__actions">
          <LinkButton to="/app/client/applications" variant="ghost">
            Назад к списку
          </LinkButton>
          <LinkButton to="/app/client/schedule" variant="primary">
            Перейти к уроку
          </LinkButton>
        </div>
      </header>

      <section className="application-summary-card" aria-label="Краткая информация о заявке">
        <div className="application-summary-card__avatar">
          {details.tutorName
            .split(' ')
            .map((part) => part[0])
            .join('')
            .slice(0, 2)}
        </div>
        <div className="application-summary-card__main">
          <div className="application-summary-card__top">
            <h2>{details.tutorName}</h2>
            <span className={`application-status application-status--${details.statusTone}`}>{details.status}</span>
          </div>
          <div className="application-summary-card__chips">
            {details.chips.map((chip) => (
              <span key={chip}>{chip}</span>
            ))}
          </div>
          <p>{details.note}</p>
        </div>
        <div className="application-summary-card__price">
          <strong>{details.price}</strong>
          <span>за урок</span>
        </div>
      </section>

      <div className="application-details__grid">
        <main className="application-details__main">
          <section className="details-card">
            <h2>Параметры занятия</h2>
            <dl className="lesson-parameters">
              {details.parameters.map((item) => (
                <div key={item.label}>
                  <dt>{item.label}</dt>
                  <dd>{item.value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="details-card">
            <h2>История заявки</h2>
            <ol className="application-timeline">
              {timeline.map((item) => (
                <li key={item.step} className={item.active ? 'is-active' : undefined}>
                  <span>{item.step}</span>
                  <div>
                    <strong>{item.title}</strong>
                    <p>{item.text}</p>
                  </div>
                </li>
              ))}
            </ol>
          </section>

          <section className="details-card">
            <div className="details-card__title-row">
              <h2>Переписка</h2>
              <LinkButton to="/app/client/messages" variant="ghost">
                Открыть чат
              </LinkButton>
            </div>
            <div className="message-preview">
              <article>
                <header>
                  <strong>{details.tutorName}</strong>
                  <time>14:20</time>
                </header>
                <p>Здравствуйте! Подтверждаю заявку и готов обсудить первое занятие в удобное для вас время.</p>
              </article>
              <article className="message-preview__own">
                <header>
                  <strong>Вы</strong>
                  <time>14:28</time>
                </header>
                <p>Отлично, давайте начнем с диагностики и короткого плана на ближайшие недели.</p>
              </article>
            </div>
          </section>
        </main>

        <aside className="application-details__aside" aria-label="Информация по уроку">
          <section className="details-side-card">
            <h2>Ближайшее занятие</h2>
            <span className="client-side-card__pill">{details.chips[0] ?? 'Скоро появится'}</span>
            <p>Онлайн-комната откроется за 30 минут до начала урока.</p>
            <LinkButton to="/app/client/schedule" variant="primary">
              Перейти к уроку
            </LinkButton>
            <LinkButton to="/app/client/schedule" variant="primary">
              Перенести время
            </LinkButton>
          </section>

          <section className="details-side-card">
            <h2>Перед уроком</h2>
            <ul className="lesson-checklist">
              {checklist.map((item) => (
                <li key={item.label} className={item.checked ? 'is-checked' : undefined}>
                  <span>{item.checked ? '✓' : '○'}</span>
                  {item.label}
                </li>
              ))}
            </ul>
          </section>

          <section className="details-side-card tutor-mini-profile">
            <div className="tutor-mini-profile__top">
              <span>
                {details.tutorName
                  .split(' ')
                  .map((part) => part[0])
                  .join('')
                  .slice(0, 2)}
              </span>
              <div>
                <strong>{details.tutorName}</strong>
                <p>Профиль в каталоге TutorSpace</p>
              </div>
            </div>
            <LinkButton to={`/tutors/${details.tutorSlug}`} variant="primary">
              Профиль репетитора
            </LinkButton>
          </section>
        </aside>
      </div>
    </div>
  );
}
