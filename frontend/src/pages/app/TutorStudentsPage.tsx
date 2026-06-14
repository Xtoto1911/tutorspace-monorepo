import { useEffect, useMemo, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { useDialog } from '../../components/dialog';
import { createTutorStudent, getTutorStudents } from '../../shared/api/tutor';
import { cn } from '../../shared/lib/cn';
import type { TutorStudentsPageData } from '../../shared/types';

const tabs = ['Активные', 'Пробные', 'На паузе', 'Архив'];

function getProgressTone(progress: number) {
  if (progress >= 75) return 'indigo';
  if (progress >= 50) return 'emerald';
  return 'amber';
}

export function TutorStudentsPage() {
  const dialog = useDialog();
  const [pageData, setPageData] = useState<TutorStudentsPageData | null>(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const loadStudents = async () => {
    setIsLoading(true);
    setError('');
    try {
      setPageData(await getTutorStudents());
    } catch {
      setPageData(null);
      setError('Не удалось загрузить учеников.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadStudents();
  }, []);

  const filteredStudents = useMemo(() => {
    const students = pageData?.students ?? [];
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return students;
    }
    return students.filter((student) =>
      student.name.toLowerCase().includes(query)
      || student.subject.toLowerCase().includes(query)
      || student.details.toLowerCase().includes(query)
      || student.note.toLowerCase().includes(query),
    );
  }, [pageData?.students, searchQuery]);

  const handleCreateStudent = async () => {
    const name = (await dialog.prompt('Имя ученика', { title: 'Новый ученик' }))?.trim() ?? '';
    if (!name) return;
    const details = (await dialog.prompt('Класс и цель обучения', { title: 'Новый ученик' }))?.trim() ?? '';
    if (!details) return;
    const subject = (await dialog.prompt('Предмет', { title: 'Новый ученик' }))?.trim() ?? '';
    if (!subject) return;
    const note = (await dialog.prompt('Заметка (необязательно)', { title: 'Новый ученик' })) ?? '';

    setIsMutating(true);
    setError('');
    try {
      await createTutorStudent({ name, details, subject, note });
      await loadStudents();
    } catch {
      setError('Не удалось добавить ученика.');
    } finally {
      setIsMutating(false);
    }
  };

  if (isLoading) {
    return <div className="tutor-students-page">Загрузка учеников...</div>;
  }

  if (error || !pageData) {
    return (
      <div className="tutor-students-page">
        <p role="alert">{error || 'Ученики недоступны.'}</p>
        <Button type="button" onClick={() => void loadStudents()}>Повторить</Button>
      </div>
    );
  }

  if (pageData.students.length === 0) {
    return (
      <div className="tutor-students-page">
        <h1>Мои ученики</h1>
        <p>Учеников пока нет. Добавьте ученика вручную или примите заявку.</p>
        <Button type="button" disabled={isMutating} onClick={() => void handleCreateStudent()}>
          Добавить ученика
        </Button>
      </div>
    );
  }

  const selected = pageData.selectedStudent;
  const selectedStudentId = pageData.students.find((student) => student.highlighted)?.id ?? pageData.students[0].id;

  return (
    <div className="tutor-students-page">
      <header className="tutor-students-page__header">
        <div>
          <p className="tutor-students-page__eyebrow">Кабинет репетитора</p>
          <h1>Мои ученики</h1>
          <p>Отслеживайте прогресс, домашние задания, оплату и историю занятий по каждому ученику.</p>
        </div>

        <Button disabled={isMutating} onClick={() => void handleCreateStudent()}>
          {isMutating ? 'Сохраняем...' : 'Добавить ученика'}
        </Button>
      </header>

      <section className="tutor-student-stats" aria-label="Сводка учеников">
        {error ? <p role="alert">{error}</p> : null}
        {pageData.stats.map((stat) => (
          <article key={stat.label} className={cn('tutor-student-stat', `tutor-student-stat--${stat.tone}`)}>
            <span className="tutor-student-stat__icon">{stat.icon}</span>
            <div>
              <strong>{stat.label}</strong>
              <p>{stat.sublabel}</p>
            </div>
            <b>{stat.value}</b>
          </article>
        ))}
      </section>

      <section className="tutor-student-toolbar" aria-label="Фильтр учеников">
        <div className="tutor-student-tabs">
          {tabs.map((tab, index) => (
            <button key={tab} className={index === 0 ? 'is-active' : undefined} type="button">
              {tab}
            </button>
          ))}
        </div>

        <label className="tutor-student-search">
          <span className="sr-only">Поиск ученика</span>
          <input
            id="tutor-student-search"
            name="tutor-student-search"
            placeholder="Поиск ученика"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
          />
          {searchQuery ? (
            <button
              type="button"
              className="tutor-student-search__clear"
              onClick={() => setSearchQuery('')}
              aria-label="Очистить поиск"
            >
              ×
            </button>
          ) : null}
        </label>

        <Button>Фильтры</Button>
      </section>

      <div className="tutor-students-layout">
        <section className="tutor-student-list" aria-label="Список учеников">
          {filteredStudents.length === 0 && searchQuery ? (
            <div className="tutor-student-empty">
              <strong>Учеников не найдено</strong>
              <p>Попробуйте изменить запрос или очистить поиск.</p>
              <Button variant="ghost" onClick={() => setSearchQuery('')}>
                Очистить поиск
              </Button>
            </div>
          ) : null}
          {filteredStudents.map((student) => {
            const progressTone = getProgressTone(student.progress);
            return (
              <article
                key={student.id}
                className={cn(
                  'tutor-student-card',
                  `tutor-student-card--${student.tone}`,
                  student.highlighted && 'is-highlighted',
                )}
              >
                <div className={cn('tutor-student-card__avatar', `tutor-student-card__avatar--${student.tone}`)}>
                  {student.initials}
                </div>

                <div className="tutor-student-card__main">
                  <div className="tutor-student-card__top">
                    <div>
                      <h2>{student.name}</h2>
                      <p>{student.details}</p>
                    </div>
                    <div className="tutor-student-card__badges">
                      <span>{student.subject}</span>
                      <strong>{student.status}</strong>
                    </div>
                  </div>

                  <p className="tutor-student-card__note">{student.note}</p>

                  <div className="tutor-student-card__footer">
                    <div className="tutor-progress">
                      <span>
                        <i style={{ width: `${student.progress}%` }} />
                      </span>
                      <strong className={`tutor-progress__value tutor-progress__value--${progressTone}`}>
                        {student.progress}% прогресса
                      </strong>
                    </div>

                    <div className="tutor-student-card__actions">
                      <Button variant="ghost">Открыть</Button>
                      <LinkButton to="/app/tutor/messages">Чат</LinkButton>
                    </div>
                  </div>
                </div>
              </article>
            );
          })}
        </section>

        <aside className="tutor-student-profile-card" aria-label="Карточка выбранного ученика">
          <div className="tutor-student-profile-card__identity">
            <span>{selected.initials}</span>
            <div>
              <h2>{selected.name}</h2>
              <p>{selected.details}</p>
            </div>
          </div>

          <div className="tutor-student-profile-card__badges">
            {selected.badges.map((badge, index) => (
              <span key={badge} className={index === 0 ? 'is-indigo' : 'is-emerald'}>
                {badge}
              </span>
            ))}
          </div>

          <h3>Ближайшие задачи</h3>
          <div className="tutor-student-task-list">
            {selected.tasks.map((task) => (
              <article key={task.title} className="tutor-student-task">
                <span className={`tutor-student-task__line tutor-student-task__line--${task.tone}`} />
                <div>
                  <strong>{task.title}</strong>
                  <p>{task.meta}</p>
                </div>
              </article>
            ))}
          </div>

          <div className="tutor-student-profile-card__progress">
            <span>
              <i style={{ width: `${selected.progress}%` }} />
            </span>
            <strong>{selected.progress}% учебного плана выполнено</strong>
          </div>

          <Button variant="ghost">Открыть профиль</Button>
          <LinkButton to="/app/tutor/messages" variant="ghost">
            Написать родителю
          </LinkButton>
        </aside>
      </div>

      <section className="tutor-student-note" aria-label="Подсказка по CRM учеников">
        <strong>Подсказка</strong>
        <p>Карточка ученика объединяет историю занятий, ДЗ, оплату и коммуникацию с родителями.</p>
        <Button>Настроить CRM</Button>
      </section>
    </div>
  );
}
