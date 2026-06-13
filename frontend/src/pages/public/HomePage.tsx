import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Badge, Button, FormField, LinkButton, TextInput, TutorCard } from '../../components/primitives';
import { getHomeContent, listSubjects, listTutors } from '../../shared/api/public';
import type { HomeContent, Subject, TutorSummary } from '../../shared/types';

const popularSearches = ['ЕГЭ', 'ОГЭ', 'Начальная школа', 'Разговорный английский', 'Онлайн сегодня'];

const workspaceRoutes = [
  {
    title: 'Для клиента',
    text: 'Найти преподавателя, отправить заявку, сохранить переписку и видеть расписание.',
    to: '/auth/sign-up/parent',
    action: 'Начать подбор',
    tone: 'client',
    items: ['Каталог', 'Заявки', 'Расписание'],
  },
  {
    title: 'Для репетитора',
    text: 'Принимать заявки, вести учеников, управлять профилем и подключать CRM-возможности.',
    to: '/auth/sign-up/tutor',
    action: 'Создать анкету',
    tone: 'tutor',
    items: ['Профиль', 'Календарь', 'CRM'],
  },
];

const trustItems = [
  {
    title: 'Без давления на выбор',
    text: 'Показываем цену, формат, отзывы и ближайшие окна, чтобы решение было спокойным.',
  },
  {
    title: 'Коммуникация в контексте',
    text: 'Заявки, сообщения и расписание не распадаются по разным каналам.',
  },
  {
    title: 'Честная модель оплаты',
    text: 'TutorSpace берет деньги за подписку и CRM-возможности, не выступая гарантом оплаты уроков.',
  },
];

const dashboardRows = [
  { label: 'Заявка', value: 'Анна, 9 класс', meta: 'математика, ЕГЭ' },
  { label: 'Окно', value: 'Сегодня 18:30', meta: 'пробный урок' },
  { label: 'Ответ', value: '15 минут', meta: 'среднее время' },
];

