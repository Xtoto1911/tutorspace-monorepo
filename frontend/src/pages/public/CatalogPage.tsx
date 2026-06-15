import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import {
  Button,
  Card,
  EmptyState,
  FormField,
  LinkButton,
  SelectInput,
  SectionHeading,
  TextInput,
  TutorCard,
} from '../../components/primitives';
import { addClientFavorite, getClientFavoritesPage, removeClientFavorite } from '../../shared/api/client';
import { listSubjects, listTutors } from '../../shared/api/public';
import { readAuthSession } from '../../shared/auth/session';
import { formatCurrency } from '../../shared/lib/format';
import type { Subject, TutorFilters, TutorSummary } from '../../shared/types';

const defaultFilters: TutorFilters = {
  query: '',
  city: '',
  subject: '',
  minRating: undefined,
  maxPrice: undefined,
  onlineOnly: false,
};

export function CatalogPage() {
  const { subjectSlug } = useParams();
  const [searchParams] = useSearchParams();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [tutors, setTutors] = useState<TutorSummary[]>([]);
  const [favoriteTutorIds, setFavoriteTutorIds] = useState<Set<string>>(() => new Set());
  const [favoriteBusyId, setFavoriteBusyId] = useState<string | null>(null);
  const [filters, setFilters] = useState<TutorFilters>(() => ({
    ...defaultFilters,
    subject: subjectSlug ?? '',
    query: searchParams.get('query') ?? '',
    city: searchParams.get('city') ?? '',
    onlineOnly: searchParams.get('online') === '1',
  }));
  const [loading, setLoading] = useState(true);
  const [subjectsError, setSubjectsError] = useState('');
  const [tutorsError, setTutorsError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const canUseFavorites = readAuthSession()?.user.role === 'parent';

  useEffect(() => {
    let alive = true;
    setSubjectsError('');

    listSubjects()
      .then((subjectList) => {
        if (alive) {
          setSubjects(subjectList);
        }
      })
      .catch((error) => {
        if (alive) {
          setSubjects([]);
          setSubjectsError(error instanceof Error ? error.message : 'Не удалось загрузить предметы.');
        }
      });

    return () => {
      alive = false;
    };
  }, [reloadKey]);

  useEffect(() => {
    if (!canUseFavorites) {
      setFavoriteTutorIds(new Set());
      return;
    }

    let alive = true;
    getClientFavoritesPage()
      .then((page) => {
        if (alive) {
          setFavoriteTutorIds(new Set(page.tutors.map((tutor) => tutor.id)));
        }
      })
      .catch(() => {});

    return () => {
      alive = false;
    };
  }, [canUseFavorites]);

  useEffect(() => {
    setFilters((current) => ({
      ...current,
      subject: subjectSlug ?? '',
    }));
  }, [subjectSlug]);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setTutorsError('');

    listTutors(filters)
      .then((result) => {
        if (alive) {
          setTutors(result);
        }
      })
      .catch((error) => {
        if (alive) {
          setTutors([]);
          setTutorsError(error instanceof Error ? error.message : 'Не удалось загрузить каталог.');
        }
      })
      .finally(() => {
        if (alive) {
          setLoading(false);
        }
      });

    return () => {
      alive = false;
    };
  }, [filters, reloadKey]);

  const selectedSubject = subjects.find((item) => item.slug === filters.subject);
  const tutorsCountLabel = `${tutors.length} ${tutors.length === 1 ? 'репетитор' : tutors.length < 5 ? 'репетитора' : 'репетиторов'}`;

  const handleToggleFavorite = async (tutor: TutorSummary) => {
    if (!canUseFavorites || favoriteBusyId) {
      return;
    }

    const nextFavorite = !favoriteTutorIds.has(tutor.id);
    setFavoriteBusyId(tutor.id);

    try {
      if (nextFavorite) {
        await addClientFavorite(tutor.id);
      } else {
        await removeClientFavorite(tutor.id);
      }

      setFavoriteTutorIds((current) => {
        const next = new Set(current);
        if (nextFavorite) {
          next.add(tutor.id);
        } else {
          next.delete(tutor.id);
        }
        return next;
      });
    } finally {
      setFavoriteBusyId(null);
    }
  };

  return (
    <div className="page-stack">
      <section className="catalog-layout">
        <Card className="filters-panel">
          <SectionHeading eyebrow="Фильтры" title="Настроить подбор" description="Каталог обновляется по данным TutorSpace." />
          <div className="filters-panel__quiet-note">
            <strong>Начните с предмета</strong>
            <span>Затем уточните город, формат и комфортную ставку.</span>
          </div>

          <div className="filters-grid">
            <FormField label="Поиск">
              <TextInput
                name="query"
                value={filters.query ?? ''}
                onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))}
                placeholder="Имя, предмет, город"
              />
            </FormField>

            <FormField label="Город">
              <TextInput
                name="city"
                value={filters.city ?? ''}
                onChange={(event) => setFilters((current) => ({ ...current, city: event.target.value }))}
                placeholder="Москва"
              />
            </FormField>

            <FormField label="Рейтинг">
              <SelectInput
                name="minRating"
                value={filters.minRating ? String(filters.minRating) : ''}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    minRating: event.target.value ? Number(event.target.value) : undefined,
                  }))
                }
              >
                <option value="">Любой</option>
                <option value="4.5">4,5+</option>
                <option value="4.8">4,8+</option>
                <option value="4.9">4,9+</option>
              </SelectInput>
            </FormField>

            <FormField label="Максимальная ставка">
              <SelectInput
                name="maxPrice"
                value={filters.maxPrice ? String(filters.maxPrice) : ''}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    maxPrice: event.target.value ? Number(event.target.value) : undefined,
                  }))
                }
              >
                <option value="">Без ограничений</option>
                <option value="1500">до 1 500 ₽</option>
                <option value="2000">до 2 000 ₽</option>
                <option value="2500">до 2 500 ₽</option>
                <option value="3000">до 3 000 ₽</option>
              </SelectInput>
            </FormField>
          </div>

          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={filters.onlineOnly ?? false}
              onChange={(event) => setFilters((current) => ({ ...current, onlineOnly: event.target.checked }))}
            />
            <span>Показывать только онлайн-формат</span>
          </label>

          {selectedSubject ? (
            <div className="filters-panel__summary">
              <span>Выбран предмет</span>
              <strong>{selectedSubject.title}</strong>
              <p>{selectedSubject.description}</p>
            </div>
          ) : null}

          <div className="filters-panel__cta">
            <Button type="button" variant="secondary" onClick={() => setFilters(defaultFilters)}>
              Сбросить
            </Button>
            <LinkButton to="/how-it-works" variant="ghost">
              Как это работает
            </LinkButton>
          </div>
        </Card>

        <div className="catalog-results">
          <SectionHeading
            eyebrow="Результаты"
            title={loading ? 'Подбираем репетиторов' : tutorsCountLabel}
            description="Подборка обновляется после изменения фильтров."
          />

          <div className="subject-pills">
            <LinkButton to="/catalog" variant={!subjectSlug ? 'primary' : 'ghost'} className="subject-pill">
              Все предметы
            </LinkButton>
            {subjects.map((subject) => (
              <LinkButton
                key={subject.slug}
                to={`/catalog/subject/${subject.slug}`}
                variant={subject.slug === subjectSlug ? 'primary' : 'ghost'}
                className="subject-pill"
              >
                {subject.title}
              </LinkButton>
            ))}
          </div>

          {loading ? <Card className="loading-panel">Загружаем каталог...</Card> : null}

          {!loading && (subjectsError || tutorsError) ? (
            <EmptyState
              title="Каталог временно недоступен"
              text={tutorsError || subjectsError}
              action={<Button type="button" variant="secondary" onClick={() => setReloadKey((value) => value + 1)}>Повторить</Button>}
            />
          ) : null}

          {!loading && !subjectsError && !tutorsError && tutors.length === 0 ? (
            <EmptyState
              title="Ничего не найдено"
              text="Попробуйте сбросить фильтры или изменить предмет, город и рейтинг."
              action={<LinkButton to="/catalog">Сбросить всё</LinkButton>}
            />
          ) : null}

          <div className="tutor-grid">
            {tutors.map((tutor) => (
              <TutorCard
                key={tutor.id}
                tutor={tutor}
                favorite={favoriteTutorIds.has(tutor.id)}
                favoriteDisabled={favoriteBusyId === tutor.id}
                onToggleFavorite={canUseFavorites ? handleToggleFavorite : undefined}
              />
            ))}
          </div>

          <Card className="catalog-note">
            <div>
              <p className="section-eyebrow">Заявка</p>
              <strong>Выберите репетитора и отправьте запрос на удобное время.</strong>
            </div>
            <span>Ответ появится в личном кабинете после подтверждения.</span>
            <div>
              <small>ориентир</small>
              <strong>{formatCurrency(2000)}</strong>
            </div>
          </Card>
        </div>
      </section>
    </div>
  );
}
