import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, LinkButton } from '../../components/primitives';
import { getClientSettings, updateClientSettings } from '../../shared/api/client';
import { updateAuthProfile } from '../../shared/api/auth';
import { clearAuthSession, updateSessionUserName } from '../../shared/auth/session';
import { logout } from '../../shared/api/public';
import { profileChangedEvent } from '../../components/layout-helpers';
import type { ClientSettings } from '../../shared/types';
import {
  getClientInitials,
  setClientProfileField,
  validateClientProfile,
  type ClientProfileErrors,
} from './client-settings-helpers';

const FALLBACK_SETTINGS: ClientSettings = {
  metrics: [],
  profile: {
    parentName: '',
    phone: '',
    email: '',
    city: '',
    timezone: 'Europe/Moscow',
    language: 'Русский',
  },
  students: [],
  notifications: [],
  privacy: [],
  searchPreferences: [],
};

export function ClientSettingsPage() {
  const navigate = useNavigate();
  const [settings, setSettings] = useState<ClientSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [errors, setErrors] = useState<ClientProfileErrors>({});
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getClientSettings()
      .then((data) => {
        if (!ignore) {
          setSettings(data);
        }
      })
      .catch(() => {
        if (!ignore) {
          setSettings(FALLBACK_SETTINGS);
          setError('Не удалось загрузить настройки. Попробуйте позже.');
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

  const handleFieldChange = (field: 'parentName' | 'email', value: string) => {
    setSettings((current) => (current ? setClientProfileField(current, field, value) : current));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setStatus('');
    setError('');
  };

  const handleSave = async () => {
    if (!settings) return;

    const validation = validateClientProfile(settings);
    if (Object.keys(validation).length > 0) {
      setErrors(validation);
      return;
    }

    setSaving(true);
    setStatus('');
    setError('');

    try {
      await updateAuthProfile({ name: settings.profile.parentName });
      updateSessionUserName(settings.profile.parentName);
      window.dispatchEvent(new Event(profileChangedEvent));

      const updated = await updateClientSettings(settings);
      setSettings(updated);
      setStatus('Настройки сохранены');
    } catch {
      setError('Не удалось сохранить настройки. Попробуйте ещё раз.');
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = async () => {
    if (deleting) return;
    const sessionRaw = window.localStorage.getItem('tutorspace.auth.session');
    const refreshToken = sessionRaw
      ? (() => {
          try {
            return JSON.parse(sessionRaw).refreshToken as string | undefined;
          } catch {
            return undefined;
          }
        })()
      : undefined;

    try {
      await logout(refreshToken);
    } catch {
      // logout API failure is not blocking — session is cleared anyway
    } finally {
      clearAuthSession();
      navigate('/auth/sign-in', { replace: true });
    }
  };

  const handleDelete = () => {
    if (typeof window !== 'undefined') {
      const confirmed = window.confirm(
        'Удаление аккаунта клиента недоступно в текущей сборке. Обратитесь в поддержку.',
      );
      if (!confirmed) {
        return;
      }
      setStatus('Запрос на удаление отправлен в поддержку.');
    }
  };

  if (loading) {
    return <div className="client-settings-workspace">Загрузка...</div>;
  }

  if (!settings) {
    return (
      <div className="client-settings-workspace">
        {error || 'Не удалось загрузить настройки.'}
      </div>
    );
  }

  const initials = getClientInitials(settings.profile.parentName);
  const displayName = settings.profile.parentName.trim() || 'Без имени';
  const displayEmail = settings.profile.email.trim() || 'Email не указан';
  const displayCity = settings.profile.city.trim() || 'Город не выбран';

  return (
    <div className="client-settings-workspace">
      <header className="client-settings-workspace__header">
        <div>
          <p className="client-settings-workspace__eyebrow">Кабинет клиента</p>
          <h1>Настройки аккаунта</h1>
          <p>Личные данные и управление аккаунтом.</p>
        </div>
        <div className="client-settings-workspace__actions">
          <LinkButton to="/app/client" variant="ghost">В кабинет</LinkButton>
          <Button onClick={handleSave} disabled={saving}>
            {saving ? 'Сохранение...' : 'Сохранить'}
          </Button>
        </div>
      </header>

      {status ? <div className="success-banner">{status}</div> : null}
      {error ? (
        <div className="settings-notice settings-notice--error" role="alert">
          {error}
        </div>
      ) : null}

      <section className="client-settings-hero-card" aria-label="Профиль клиента">
        <div className="client-settings-hero-card__identity">
          <span className="client-settings-hero-card__avatar" aria-hidden="true">{initials}</span>
          <div>
            <h2>{displayName}</h2>
            <p>{displayEmail}</p>
          </div>
        </div>
        <div className="client-settings-hero-card__meta">
          <span>Профиль клиента</span>
          <strong>{displayCity}</strong>
        </div>
      </section>

      <div className="client-settings-layout">
        <main className="client-settings-editor" aria-label="Редактор настроек">
          <section className="client-settings-card">
            <div className="client-settings-card__heading">
              <h2>Личные данные</h2>
              <p>Эти данные видны в заявках и переписке с репетитором.</p>
            </div>

            <div className="client-settings-form-grid">
              <label className="client-settings-field" htmlFor="client-settings-parentName">
                <span>Имя</span>
                <input
                  id="client-settings-parentName"
                  name="parentName"
                  autoComplete="name"
                  value={settings.profile.parentName}
                  onChange={(event) => handleFieldChange('parentName', event.target.value)}
                />
                {errors.parentName ? (
                  <small className="settings-field__error">{errors.parentName}</small>
                ) : null}
              </label>
              <label className="client-settings-field" htmlFor="client-settings-email">
                <span>Email</span>
                <input
                  id="client-settings-email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  value={settings.profile.email}
                  onChange={(event) => handleFieldChange('email', event.target.value)}
                />
                {errors.email ? (
                  <small className="settings-field__error">{errors.email}</small>
                ) : null}
              </label>
            </div>
          </section>

          <section className="client-settings-card" aria-label="Действия аккаунта">
            <div className="client-settings-card__heading">
              <h2>Действия аккаунта</h2>
              <p>Выход и удаление аккаунта.</p>
            </div>
            <div className="client-settings-account-actions">
              <Button type="button" variant="ghost" onClick={() => void handleLogout()}>
                Выйти
              </Button>
              <Button
                type="button"
                variant="ghost"
                className="client-settings-account-actions__delete"
                onClick={handleDelete}
                disabled={deleting}
              >
                Удалить аккаунт
              </Button>
            </div>
          </section>
        </main>

        <aside className="client-settings-side" aria-label="Подсказка по настройкам">
          <section className="client-settings-card">
            <div className="client-settings-card__heading">
              <h2>Подсказка</h2>
              <p>Что появится в следующих обновлениях.</p>
            </div>
            <p className="client-settings-side__copy">
              Здесь будут уведомления, приватность, ученики и предпочтения поиска —
              мы добавим их в следующих проходах, синхронно с CRM.
            </p>
            <LinkButton to="/app/client/notifications" variant="ghost" className="client-settings-side__cta">
              Открыть уведомления
            </LinkButton>
          </section>
        </aside>
      </div>
    </div>
  );
}
