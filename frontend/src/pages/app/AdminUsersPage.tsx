import { useEffect, useState, type FormEvent } from 'react';
import { Button, FormField, LinkButton, TextInput } from '../../components/primitives';
import { createModeratorAccount, getAdminUsersPage, updateAdminUser } from '../../shared/api/admin';
import { cn } from '../../shared/lib/cn';
import type { AdminUsersPageData } from '../../shared/types';
import { emptyAdminUsersPage, filterAdminUsers, getReadableError } from './admin-live-helpers';
import { getDefaultAdminUserId, toAdminUserDetail } from './admin-users-helpers';

export function AdminUsersPage() {
  const [page, setPage] = useState<AdminUsersPageData>(emptyAdminUsersPage);
  const [selectedUserId, setSelectedUserId] = useState('');
  const [activeTab, setActiveTab] = useState('Все');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionStatus, setActionStatus] = useState('');
  const [moderatorEmail, setModeratorEmail] = useState('');
  const [moderatorPassword, setModeratorPassword] = useState('');
  const [moderatorStatus, setModeratorStatus] = useState('');
  const selectedUser = page.users.find((user) => user.id === selectedUserId) ?? page.users[0];
  const selected = toAdminUserDetail(selectedUser, page.selected);
  const filteredUsers = filterAdminUsers(page.users, activeTab, searchQuery);

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getAdminUsersPage()
      .then((data) => {
        if (!ignore) {
          setPage(data);
          setSelectedUserId((current) => (data.users.some((user) => user.id === current) ? current : getDefaultAdminUserId(data.users)));
          setError('');
        }
      })
      .catch((error) => {
        if (!ignore) {
          setPage(emptyAdminUsersPage);
          setSelectedUserId('');
          setError(getReadableError(error, 'Не удалось загрузить пользователей.'));
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, []);

  const reloadUsers = async () => {
    const data = await getAdminUsersPage();
    setPage(data);
    setSelectedUserId((current) => (data.users.some((user) => user.id === current) ? current : getDefaultAdminUserId(data.users)));
    setError('');
  };

  const handleUserAction = async (action: string) => {
    if (!selectedUser) {
      return;
    }

    setActionStatus('Сохраняем изменение...');
    try {
      await updateAdminUser(selectedUser.id, action);
      await reloadUsers();
      setActionStatus('Изменение сохранено.');
    } catch (error) {
      setActionStatus(error instanceof Error ? error.message : 'Не удалось сохранить изменение.');
    }
  };

  const handleCreateModerator = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setModeratorStatus('Создаем аккаунт модератора...');
    try {
      await createModeratorAccount({
        email: moderatorEmail.trim(),
        password: moderatorPassword,
      });
      await reloadUsers();
      setModeratorEmail('');
      setModeratorPassword('');
      setModeratorStatus('Модератор создан.');
    } catch (error) {
      setModeratorStatus(error instanceof Error ? error.message : 'Не удалось создать модератора.');
    }
  };

  return (
    <div className="admin-users">
      <header className="admin-users__header">
        <div>
          <p className="admin-users__eyebrow">Администрирование</p>
          <h1>Пользователи платформы</h1>
          <p>Управляйте аккаунтами учеников, родителей, репетиторов и модераторов.</p>
        </div>
        <Button variant="ghost" disabled title="Экспорт появится позже">Экспорт позже</Button>
      </header>

      {loading ? <section className="admin-live-state" aria-live="polite">Загружаем пользователей...</section> : null}
      {error ? <section className="admin-live-state admin-live-state--error" aria-live="polite">{error}</section> : null}

      <section className="admin-users-metrics" aria-label="Сводка по пользователям">
        {page.metrics.map((metric) => (
          <article key={metric.title} className={cn('admin-users-metric', `admin-users-metric--${metric.tone}`)}>
            <span>{metric.icon}</span>
            <strong>{metric.value}</strong>
            <div>
              <b>{metric.title}</b>
              <p>{metric.meta}</p>
            </div>
          </article>
        ))}
      </section>

      <section className="admin-users-toolbar" aria-label="Фильтры пользователей">
        <div className="admin-users-tabs">
          {page.tabs.map((tab) => (
            <button
              key={tab}
              className={cn(activeTab === tab && 'is-active')}
              type="button"
              onClick={() => setActiveTab(tab)}
            >
              {tab}
            </button>
          ))}
        </div>
        <label className="admin-users-search">
          <span>⌕</span>
          <input
            id="admin-users-search"
            name="admin-users-search"
            aria-label="Поиск пользователей"
            placeholder="Email, ID или имя"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
          />
        </label>
        <Button variant="ghost" disabled title="Табы и поиск уже применяют фильтры">Фильтры</Button>
      </section>

      <div className="admin-users-layout">
        <main className="admin-users-table" aria-label="Список пользователей">
          <div className="admin-users-table__heading">
            <h2>Список пользователей</h2>
            <p>Последние регистрации и аккаунты с событиями</p>
          </div>

          <div className="admin-users-table__head" aria-hidden="true">
            <span>Пользователь</span>
            <span>Роль</span>
            <span>Статус</span>
            <span>Риск</span>
            <span />
          </div>

          <div className="admin-users-list">
            {filteredUsers.length === 0 ? (
              <div className="admin-live-state">
                {page.users.length === 0 ? 'Пользователей нет.' : 'Пользователи не найдены по текущему фильтру.'}
              </div>
            ) : filteredUsers.map((user) => (
              <article key={user.id} className={cn('admin-user-row', selectedUser?.id === user.id && 'is-active')}>
                <div className="admin-user-row__person">
                  <span className={cn('admin-user-row__avatar', `admin-user-row__avatar--${user.tone}`)}>{user.initials}</span>
                  <div>
                    <strong>{user.name}</strong>
                    <p>{user.email}</p>
                  </div>
                </div>
                <span className="admin-user-pill">{user.role}</span>
                <span className={cn('admin-user-pill', user.status === 'Блок' && 'is-danger')}>{user.status}</span>
                <span className={cn('admin-risk-pill', `admin-risk-pill--${user.risk.toLowerCase()}`)}>{user.risk}</span>
                <button
                  type="button"
                  aria-label={`Открыть действия пользователя ${user.name}`}
                  onClick={() => {
                    setSelectedUserId(user.id);
                    setActionStatus('');
                  }}
                >
                  •••
                </button>
              </article>
            ))}
          </div>
        </main>

        <aside className="admin-user-detail" aria-label="Карточка пользователя">
          <div className="admin-user-detail__heading">
            <h2>Карточка пользователя</h2>
            <p>Аккаунт с повышенным риском</p>
          </div>

          <div className="admin-user-detail__profile">
            <span>{selected.initials}</span>
            <div>
              <strong>{selected.name}</strong>
              <p>{selected.email}</p>
            </div>
          </div>

          <div className="admin-user-detail__badges">
            <span>{selected.role}</span>
            <span className="is-danger">{selected.status}</span>
          </div>

          <section className="admin-user-signals">
            <h3>{selected.riskTitle}</h3>
            {selected.signals.map((signal) => (
              <article key={signal.title}>
                <i />
                <div>
                  <strong>{signal.title}</strong>
                  <p>{signal.meta}</p>
                </div>
              </article>
            ))}
          </section>

          <div className="admin-user-detail__actions">
            {selected.actions.map((action, index) => (
              <Button
                key={action}
                variant={index === 1 ? 'primary' : 'ghost'}
                onClick={() => void handleUserAction(action)}
              >
                {action}
              </Button>
            ))}
          </div>
          {selected.actions.length === 0 ? <p className="admin-user-detail__status">Для выбранного аккаунта нет доступных действий.</p> : null}
          {actionStatus ? <p className="admin-user-detail__status">{actionStatus}</p> : null}
        </aside>
      </div>

      <section className="admin-users-cards" aria-label="Дополнительные действия администратора">
        <article className="admin-users-card">
          <h2>{page.cards.audit.title}</h2>
          <p>{page.cards.audit.text}</p>
          <LinkButton to="/admin/audit" variant="ghost">
            {page.cards.audit.action}
          </LinkButton>
        </article>

        <article className="admin-users-card">
          <span>{page.cards.roles.label}</span>
          <strong>{page.cards.roles.value}</strong>
          <p>{page.cards.roles.text}</p>
          <form className="admin-moderator-form" onSubmit={(event) => void handleCreateModerator(event)}>
            <FormField label="Email модератора">
              <TextInput
                type="email"
                value={moderatorEmail}
                onChange={(event) => setModeratorEmail(event.target.value)}
                placeholder="moderator@example.com"
                required
              />
            </FormField>
            <FormField label="Пароль">
              <TextInput
                type="password"
                value={moderatorPassword}
                onChange={(event) => setModeratorPassword(event.target.value)}
                placeholder="Минимум 6 символов"
                minLength={6}
                required
              />
            </FormField>
            <Button type="submit" variant="primary">
              Создать модератора
            </Button>
            {moderatorStatus ? <p className="admin-user-detail__status">{moderatorStatus}</p> : null}
          </form>
        </article>

        <article className="admin-users-card">
          <h2>{page.cards.moderation.title}</h2>
          <p>{page.cards.moderation.text}</p>
          <LinkButton to="/moderator/review-moderation" variant="ghost">
            {page.cards.moderation.action}
          </LinkButton>
        </article>

        <article className="admin-users-note">
          <div>
            <h2>{page.cards.note.title}</h2>
            <p>{page.cards.note.text}</p>
          </div>
          {page.cards.note.action ? <Button variant="ghost">{page.cards.note.action}</Button> : null}
        </article>
      </section>
    </div>
  );
}
