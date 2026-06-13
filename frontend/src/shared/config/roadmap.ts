import type { RoadmapSection } from '../types';

export const roadmapSections: RoadmapSection[] = [
  {
    key: 'client',
    title: 'Кабинет клиента',
    summary: 'Маршруты для заявок, расписания, сообщений и избранного.',
    accent: '#4f46e5',
    routes: [
      {
        index: true,
        title: 'Обзор клиента',
        description: 'Сводка по ближайшим занятиям, активным заявкам и оплатам.',
        api: [
          { method: 'GET', path: '/api/client/dashboard', description: 'Получить дашборд клиента' },
          { method: 'GET', path: '/api/client/activity', description: 'Получить ленту последних действий' },
        ],
      },
      {
        path: 'applications',
        title: 'Заявки',
        description: 'Список заявок на занятия и история их статусов.',
        api: [
          { method: 'GET', path: '/api/client/applications', description: 'Получить заявки клиента' },
          { method: 'PATCH', path: '/api/client/applications/{id}/cancel', description: 'Отменить заявку' },
        ],
      },
      {
        path: 'schedule',
        title: 'Расписание',
        description: 'Календарь занятий, переносы и подтверждения.',
        api: [{ method: 'GET', path: '/api/client/schedule', description: 'Получить расписание клиента' }],
      },
      {
        path: 'messages',
        title: 'Сообщения',
        description: 'Диалоги с репетиторами и системные уведомления.',
        api: [
          { method: 'GET', path: '/api/client/messages', description: 'Получить список диалогов' },
          { method: 'POST', path: '/api/client/messages/:threadId', description: 'Отправить сообщение' },
        ],
      },
      {
        path: 'favorites',
        title: 'Избранное',
        description: 'Сохраненные репетиторы и быстрый повторный контакт.',
        api: [
          { method: 'GET', path: '/api/client/favorites', description: 'Получить избранных репетиторов' },
          { method: 'POST', path: '/api/client/favorites/:tutorId', description: 'Добавить или удалить из избранного' },
        ],
      },
      {
        path: 'history',
        title: 'История',
        description: 'Завершенные занятия, оплаты и архив действий.',
        api: [{ method: 'GET', path: '/api/client/history', description: 'Получить историю клиента' }],
      },
      {
        path: 'settings',
        title: 'Настройки',
        description: 'Профиль, приватность, уведомления и способы связи.',
        api: [
          { method: 'GET', path: '/api/client/settings', description: 'Получить настройки клиента' },
          { method: 'PATCH', path: '/api/client/settings', description: 'Сохранить настройки клиента' },
        ],
      },
    ],
  },
  {
    key: 'tutor',
    title: 'Кабинет репетитора',
    summary: 'Маршруты для заявок, календаря, учеников и профиля.',
    accent: '#059669',
    routes: [
      {
        index: true,
        title: 'Обзор репетитора',
        description: 'Заявки, предстоящие уроки, загрузка и быстрые действия.',
        api: [
          { method: 'GET', path: '/api/tutor/dashboard', description: 'Получить дашборд репетитора' },
          { method: 'GET', path: '/api/tutor/notifications', description: 'Получить уведомления репетитора' },
        ],
      },
      {
        path: 'requests',
        title: 'Заявки',
        description: 'Очередь заявок от клиентов и решения по ним.',
        api: [
          { method: 'GET', path: '/api/tutor/requests', description: 'Получить заявки репетитора' },
          { method: 'PATCH', path: '/api/tutor/requests/{id}/accept', description: 'Принять заявку' },
          { method: 'PATCH', path: '/api/tutor/requests/{id}/reject', description: 'Отклонить заявку' },
        ],
      },
      {
        path: 'calendar',
        title: 'Календарь',
        description: 'Занятия, переносы и свободные слоты.',
        api: [
          { method: 'GET', path: '/api/tutor/calendar', description: 'Получить календарь' },
          { method: 'POST', path: '/api/tutor/lessons', description: 'Создать занятие' },
        ],
      },
      {
        path: 'students',
        title: 'Ученики',
        description: 'Карточки учеников, заметки и прогресс.',
        api: [{ method: 'GET', path: '/api/tutor/students', description: 'Получить список учеников' }],
      },
      {
        path: 'messages',
        title: 'Сообщения',
        description: 'Общение с клиентами и автоответы.',
        api: [
          { method: 'GET', path: '/api/tutor/messages', description: 'Получить диалоги репетитора' },
          { method: 'POST', path: '/api/tutor/messages/:threadId', description: 'Отправить сообщение' },
        ],
      },
      {
        path: 'profile',
        title: 'Профиль',
        description: 'Публичная анкета, предметы, цены и описание.',
        api: [
          { method: 'GET', path: '/api/tutor/profile', description: 'Получить профиль репетитора' },
          { method: 'PATCH', path: '/api/tutor/profile', description: 'Сохранить профиль репетитора' },
        ],
      },
      {
        path: 'settings',
        title: 'Настройки',
        description: 'Контакты, приватность, уведомления и доступность.',
        api: [
          { method: 'GET', path: '/api/tutor/settings', description: 'Получить настройки репетитора' },
          { method: 'PATCH', path: '/api/tutor/settings', description: 'Обновить настройки репетитора' },
        ],
      },
      {
        path: 'subscription',
        title: 'Подписка',
        description: 'Тариф, продление и платежные статусы.',
        api: [
          { method: 'GET', path: '/api/tutor/subscription', description: 'Получить тариф и подписку' },
          { method: 'POST', path: '/api/tutor/subscription/checkout', description: 'Оформить продление' },
        ],
      },
    ],
  },
  {
    key: 'admin',
    title: 'Админ-панель',
    summary: 'Маршруты для пользователей, статистики, подписок и аудита.',
    accent: '#0f766e',
    routes: [
      {
        index: true,
        title: 'Обзор администратора',
        description: 'Ключевые показатели, здоровье платформы и быстрые переходы.',
        api: [
          { method: 'GET', path: '/api/admin/dashboard', description: 'Получить сводку админа' },
          { method: 'GET', path: '/api/admin/alerts', description: 'Получить системные алерты' },
        ],
      },
      {
        path: 'users',
        title: 'Пользователи',
        description: 'Поиск, блокировки, роли и статусы учетных записей.',
        api: [
          { method: 'GET', path: '/api/admin/users', description: 'Получить список пользователей' },
          { method: 'PATCH', path: '/api/admin/users/:id', description: 'Обновить профиль пользователя' },
        ],
      },
      {
        path: 'site-stats',
        title: 'Статистика',
        description: 'Метрики платформы, конверсии и динамика использования.',
        api: [{ method: 'GET', path: '/api/admin/site-stats', description: 'Получить метрики платформы' }],
      },
      {
        path: 'subscriptions',
        title: 'Подписки',
        description: 'Тарифные планы, продления и платежные статусы.',
        api: [
          { method: 'GET', path: '/api/admin/subscriptions', description: 'Получить список подписок' },
          { method: 'PATCH', path: '/api/admin/subscriptions/:id', description: 'Изменить подписку' },
        ],
      },
      {
        path: 'audit',
        title: 'Аудит',
        description: 'Журнал административных действий и проверок.',
        api: [{ method: 'GET', path: '/api/admin/audit', description: 'Получить журнал аудита' }],
      },
      {
        path: 'settings',
        title: 'Настройки',
        description: 'Системные ограничения, роли и параметры платформы.',
        api: [{ method: 'PATCH', path: '/api/admin/settings', description: 'Обновить системные настройки' }],
      },
    ],
  },
  {
    key: 'moderator',
    title: 'Модерация',
    summary: 'Маршруты для жалоб, отзывов и проверок контента.',
    accent: '#b45309',
    routes: [
      {
        index: true,
        title: 'Обзор модератора',
        description: 'Новые жалобы, очередь проверки и решенные обращения.',
        api: [
          { method: 'GET', path: '/api/moderator/dashboard', description: 'Получить обзор модерации' },
          { method: 'GET', path: '/api/moderator/queue', description: 'Получить очередь на проверку' },
        ],
      },
      {
        path: 'complaints',
        title: 'Жалобы',
        description: 'Обработка жалоб на отзывы и профили.',
        api: [
          { method: 'GET', path: '/api/moderator/complaints', description: 'Получить список жалоб' },
          { method: 'POST', path: '/api/moderator/complaints/:id/resolve', description: 'Решить жалобу' },
        ],
      },
      {
        path: 'review-moderation',
        title: 'Модерация отзывов',
        description: 'Проверка отзывов перед публикацией и реакция на нарушения.',
        api: [
          { method: 'GET', path: '/api/moderator/review-moderation', description: 'Получить отзывы на модерацию' },
          { method: 'POST', path: '/api/moderator/review-moderation/:id/approve', description: 'Одобрить отзыв' },
          { method: 'POST', path: '/api/moderator/review-moderation/:id/reject', description: 'Отклонить отзыв' },
        ],
      },
    ],
  },
];
