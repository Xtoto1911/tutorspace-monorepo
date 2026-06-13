import { useMemo } from 'react';
import { Badge, Card, LinkButton, SectionHeading } from '../../components/primitives';
import { roadmapSections } from '../../shared/config/roadmap';
import type { RoadmapRoute, RoadmapSection } from '../../shared/types';
import { cn } from '../../shared/lib/cn';

const sectionBasePaths: Record<RoadmapSection['key'], string> = {
  client: '/app/client',
  tutor: '/app/tutor',
  admin: '/admin',
  moderator: '/moderator',
};

const accentTone: Record<RoadmapSection['key'], 'indigo' | 'emerald' | 'amber' | 'slate'> = {
  client: 'indigo',
  tutor: 'emerald',
  admin: 'emerald',
  moderator: 'amber',
};

export function SectionPlaceholderPage({
  sectionKey,
  route,
}: {
  sectionKey: RoadmapSection['key'];
  route: RoadmapRoute;
}) {
  const section = roadmapSections.find((item) => item.key === sectionKey);

  const currentPath = useMemo(() => {
    const basePath = sectionBasePaths[sectionKey];
    return route.index ? basePath : `${basePath}/${route.path}`;
  }, [route.index, route.path, sectionKey]);

  if (!section) {
    return null;
  }

  return (
    <div className="page-stack">
      <section className="page-intro">
        <div>
          <Badge tone={accentTone[sectionKey]}>{section.title}</Badge>
          <h1>{route.title}</h1>
          <p>{route.description}</p>
        </div>
        <div className="page-intro__actions">
          <LinkButton to={sectionBasePaths[sectionKey]} variant="secondary">
            Корень секции
          </LinkButton>
          <LinkButton to="/" variant="ghost">
            На public
          </LinkButton>
        </div>
      </section>

      <section className="placeholder-grid">
        <Card className="placeholder-card placeholder-card--hero">
          <p className="section-eyebrow">Текущий маршрут</p>
          <h2>{currentPath}</h2>
          <p>
            Это заглушка маршрута. Макеты уже ждут своего backend: когда придут реальные данные, достаточно будет
            заменить содержимое страницы, не меняя URL.
          </p>
        </Card>

        <Card className="placeholder-card">
          <SectionHeading eyebrow="API stub" title="Будущие запросы" />
          <div className="stub-list">
            {route.api.map((api) => (
              <article key={`${api.method}-${api.path}`} className={cn('stub-item', `stub-item--${api.method.toLowerCase()}`)}>
                <strong>
                  {api.method} {api.path}
                </strong>
                <p>{api.description}</p>
              </article>
            ))}
          </div>
        </Card>

        <Card className="placeholder-card">
          <SectionHeading eyebrow="План секции" title="Все маршруты этой области" />
          <div className="stub-list">
            {section.routes.map((item) => {
              const path = item.index ? sectionBasePaths[sectionKey] : `${sectionBasePaths[sectionKey]}/${item.path}`;
              return (
                <article key={item.title} className={cn('stub-item', path === currentPath && 'stub-item--active')}>
                  <strong>{item.title}</strong>
                  <p>{path}</p>
                </article>
              );
            })}
          </div>
        </Card>
      </section>
    </div>
  );
}
