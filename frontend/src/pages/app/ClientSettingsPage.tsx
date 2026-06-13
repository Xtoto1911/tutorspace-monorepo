import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { getClientSettings, updateClientSettings } from '../../shared/api/client';
import { cn } from '../../shared/lib/cn';
import type { ClientSettings } from '../../shared/types';

type FormErrors = Partial<Record<string, string>>;

function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function validatePhone(phone: string): boolean {
  return /^\+?[\d\s\-()]{10,20}$/.test(phone);
}

export function ClientSettingsPage() {
  const [settings, setSettings] = useState<ClientSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});
  const [saveError, setSaveError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const loadSettings = () => {
    setLoading(true);
    setLoadError(null);
    getClientSettings()
      .then((data) => {
        setSettings(data);
        setLoading(false);
      })
      .catch(() => {
        setSettings(null);
        setLoadError('Не удалось загрузить настройки.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadSettings();
  }, []);

  const validate = (): boolean => {
    const next: FormErrors = {};
    if (!settings) return false;
    const p = settings.profile;

    if (!p.parentName || p.parentName.trim().length < 2) {
      next.parentName = 'Имя должно содержать минимум 2 символа';
    } else if (p.parentName.length > 100) {
      next.parentName = 'Имя не должно превышать 100 символов';
    }

    if (!p.phone || !validatePhone(p.phone)) {
      next.phone = 'Введите корректный телефон';
    }

    if (!p.email || !validateEmail(p.email)) {
      next.email = 'Введите корректный email';
    }

    if (!p.city || p.city.trim().length < 1) {
      next.city = 'Укажите город';
    } else if (p.city.length > 100) {
      next.city = 'Город не должен превышать 100 символов';
    }

    if (!p.timezone || p.timezone.trim().length < 1) {
      next.timezone = 'Укажите часовой пояс';
    }

    if (!p.language || p.language.trim().length < 1) {
      next.language = 'Укажите язык интерфейса';
    }

    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleChange = (field: string, value: string) => {
    setSettings((prev) => prev ? ({
      ...prev,
      profile: {
        ...prev.profile,
        [field]: value,
      },
    }) : prev);
    setSaveSuccess(false);
    setSaveError(null);
  };

  const handleToggle = (section: 'notifications' | 'privacy', index: number) => {
    setSettings((prev) => {
      if (!prev) return prev;
      const list = [...prev[section]];
      list[index] = { ...list[index], enabled: !list[index].enabled };
      return { ...prev, [section]: list };
    });
    setSaveSuccess(false);
    setSaveError(null);
  };

  const handleSave = async () => {
    if (!settings || !validate()) return;
    setSaving(true);
    setSaveError(null);
    setSaveSuccess(false);
    try {
      const updated = await updateClientSettings(settings);
      setSettings(updated);
      setSaveSuccess(true);
    } catch {
      setSaveError('Не удалось сохранить настройки. Попробуйте ещё раз.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="client-settings">Загрузка...</div>;
  }

  if (loadError || !settings) {
    return (
      <div className="client-settings">
        <p role="alert">{loadError || 'Настройки недоступны.'}</p>
        <Button type="button" onClick={loadSettings}>Повторить</Button>
      </div>
    );
  }

  return (
    <div className="client-settings">
      <header className="client-settings__header">
        <div>
          {saveSuccess && <span className="client-settings__success">Настройки сохранены</span>}
          {saveError && <span className="client-settings__error">{saveError}</span>}
          {!saveSuccess && !saveError && <span>&nbsp;</span>}
          <h1>Настройки аккаунта</h1>
          <p>Профиль, уведомления, безопасность и предпочтения поиска репетиторов.</p>
        </div>

        <Button onClick={handleSave} disabled={saving}>
          {saving ? 'Сохранение...' : 'Сохранить изменения'}
        </Button>
      </header>

      <section className="settings-stats" aria-label="Сводка настроек">
        {settings.metrics.map((metric) => (
          <article key={metric.label} className={cn('settings-stat', `settings-stat--${metric.tone}`)}>
            <span className="settings-stat__icon">{metric.icon}</span>
            <strong>{metric.value}</strong>
            <span>{metric.label}</span>
            <small>{metric.sublabel}</small>
          </article>
        ))}
      </section>

      <div className="settings-main-grid">
        <section className="settings-card settings-profile-card" aria-label="Личные данные">
          <div className="settings-card__heading">
            <h2>Личные данные</h2>
            <p>Эти данные видны только в заявках и переписке с репетитором.</p>
          </div>

          <div className="settings-profile-grid">
            <label className="settings-field" htmlFor="client-settings-parentName">
              <span>Имя родителя</span>
              <input
                id="client-settings-parentName"
                name="parentName"
                value={settings.profile.parentName}
                autoComplete="name"
                onChange={(e) => handleChange('parentName', e.target.value)}
              />
              {errors.parentName && <small className="settings-field__error">{errors.parentName}</small>}
            </label>
            <label className="settings-field" htmlFor="client-settings-phone">
              <span>Телефон</span>
              <input
                id="client-settings-phone"
                name="phone"
                value={settings.profile.phone}
                autoComplete="tel"
                onChange={(e) => handleChange('phone', e.target.value)}
              />
              {errors.phone && <small className="settings-field__error">{errors.phone}</small>}
            </label>
            <label className="settings-field" htmlFor="client-settings-email">
              <span>Email</span>
              <input
                id="client-settings-email"
                name="email"
                value={settings.profile.email}
                autoComplete="email"
                onChange={(e) => handleChange('email', e.target.value)}
              />
              {errors.email && <small className="settings-field__error">{errors.email}</small>}
            </label>
            <label className="settings-field" htmlFor="client-settings-city">
              <span>Город</span>
              <input
                id="client-settings-city"
                name="city"
                value={settings.profile.city}
                autoComplete="address-level2"
                onChange={(e) => handleChange('city', e.target.value)}
              />
              {errors.city && <small className="settings-field__error">{errors.city}</small>}
            </label>
            <label className="settings-field" htmlFor="client-settings-timezone">
              <span>Часовой пояс</span>
              <input
                id="client-settings-timezone"
                name="timezone"
                value={settings.profile.timezone}
                autoComplete="off"
                onChange={(e) => handleChange('timezone', e.target.value)}
              />
              {errors.timezone && <small className="settings-field__error">{errors.timezone}</small>}
            </label>
            <label className="settings-field" htmlFor="client-settings-language">
              <span>Язык интерфейса</span>
              <input
                id="client-settings-language"
                name="language"
                value={settings.profile.language}
                autoComplete="language"
                onChange={(e) => handleChange('language', e.target.value)}
              />
              {errors.language && <small className="settings-field__error">{errors.language}</small>}
            </label>
          </div>
        </section>

        <section className="settings-card settings-students-card" aria-label="Ученики">
          <div className="settings-card__heading">
            <h2>Ученики</h2>
            <p>Профили детей помогают точнее подбирать репетиторов.</p>
          </div>

          <div className="settings-students-list">
            {settings.students.length === 0 ? (
              <div className="settings-empty-state">
                <strong>Ученики пока не добавлены</strong>
                <p>Когда появится live-профиль ученика, он будет показан здесь без демо-данных.</p>
              </div>
            ) : (
              settings.students.map((student) => (
                <article key={student.id} className="settings-student-row">
                  <span className={cn('settings-student-row__dot', `settings-student-row__dot--${student.tone}`)} />
                  <div>
                    <strong>{student.name}</strong>
                    <p>{student.details}</p>
                  </div>
                  <span className={cn('settings-pill', `settings-pill--${student.tone}`)}>{student.goal}</span>
                </article>
              ))
            )}
          </div>
        </section>
      </div>

      <div className="settings-main-grid">
        <section className="settings-card" aria-label="Уведомления">
          <div className="settings-card__heading">
            <h2>Уведомления</h2>
            <p>Выберите, какие события отправлять по email и push.</p>
          </div>

          <div className="settings-toggle-list">
            {settings.notifications.map((item, index) => (
              <article key={item.id} className="settings-toggle-row">
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.description}</p>
                </div>
                <button
                  className={cn('settings-switch', item.enabled && 'is-on')}
                  type="button"
                  aria-pressed={item.enabled}
                  aria-label={item.title}
                  onClick={() => handleToggle('notifications', index)}
                />
              </article>
            ))}
          </div>
        </section>

        <section className="settings-card" aria-label="Приватность и безопасность">
          <div className="settings-card__heading">
            <h2>Приватность и безопасность</h2>
            <p>Контроль доступа к данным и действиям в аккаунте.</p>
          </div>

          <div className="settings-toggle-list">
            {settings.privacy.map((item, index) => (
              <article key={item.id} className="settings-toggle-row">
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.description}</p>
                </div>
                <button
                  className={cn('settings-switch', item.enabled && 'is-on')}
                  type="button"
                  aria-pressed={item.enabled}
                  aria-label={item.title}
                  onClick={() => handleToggle('privacy', index)}
                />
              </article>
            ))}
          </div>
        </section>
      </div>

      <div className="settings-bottom-grid">
        <section className="settings-card settings-preferences-card" aria-label="Предпочтения поиска">
          <h2>Предпочтения поиска</h2>
          <div className="settings-preference-list">
            {settings.searchPreferences.length === 0 ? (
              <p className="settings-empty-copy">Предпочтения появятся после сохранения поиска или заявки.</p>
            ) : (
              settings.searchPreferences.map((item, index) => (
                <span key={item} className={`settings-preference settings-preference--${index + 1}`}>
                  {item}
                </span>
              ))
            )}
          </div>
        </section>

        <section className="settings-card settings-account-card" aria-label="Действия аккаунта">
          <h2>Действия аккаунта</h2>
          <div>
            <Button variant="ghost">Выйти</Button>
            <Button variant="ghost" className="settings-account-card__delete">
              Удалить
            </Button>
          </div>
        </section>
      </div>
    </div>
  );
}
