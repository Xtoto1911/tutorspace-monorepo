import { useEffect, useState } from 'react';
import { Button } from '../../components/primitives';
import { getAdminSettingsPage } from '../../shared/api/admin';
import { cn } from '../../shared/lib/cn';
import type { AdminSettingsPageData } from '../../shared/types';
import { emptyAdminSettingsPage, getReadableError } from './admin-live-helpers';

export function AdminSettingsPage() {
  const [page, setPage] = useState<AdminSettingsPageData>(emptyAdminSettingsPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    setLoading(true);
    setError('');
    getAdminSettingsPage()
      .then((data) => {
        if (!ignore) {
          setPage(data);
          setError('');
        }
      })
      .catch((error) => {
        if (!ignore) {
          setPage(emptyAdminSettingsPage);
          setError(getReadableError(error, 'Не удалось загрузить настройки платформы.'));
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

  return (
    <div className="admin-settings">
      <header className="admin-settings__header">
        <div>
          <p className="admin-settings__eyebrow">Администрирование</p>
          <h1>Настройки платформы</h1>
          <p>Системные ограничения, роли, параметры безопасности и правила модерации TutorSpace.</p>
        </div>
        <div className="admin-settings__actions">
          <Button variant="ghost" disabled title="Настройки доступны только для просмотра">Только просмотр</Button>
        </div>
      </header>

      {loading ? <section className="admin-live-state" aria-live="polite">Загружаем системную сводку...</section> : null}
      {error ? <section className="admin-live-state admin-live-state--error" aria-live="polite">{error}</section> : null}

      <section className="admin-settings-metrics" aria-label="Сводка настроек">
        {page.metrics.map((metric) => (
          <article key={metric.label} className={cn('admin-settings-metric', `admin-settings-metric--${metric.tone}`)}>
            <span>{metric.icon}</span>
            <div>
              <strong>{metric.value}</strong>
              <p>{metric.label}</p>
            </div>
            <b>{metric.hint}</b>
          </article>
        ))}
      </section>

      <section className="admin-settings-layout" aria-label="Основные настройки">
        <main className="admin-settings-main">
          <article className="admin-settings-card">
            <div className="admin-settings-card__heading">
              <h2>Безопасность</h2>
              <p>Текущая read-only сводка backend-конфига и JWT-политик.</p>
            </div>
            <div className="admin-settings-toggle-list">
              {page.security.length === 0 ? (
                <div className="admin-live-state">Нет live-данных по безопасности.</div>
              ) : page.security.map((item) => (
                <div key={item.title} className="admin-settings-toggle">
                  <div>
                    <strong>{item.title}</strong>
                    <p>{item.description}</p>
                  </div>
                  <span className={cn(item.enabled && 'is-enabled')}>{item.enabled ? 'Вкл' : 'Выкл'}</span>
                </div>
              ))}
            </div>
          </article>

          <article className="admin-settings-card">
            <div className="admin-settings-card__heading admin-settings-card__heading--row">
              <div>
                <h2>Роли и доступы</h2>
                <p>Роли берутся из live auth-service.</p>
              </div>
              <Button variant="ghost" disabled title="RBAC редактируется через действия пользователей">RBAC read-only</Button>
            </div>
            <div className="admin-role-grid">
              {page.roles.length === 0 ? (
                <div className="admin-live-state">Роли не получены из auth-service.</div>
              ) : page.roles.map((role) => (
                <article key={role.title} className="admin-role-card">
                  <strong>{role.title}</strong>
                  <p>{role.description}</p>
                  <span>{role.users}</span>
                </article>
              ))}
            </div>
          </article>

          <article className="admin-settings-card">
            <div className="admin-settings-card__heading">
              <h2>Лимиты тарифов и CRM</h2>
              <p>Лимиты показываются только если backend возвращает live-конфигурацию.</p>
            </div>
            <div className="admin-limit-list">
              {page.limits.length === 0 ? (
                <div className="admin-live-state">Настраиваемых лимитов пока нет.</div>
              ) : page.limits.map((limit) => (
                <div key={limit.label} className="admin-limit-row">
                  <span>{limit.label}</span>
                  <strong>{limit.value}</strong>
                  <p>{limit.description}</p>
                </div>
              ))}
            </div>
          </article>
        </main>

        <aside className="admin-settings-side">
          <article className="admin-settings-card">
            <div className="admin-settings-card__heading">
              <h2>Сервисы</h2>
              <p>Текущий статус архитектурной подготовки.</p>
            </div>
            <div className="admin-service-settings-list">
              {page.services.length === 0 ? (
                <div className="admin-live-state">Нет live-статусов сервисов.</div>
              ) : page.services.map((service) => (
                <div key={service.name} className="admin-service-setting">
                  <i className={`admin-service-setting__dot admin-service-setting__dot--${service.tone}`} />
                  <div>
                    <strong>{service.name}</strong>
                    <p>{service.description}</p>
                  </div>
                  <span>{service.status}</span>
                </div>
              ))}
            </div>
          </article>

          <article className="admin-settings-note">
            <h2>Важно</h2>
            <p>Страница работает как read-only системная сводка: она не отправляет изменения в backend.</p>
          </article>
        </aside>
      </section>

      <section className="admin-policy-grid" aria-label="Правила платформы">
        {page.policies.length === 0 ? (
          <div className="admin-live-state">Политики платформы пока не настроены.</div>
        ) : page.policies.map((policy) => (
          <article key={policy.title} className="admin-policy-card">
            <h2>{policy.title}</h2>
            <p>{policy.text}</p>
            <div>
              {policy.tags.map((tag) => (
                <span key={tag}>{tag}</span>
              ))}
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
