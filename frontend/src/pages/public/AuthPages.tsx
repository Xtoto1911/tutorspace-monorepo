import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Badge, Button, EmptyState, FormField, LinkButton, TextArea, TextInput } from '../../components/primitives';
import { completePasswordReset, listSubjects, registerParent, registerTutor, requestPasswordReset, signIn } from '../../shared/api/public';
import { getAppRouteForRole, writeAuthSession } from '../../shared/auth/session';

function SubjectPicker({
  availableSubjects,
  selectedSubjects,
  onToggleSubject,
}: {
  availableSubjects: string[];
  selectedSubjects: string[];
  onToggleSubject: (subject: string) => void;
}) {
  return (
    <div className="subject-picker">
      <div className="subject-picker__options" aria-label="Доступные предметы">
        {availableSubjects.map((subject) => {
          const selected = selectedSubjects.includes(subject);

          return (
            <button
              key={subject}
              type="button"
              className={selected ? 'is-selected' : undefined}
              aria-pressed={selected}
              onClick={() => onToggleSubject(subject)}
            >
              <span aria-hidden="true">{selected ? '✓' : '+'}</span>
              {subject}
            </button>
          );
        })}
      </div>
      <div className="subject-picker__selected" aria-live="polite">
        {selectedSubjects.length > 0 ? (
          selectedSubjects.map((subject) => (
            <button key={subject} type="button" onClick={() => onToggleSubject(subject)} aria-label={`Убрать ${subject}`}>
              {subject}
              <span aria-hidden="true">×</span>
            </button>
          ))
        ) : (
          <span>Выберите хотя бы один предмет</span>
        )}
      </div>
    </div>
  );
}

function AuthShell({
  eyebrow,
  title,
  description,
  asideTitle,
  asideText,
  asideItems,
  backLinkTo,
  backLinkLabel,
  children,
}: {
  eyebrow: string;
  title: string;
  description: string;
  asideTitle: string;
  asideText: string;
  asideItems: string[];
  backLinkTo?: string;
  backLinkLabel?: string;
  children: ReactNode;
}) {
  return (
    <section className="auth-screen">
      <aside className="auth-screen__aside">
        <Badge tone="indigo">{eyebrow}</Badge>
        <h1>{asideTitle}</h1>
        <p>{asideText}</p>
        <div className="auth-screen__mini-flow" aria-hidden="true">
          <span>Аккаунт</span>
          <span>Роль</span>
          <span>Кабинет</span>
        </div>
        <div className="auth-screen__summary">
          {asideItems.map((item) => (
            <span key={item}>{item}</span>
          ))}
        </div>
      </aside>

      <div className="auth-screen__panel">
        {backLinkTo ? (
          <div className="auth-screen__back-row">
            <LinkButton to={backLinkTo} variant="ghost" className="auth-screen__back">
              <span aria-hidden="true">←</span>
              <span>{backLinkLabel ?? 'Назад'}</span>
            </LinkButton>
          </div>
        ) : null}
        <div className="auth-screen__heading">
          <p className="section-eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
        {children}
      </div>
    </section>
  );
}

