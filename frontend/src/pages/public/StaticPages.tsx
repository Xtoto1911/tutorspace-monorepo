import { Card, LinkButton, SectionHeading, Badge } from '../../components/primitives';
import { additionalFaq, homeContent } from '../../shared/config/site';

const trustSignals = ['Без гаранта оплаты уроков', 'Заявки в личном кабинете', 'Модерация отзывов'];

export function HowItWorksPage() {
  return (
    <div className="page-stack">
      <section className="page-intro">
        <div>
          <Badge tone="indigo">Как это работает</Badge>
          <h1>Три шага от поиска до первого занятия</h1>
          <p>Маршрут остается коротким: выбрать преподавателя, отправить заявку, продолжить общение в кабинете.</p>
          <div className="page-intro__proof">
            {trustSignals.map((signal) => (
              <span key={signal}>{signal}</span>
            ))}
          </div>
        </div>
        <div className="public-story-card">
          <span>Средний путь</span>
          <strong>15 минут до первой заявки</strong>
          <p>Без лишних шагов, но с понятным переходом в CRM.</p>
        </div>
      </section>

      <div className="step-grid">
        {homeContent.process.map((step, index) => (
          <Card key={step.title} className="step-card">
            <span className="step-card__index">0{index + 1}</span>
            <h3>{step.title}</h3>
            <p>{step.text}</p>
          </Card>
        ))}
      </div>

      <Card className="info-card">
        <SectionHeading eyebrow="Следующий этап" title="После заявки" />
        <p>
          Клиент увидит заявки, расписание и переписку, а репетитор получит календарь,
          студентов и статистику.
        </p>
        <LinkButton to="/catalog" variant="ghost">
          Перейти в каталог
        </LinkButton>
      </Card>
    </div>
  );
}

export function AboutPage() {
  return (
    <div className="page-stack">
      <section className="page-intro">
        <div>
          <Badge tone="emerald">О проекте</Badge>
          <h1>TutorSpace: площадка для прозрачного подбора репетиторов.</h1>
          <p>
            Мы сразу разводим public, app, tutor, admin и moderator области, чтобы дизайн и backend могли
            расти без конфликтов.
          </p>
        </div>
        <div className="public-story-card public-story-card--clay">
          <span>Что важно</span>
          <strong>Спокойный выбор вместо давления</strong>
          <p>Каталог помогает сравнить, а CRM помогает не потерять договоренности.</p>
        </div>
      </section>

      <div className="info-grid">
        <Card className="info-card">
          <SectionHeading eyebrow="Принципы" title="Чем хороша эта структура" />
          <ul className="bullet-list">
            <li>Разные роли получают свои маршруты и оболочки.</li>
            <li>Каталог и профили уже работают через отдельные API stubs.</li>
            <li>Переход на живой backend не ломает public-сцену.</li>
          </ul>
        </Card>
        <Card className="info-card">
          <SectionHeading eyebrow="Сценарии" title="Что уже заложено в UX" />
          <ul className="bullet-list">
            <li>Каталог и карточка репетитора.</li>
            <li>Вход и две ветки регистрации.</li>
            <li>Будущие личные кабинеты и админ-панели.</li>
          </ul>
        </Card>
      </div>
    </div>
  );
}

export function FaqPage() {
  return (
    <div className="page-stack">
      <section className="page-intro">
        <div>
          <Badge tone="amber">FAQ</Badge>
          <h1>Вопросы, которые обычно закрывают на этапе выбора</h1>
          <p>Короткие ответы про поиск, заявки, коммуникацию, подписки и модерацию.</p>
        </div>
      </section>

      <div className="faq-list faq-list--wide">
        {[...homeContent.faq, ...additionalFaq].map((item) => (
          <Card key={item.question} className="faq-item">
            <strong>{item.question}</strong>
            <p>{item.answer}</p>
          </Card>
        ))}
      </div>
    </div>
  );
}

export function ContactsPage() {
  return (
    <div className="page-stack">
      <section className="page-intro">
        <div>
          <Badge tone="indigo">Контакты</Badge>
          <h1>Если нужна помощь по платформе, вот рабочие каналы</h1>
          <p>Пишите по вопросам аккаунта, заявок, профиля преподавателя, отзывов и модерации.</p>
        </div>
        <div className="public-story-card">
          <span>Поддержка</span>
          <strong>10:00 - 19:00</strong>
          <p>Сначала отвечаем на вопросы, которые блокируют учебный процесс.</p>
        </div>
      </section>

      <div className="info-grid">
        <Card className="info-card">
          <SectionHeading eyebrow="Поддержка" title="Связаться с командой" />
          <p>support@tutorspace.ru</p>
          <p>Telegram: @tutorspace_support</p>
          <p>Время ответа: 10:00 - 19:00</p>
        </Card>
        <Card className="info-card">
          <SectionHeading eyebrow="Для партнеров" title="Письмо о сотрудничестве" />
          <p>partners@tutorspace.ru</p>
          <p>Интеграции, методические программы и пилоты для школ.</p>
        </Card>
      </div>
    </div>
  );
}

export function PrivacyPage() {
  return (
    <div className="page-stack">
      <section className="page-intro">
        <div>
          <Badge tone="slate">Политика</Badge>
          <h1>Политика конфиденциальности</h1>
          <p>Как TutorSpace хранит данные аккаунта, заявок, профилей и коммуникации.</p>
        </div>
      </section>
      <Card className="article-card">
        <p>
          Мы собираем только те данные, которые нужны для регистрации, поиска репетитора и работы с заявками.
        </p>
        <p>
          В будущем этот текст будет связан с backend-юридическими страницами, но уже сейчас route готов.
        </p>
        <p>
          Доступ к данным ограничивается ролью пользователя, а видимость профиля можно настраивать в личном
          кабинете.
        </p>
      </Card>
    </div>
  );
}

export function TermsPage() {
  return (
    <div className="page-stack">
      <section className="page-intro">
        <div>
          <Badge tone="slate">Правила</Badge>
          <h1>Пользовательское соглашение</h1>
          <p>Правила использования каталога, кабинетов, модерации и подписок для CRM-возможностей.</p>
        </div>
      </section>
      <Card className="article-card">
        <p>Платформа используется для подбора репетиторов, общения и организации занятий.</p>
        <p>Роли, подписки и модерация уже учтены в роутинге, чтобы позже не пришлось менять URL.</p>
        <p>Пользователь обязуется не злоупотреблять сервисом, а репетитор - поддерживать актуальность анкеты.</p>
      </Card>
    </div>
  );
}
