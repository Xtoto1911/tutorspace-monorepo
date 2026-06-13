import type { FaqItem, HomeContent } from '../types';

export const brandName = 'TutorSpace';
export const brandTagline = 'Платформа для подбора репетиторов и управления уроками';

export const publicNavigation = [
  { label: 'Главная', to: '/' },
  { label: 'Каталог', to: '/catalog' },
  { label: 'Как это работает', to: '/how-it-works' },
];

export const publicFooterColumns = [
  {
    title: 'Сервис',
    links: [
      { label: 'Каталог', to: '/catalog' },
      { label: 'Как это работает', to: '/how-it-works' },
      { label: 'Для репетиторов', to: '/auth/sign-up/tutor' },
      { label: 'Безопасность', to: '/privacy' },
    ],
  },
  {
    title: 'Кабинет',
    links: [
      { label: 'Мой раздел', to: '/app/client' },
      { label: 'Расписание', to: '/app/client/schedule' },
      { label: 'Сообщения', to: '/app/client/messages' },
      { label: 'Тарифы', to: '/app/tutor/subscription' },
    ],
  },
  {
    title: 'Контакты',
    links: [
      { label: 'support@tutorspace.ru', to: '/contacts' },
      { label: 'Москва, онлайн по всей России', to: '/contacts' },
      { label: 'Помощь модератора 08:00-21:00', to: '/contacts' },
    ],
  },
];

export const homeContent: HomeContent = {
  metrics: [
    { label: 'репетиторов', value: '1200+', tone: 'indigo' },
    { label: 'средний рейтинг', value: '4.8', tone: 'emerald' },
    { label: 'до первой заявки', value: '15 мин', tone: 'amber' },
  ],
  process: [
    {
      title: 'Выберите преподавателя',
      text: 'Сравните опыт, цену, отзывы и формат занятий.',
    },
    {
      title: 'Отправьте заявку',
      text: 'Репетитор увидит запрос и подберет удобное время.',
    },
    {
      title: 'Следите за статусом',
      text: 'Все заявки, занятия и уведомления собраны в кабинете.',
    },
  ],
  testimonials: [
    {
      author: 'Елена',
      role: 'мама ученика 8 класса',
      text: 'Удобно, что можно сразу понять, кто ведет занятия, сколько стоит урок и когда есть свободное окно.',
    },
    {
      author: 'Михаил',
      role: 'ученик 11 класса',
      text: 'Я быстро выбрал преподавателя по рейтингу и отзывам, а дальше все было уже очень прозрачно.',
    },
  ],
  faq: [
    {
      question: 'Как выбрать репетитора?',
      answer: 'Смотрите на рейтинг, опыт, предметы, стоимость и свободные слоты в расписании.',
    },
    {
      question: 'Можно ли зарегистрироваться как репетитор?',
      answer: 'Да, в форме регистрации можно выбрать роль репетитора и сразу заполнить профиль.',
    },
    {
      question: 'Как работает заявка на занятие?',
      answer: 'Клиент отправляет заявку, репетитор ее принимает или отклоняет, после чего создается занятие.',
    },
  ],
};

export const additionalFaq: FaqItem[] = [
  {
    question: 'Как проходят оплаты?',
    answer: 'TutorSpace не принимает оплату за уроки и не выступает гарантом расчетов. Репетиторы оплачивают только CRM-подписку.',
  },
  {
    question: 'Можно ли работать онлайн и офлайн?',
    answer: 'Да, репетитор указывает доступные форматы в профиле и согласует детали занятия с клиентом.',
  },
  {
    question: 'Что будет в личном кабинете клиента?',
    answer: 'Заявки, расписание, сообщения, избранное, история и настройки профиля.',
  },
];