export function HomePage() {
  const navigate = useNavigate();
  const [content, setContent] = useState<HomeContent | null>(null);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [featuredTutors, setFeaturedTutors] = useState<TutorSummary[]>([]);
  const [query, setQuery] = useState('');
  const [format, setFormat] = useState('');
  const [budget, setBudget] = useState('');

  useEffect(() => {
    let alive = true;

    Promise.all([getHomeContent(), listSubjects(), listTutors({ minRating: 4.7 })]).then(([home, subjectList, tutorList]) => {
      if (!alive) {
        return;
      }

      setContent(home);
      setSubjects(subjectList);
      setFeaturedTutors(tutorList.filter((tutor) => tutor.featured).slice(0, 3));
    });

    return () => {
      alive = false;
    };
  }, []);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const params = new URLSearchParams();
    const normalizedBudget = budget.replace(/\D/g, '');

    if (query.trim()) {
      params.set('query', query.trim());
    }
    if (format.toLowerCase().includes('онлайн')) {
      params.set('online', '1');
    }
    if (normalizedBudget) {
      params.set('maxPrice', normalizedBudget);
    }

    navigate(`/catalog${params.toString() ? `?${params.toString()}` : ''}`);
  };

  const leadTutor = featuredTutors[0];

  return (
    <div className="home-page">
      <section className="home-hero" aria-labelledby="home-title">
        <div className="home-hero__copy">
          <Badge tone="emerald">TutorSpace marketplace + CRM</Badge>
          <h1 id="home-title">Подберите репетитора и ведите обучение без хаоса</h1>
          <p>
            Каталог помогает спокойно выбрать преподавателя, а личные кабинеты собирают заявки,
            сообщения, расписание и CRM-инструменты в одном месте.
          </p>

          <div className="home-hero__actions">
            <LinkButton to="/catalog" variant="primary">
              Смотреть каталог
            </LinkButton>
            <LinkButton to="/auth/sign-up/tutor" variant="ghost">
              Я репетитор
            </LinkButton>
          </div>
        </div>

        <aside className="home-hero__desk" aria-label="Превью рабочего процесса TutorSpace">
          <div className="home-desk-card home-desk-card--profile">
            <div className="home-desk-card__avatar">{leadTutor?.name.split(' ').map((part) => part[0]).join('').slice(0, 2) ?? 'TS'}</div>
            <div>
              <span>Подходит по цели</span>
              <strong>{leadTutor?.name ?? 'Анна Смирнова'}</strong>
              <p>{leadTutor?.subjects.slice(0, 2).join(' · ') ?? 'Математика · ЕГЭ'}</p>
            </div>
            <b>{leadTutor?.rating.toFixed(1) ?? '4.9'}</b>
          </div>

          <div className="home-desk-card home-desk-card--board">
            {dashboardRows.map((row) => (
              <div key={row.label} className="home-desk-row">
                <span>{row.label}</span>
                <strong>{row.value}</strong>
                <small>{row.meta}</small>
              </div>
            ))}
          </div>

          <div className="home-desk-card home-desk-card--message">
            <span>Сообщение репетитора</span>
            <p>Начнем с диагностики, затем соберем план на ближайшие четыре недели.</p>
          </div>
        </aside>

        <form className="home-search" onSubmit={handleSearch}>
          <FormField label="Что изучаем">
            <TextInput
              name="query"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Математика, английский, ЕГЭ"
            />
          </FormField>
          <FormField label="Формат">
            <TextInput name="format" value={format} onChange={(event) => setFormat(event.target.value)} placeholder="Онлайн или очно" />
          </FormField>
          <FormField label="Бюджет">
            <TextInput name="budget" value={budget} onChange={(event) => setBudget(event.target.value)} placeholder="до 1500 ₽/час" />
          </FormField>
          <Button type="submit">Найти</Button>

          <div className="home-search__tags" aria-label="Популярные запросы">
            {popularSearches.map((tag) => (
              <LinkButton key={tag} to={`/catalog?query=${encodeURIComponent(tag)}`} variant="ghost">
                {tag}
              </LinkButton>
            ))}
          </div>
        </form>
      </section>

      <section className="home-metrics" aria-label="Ключевые показатели">
        {content?.metrics.map((metric) => (
          <article key={metric.label} className={`home-metric home-metric--${metric.tone}`}>
            <span>{metric.label}</span>
            <strong>{metric.value}</strong>
          </article>
        ))}
      </section>

      <section className="home-workspaces" aria-labelledby="workspaces-title">
        <div className="home-section-heading">
          <span>Две рабочие зоны</span>
          <h2 id="workspaces-title">Сайт не заканчивается на заявке</h2>
          <p>После выбора начинается настоящая работа: договориться, не потерять контакт, вести расписание и видеть историю.</p>
        </div>

        <div className="home-workspace-grid">
          {workspaceRoutes.map((route) => (
            <article key={route.title} className={`home-workspace home-workspace--${route.tone}`}>
              <div>
                <h3>{route.title}</h3>
                <p>{route.text}</p>
              </div>
              <div className="home-workspace__items">
                {route.items.map((item) => (
                  <span key={item}>{item}</span>
                ))}
              </div>
              <LinkButton to={route.to} variant={route.tone === 'tutor' ? 'primary' : 'ghost'}>
                {route.action}
              </LinkButton>
            </article>
          ))}
        </div>
      </section>

      <section className="home-subjects" aria-labelledby="subjects-title">
        <div className="home-section-heading home-section-heading--row">
          <div>
            <span>Направления</span>
            <h2 id="subjects-title">Начните с предмета, а не с бесконечного списка</h2>
          </div>
          <LinkButton to="/catalog" variant="ghost">
            Все предметы
          </LinkButton>
        </div>

        <div className="home-subject-grid">
          {subjects.map((subject, index) => (
            <LinkButton key={subject.slug} to={`/catalog/subject/${subject.slug}`} variant="soft" className="home-subject">
              <span className="home-subject__index">{String(index + 1).padStart(2, '0')}</span>
              <span className="home-subject__swatch" style={{ background: subject.accent }} />
              <strong>{subject.title}</strong>
              <small>{subject.description}</small>
              <b>{subject.tutorCount == null ? 'Количество уточняется' : `${subject.tutorCount} репетиторов`}</b>
            </LinkButton>
          ))}
        </div>
      </section>

      <section className="home-featured" aria-labelledby="featured-title">
        <div className="home-section-heading home-section-heading--row">
          <div>
            <span>Рекомендации</span>
            <h2 id="featured-title">Преподаватели, с которых можно начать</h2>
          </div>
          <LinkButton to="/catalog" variant="ghost">
            Открыть каталог
          </LinkButton>
        </div>

        <div className="home-featured__layout">
          <div className="home-featured__cards">
            {featuredTutors.map((tutor) => (
              <TutorCard key={tutor.id} tutor={tutor} />
            ))}
          </div>

          <aside className="home-insight-card">
            <span>Как выбирать</span>
            <h3>Смотрите не только рейтинг</h3>
            <p>Важнее совпадение по цели, формату, расписанию и стилю обратной связи.</p>
            <ul>
              <li>Проверьте ближайшие окна.</li>
              <li>Сравните цену за 60 минут.</li>
              <li>Оставьте заявку из профиля.</li>
            </ul>
          </aside>
        </div>
      </section>

      <section className="home-flow" aria-labelledby="flow-title">
        <div className="home-section-heading">
          <span>Маршрут</span>
          <h2 id="flow-title">Понятный путь без лишних экранов</h2>
        </div>

        <div className="home-flow__grid">
          {content?.process.map((step, index) => (
            <article key={step.title} className="home-flow-step">
              <span>{String(index + 1).padStart(2, '0')}</span>
              <h3>{step.title}</h3>
              <p>{step.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="home-trust" aria-labelledby="trust-title">
        <div className="home-section-heading">
          <span>Доверие</span>
          <h2 id="trust-title">Спокойный интерфейс для серьезных договоренностей</h2>
        </div>

        <div className="home-trust__grid">
          {trustItems.map((item) => (
            <article key={item.title} className="home-trust-card">
              <h3>{item.title}</h3>
              <p>{item.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="home-final">
        <div>
          <span>Готовы начать</span>
          <h2>Выберите преподавателя сейчас, а организацию обучения оставьте TutorSpace</h2>
        </div>
        <div className="home-final__actions">
          <LinkButton to="/catalog" variant="primary">
            Найти репетитора
          </LinkButton>
          <LinkButton to="/auth/sign-up/tutor" variant="ghost">
            Подключить CRM
          </LinkButton>
        </div>
      </section>
    </div>
  );
}