export function SignInPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('');
  const [form, setForm] = useState({ email: '', password: '' });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    try {
      const response = await signIn(form);

      writeAuthSession(response);
      setStatus(`Вход выполнен как ${response.user.role}.`);
      navigate(getAppRouteForRole(response.user.role));
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Не удалось войти. Проверьте данные.');
    }
  };

  return (
    <AuthShell
      eyebrow="Вход"
      title="Войти в аккаунт"
      description="Используйте почту и пароль, чтобы перейти в свой кабинет."
      asideTitle="Все заявки и занятия в одном месте"
      asideText="После входа вы увидите расписание, сообщения, статусы заявок и сохраненных преподавателей."
      asideItems={['Расписание уроков', 'Переписка с репетиторами', 'Статусы заявок']}
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <FormField label="Email">
          <TextInput
            name="email"
            type="email"
            value={form.email}
            onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
            placeholder="mail@example.com"
            required
          />
        </FormField>
        <FormField label="Пароль">
          <TextInput
            name="password"
            type="password"
            value={form.password}
            onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
            placeholder="Введите пароль"
            minLength={6}
            required
          />
        </FormField>
        <Button type="submit">Войти</Button>
      </form>

      <div className="auth-card__footer">
        <LinkButton to="/auth/forgot-password" variant="ghost">
          Забыли пароль?
        </LinkButton>
        <LinkButton to="/auth/sign-up" variant="ghost">
          Создать аккаунт
        </LinkButton>
      </div>

      {status ? <div className="success-banner">{status}</div> : null}
    </AuthShell>
  );
}

export function SignUpChoicePage() {
  return (
    <AuthShell
      eyebrow="Регистрация"
      title="Выберите роль"
      description="Форма дальше адаптируется под клиента, ученика или репетитора."
      asideTitle="Один аккаунт для обучения и работы"
      asideText="TutorSpace разводит сценарии по ролям, но сохраняет общий стиль кабинета и понятный маршрут после регистрации."
      asideItems={['Заявки и статусы', 'Профиль репетитора', 'Календарь занятий']}
    >
      <div className="auth-role-list">
        <Link to="/auth/sign-up/parent" className="auth-role-card">
          <Badge tone="indigo">Клиент / ученик</Badge>
          <strong>Найти репетитора и вести обучение</strong>
          <span>Заявки, расписание, переписка и история занятий в личном кабинете.</span>
        </Link>
        <Link to="/auth/sign-up/tutor" className="auth-role-card auth-role-card--accent">
          <Badge tone="emerald">Репетитор</Badge>
          <strong>Заполнить профиль и принимать заявки</strong>
          <span>Анкета, предметы, тарифы и будущая аналитика по ученикам.</span>
        </Link>
      </div>

      <div className="auth-card__footer">
        <LinkButton to="/auth/sign-in" variant="ghost">
          Уже есть аккаунт
        </LinkButton>
      </div>
    </AuthShell>
  );
}

export function ParentRegistrationPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('');
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
  });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    try {
      const response = await registerParent(form);

      writeAuthSession(response);
      setStatus('Аккаунт создан. Теперь можно выбрать репетитора и отправить заявку.');
      navigate(getAppRouteForRole(response.user.role));
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Не удалось создать аккаунт.');
    }
  };

  return (
    <AuthShell
      eyebrow="Регистрация"
      title="Аккаунт клиента"
      description="Создайте общий клиентский аккаунт для поиска репетиторов, заявок и расписания."
      asideTitle="Быстрый старт для поиска преподавателя"
      asideText="После регистрации вы сможете отправлять заявки, хранить избранных репетиторов и видеть расписание уроков."
      asideItems={['Общий клиентский профиль', 'Избранные репетиторы', 'Заявки из профиля']}
      backLinkTo="/auth/sign-up"
      backLinkLabel="Выбор роли"
    >
      <form className="auth-form auth-form--grid" onSubmit={handleSubmit}>
        <FormField label="Имя">
          <TextInput name="name" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} required />
        </FormField>
        <FormField label="Email">
          <TextInput
            name="email"
            type="email"
            value={form.email}
            onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
            required
          />
        </FormField>
        <FormField label="Пароль">
          <TextInput
            name="password"
            type="password"
            value={form.password}
            onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
            minLength={6}
            required
          />
        </FormField>
        <Button type="submit">Создать аккаунт</Button>
      </form>

      {status ? <div className="success-banner">{status}</div> : null}
    </AuthShell>
  );
}

