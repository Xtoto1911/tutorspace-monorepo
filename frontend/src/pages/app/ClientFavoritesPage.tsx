import { useEffect, useState } from 'react';
import { Card, EmptyState, LinkButton, Button } from '../../components/primitives';
import { getClientFavoritesPage, removeClientFavorite } from '../../shared/api/client';
import type { ClientFavoritesPageData } from '../../shared/types';

type FavoriteTutor = ClientFavoritesPageData['tutors'][number];

function getInitials(name: string) {
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2);
}

export function ClientFavoritesPage() {
  const [page, setPage] = useState<ClientFavoritesPageData>({ tutors: [] });
  const [loading, setLoading] = useState(true);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [error, setError] = useState('');

  const loadFavorites = () => {
    setLoading(true);
    setError('');
    getClientFavoritesPage()
      .then(setPage)
      .catch(() => {
        setPage({ tutors: [] });
        setError('Не удалось загрузить избранное.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadFavorites();
  }, []);

  const handleRemove = async (tutor: FavoriteTutor) => {
    setRemovingId(tutor.id);

    try {
      await removeClientFavorite(tutor.id);
      setPage((current) => ({
        tutors: current.tutors.filter((item) => item.id !== tutor.id),
      }));
    } catch {
      setError('Не удалось удалить репетитора из избранного.');
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <div className="client-favorites">
      <header className="client-favorites__header">
        <div>
          <p className="client-favorites__eyebrow">CLIENT AREA</p>
          <h1>Избранные репетиторы</h1>
          <p>Сохранённые анкеты для быстрого возврата к выбору.</p>
        </div>

        <div className="client-favorites__actions">
          <LinkButton to="/catalog">В каталог</LinkButton>
        </div>
      </header>

      {loading ? <Card className="loading-panel">Загружаем избранное...</Card> : null}

      {!loading && error ? (
        <Card className="loading-panel">
          <p role="alert">{error}</p>
          <Button type="button" onClick={loadFavorites}>Повторить</Button>
        </Card>
      ) : null}

      {!loading && !error && page.tutors.length === 0 ? (
        <EmptyState
          title="В избранном пока нет репетиторов"
          text="Откройте каталог и сохраните тех, к кому хотите вернуться."
          action={<LinkButton to="/catalog">Перейти в каталог</LinkButton>}
        />
      ) : null}

      {!loading && !error && page.tutors.length > 0 ? (
        <section className="favorites-layout favorites-layout--simple" aria-label="Избранные репетиторы">
          {page.tutors.map((tutor) => (
            <Card key={tutor.id} className="favorite-tutor-card favorite-tutor-card--simple">
              <div className="favorite-tutor-card__avatar">{getInitials(tutor.name)}</div>
              <div className="favorite-tutor-card__identity">
                <h2>{tutor.name}</h2>
                <p className="favorite-tutor-card__subject">{tutor.subject}</p>
                <p className="favorite-tutor-card__meta">{tutor.meta}</p>
              </div>

              <div className="favorite-tutor-card__badges">
                <span className="favorite-tutor-card__rating">★ {tutor.rating}</span>
                <span className="favorite-tutor-card__price">{tutor.price}</span>
                <span>{tutor.savedAt}</span>
              </div>

              <div className="favorite-tutor-card__actions">
                <LinkButton to={`/tutors/${tutor.slug}`} variant="primary">
                  Открыть профиль
                </LinkButton>
                <Button
                  type="button"
                  variant="ghost"
                  disabled={removingId === tutor.id}
                  onClick={() => void handleRemove(tutor)}
                >
                  Убрать
                </Button>
              </div>
            </Card>
          ))}
        </section>
      ) : null}
    </div>
  );
}
