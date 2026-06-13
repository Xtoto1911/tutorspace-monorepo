import { Card, LinkButton } from '../../components/primitives';

export function NotFoundPage() {
  return (
    <div className="not-found">
      <Card className="not-found__card">
        <p className="section-eyebrow">404</p>
        <h1>Страница не найдена</h1>
        <p>Путь либо еще не добавлен, либо уже переехал в другую роль. Вернемся туда, где можно продолжить задачу.</p>
        <div className="not-found__map" aria-label="Быстрые разделы">
          <span>Каталог</span>
          <span>Кабинет</span>
          <span>Поддержка</span>
        </div>
        <div className="not-found__actions">
          <LinkButton to="/" variant="primary">
            На главную
          </LinkButton>
          <LinkButton to="/catalog" variant="secondary">
            В каталог
          </LinkButton>
        </div>
      </Card>
    </div>
  );
}
