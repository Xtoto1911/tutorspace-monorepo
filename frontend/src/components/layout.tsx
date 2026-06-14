import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { brandName, brandTagline, publicFooterColumns, publicNavigation } from '../shared/config/site';
import { cn } from '../shared/lib/cn';
import { Button, LinkButton } from './primitives';
import { roadmapSections } from '../shared/config/roadmap';
import { Link, NavLink } from 'react-router-dom';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { clearAuthSession, readAuthSession } from '../shared/auth/session';
import type { AuthSession, Role, TutorRequestsPageData, TutorSubscriptionPageData } from '../shared/types';
import { logout } from '../shared/api/public';
import { getTutorRequests, getTutorSubscription } from '../shared/api/tutor';
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
  type WorkspaceNotification,
} from '../shared/api/notifications';
import {
  getWorkspaceNotificationButtonLabel,
  getWorkspaceNotificationPanelState,
  getPublicHeaderViewModel,
  getTutorNextRequestSidebarCard,
  getTutorSubscriptionSidebarCard,
  tutorSidebarRefreshEvent,
} from './layout-helpers';

function BaseShell({ children }: { children: ReactNode }) {
  return <div className="app-shell">{children}</div>;
}

export function PublicHeader() {
  const navigate = useNavigate();
  const session = readAuthSession();
  const viewModel = getPublicHeaderViewModel(session);

  const handleLogout = async () => {
    try {
      await logout(session?.refreshToken);
    } catch {
      // logout API failure is not blocking — session is cleared anyway
    } finally {
      clearAuthSession();
      navigate('/', { replace: true });
    }
  };

  return (
    <header className="public-header">
      <Link to="/" className="brand">
        <span className="brand-mark">T</span>
        <span>
          <strong>{brandName}</strong>
          <small>{brandTagline}</small>
        </span>
      </Link>

      <nav className="public-nav" aria-label="Основная навигация">
        {publicNavigation.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => cn('public-nav__link', isActive && 'is-active')}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="public-header__actions">
        {viewModel.isAuthenticated ? (
          <>
            <LinkButton to={viewModel.dashboardTo ?? '/'} variant="ghost">
              {viewModel.dashboardLabel}
            </LinkButton>
            <Button type="button" variant="primary" onClick={() => void handleLogout()}>
              Выйти
            </Button>
          </>
        ) : (
          <>
            <LinkButton to="/auth/sign-in" variant="ghost">
              Войти
            </LinkButton>
            <LinkButton to="/auth/sign-up" variant="primary">
              Регистрация
            </LinkButton>
          </>
        )}
      </div>
    </header>
  );
}

export function PublicFooter() {
  return (
    <footer className="public-footer">
      <div className="public-footer__brand">
        <div className="brand brand--footer">
          <span className="brand-mark">T</span>
          <span>
            <strong>{brandName}</strong>
            <small>{brandTagline}</small>
          </span>
        </div>
        <p>
          Платформа для поиска репетиторов, записи на занятия и безопасной коммуникации между учениками,
          родителями и преподавателями.
        </p>
      </div>

      <div className="public-footer__links">
        {publicFooterColumns.map((column) => (
          <div key={column.title}>
            <h3>{column.title}</h3>
            {column.links.map((link, index) => (
              <Link key={`${column.title}-${index}-${link.label}`} to={link.to}>
                {link.label}
              </Link>
            ))}
          </div>
        ))}
      </div>
    </footer>
  );
}

export function PublicLayout() {
  return (
    <BaseShell>
      <div className="page-gradient page-gradient--public" />
      <PublicHeader />
      <main className="page-main">
        <Outlet />
      </main>
      <PublicFooter />
    </BaseShell>
  );
}

