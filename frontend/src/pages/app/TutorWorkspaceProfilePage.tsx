import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Button, LinkButton } from '../../components/primitives';
import {
  getTutorSettings,
  getTutorWorkspaceProfile,
  updateTutorSettings,
  updateTutorWorkspaceProfile,
} from '../../shared/api/tutor';
import { updateAuthProfile } from '../../shared/api/auth';
import { updateSessionUserName } from '../../shared/auth/session';
import { profileChangedEvent } from '../../components/layout-helpers';
import { cn } from '../../shared/lib/cn';
import type { TutorSettingsPageData, TutorWorkspaceProfilePageData } from '../../shared/types';
import {
  buildTutorSettingsFromWorkspaceProfile,
  setTutorHidden,
  setWorkspaceProfileField,
  shouldShowTutorProfileOnboarding,
  toggleTutorSubject,
} from './tutor-profile-settings-helpers';

type FormErrors = Partial<Record<string, string>>;

export function TutorWorkspaceProfilePage() {
  const [searchParams] = useSearchParams();
  const [profile, setProfile] = useState<TutorWorkspaceProfilePageData | null>(null);
  const [settings, setSettings] = useState<TutorSettingsPageData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});

  useEffect(() => {
    Promise.all([getTutorWorkspaceProfile(), getTutorSettings()])
      .then(([data, tutorSettings]) => {
        setProfile(data);
        setSettings(tutorSettings);
        setLoading(false);
      })
      .catch(() => {
        setStatus('Не удалось загрузить профиль и настройки из CRM.');
        setLoading(false);
      });
  }, []);

  const validate = (): boolean => {
    if (!profile) return false;

    const next: FormErrors = {};
    const headline = profile.fields.find((field) => field.id === 'headline')?.value ?? '';
    const about = profile.fields.find((field) => field.id === 'about')?.value ?? '';

    if (!profile.identity.name || profile.identity.name.trim().length < 2) {
      next.name = 'ФИО должно содержать минимум 2 символа';
    } else if (profile.identity.name.length > 100) {
      next.name = 'ФИО не должно превышать 100 символов';
    }

    if (!profile.identity.city || profile.identity.city.trim().length < 1) {
      next.city = 'Укажите город';
    } else if (profile.identity.city.length > 100) {
      next.city = 'Город не должен превышать 100 символов';
    }

    if (headline.length > 200) {
      next.headline = 'Заголовок не должен превышать 200 символов';
    }

    if (about.length > 600) {
      next.about = 'Описание не должно превышать 600 символов';
    }

    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleFieldChange = (fieldId: string, value: string) => {
    setProfile((current) => (current ? setWorkspaceProfileField(current, fieldId, value) : current));
    setStatus('');
    setErrors((current) => ({ ...current, [fieldId]: undefined }));
  };

  const handleSubjectToggle = (subject: string) => {
    setSettings((current) => (current ? toggleTutorSubject(current, subject) : current));
    setStatus('');
  };

  const handleHiddenToggle = async () => {
    if (!profile || !settings || saving) return;

    const previousSettings = settings;
    const nextSettings = setTutorHidden(settings, !settings.isHidden);
    setSettings(nextSettings);
    setSaving(true);
    setStatus('');

    try {
      const updatedSettings = await updateTutorSettings(buildTutorSettingsFromWorkspaceProfile(profile, nextSettings));
      const updatedProfile = await getTutorWorkspaceProfile();
      setSettings(updatedSettings);
      setProfile(updatedProfile);
      setStatus(updatedSettings.isHidden ? 'Профиль скрыт из каталога.' : 'Профиль снова виден в каталоге.');
    } catch {
      setSettings(previousSettings);
      setStatus('Не удалось обновить видимость профиля.');
    } finally {
      setSaving(false);
    }
  };

  const handleSave = async () => {
    if (!profile || !settings) return;
    if (!validate()) return;

    setSaving(true);
    setStatus('');
    try {
      await updateAuthProfile({ name: profile.identity.name });
      updateSessionUserName(profile.identity.name);
      window.dispatchEvent(new Event(profileChangedEvent));

      const updated = await updateTutorWorkspaceProfile(profile);
      const settingsPayload = buildTutorSettingsFromWorkspaceProfile(updated, settings);
      const updatedSettings = await updateTutorSettings(settingsPayload);
      const refreshedProfile = await getTutorWorkspaceProfile();
      setProfile(refreshedProfile);
      setSettings(updatedSettings);
      setStatus('Профиль и настройки сохранены в CRM.');
    } catch {
      setStatus('Не удалось сохранить профиль и настройки.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="tutor-profile-workspace">Загрузка...</div>;
  }

  if (!profile) {
    return <div className="tutor-profile-workspace">{status || 'Не удалось загрузить профиль и настройки из CRM.'}</div>;
  }

  const showOnboarding = settings ? shouldShowTutorProfileOnboarding(
    profile,
    settings,
    searchParams.get('welcome') === '1',
  ) : false;

  return (
    <div className="tutor-profile-workspace">
      <header className="tutor-profile-workspace__header">
        <div>
          <p className="tutor-profile-workspace__eyebrow">Кабинет репетитора</p>
          <h1>Профиль репетитора</h1>
          <p>Редактируйте публичную анкету, предметы, видимость карточки и уведомления.</p>
        </div>

        <div className="tutor-profile-workspace__actions">
          <LinkButton to="/catalog" variant="ghost">
            Посмотреть витрину
          </LinkButton>
          <Button onClick={handleSave} disabled={saving}>{saving ? 'Сохранение...' : 'Сохранить профиль'}</Button>
        </div>
      </header>
      {status ? <div className="success-banner">{status}</div> : null}

      {settings && showOnboarding ? (
        <section className="tutor-profile-onboarding" aria-label="Первый запуск профиля репетитора">
          <div className="tutor-profile-onboarding__copy">
            <span>Карточка скрыта из каталога</span>
            <h2>Заполните профиль спокойно, ученики пока его не видят</h2>
            <p>
              Проверьте основную информацию, выберите предметы и добавьте описание. Когда карточка будет готова, откройте
              видимость в правой колонке.
            </p>
          </div>

          <div className="tutor-profile-onboarding__steps" aria-label="Шаги подготовки карточки">
            {profile.checklist.map((item, index) => (
              <article key={item.title} className={item.done ? 'is-done' : undefined}>
                <b>{index + 1}</b>
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.text}</p>
                </div>
              </article>
            ))}
            <article className={!settings.isHidden ? 'is-done' : undefined}>
              <b>{profile.checklist.length + 1}</b>
              <div>
                <strong>Открыть карточку</strong>
                <p>Снимите скрытие профиля, когда описание и предметы готовы.</p>
              </div>
            </article>
          </div>

          <div className="tutor-profile-onboarding__actions">
            <Button onClick={handleSave} disabled={saving}>{saving ? 'Сохранение...' : 'Сохранить профиль'}</Button>
            {settings.isHidden ? (
              <Button variant="ghost" onClick={handleHiddenToggle} disabled={saving}>
                Открыть карточку
              </Button>
            ) : null}
          </div>
        </section>
      ) : null}

      <section className="tutor-profile-hero-card" aria-label="Статус публичного профиля">
        <div className="tutor-profile-hero-card__identity">
          <span className="tutor-profile-avatar">{profile.identity.avatarInitials}</span>
          <div>
            <div className="tutor-profile-hero-card__title">
              <h2>{profile.identity.name}</h2>
              {profile.identity.verified ? <strong>Проверен</strong> : null}
            </div>
            <p>{profile.identity.role} · {profile.identity.city}</p>
          </div>
        </div>

        <div className="tutor-profile-completion">
          <div>
            <span>{profile.identity.status}</span>
            <strong>{profile.identity.completion}% заполнено</strong>
          </div>
          <span className="tutor-profile-completion__bar">
            <i style={{ width: `${profile.identity.completion}%` }} />
          </span>
        </div>
      </section>

      <div className="tutor-profile-layout">
        <main className="tutor-profile-editor" aria-label="Редактор профиля">
          <section className="tutor-profile-card tutor-profile-form-card">
            <div className="tutor-profile-card__heading">
              <h2>Основная информация</h2>
              <p>Эти данные попадают в каталог, профиль и карточку заявки.</p>
            </div>

            <div className="tutor-profile-form-grid">
              {profile.fields.map((field) => (
                <label
                  key={field.id}
                  className={cn('tutor-profile-field', field.type === 'textarea' && 'tutor-profile-field--wide')}
                  htmlFor={`tutor-profile-${field.id}`}
                >
                  <span>{field.label}</span>
                  {field.type === 'textarea' ? (
                    <textarea
                      id={`tutor-profile-${field.id}`}
                      name={field.id}
                      value={field.value}
                      maxLength={field.id === 'about' ? 600 : undefined}
                      onChange={(event) => handleFieldChange(field.id, event.target.value)}
                    />
                  ) : (
                    <input
                      id={`tutor-profile-${field.id}`}
                      name={field.id}
                      value={field.value}
                      onChange={(event) => handleFieldChange(field.id, event.target.value)}
                    />
                  )}
                  {errors[field.id] ? <small className="tutor-settings-field__error">{errors[field.id]}</small> : null}
                </label>
              ))}
            </div>
          </section>

          <section className="tutor-profile-card">
            <div className="tutor-profile-card__heading tutor-profile-card__heading--row">
              <div>
                <h2>Предметы и цены</h2>
                <p>Направления, которые видят ученики при поиске.</p>
              </div>
            </div>

            {settings ? (
              <>
                <div className="tutor-settings-subject-list tutor-settings-subject-list--editable">
                  {settings.availableSubjects.map((subject) => (
                    <label key={subject} className="tutor-settings-subject-checkbox">
                      <input
                        type="checkbox"
                        checked={settings.subjects.includes(subject)}
                        onChange={() => handleSubjectToggle(subject)}
                      />
                      <span>{subject}</span>
                    </label>
                  ))}
                </div>
                <p className="tutor-settings-notice">Изменение предметов может отправить профиль на повторную модерацию.</p>
              </>
            ) : (
              <div className="tutor-profile-subject-list">
                {profile.subjects.map((subject) => (
                  <article key={subject.title} className="tutor-profile-subject-row">
                    <span className={cn('tutor-profile-subject-row__mark', `tutor-profile-subject-row__mark--${subject.tone}`)} />
                    <div>
                      <strong>{subject.title}</strong>
                      <p>{subject.meta}</p>
                    </div>
                    <b>{subject.price}</b>
                  </article>
                ))}
              </div>
            )}
          </section>

        </main>

        <aside className="tutor-profile-side" aria-label="Превью и рекомендации профиля">
          {settings ? (
            <section className="tutor-settings-card">
              <div className="tutor-settings-card__heading">
                <h2>Видимость</h2>
              </div>
              <div className="tutor-settings-toggle-list">
                <article className="tutor-settings-toggle">
                  <div>
                    <strong>Скрыть профиль из каталога</strong>
                    <p>Карточка не будет отображаться в поиске и каталоге</p>
                  </div>
                  <button
                    className={cn('settings-switch', settings.isHidden && 'is-on')}
                    type="button"
                    aria-pressed={settings.isHidden}
                    disabled={saving}
                    onClick={handleHiddenToggle}
                  />
                </article>
              </div>
          </section>
        ) : null}

      </aside>
      </div>
    </div>
  );
}
