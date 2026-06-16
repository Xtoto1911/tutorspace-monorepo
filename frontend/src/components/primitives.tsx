import {
  forwardRef,
  useState,
  type ButtonHTMLAttributes,
  type ComponentProps,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type SVGProps,
  type TextareaHTMLAttributes,
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
  error,
  valid,
  children,
}: {
  label: string;
  hint?: string;
  error?: string;
  valid?: boolean;
  children: ReactNode;
}) {
  const modifiers: string[] = [];
  if (error) modifiers.push('is-invalid');
  if (valid && !error) modifiers.push('is-valid');
  return (
    <label className={cn('form-field', modifiers.length > 0 ? modifiers.join(' ') : undefined)}>
      <span className="form-label">{label}</span>
      {children}
      <span className={error ? 'form-error' : 'form-hint'} aria-live="polite">
        {error || hint || ''}
      </span>
    </label>
  );
}

export function TextInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn('control', props.className)} />;
}

export function TextArea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={cn('control', 'control-textarea', props.className)} />;
}

export function SelectInput(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={cn('control', props.className)} />;
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

function EyeIcon({ open, ...props }: { open: boolean } & SVGProps<SVGSVGElement>) {
  if (open) {
    return (
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
      <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-6.5 0-10-7-10-7a18.45 18.45 0 0 1 4.22-5.39" />
      <path d="M9.9 4.24A10.94 10.94 0 0 1 12 4c6.5 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19" />
      <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
      <line x1="2" y1="2" x2="22" y2="22" />
    </svg>
  );
}

export const PasswordInput = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function PasswordInput({ className, type, ...props }, ref) {
    const [visible, setVisible] = useState(false);
    return (
      <span className={cn('password-input', className)}>
        <input
          {...props}
          ref={ref}
          type={visible ? 'text' : 'password'}
          className="control"
        />
        <button
          type="button"
          className="password-input__toggle"
          aria-label={visible ? 'Скрыть пароль' : 'Показать пароль'}
          aria-pressed={visible}
          onClick={() => setVisible((value) => !value)}
        >
          <EyeIcon open={visible} />
        </button>
      </span>
    );
  }
);