export function AuthLayout() {
  return (
    <BaseShell>
      <div className="page-gradient page-gradient--auth" />
      <main className="auth-layout">
        <section className="auth-layout__brand">
          <Link to="/" className="brand brand--auth">
            <span className="brand-mark">T</span>
            <span>
              <strong>{brandName}</strong>
              <small>{brandTagline}</small>
            </span>
          </Link>

          <div className="auth-layout__copy">
            <p className="section-eyebrow">Вход и регистрация</p>
            <h1>Единый маршрут для родителей, учеников и репетиторов.</h1>
            <p>
              Мы сразу заложили отдельные сценарии регистрации, чтобы backend и интерфейс не приходилось
              переделывать, когда добавятся роли, статусы и платежи.
            </p>
          </div>

          <div className="auth-layout__highlights">
            <article className="mini-tile">
              <strong>Parent flow</strong>
              <span>Быстрая регистрация с данными ребенка и цели обучения.</span>
            </article>
            <article className="mini-tile">
              <strong>Tutor flow</strong>
              <span>Профиль, предметы, опыт и первая анкета.</span>
            </article>
            <article className="mini-tile">
              <strong>Future-ready</strong>
              <span>Структура маршрутов уже готова к backend-слою.</span>
            </article>
          </div>
        </section>

        <section className="auth-layout__content">
          <Outlet />
        </section>
      </main>
    </BaseShell>
  );
}

const sectionBasePaths: Record<string, string> = {
  client: '/app/client',
  tutor: '/app/tutor',
  admin: '/admin',
  moderator: '/moderator',
};

const sectionAccentMap: Record<string, string> = {
  client: 'indigo',
  tutor: 'emerald',
  admin: 'teal',
  moderator: 'amber',
};

const sectionChrome: Record<
  keyof typeof sectionBasePaths,
  {
    subtitle: string;
    profileName: string;
    profileInitials: string;
    profileRole: string;
    navTitle: string;
    primaryCardTitle: string;
    primaryCardMeta: string;
    primaryCardText: string;
    primaryCardButton: string;
    secondaryCardTitle: string;
    secondaryCardText: string;
    secondaryCardPill: string;
  }
> = {
  client: {
    subtitle: 'Кабинет клиента',
    profileName: 'Анна Родионова',
    profileInitials: 'АР',
    profileRole: 'клиент',
    navTitle: 'Навигация клиента',
    primaryCardTitle: 'Расписание',
    primaryCardMeta: 'Live CRM',
    primaryCardText: 'Ближайшие уроки и доступные действия подтягиваются из расписания клиента.',
    primaryCardButton: 'Открыть расписание',
    secondaryCardTitle: 'Подсказка',
    secondaryCardText: 'Переносы видны в карточке урока после синхронизации с репетитором.',
    secondaryCardPill: 'к расписанию',
  },
  tutor: {
    subtitle: 'Кабинет репетитора',
    profileName: 'Екатерина Лебедева',
    profileInitials: 'ЕЛ',
    profileRole: 'репетитор',
    navTitle: 'Навигация репетитора',
    primaryCardTitle: 'Следующая заявка',
    primaryCardMeta: 'Сегодня, 18:30',
    primaryCardText: 'Новая заявка по математике. Ответьте родителю, чтобы не потерять контакт.',
    primaryCardButton: 'Открыть заявку',
    secondaryCardTitle: 'Подписка',
    secondaryCardText: 'Статус подписки и тарифы доступны в разделе подписки после проверки оплаты.',
    secondaryCardPill: 'проверить тариф',
  },
  admin: {
    subtitle: 'Админ-панель',
    profileName: 'Администратор',
    profileInitials: 'АД',
    profileRole: 'управление платформой',
    navTitle: 'Навигация администратора',
    primaryCardTitle: 'Системный статус',
    primaryCardMeta: 'Сейчас',
    primaryCardText: 'Проверяйте сервисы и алерты на dashboard: shell не подменяет live-статус.',
    primaryCardButton: 'Проверить статус',
    secondaryCardTitle: 'Контроль',
    secondaryCardText: 'Если алертов нет, раздел покажет спокойное empty state без лишней тревоги.',
    secondaryCardPill: 'dashboard',
  },
  moderator: {
    subtitle: 'Модерация',
    profileName: 'Модератор',
    profileInitials: 'МД',
    profileRole: 'проверка контента',
    navTitle: 'Навигация модератора',
    primaryCardTitle: 'Очередь проверки',
    primaryCardMeta: 'Сегодня',
    primaryCardText: 'Отзывы и жалобы показываются в live-очередях модерации.',
    primaryCardButton: 'Открыть очередь',
    secondaryCardTitle: 'Приоритет',
    secondaryCardText: 'Количество задач берите из очереди: shell оставляет только быстрый переход.',
    secondaryCardPill: 'к очереди',
  },
};

