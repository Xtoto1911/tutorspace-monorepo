import type {
  ButtonHTMLAttributes,
  ComponentProps,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react';
import { Link } from 'react-router-dom';
import { cn } from '../shared/lib/cn';
import { formatCurrency, formatRating, formatShortDate } from '../shared/lib/format';
import type { TutorSummary } from '../shared/types';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'soft';

export function Button({
  className,
  variant = 'primary',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  return <button className={cn('btn', `btn-${variant}`, className)} {...props} />;
}

export function LinkButton({
  className,
  variant = 'primary',
  ...props
}: ComponentProps<typeof Link> & { variant?: ButtonVariant }) {
  return <Link className={cn('btn', `btn-${variant}`, className)} {...props} />;
}

export function Card({ className, children, ...props }: ComponentProps<'section'>) {
  return (
    <section className={cn('card', className)} {...props}>
      {children}
    </section>
  );
}

export function Badge({
  className,
  tone = 'slate',
  children,
}: {
  className?: string;
  tone?: 'slate' | 'indigo' | 'emerald' | 'amber' | 'rose';
  children: ReactNode;
}) {
  return <span className={cn('badge', `badge-${tone}`, className)}>{children}</span>;
}

export function SectionHeading({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="section-heading">
      <div>
        {eyebrow ? <p className="section-eyebrow">{eyebrow}</p> : null}
        <h2>{title}</h2>
        {description ? <p className="section-description">{description}</p> : null}
      </div>
      {actions ? <div className="section-actions">{actions}</div> : null}
    </div>
  );
}

export function FormField({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="form-field">
      <span className="form-label">{label}</span>
      {children}
      {hint ? <span className="form-hint">{hint}</span> : null}
    </label>
  );
}

export function TextInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn('control', props.className)} {...props} />;
}

export function TextArea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn('control', 'control-textarea', props.className)} {...props} />;
}

export function SelectInput(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={cn('control', props.className)} {...props} />;
}

export function MetricCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: 'indigo' | 'emerald' | 'amber';
}) {
  return (
    <Card className={cn('metric-card', `metric-card-${tone}`)}>
      <p className="metric-label">{label}</p>
      <strong className="metric-value">{value}</strong>
    </Card>
  );
}

export function TutorCard({
  tutor,
  favorite = false,
  favoriteDisabled = false,
  onToggleFavorite,
}: {
  tutor: TutorSummary;
  favorite?: boolean;
  favoriteDisabled?: boolean;
  onToggleFavorite?: (tutor: TutorSummary) => void;
}) {
  return (
    <Card className="tutor-card">
      <div className="tutor-card__top">
        <div className={cn('tutor-avatar', 'tutor-avatar--card', tutor.avatarTone)}>
          <span>{tutor.name.split(' ').map((part) => part[0]).join('').slice(0, 2)}</span>
        </div>
        <div className="tutor-card__identity">
          <div className="tutor-card__title-row">
            <h3>{tutor.name}</h3>
            {tutor.verified ? <Badge tone="emerald">Проверен</Badge> : null}
          </div>
          <p className="tutor-card__city">
            {tutor.city} · {tutor.experienceYears} лет опыта · свободно {tutor.nextAvailable.toLowerCase()}
          </p>
        </div>
      </div>

      <div className="tutor-card__badges">
        <Badge tone="indigo">
          {formatRating(tutor.rating)} · {tutor.reviewCount} отзывов
        </Badge>
      </div>

      <p className="tutor-card__meta">
        {tutor.tags.slice(0, 2).join(' · ')} · {tutor.subjects.slice(0, 2).join(' · ')}
      </p>

      <div className="tutor-card__subjects">
        {tutor.subjects.slice(0, 2).map((subject) => (
          <Badge key={subject} tone="slate">
            {subject}
          </Badge>
        ))}
        {tutor.tags.slice(0, 2).map((tag) => (
          <Badge key={tag} tone="slate">
            {tag}
          </Badge>
        ))}
      </div>

      <p className="tutor-card__about">{tutor.about}</p>

      <div className="tutor-card__footer">
        <div className="tutor-card__price">
          <strong>{formatCurrency(tutor.hourlyRate)}</strong>
          <span>за 60 минут</span>
        </div>
        <div className="tutor-card__actions">
          {onToggleFavorite ? (
            <Button
              type="button"
              variant={favorite ? 'secondary' : 'ghost'}
              className="favorite-toggle"
              aria-pressed={favorite}
              aria-label={favorite ? `Убрать ${tutor.name} из избранного` : `Добавить ${tutor.name} в избранное`}
              disabled={favoriteDisabled}
              onClick={() => onToggleFavorite(tutor)}
            >
              {favorite ? '♥' : '♡'}
            </Button>
          ) : null}
          <LinkButton to={`/tutors/${tutor.slug}`} variant="secondary">
            Открыть профиль
          </LinkButton>
        </div>
      </div>
    </Card>
  );
}

export function EmptyState({
  title,
  text,
  action,
}: {
  title: string;
  text: string;
  action?: ReactNode;
}) {
  return (
    <Card className="empty-state">
      <div>
        <h3>{title}</h3>
        <p>{text}</p>
      </div>
      {action ? <div>{action}</div> : null}
    </Card>
  );
}

export function ReviewCard({
  id,
  author,
  date,
  rating,
  text,
  lesson,
  onReport,
  reportStatus,
}: {
  id: string;
  author: string;
  date: string;
  rating: number;
  text: string;
  lesson: string;
  onReport?: (reviewId: string) => void;
  reportStatus?: string;
}) {
  return (
    <Card className="review-card">
      <div className="review-card__header">
        <div>
          <strong>{author}</strong>
          <p>{lesson}</p>
        </div>
        <Badge tone="amber">{formatRating(rating)}</Badge>
      </div>
      <p className="review-card__text">{text}</p>
      <div className="review-card__footer">
        <span className="review-card__date">{formatShortDate(date)}</span>
        {onReport ? (
          <button className="review-card__report" type="button" onClick={() => onReport(id)}>
            Пожаловаться
          </button>
        ) : null}
      </div>
      {reportStatus ? <span className="review-card__status">{reportStatus}</span> : null}
    </Card>
  );
}