export function TutorRegistrationPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('');
  const [availableSubjects, setAvailableSubjects] = useState<string[]>([]);
  const [subjectsLoading, setSubjectsLoading] = useState(true);
  const [subjectsError, setSubjectsError] = useState('');
  const [subjectsLoadKey, setSubjectsLoadKey] = useState(0);
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    subjects: [] as string[],
    experienceYears: '5',
    city: 'Москва',
    about: '',
  });

  useEffect(() => {
    let active = true;
    setSubjectsLoading(true);
    setSubjectsError('');

    listSubjects()
      .then((subjects) => {
        if (active) {
          setAvailableSubjects(subjects.map((subject) => subject.title).filter(Boolean));
        }
      })
      .catch((error) => {
        if (active) {
          setAvailableSubjects([]);
          setSubjectsError(error instanceof Error ? error.message : 'Не удалось загрузить предметы.');
        }
      })
      .finally(() => {
        if (active) {
          setSubjectsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [subjectsLoadKey]);

  const handleSubjectToggle = (subject: string) => {
    setForm((current) => {
      const subjects = current.subjects.includes(subject)
        ? current.subjects.filter((item) => item !== subject)
        : [...current.subjects, subject];

      return { ...current, subjects };
    });
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (form.subjects.length === 0) {
      setStatus('Выберите хотя бы один предмет из списка.');
      return;
    }

    try {
      const response = await registerTutor(form);

      writeAuthSession(response);
      setStatus('Профиль создан. Дальше можно перейти к заполнению кабинета и модерации.');
      navigate('/app/tutor/profile?welcome=1');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Не удалось создать профиль.');
    }
  };

  return (
    <AuthShell
      eyebrow="Для репетиторов"
      title="Регистрация репетитора"
      description="Заполните основные данные, выберите предметы и кратко опишите подход."
      asideTitle="Профиль, заявки и расписание в одном кабинете"
      asideText="Макет из Figma ведет репетитора через компактную форму и сразу показывает, что будет в личном разделе."
      asideItems={['Анкета и предметы', 'Проверка профиля', 'Календарь и заявки']}
      backLinkTo="/auth/sign-up"
      backLinkLabel="Выбор роли"
    >
      <form className="auth-form auth-form--grid" onSubmit={handleSubmit}>
        <FormField label="ФИО">
          <TextInput name="name" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} required />
        </FormField>
        <FormField label="Email">
          <TextInput
            name="email"
            type="email"
            value={form.email}
            onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
            required
          />
        </FormField>
        <FormField label="Пароль">
          <TextInput
            name="password"
            type="password"
            value={form.password}
            onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
            minLength={6}
            required
          />
        </FormField>
        <div className="form-field form-field--wide">
          <span className="form-label">Преподаваемые предметы</span>
          {subjectsLoading ? <div className="loading-panel">Загружаем предметы...</div> : null}
          {!subjectsLoading && subjectsError ? (
            <EmptyState
              title="Предметы временно недоступны"
              text={subjectsError}
              action={<Button type="button" variant="secondary" onClick={() => setSubjectsLoadKey((value) => value + 1)}>Повторить</Button>}
            />
          ) : null}
          {!subjectsLoading && !subjectsError && availableSubjects.length === 0 ? (
            <EmptyState
              title="Список предметов пока пуст"
              text="Регистрация репетитора станет доступна после добавления предметов в каталог."
            />
          ) : null}
          {!subjectsLoading && !subjectsError && availableSubjects.length > 0 ? (
            <SubjectPicker
              availableSubjects={availableSubjects}
              selectedSubjects={form.subjects}
              onToggleSubject={handleSubjectToggle}
            />
          ) : null}
          <span className="form-hint">Нажмите на предмет, чтобы добавить или убрать его из итогового списка.</span>
        </div>
        <FormField label="Опыт, лет">
          <TextInput
            name="experienceYears"
            type="number"
            value={form.experienceYears}
            onChange={(event) => setForm((current) => ({ ...current, experienceYears: event.target.value }))}
            min="0"
          />
        </FormField>
        <FormField label="Город">
          <TextInput name="city" value={form.city} onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))} />
        </FormField>
        <FormField label="О себе">
          <TextArea name="about" rows={4} value={form.about} onChange={(event) => setForm((current) => ({ ...current, about: event.target.value }))} />
        </FormField>
        <Button type="submit" disabled={subjectsLoading || Boolean(subjectsError) || availableSubjects.length === 0}>
          Создать профиль
        </Button>
      </form>

      {status ? <div className="success-banner">{status}</div> : null}
    </AuthShell>
  );
}

