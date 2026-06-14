import { useEffect, useState } from 'react';
import { Button, LinkButton } from '../../components/primitives';
import { getClientSchedulePage, requestClientLessonReschedule } from '../../shared/api/client';
import { cn } from '../../shared/lib/cn';
import type { ClientSchedulePageData } from '../../shared/types';
import {
  getClientRescheduleSuccessMessage,
  getClientScheduleWeekRange,
  getClientScheduleMessageLink,
  getClientSchedulePrimaryLink,
  getClientScheduleUpcomingLessons,
  shiftClientScheduleWeekStart,
} from './client-schedule-helpers';

export function ClientSchedulePage() {
  const [selectedWeekStart, setSelectedWeekStart] = useState(() => getClientScheduleWeekRange(new Date()).from);
  const [pageData, setPageData] = useState<ClientSchedulePageData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [rescheduleNotice, setRescheduleNotice] = useState<string | null>(null);

  async function loadPage(weekStart = selectedWeekStart) {
    setIsLoading(true);
    setError(null);

    try {
      const range = getClientScheduleWeekRange(new Date(`${weekStart}T12:00:00`));
      setPageData(await getClientSchedulePage(range.from, range.to));
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить расписание.');
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadPage(selectedWeekStart);
  }, [selectedWeekStart]);

  async function handleReschedule(lessonId: string) {
    setIsSubmitting(true);
    setError(null);
    setRescheduleNotice(null);
    const lessonTime = upcomingLessons.find((lesson) => lesson.lessonId === lessonId)?.time ?? 'выбранного времени';

    try {
      await requestClientLessonReschedule(lessonId, { message: 'Нужен перенос занятия.' });
      await loadPage();
      setRescheduleNotice(getClientRescheduleSuccessMessage(lessonTime));
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Не удалось отправить запрос на перенос.');
    } finally {
      setIsSubmitting(false);
    }
  }

  const upcomingLessons = pageData ? getClientScheduleUpcomingLessons(pageData) : [];
  const nextLesson = upcomingLessons[0] ?? null;
  const primaryLessonLink = nextLesson ? getClientSchedulePrimaryLink(nextLesson) : '/app/client/applications';
  const primaryMessageLink = nextLesson ? getClientScheduleMessageLink(nextLesson) : null;
  const selectedWeekIsEmpty = pageData ? pageData.events.length === 0 : false;
  const hasScheduleData = pageData ? !pageData.emptyState.show : false;
  const currentWeekStart = getClientScheduleWeekRange(new Date()).from;

  if (isLoading && !pageData) {
    return (
      <div className="client-schedule">
        <section className="schedule-loading-state" aria-live="polite">
          <h2>Загружаем расписание</h2>
          <p>Подтягиваем ближайшие уроки, переносы и доступные действия.</p>
        </section>
      </div>
    );
  }

  if (!pageData) {
    return (
      <div className="client-schedule">
        <p role="alert">{error || 'Расписание недоступно.'}</p>
        <Button type="button" onClick={() => void loadPage()}>Повторить</Button>
      </div>
    );
  }

  return (
    <div className="client-schedule">
      <header className="client-schedule__header">
        <div>
          <p className="client-schedule__eyebrow">CLIENT AREA</p>
          <h1>Расписание клиента</h1>
          <p>Календарь занятий, ближайшие уроки и запросы на перенос из live CRM-данных.</p>
        </div>

        <div className="client-schedule__actions">
          <Button
            variant="ghost"
            className="schedule-reschedule-action"
            disabled={!nextLesson?.canReschedule || isSubmitting || isLoading}
            onClick={() => {
              if (nextLesson) {
                void handleReschedule(nextLesson.lessonId);
              }
            }}
          >
            {isSubmitting ? 'Отправляем...' : 'Запросить перенос'}
          </Button>
          <LinkButton to={primaryLessonLink}>{nextLesson ? 'Открыть урок' : 'К заявкам'}</LinkButton>
        </div>
      </header>

      <>
          {error ? (
            <div className="schedule-inline-error" role="alert">
              {error}
            </div>
          ) : null}

          {rescheduleNotice ? (
            <div className="schedule-inline-status" role="status">
              {rescheduleNotice}
            </div>
          ) : null}

          <section className="schedule-metrics" aria-label="Сводка расписания">
            {pageData.metrics.map((metric) => (
              <article key={metric.label} className={cn('schedule-metric', `schedule-metric--${metric.tone}`)}>
                <span className="schedule-metric__icon">{metric.icon}</span>
                <strong>{metric.value}</strong>
                <span>
                  {metric.label} / {metric.caption}
                </span>
              </article>
            ))}
          </section>

          <section className="schedule-toolbar" aria-label="Управление календарем">
            <div className="schedule-view-switch" aria-label="Текущий режим календаря">
              <span className="is-active">Неделя</span>
              <span>Live CRM</span>
            </div>

            <div className="schedule-period">
              <Button
                variant="ghost"
                className="schedule-period__nav"
                aria-label="Предыдущая неделя"
                disabled={isLoading || isSubmitting}
                onClick={() => {
                  setSelectedWeekStart((weekStart) => shiftClientScheduleWeekStart(weekStart, -1));
                }}
              >
                ←
              </Button>
              <div className="schedule-period__copy">
                <span className="schedule-period__caption">Выбранная неделя</span>
                <strong>{pageData.period.label}</strong>
                <span>{pageData.period.from && pageData.period.to ? `${pageData.period.from} - ${pageData.period.to}` : 'данные из CRM'}</span>
                <button
                  className="schedule-period__today"
                  type="button"
                  disabled={selectedWeekStart === currentWeekStart || isLoading || isSubmitting}
                  onClick={() => {
                    setSelectedWeekStart(currentWeekStart);
                  }}
                >
                  Текущая неделя
                </button>
              </div>
              <Button
                variant="ghost"
                className="schedule-period__nav"
                aria-label="Следующая неделя"
                disabled={isLoading || isSubmitting}
                onClick={() => {
                  setSelectedWeekStart((weekStart) => shiftClientScheduleWeekStart(weekStart, 1));
                }}
              >
                →
              </Button>
            </div>

            <Button
              variant="ghost"
              className="schedule-toolbar__sync"
              disabled={isLoading || isSubmitting}
              onClick={() => {
                void loadPage();
              }}
            >
              Синхронизировать
            </Button>
          </section>

          <div className="schedule-layout">
            <section className="week-calendar-card" aria-label="Неделя занятий">
              <div className="week-calendar-card__heading">
                <div>
                  <h2>Неделя занятий</h2>
                  <p>{selectedWeekIsEmpty ? 'На выбранной неделе пока нет занятий.' : 'Уроки и подтвержденные окна из актуального расписания.'}</p>
                </div>
              </div>

              <div className="week-calendar-card__days" aria-label="Дни недели">
                {pageData.weekDays.map((item) => (
                  <button key={`${item.day}-${item.date}`} className={item.active ? 'is-active' : undefined} type="button">
                    <span>{item.day}</span>
                    <strong>{item.date}</strong>
                  </button>
                ))}
              </div>

              {selectedWeekIsEmpty ? (
                <div className="schedule-empty-state">
                  <h3>На этой неделе занятий нет</h3>
                  <p>Переключите неделю стрелками или вернитесь к текущей неделе.</p>
                </div>
              ) : null}

              <div className="week-calendar-scroll">
                <div className="week-calendar-grid">
                  <div className="week-calendar-grid__corner" />
                  {pageData.weekDays.map((item) => (
                    <div key={`head-${item.day}-${item.date}`} className="week-calendar-grid__head">
                      {item.day}
                    </div>
                  ))}

                  {pageData.timeSlots.map((slot, slotIndex) => (
                    <div
                      key={slot}
                      className="week-calendar-grid__time"
                      style={{ gridColumn: '1', gridRow: String(slotIndex + 2) }}
                    >
                      {slot}
                    </div>
                  ))}

                  {pageData.timeSlots.flatMap((slot, slotIndex) =>
                    pageData.weekDays.map((day, dayIndex) => (
                      <div
                        key={`cell-${slot}-${day.day}-${day.date}`}
                        className="week-calendar-grid__cell"
                        style={{ gridColumn: String(dayIndex + 2), gridRow: String(slotIndex + 2) }}
                      />
                    )),
                  )}

                  {pageData.events.map((event) => (
                    <article
                      key={event.id}
                      className={cn('schedule-event', `schedule-event--${event.tone}`)}
                      style={{ gridColumn: String(event.column + 1), gridRow: String(event.row + 2) }}
                    >
                      <strong>{event.title}</strong>
                      <span>{event.meta}</span>
                      <small>{event.time}</small>
                    </article>
                  ))}
                </div>
              </div>
            </section>

            <aside className="schedule-side-panel" aria-label="Ближайшие события">
              <section className="schedule-next-card">
                <div className="schedule-next-card__title">
                  <h2>Следующее занятие</h2>
                  <span>{nextLesson?.time ?? 'ожидание'}</span>
                </div>

                {nextLesson ? (
                  <div className="schedule-next-card__lesson">
                    <strong>{nextLesson.time}</strong>
                    <span>{nextLesson.title}</span>
                    <p>{nextLesson.details}</p>
                    <div className="schedule-next-card__lesson-actions">
                      <LinkButton to={primaryLessonLink}>Открыть урок</LinkButton>
                      {nextLesson.canReschedule ? (
                        <Button
                          variant="ghost"
                          className="schedule-reschedule-action"
                          disabled={isSubmitting}
                          onClick={() => {
                            void handleReschedule(nextLesson.lessonId);
                          }}
                        >
                          {isSubmitting ? 'Отправляем...' : 'Запросить перенос'}
                        </Button>
                      ) : null}
                      {nextLesson.canMessageTutor && primaryMessageLink ? (
                        <LinkButton to={primaryMessageLink} variant="ghost">
                          Написать репетитору
                        </LinkButton>
                      ) : null}
                    </div>
                  </div>
                ) : hasScheduleData ? (
                  <div className="schedule-empty-state">
                    <h3>Ближайших занятий нет</h3>
                    <p>В календаре остались прошедшие уроки или служебные события. Новые занятия появятся здесь после назначения.</p>
                  </div>
                ) : (
                  <div className="schedule-empty-state">
                    <h3>{pageData.emptyState.title}</h3>
                    <p>{pageData.emptyState.text}</p>
                  </div>
                )}

                <div className="schedule-reschedules">
                  <h3>Запросы на перенос</h3>
                  {pageData.rescheduleRequests.length === 0 ? (
                    <p>Активных запросов на перенос сейчас нет.</p>
                  ) : (
                    pageData.rescheduleRequests.map((request) => (
                      <article key={request.id} className="reschedule-row">
                        <span className={`reschedule-row__time reschedule-row__time--${request.tone}`}>{request.time}</span>
                        <div>
                          <strong>{request.title}</strong>
                          <p>{request.status}</p>
                        </div>
                      </article>
                    ))
                  )}
                </div>

                <div className="mini-month">
                  <div className="mini-month__heading">
                    <strong>{pageData.period.label}</strong>
                    <span>{pageData.period.from && pageData.period.to ? `${pageData.period.from} - ${pageData.period.to}` : 'текущий период'}</span>
                  </div>
                  <div className="mini-month__days">
                    {pageData.miniMonthDays.map((day) => (
                      <span key={day.date} className={day.active ? 'is-active' : undefined}>
                        {day.date}
                      </span>
                    ))}
                  </div>
                </div>
              </section>
            </aside>
          </div>

          <section className="schedule-bottom-grid" aria-label="Дополнительная информация">
            <article className="schedule-mini-card schedule-mini-card--lessons">
              <h2>Ближайшие занятия</h2>
              {upcomingLessons.length === 0 ? (
                <p>{hasScheduleData ? 'Будущих занятий сейчас нет, но история расписания уже синхронизирована с CRM.' : pageData.emptyState.text}</p>
              ) : (
                <div className="schedule-lesson-list">
                  {upcomingLessons.map((lesson) => (
                    <div key={lesson.id}>
                      <span>{lesson.time}</span>
                      <div>
                        <strong>{lesson.title}</strong>
                        <p>{lesson.details}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="schedule-mini-card">
              <h2>Запросы на перенос</h2>
              {pageData.rescheduleRequests.length === 0 ? (
                <p>Когда появятся запросы на перенос, они будут видны здесь и в карточке ближайшего урока.</p>
              ) : (
                <div className="schedule-lesson-list">
                  {pageData.rescheduleRequests.map((request) => (
                    <div key={request.id}>
                      <span>{request.time}</span>
                      <div>
                        <strong>{request.title}</strong>
                        <p>{request.status}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="schedule-terms-card">
              <h2>Что можно сделать сейчас</h2>
              <p>
                {nextLesson
                  ? 'Откройте заявку по уроку, обсудите детали с репетитором или отправьте запрос на перенос прямо со страницы.'
                  : 'Перейдите к заявкам, чтобы выбрать репетитора и назначить первое занятие.'}
              </p>
              <span>{nextLesson ? nextLesson.title : 'Нет активных уроков'}</span>
              <LinkButton to={primaryLessonLink} variant="ghost">
                {nextLesson ? 'Открыть заявку' : 'Перейти к заявкам'}
              </LinkButton>
            </article>
          </section>
      </>
    </div>
  );
}