const clientNavLabelMap: Record<string, string> = {
  applications: 'Мои заявки',
  favorites: 'Избранные',
};

const tutorNavLabelMap: Record<string, string> = {
  requests: 'Заявки',
};

const tutorRouteOrder = ['requests', 'calendar', 'students', 'profile', 'messages', 'subscription'];

const roleLabelMap: Record<Role, string> = {
  parent: 'клиент',
  tutor: 'репетитор',
  admin: 'admin',
  moderator: 'moderator',
};

function getInitials(name: string) {
  const initials = name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0]?.toUpperCase())
    .join('')
    .slice(0, 2);

  return initials || 'TS';
}

function formatNotificationTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function getWorkspaceUser(
  user: AuthSession['user'] | undefined,
  fallback: { profileName: string; profileRole: string },
) {
  const name = user?.name || fallback.profileName;

  return {
    name,
    initials: getInitials(name),
    role: user ? roleLabelMap[user.role] : fallback.profileRole,
  };
}

function getWorkspaceRouteLabel(sectionKey: keyof typeof sectionBasePaths, route: (typeof roadmapSections)[number]['routes'][number]) {
  if (sectionKey === 'client' && route.path && clientNavLabelMap[route.path]) {
    return clientNavLabelMap[route.path];
  }

  if (sectionKey === 'tutor' && route.path && tutorNavLabelMap[route.path]) {
    return tutorNavLabelMap[route.path];
  }

  return route.title;
}

function getWorkspaceProfileLinkTarget(sectionKey: keyof typeof sectionBasePaths): string {
  if (sectionKey === 'tutor') {
    return '/app/tutor';
  }
  if (sectionKey === 'client') {
    return '/app/client';
  }
  if (sectionKey === 'admin') {
    return '/admin';
  }
  if (sectionKey === 'moderator') {
    return '/moderator';
  }
  return '#';
}