export function ForgotPasswordPage() {
  const [status, setStatus] = useState('');
  const [statusTone, setStatusTone] = useState<'success' | 'error'>('success');
  const [submitting, setSubmitting] = useState(false);
  const [email, setEmail] = useState('');

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const response = await requestPasswordReset(email);
      setStatusTone('success');
      setStatus(response.message);
    } catch (error) {
      setStatusTone('error');
      setStatus(error instanceof Error ? error.message : 'Не удалось отправить ссылку. Попробуйте ещё раз.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthShell
      eyebrow="Восстановление"
      title="Сбросить пароль"
      description="Отправим письмо на почту и оставим маршрут для reset-flow."
      asideTitle="Верните доступ без обращения в поддержку"
      asideText="Введите почту аккаунта, а мы отправим ссылку для создания нового пароля."
      asideItems={['Письмо на email', 'Новый пароль', 'Возврат в кабинет']}
      backLinkTo="/auth/sign-in"
      backLinkLabel="Войти"
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <FormField label="Email">
          <TextInput name="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        </FormField>
        <Button type="submit" disabled={submitting}>{submitting ? 'Отправляем...' : 'Отправить ссылку'}</Button>
      </form>
      {status ? <div className={statusTone === 'error' ? 'error-banner' : 'success-banner'}>{status}</div> : null}
    </AuthShell>
  );
}

export function ResetPasswordPage() {
  const { token } = useParams();
  const [status, setStatus] = useState('');
  const [statusTone, setStatusTone] = useState<'success' | 'error'>('success');
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ password: '', confirmPassword: '' });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) {
      setStatusTone('error');
      setStatus('Ссылка восстановления не содержит токен.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setStatusTone('error');
      setStatus('Пароли не совпадают.');
      return;
    }

    setSubmitting(true);
    try {
      const response = await completePasswordReset(token, form.password);
      setStatusTone('success');
      setStatus(response.message);
    } catch (error) {
      setStatusTone('error');
      setStatus(error instanceof Error ? error.message : 'Не удалось обновить пароль.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthShell
      eyebrow="Восстановление"
      title="Создать новый пароль"
      description={token ? 'Введите новый пароль для завершения восстановления.' : 'Ссылка восстановления повреждена или неполна.'}
      asideTitle="Последний шаг восстановления"
      asideText="После обновления пароля вы сможете войти и продолжить работу в своем кабинете."
      asideItems={['Проверка токена', 'Новый пароль', 'Вход в TutorSpace']}
      backLinkTo="/auth/sign-in"
      backLinkLabel="Войти"
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <FormField label="Новый пароль">
          <TextInput
            name="password"
            type="password"
            value={form.password}
            onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
            minLength={8}
            required
          />
        </FormField>
        <FormField label="Повторите пароль">
          <TextInput
            name="confirmPassword"
            type="password"
            value={form.confirmPassword}
            onChange={(event) => setForm((current) => ({ ...current, confirmPassword: event.target.value }))}
            minLength={8}
            required
          />
        </FormField>
        <Button type="submit" disabled={submitting || !token}>{submitting ? 'Обновляем...' : 'Обновить пароль'}</Button>
      </form>
      {status ? <div className={statusTone === 'error' ? 'error-banner' : 'success-banner'}>{status}</div> : null}
    </AuthShell>
  );
}
