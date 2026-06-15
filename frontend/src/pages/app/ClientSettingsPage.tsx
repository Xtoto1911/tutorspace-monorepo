import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { getClientSettings, updateClientSettings } from '../../shared/api/client';
import type { ClientSettings } from '../../shared/types';

type FormErrors = Partial<Record<string, string>>;

function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
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

    if (!p.email || !validateEmail(p.email)) {
      next.email = 'Введите корректный email';
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
          <h1>Настройки аккаунта</h1>
          <p>Личные данные и управление аккаунтом.</p>
        </div>

        <Button onClick={handleSave} disabled={saving}>
          {saving ? 'Сохранение...' : 'Сохранить изменения'}
        </Button>
      </header>

      {saveSuccess ? (
        <div className="settings-notice settings-notice--success" role="status">
          Настройки сохранены
        </div>
      ) : null}
      {saveError ? (
        <div className="settings-notice settings-notice--error" role="alert">
          {saveError}
        </div>
      ) : null}

      <div className="settings-main-grid">
        <section className="settings-card settings-profile-card" aria-label="Личные данные">
          <div className="settings-card__heading">
            <h2>Личные данные</h2>
            <p>Эти данные видны только в заявках и переписке с репетитором.</p>
          </div>

          <div className="settings-profile-grid">
            <label className="settings-field" htmlFor="client-settings-parentName">
              <span>Имя</span>
              <input
                id="client-settings-parentName"
                name="parentName"
                value={settings.profile.parentName}
                autoComplete="name"
                onChange={(e) => handleChange('parentName', e.target.value)}
              />
              {errors.parentName && <small className="settings-field__error">{errors.parentName}</small>}
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
          </div>
        </section>

        <section className="settings-card settings-account-card" aria-label="Действия аккаунта">
          <h2>Действия аккаунта</h2>
          <div>
            <Button variant="ghost">
              <span className="settings-account-card__icon" aria-hidden="true">↪</span>
              Выйти
            </Button>
            <Button variant="ghost" className="settings-account-card__delete">
              <span className="settings-account-card__icon" aria-hidden="true">×</span>
              Удалить
            </Button>
          </div>
        </section>
      </div>
    </div>
  );
}