export function SectionLayout({ sectionKey }: { sectionKey: keyof typeof sectionBasePaths }) {
  const section = roadmapSections.find((item) => item.key === sectionKey);
  const location = useLocation();
  const navigate = useNavigate();
  const session = readAuthSession();
  const [tutorRequestsSummary, setTutorRequestsSummary] = useState<TutorRequestsPageData | null>(null);
  const [tutorSubscriptionSummary, setTutorSubscriptionSummary] = useState<TutorSubscriptionPageData | null>(null);
  const [notifications, setNotifications] = useState<WorkspaceNotification[]>([]);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notificationsError, setNotificationsError] = useState<string | null>(null);
  const [navigationOpen, setNavigationOpen] = useState(false);

  useEffect(() => {
    setNavigationOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (sectionKey !== 'tutor') {
      return;
    }

    let active = true;

    const loadTutorSidebarSummary = () => {
      Promise.allSettled([getTutorRequests(), getTutorSubscription()]).then(([requestsResult, subscriptionResult]) => {
        if (!active) {
          return;
        }

        setTutorRequestsSummary(requestsResult.status === 'fulfilled' ? requestsResult.value : null);
        setTutorSubscriptionSummary(subscriptionResult.status === 'fulfilled' ? subscriptionResult.value : null);
      });
    };

    loadTutorSidebarSummary();
    window.addEventListener(tutorSidebarRefreshEvent, loadTutorSidebarSummary);

    return () => {
      active = false;
      window.removeEventListener(tutorSidebarRefreshEvent, loadTutorSidebarSummary);
    };
  }, [sectionKey]);

  useEffect(() => {
    const canLoadNotifications = sectionKey === 'client' || sectionKey === 'tutor';
    if (!canLoadNotifications) {
      setNotifications([]);
      setUnreadNotifications(0);
      setNotificationsError(null);
      setNotificationsOpen(false);
      return;
    }

    let active = true;

    Promise.all([getNotifications(), getUnreadNotificationCount()])
      .then(([items, count]) => {
        if (!active) {
          return;
        }
        setNotifications(items);
        setUnreadNotifications(count.unreadCount);
        setNotificationsError(null);
      })
      .catch((error: unknown) => {
        if (!active) {
          return;
        }
        setNotifications([]);
        setUnreadNotifications(0);
        setNotificationsError(error instanceof Error ? error.message : 'Notification service unavailable');
      });

    return () => {
      active = false;
    };
  }, [sectionKey]);

  if (!section) {
    return <Outlet />;
  }

  const basePath = sectionBasePaths[sectionKey];
  const accent = sectionAccentMap[sectionKey];
  const chrome = sectionChrome[sectionKey];
  const activeRoute =
    section.routes.find((route) => {
      const path = route.index ? basePath : `${basePath}/${route.path}`;
      return route.index ? location.pathname === path : location.pathname.startsWith(path);
    }) ?? section.routes[0];
  const visibleRoutes =
    sectionKey === 'client' || sectionKey === 'tutor'
      ? section.routes.filter((route) => !route.index)
      : section.routes;
  const activeRouteLabel =
    sectionKey === 'client' && location.pathname.includes('/applications/')
      ? 'Детали заявки'
      : sectionKey === 'tutor' && activeRoute.path === 'requests'
        ? 'Заявки учеников'
      : getWorkspaceRouteLabel(sectionKey, activeRoute);
  const orderedVisibleRoutes =
    sectionKey === 'tutor'
      ? [...visibleRoutes].sort((left, right) => {
          const leftIndex = tutorRouteOrder.indexOf(left.path ?? '');
          const rightIndex = tutorRouteOrder.indexOf(right.path ?? '');
          return (leftIndex === -1 ? 99 : leftIndex) - (rightIndex === -1 ? 99 : rightIndex);
        })
      : visibleRoutes;
  const sidebarCards = useMemo(() => {
    if (sectionKey === 'tutor') {
      return {
        primary: getTutorNextRequestSidebarCard(tutorRequestsSummary),
        secondary: getTutorSubscriptionSidebarCard(tutorSubscriptionSummary),
      };
    }

    return {
      primary: {
        title: chrome.primaryCardTitle,
        meta: chrome.primaryCardMeta,
        text: chrome.primaryCardText,
        actionLabel: chrome.primaryCardButton,
        to: sectionKey === 'client'
          ? '/app/client/schedule'
          : sectionKey === 'admin'
            ? '/admin'
            : sectionKey === 'moderator'
              ? '/moderator/review-moderation'
              : undefined,
      },
      secondary: {
        title: chrome.secondaryCardTitle,
        text: chrome.secondaryCardText,
        pill: chrome.secondaryCardPill,
        to: sectionKey === 'client'
          ? '/app/client/schedule'
          : sectionKey === 'admin'
            ? '/admin'
            : sectionKey === 'moderator'
              ? '/moderator/review-moderation'
              : undefined,
      },
    };
  }, [chrome, sectionKey, tutorRequestsSummary, tutorSubscriptionSummary]);
  const workspaceUser = getWorkspaceUser(session?.user, chrome);
  const canShowNotifications = sectionKey === 'client' || sectionKey === 'tutor';
  const notificationPanelState = getWorkspaceNotificationPanelState(notifications, notificationsError);
  const workspaceSearchPlaceholder =
    sectionKey === 'tutor' ? 'Поиск по ученикам и заявкам' : 'Поиск по кабинету';
  const handleNotificationOpen = () => {
    setNotificationsOpen((isOpen) => !isOpen);
  };
  const handleReadAllNotifications = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((items) => items.map((item) => ({ ...item, read: true })));
      setUnreadNotifications(0);
    } catch (error) {
      setNotificationsError(error instanceof Error ? error.message : 'Не удалось отметить уведомления прочитанными.');
    }
  };
  const handleNotificationAction = async (notification: WorkspaceNotification) => {
    if (!notification.read) {
      try {
        await markNotificationRead(notification.id);
        setNotifications((items) =>
          items.map((item) => item.id === notification.id ? { ...item, read: true } : item),
        );
        setUnreadNotifications((count) => Math.max(0, count - 1));
      } catch (error) {
        setNotificationsError(error instanceof Error ? error.message : 'Не удалось отметить уведомление прочитанным.');
        return;
      }
    }

    setNotificationsOpen(false);
    if (notification.actionUrl) {
      navigate(notification.actionUrl);
    }
  };
  const handleLogout = async () => {
    try {
      await logout(session?.refreshToken);
    } catch {
      // logout API failure is not blocking — session is cleared anyway
    } finally {
      clearAuthSession();
      navigate('/auth/sign-in', { replace: true });
    }
  };

  return (
    <BaseShell>
      <main className={cn('workspace-shell', `workspace-shell--${accent}`)}>
        <div className="workspace-glow workspace-glow--indigo" />
        <div className="workspace-glow workspace-glow--green" />

        <header className="workspace-topbar">
          <Link to="/" className="workspace-logo">
            <span className="workspace-logo__mark">T</span>
            <span>
              <strong>{brandName}</strong>
              <small>{chrome.subtitle}</small>
            </span>
          </Link>

          <button
            className="workspace-menu-toggle"
            type="button"
            aria-controls={`workspace-sidebar-${sectionKey}`}
            aria-expanded={navigationOpen}
            aria-label={navigationOpen ? 'Закрыть навигацию кабинета' : 'Открыть навигацию кабинета'}
            onClick={() => setNavigationOpen((isOpen) => !isOpen)}
          >
            <span aria-hidden="true">{navigationOpen ? '×' : '☰'}</span>
            Меню
          </button>

          <div className="workspace-context">
            <span className="workspace-context__dot" />
            <span>
              <small>{sectionKey === 'client' ? 'клиента' : section.title.toLowerCase()}</small>
              <strong>{activeRouteLabel}</strong>
            </span>
          </div>

          <label className="workspace-search">
            <span>⌕</span>
            <input
              id={`workspace-search-${sectionKey}`}
              name="workspace-search"
              aria-label="Поиск по кабинету"
              placeholder={workspaceSearchPlaceholder}
            />
          </label>

          <LinkButton to="/contacts" variant="ghost" className="workspace-topbar__support">
            Помощь
          </LinkButton>
          {canShowNotifications ? (
            <div className="workspace-notifications">
              <button
                className="workspace-topbar__notifications"
                type="button"
                onClick={handleNotificationOpen}
                aria-expanded={notificationsOpen}
              >
                <span>{getWorkspaceNotificationButtonLabel(unreadNotifications)}</span>
                {unreadNotifications > 0 ? <strong>{unreadNotifications}</strong> : null}
              </button>
              {notificationsOpen ? (
                <section className="workspace-notifications-panel" aria-label="Уведомления">
                  <div className="workspace-notifications-panel__header">
                    <span>
                      <strong>{notificationPanelState.title}</strong>
                      <small>{notificationPanelState.text}</small>
                    </span>
                    {notifications.length > 0 ? (
                      <button type="button" onClick={handleReadAllNotifications}>
                        Прочитать все
                      </button>
                    ) : null}
                  </div>
                  {notifications.length > 0 ? (
                    <div className="workspace-notifications-list">
                      {notifications.slice(0, 6).map((notification) => (
                        <button
                          key={notification.id}
                          type="button"
                          className={cn('workspace-notification', !notification.read && 'is-unread')}
                          onClick={() => void handleNotificationAction(notification)}
                        >
                          <span className="workspace-notification__dot" />
                          <span>
                            <strong>{notification.title}</strong>
                            <small>{notification.message}</small>
                            <em>{formatNotificationTime(notification.createdAt)}</em>
                          </span>
                        </button>
                      ))}
                    </div>
                  ) : (
                    <p className={cn('workspace-notifications-empty', `is-${notificationPanelState.tone}`)}>
                      {notificationPanelState.text}
                    </p>
                  )}
                </section>
              ) : null}
            </div>
          ) : null}

          <div className="workspace-user-card">
            <span className="workspace-user__avatar">{workspaceUser.initials}</span>
            <span>
              <strong>{workspaceUser.name}</strong>
              <small>{workspaceUser.role}</small>
            </span>
            <button className="workspace-logout" type="button" onClick={handleLogout}>
              Выйти
            </button>
          </div>
        </header>

        <div className="workspace-body">
          <aside
            id={`workspace-sidebar-${sectionKey}`}
            className={cn('workspace-sidebar', navigationOpen && 'is-open')}
          >
            <Link
              to={getWorkspaceProfileLinkTarget(sectionKey)}
              className="workspace-profile workspace-profile--link"
            >
              <span className="workspace-profile__avatar">{workspaceUser.initials}</span>
              <span>
                <strong>{workspaceUser.name}</strong>
                <small>{workspaceUser.role}</small>
              </span>
            </Link>
            <button className="workspace-sidebar__logout" type="button" onClick={handleLogout}>
              Выйти из аккаунта
            </button>

            <div className="workspace-sidebar__divider" />
            <p className="workspace-sidebar__label">{chrome.navTitle}</p>

            <nav className="workspace-nav" aria-label={section.title}>
              {orderedVisibleRoutes.map((route) => {
                if (sectionKey === 'tutor' && route.path === 'settings') {
                  return null;
                }

                const path = route.index ? basePath : `${basePath}/${route.path}`;
                const label = getWorkspaceRouteLabel(sectionKey, route);
                return (
                  <NavLink
                    key={label}
                    to={path}
                    end={route.index}
                    className={({ isActive }) => cn('workspace-nav__link', isActive && 'is-active')}
                  >
                    <span className="workspace-nav__dot" />
                    <strong>{label}</strong>
                    <span className="workspace-nav__bar" />
                  </NavLink>
                );
              })}
            </nav>

            <section className="workspace-sidebar-card workspace-sidebar-card--dark">
              <h2>{sidebarCards.primary.title}</h2>
              {sidebarCards.primary.meta ? <span>{sidebarCards.primary.meta}</span> : null}
              <p>{sidebarCards.primary.text}</p>
              {sidebarCards.primary.to && sidebarCards.primary.actionLabel ? (
                <Link to={sidebarCards.primary.to} className="workspace-sidebar-card__action">
                  {sidebarCards.primary.actionLabel}
                </Link>
              ) : sidebarCards.primary.actionLabel ? (
                <button type="button">{sidebarCards.primary.actionLabel}</button>
              ) : null}
            </section>

            <section className="workspace-sidebar-card">
              <h2>{sidebarCards.secondary.title}</h2>
              <p>{sidebarCards.secondary.text}</p>
              {sidebarCards.secondary.to && sidebarCards.secondary.pill ? (
                <Link to={sidebarCards.secondary.to} className="workspace-sidebar-card__pill">
                  {sidebarCards.secondary.pill}
                </Link>
              ) : sidebarCards.secondary.pill ? (
                <strong>{sidebarCards.secondary.pill}</strong>
              ) : null}
            </section>
          </aside>

          <section className="workspace-content">
            <Outlet />
          </section>
        </div>

        <footer className="workspace-footer">
          <strong>TutorSpace CRM</strong>
          <span className="workspace-footer__status">Статус системы: смотрите live-панели раздела</span>
          <Link to="/contacts">Поддержка</Link>
          <Link to="/faq">Документация</Link>
          <span>© 2026 TutorSpace</span>
        </footer>
      </main>
    </BaseShell>
  );
}
