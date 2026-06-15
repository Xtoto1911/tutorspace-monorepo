import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Button, LinkButton } from '../../components/primitives';
import { useDialog } from '../../components/dialog';
import {
  createTutorAvailabilitySchedule,
  createTutorLesson,
  createTutorSlotOverride,
  getTutorLessonCandidates,
  getTutorCalendar,
  getTutorCalendarSlots,
  getTutorStudents,
  getTutorAvailabilitySchedules,
  rescheduleTutorLesson,
  updateTutorAvailabilitySchedule,
  deleteTutorAvailabilitySchedule,
} from '../../shared/api/tutor';
import { cn } from '../../shared/lib/cn';
import type { AvailabilitySchedule, TutorCalendarPageData, TutorLessonCandidate, TutorStudentsPageData } from '../../shared/types';
import {
  buildCalendarWeekDays,
  buildCellLessonPayload,
  buildLessonReschedulePayload,
  buildSlotLessonPayload,
  formatLessonRequestStartTime,
  getLessonModalDefaults,
  getTutorCalendarGridPosition,
  getTutorCalendarDisplayStats,
  isCalendarCellInPast,
  type LessonRescheduleScope,
} from './tutor-calendar-helpers';
import { getCalendarWorkdayEmptyState } from './tutor-workday-helpers';

function getMonday(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  d.setDate(diff);
  d.setHours(0, 0, 0, 0);
  return d;
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  result.setHours(0, 0, 0, 0);
  return result;
}

function formatDateISO(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function formatPeriod(weekStart: Date): string {
  const weekEnd = addDays(weekStart, 6);
  const months = ['января','февраля','марта','апреля','мая','июня','июля','августа','сентября','октября','ноября','декабря'];
  const sameMonth = weekStart.getMonth() === weekEnd.getMonth();
  const sameYear = weekStart.getFullYear() === weekEnd.getFullYear();
  if (sameMonth) {
    return sameYear
      ? `${weekStart.getDate()}–${weekEnd.getDate()} ${months[weekStart.getMonth()]} ${weekStart.getFullYear()}`
      : `${weekStart.getDate()} ${months[weekStart.getMonth()]} ${weekStart.getFullYear()} – ${weekEnd.getDate()} ${months[weekEnd.getMonth()]} ${weekEnd.getFullYear()}`;
  }
  return sameYear
    ? `${weekStart.getDate()} ${months[weekStart.getMonth()]} – ${weekEnd.getDate()} ${months[weekEnd.getMonth()]} ${weekStart.getFullYear()}`
    : `${weekStart.getDate()} ${months[weekStart.getMonth()]} ${weekStart.getFullYear()} – ${weekEnd.getDate()} ${months[weekEnd.getMonth()]} ${weekEnd.getFullYear()}`;
}

const legend = [
  { label: 'Занятие', tone: 'indigo' },
  { label: 'Пробный', tone: 'emerald' },
  { label: 'Окно', tone: 'slate' },
];

export function TutorCalendarPage() {
  const [searchParams] = useSearchParams();
  const dialog = useDialog();
  const [pageData, setPageData] = useState<TutorCalendarPageData | null>(null);
  const [viewMode, setViewMode] = useState<'lessons' | 'availability'>('lessons');
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [currentWeekStart, setCurrentWeekStart] = useState<Date>(getMonday(new Date()));
  const [showLessonModal, setShowLessonModal] = useState(false);
  const [showLessonDetailModal, setShowLessonDetailModal] = useState(false);
  const [selectedLesson, setSelectedLesson] = useState<TutorCalendarPageData['events'][0] | null>(null);
  const [selectedCell, setSelectedCell] = useState<{ time: string; date: string } | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [calendarNow, setCalendarNow] = useState(() => new Date());
  const [schedules, setSchedules] = useState<AvailabilitySchedule[]>([]);
  const [editingSchedule, setEditingSchedule] = useState<AvailabilitySchedule | null>(null);
  const [showCreateSlotModal, setShowCreateSlotModal] = useState(false);
  const [showBookSlotModal, setShowBookSlotModal] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState<TutorCalendarPageData['slots'][0] | null>(null);
  const [lessonCandidates, setLessonCandidates] = useState<TutorLessonCandidate[]>([]);
  const [studentsData, setStudentsData] = useState<TutorStudentsPageData | null>(null);
  const [lessonMode, setLessonMode] = useState<'platform' | 'manual'>('manual');
  const [studentInputMode, setStudentInputMode] = useState<'existing' | 'new'>('existing');
  const [selectedApplicationId, setSelectedApplicationId] = useState('');
  const [deletingScheduleId, setDeletingScheduleId] = useState<string | null>(null);
  const loadRequestIdRef = useRef(0);
  const requestedApplicationId = searchParams.get('applicationId') ?? '';
  const lessonRequestOptions = useMemo(() => [
    ...lessonCandidates,
    ...(pageData?.pendingRequests ?? [])
      .filter((request) => !lessonCandidates.some((candidate) => candidate.applicationId === request.applicationId))
      .map((request) => ({
        applicationId: request.applicationId,
        studentId: request.studentId,
        studentName: request.studentName,
        subject: request.subject,
        format: request.format ?? 'Онлайн',
        desiredStartTime: request.desiredStartTime,
        gradeGoal: request.gradeGoal,
      })),
  ], [lessonCandidates, pageData?.pendingRequests]);

  const loadCalendarData = async (weekStart?: Date) => {
    const requestId = ++loadRequestIdRef.current;
    setIsLoading(true);
    const start = weekStart || currentWeekStart;
    const from = formatDateISO(start);
    const to = formatDateISO(addDays(start, 6));
    setError('');
    try {
      const data = await getTutorCalendar(from, to);
      const [candidatesResult, studentsResult] = await Promise.allSettled([
        getTutorLessonCandidates(),
        getTutorStudents(),
      ]);
      if (loadRequestIdRef.current !== requestId) {
        return;
      }
      setPageData(data);
      setLessonCandidates(candidatesResult.status === 'fulfilled' ? candidatesResult.value : []);
      setStudentsData(studentsResult.status === 'fulfilled' ? studentsResult.value : null);
      if (candidatesResult.status === 'rejected' || studentsResult.status === 'rejected') {
        setError('Календарь загружен, но часть дополнительных данных недоступна.');
      }
    } catch {
      if (loadRequestIdRef.current !== requestId) {
        return;
      }
      setPageData(null);
      setLessonCandidates([]);
      setStudentsData(null);
      setError('Не удалось загрузить календарь.');
    } finally {
      if (loadRequestIdRef.current === requestId) {
        setIsLoading(false);
      }
    }
  };

  const loadAvailabilityData = async (weekStart?: Date) => {
    const requestId = ++loadRequestIdRef.current;
    setIsLoading(true);
    setError('');
    try {
      const start = weekStart || currentWeekStart;
      const from = formatDateISO(start);
      const to = formatDateISO(addDays(start, 13));
      const [slotsResult, schedulesResult, candidatesResult, studentsResult] = await Promise.allSettled([
        getTutorCalendarSlots(from, to),
        getTutorAvailabilitySchedules(),
        getTutorLessonCandidates(),
        getTutorStudents(),
      ]);
      if (loadRequestIdRef.current !== requestId) {
        return;
      }
      if (slotsResult.status === 'fulfilled') {
        setPageData((prev) => prev ? ({ ...prev, slots: slotsResult.value }) : prev);
      }
      setSchedules(schedulesResult.status === 'fulfilled' ? schedulesResult.value : []);
      setLessonCandidates(candidatesResult.status === 'fulfilled' ? candidatesResult.value : []);
      setStudentsData(studentsResult.status === 'fulfilled' ? studentsResult.value : null);
      if ([slotsResult, schedulesResult, candidatesResult, studentsResult].some((result) => result.status === 'rejected')) {
        setError('Часть данных доступности временно недоступна.');
      }
    } finally {
      if (loadRequestIdRef.current === requestId) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    void loadCalendarData(currentWeekStart);
  }, []);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setCalendarNow(new Date());
    }, 60_000);

    return () => window.clearInterval(intervalId);
  }, []);

  useEffect(() => {
    if (viewMode === 'availability') {
      void loadAvailabilityData(currentWeekStart);
    } else {
      void loadCalendarData(currentWeekStart);
    }
  }, [viewMode]);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        closeLessonModal();
      }
    };
    if (showLessonModal) {
      window.addEventListener('keydown', handleEsc);
      return () => window.removeEventListener('keydown', handleEsc);
    }
  }, [showLessonModal]);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowLessonDetailModal(false);
      }
    };
    if (showLessonDetailModal) {
      window.addEventListener('keydown', handleEsc);
      return () => window.removeEventListener('keydown', handleEsc);
    }
  }, [showLessonDetailModal]);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowScheduleModal(false);
      }
    };
    if (showScheduleModal) {
      window.addEventListener('keydown', handleEsc);
      return () => window.removeEventListener('keydown', handleEsc);
    }
  }, [showScheduleModal]);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setEditingSchedule(null);
      }
    };
    if (editingSchedule) {
      window.addEventListener('keydown', handleEsc);
      return () => window.removeEventListener('keydown', handleEsc);
    }
  }, [editingSchedule]);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowCreateSlotModal(false);
        setShowBookSlotModal(false);
        setSelectedSlot(null);
      }
    };
    if (showCreateSlotModal || showBookSlotModal) {
      window.addEventListener('keydown', handleEsc);
      return () => window.removeEventListener('keydown', handleEsc);
    }
  }, [showCreateSlotModal, showBookSlotModal]);

  useEffect(() => {
    if (lessonMode !== 'platform') {
      return;
    }

    if (lessonRequestOptions.length === 0) {
      setSelectedApplicationId('');
      return;
    }

    setSelectedApplicationId((current) => {
      if (current && lessonRequestOptions.some((candidate) => candidate.applicationId === current)) {
        return current;
      }

      return lessonRequestOptions[0]?.applicationId ?? '';
    });
  }, [lessonRequestOptions, lessonMode]);

  useEffect(() => {
    if (!requestedApplicationId || lessonRequestOptions.length === 0) {
      return;
    }

    if (lessonRequestOptions.some((candidate) => candidate.applicationId === requestedApplicationId)) {
      setLessonMode('platform');
      setSelectedApplicationId(requestedApplicationId);
      setShowLessonModal(true);
    }
  }, [requestedApplicationId, lessonRequestOptions]);

  const openLessonModalForApplication = (applicationId?: string) => {
    const fallbackApplicationId = applicationId || lessonRequestOptions[0]?.applicationId || '';
    setLessonMode(fallbackApplicationId ? 'platform' : 'manual');
    setSelectedApplicationId(fallbackApplicationId);
    setStudentInputMode('existing');
    setShowLessonModal(true);
  };

  const handleCreateLessonSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const recurrenceType = String(formData.get('recurrenceType'));
    const studentId = lessonMode === 'manual' && studentInputMode === 'existing'
      ? String(formData.get('studentId') ?? '')
      : '';
    const payload = buildCellLessonPayload({
      mode: lessonMode,
      applicationId: selectedApplicationId,
      studentId: studentId || undefined,
      date: String(formData.get('date')),
      time: String(formData.get('time')),
      durationMinutes: Number(formData.get('duration')),
      studentName: String(formData.get('studentName')),
      subject: String(formData.get('subject')),
      format: String(formData.get('format')),
      recurrence: recurrenceType === 'WEEKLY'
        ? { type: 'WEEKLY', occurrences: Number(formData.get('recurrenceCount') ?? 8) }
        : undefined,
    });
    try {
      await createTutorLesson(payload);
      closeLessonModal();
      await loadCalendarData(currentWeekStart);
    } catch (err) {
      await dialog.alert('Не удалось создать занятие: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handleCellClick = (date: string, time: string) => {
    if (isCalendarCellInPast(date, time, calendarNow)) {
      return;
    }
    setLessonMode('manual');
    setSelectedApplicationId('');
    setStudentInputMode('existing');
    setSelectedCell({ time, date });
    setShowLessonModal(true);
  };

  const handleEventClick = (event: TutorCalendarPageData['events'][number]) => {
    setSelectedLesson(event);
    setShowLessonDetailModal(true);
  };

  const handleCreateSchedule = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const days = formData.getAll('days').map((v) => Number(v));
    const startTime = String(formData.get('startTime') ?? '09:00');
    const endTime = String(formData.get('endTime') ?? '17:00');
    const slotDurationMinutes = Number(formData.get('duration') ?? 60);
    const validUntil = formData.get('validUntil') as string | null;
    const indefinite = formData.get('indefinite') === 'on';
    const createdScheduleIds: string[] = [];

    if (days.length === 0) {
      await dialog.alert('Выберите хотя бы один день недели.');
      return;
    }

    try {
      for (const dayOfWeek of days) {
        const created = await createTutorAvailabilitySchedule({
          dayOfWeek,
          startTime,
          endTime,
          slotDurationMinutes,
          active: true,
          validUntil: indefinite || !validUntil ? null : validUntil,
        });
        createdScheduleIds.push(created.id);
      }

      await loadAvailabilityData(currentWeekStart);
      setShowScheduleModal(false);
      form.reset();
    } catch (err) {
      await Promise.allSettled(createdScheduleIds.map((id) => deleteTutorAvailabilitySchedule(id)));
      await dialog.alert('Не удалось сохранить правило: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handleDeleteSchedule = async (id: string) => {
    if (deletingScheduleId) return;
    if (!(await dialog.confirm('Удалить это правило?'))) return;
    setDeletingScheduleId(id);
    try {
      await deleteTutorAvailabilitySchedule(id);
      await loadAvailabilityData(currentWeekStart);
    } catch (err) {
      const msg = err instanceof Error ? err.message : '';
      if (/not found|404/i.test(msg)) {
        await loadAvailabilityData(currentWeekStart);
        await dialog.alert('Уже удалено');
        return;
      }
      await dialog.alert('Не удалось удалить: ' + (msg || 'Unknown error'));
    } finally {
      setDeletingScheduleId(null);
    }
  };

  const handleUpdateSchedule = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingSchedule) return;
    const form = e.currentTarget;
    const formData = new FormData(form);
    const payload: Partial<AvailabilitySchedule> = {
      dayOfWeek: editingSchedule.dayOfWeek,
      startTime: String(formData.get('startTime')),
      endTime: String(formData.get('endTime')),
      slotDurationMinutes: Number(formData.get('duration')),
      subjectId: editingSchedule.subjectId ?? null,
      validUntil: formData.get('validUntil') as string | null || null,
      active: formData.get('active') === 'on',
    };
    try {
      await updateTutorAvailabilitySchedule(editingSchedule.id, payload);
      setEditingSchedule(null);
      await loadAvailabilityData();
    } catch (err) {
      await dialog.alert('Не удалось обновить: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handleCreateSlot = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const date = String(formData.get('date'));
    const startTime = String(formData.get('startTime'));
    const duration = Number(formData.get('duration'));
    try {
      await createTutorSlotOverride({
        date,
        startTime,
        durationMinutes: duration,
        type: 'EXTRA',
      });
      setShowCreateSlotModal(false);
      await handleSync();
    } catch (err) {
      await dialog.alert('Не удалось создать окно: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handleBookSlot = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedSlot) return;
    const form = e.currentTarget;
    const formData = new FormData(form);
    const recurrenceType = String(formData.get('recurrenceType'));
    const studentId = studentInputMode === 'existing' ? String(formData.get('studentId') ?? '') : '';
    const payload = buildSlotLessonPayload({
      mode: 'manual',
      studentId: studentId || undefined,
      slotDate: selectedSlot.date,
      slotStartTime: selectedSlot.startTime.slice(0, 5),
      slotEndTime: selectedSlot.endTime.slice(0, 5),
      studentName: String(formData.get('studentName')),
      subject: String(formData.get('subject')),
      format: String(formData.get('format')),
      recurrence: recurrenceType === 'WEEKLY'
        ? { type: 'WEEKLY', occurrences: Number(formData.get('recurrenceCount') ?? 8) }
        : undefined,
    });
    try {
      await createTutorLesson(payload);
      setShowBookSlotModal(false);
      setSelectedSlot(null);
      await handleSync();
    } catch (err) {
      await dialog.alert('Не удалось записать ученика: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handleRescheduleLesson = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedLesson) return;
    const formData = new FormData(e.currentTarget);
    const scope = String(formData.get('scope') ?? 'SINGLE') as LessonRescheduleScope;
    const payload = buildLessonReschedulePayload(
      String(formData.get('date')),
      String(formData.get('time')),
      scope,
    );
    try {
      await rescheduleTutorLesson(selectedLesson.id, payload);
      setShowLessonDetailModal(false);
      setSelectedLesson(null);
      await handleSync();
    } catch (err) {
      await dialog.alert('Не удалось перенести урок: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const goToPreviousWeek = () => {
    const newWeek = addDays(currentWeekStart, -7);
    setCurrentWeekStart(newWeek);
    void (viewMode === 'availability' ? loadAvailabilityData(newWeek) : loadCalendarData(newWeek));
  };

  const goToNextWeek = () => {
    const newWeek = addDays(currentWeekStart, 7);
    setCurrentWeekStart(newWeek);
    void (viewMode === 'availability' ? loadAvailabilityData(newWeek) : loadCalendarData(newWeek));
  };

  const handleSync = async () => {
    if (viewMode === 'lessons') {
      await loadCalendarData(currentWeekStart);
    } else {
      await loadAvailabilityData(currentWeekStart);
    }
  };

  const closeLessonModal = () => {
    setShowLessonModal(false);
    setSelectedCell(null);
    setLessonMode('manual');
    setStudentInputMode('existing');
    setSelectedApplicationId('');
  };

  const selectedLessonRequest = lessonRequestOptions.find((candidate) => candidate.applicationId === selectedApplicationId) ?? null;
  const lessonModalDefaults = getLessonModalDefaults({
    fallbackDate: formatDateISO(new Date()),
    selectedCell,
    selectedLessonRequest,
  });
  const studentOptions = studentsData?.students.filter((student) => !student.id.includes('placeholder')) ?? [];

  if (isLoading && !pageData) {
    return <div className="tutor-calendar-page">Загрузка календаря...</div>;
  }

  if (error && !pageData) {
    return (
      <div className="tutor-calendar-page">
        <p role="alert">{error}</p>
        <Button type="button" onClick={() => void loadCalendarData(currentWeekStart)}>Повторить</Button>
      </div>
    );
  }

  if (!pageData) {
    return null;
  }

  const calendarEmptyState = getCalendarWorkdayEmptyState(pageData);

  const slotsByDate = pageData.slots.reduce<Record<string, typeof pageData.slots>>((acc, slot) => {
    if (!acc[slot.date]) acc[slot.date] = [];
    acc[slot.date].push(slot);
    return acc;
  }, {});
  const calendarWeekDays = buildCalendarWeekDays(currentWeekStart, calendarNow);
  const displayStats = getTutorCalendarDisplayStats(pageData).filter((stat) => stat.tone !== 'amber');

  return (
    <div className="tutor-calendar-page">
      <header className="tutor-calendar-page__header">
        <div>
          <p className="tutor-calendar-page__eyebrow">Кабинет репетитора</p>
          <h1>Календарь занятий</h1>
          <p>Планируйте уроки, отмечайте свободные окна и быстро переносите занятия без путаницы.</p>
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <Button
            onClick={() => openLessonModalForApplication()}
          >
            Добавить занятие
          </Button>
          <Button variant="ghost" onClick={() => setShowCreateSlotModal(true)}>Создать окно</Button>
        </div>
      </header>

      <section className="tutor-calendar-stats" aria-label="Сводка календаря">
        {error ? <p role="alert">{error}</p> : null}
        {displayStats.map((stat) => (
          <article key={stat.label} className={cn('tutor-calendar-stat', `tutor-calendar-stat--${stat.tone}`)}>
            <span className="tutor-calendar-stat__icon">{stat.icon}</span>
            <div>
              <strong>{stat.label}</strong>
              <p>{stat.sublabel}</p>
            </div>
            <b>{stat.value}</b>
          </article>
        ))}
      </section>

      <section className="tutor-calendar-toolbar" aria-label="Управление календарем">
        <div className="tutor-calendar-view-switch">
          <button
            className={viewMode === 'lessons' ? 'is-active' : undefined}
            type="button"
            onClick={() => setViewMode('lessons')}
          >
            Неделя занятий
          </button>
          <button
            className={viewMode === 'availability' ? 'is-active' : undefined}
            type="button"
            onClick={() => setViewMode('availability')}
          >
            Моя доступность
          </button>
        </div>

        <div className="tutor-calendar-period">
          <button type="button" aria-label="Предыдущая неделя" onClick={goToPreviousWeek}>
            ←
          </button>
          <strong>{formatPeriod(currentWeekStart)}</strong>
          <button type="button" aria-label="Следующая неделя" onClick={goToNextWeek}>
            →
          </button>
        </div>

        <Button onClick={() => void handleSync()}>Обновить</Button>
      </section>

      <div className="tutor-calendar-layout">
        {viewMode === 'lessons' && (
          <section className="tutor-week-card" aria-label="Расписание недели">
            <div className="tutor-week-card__heading">
              <div>
                <h2>Расписание недели</h2>
                <p>Занятия, пробные уроки и свободные окна</p>
              </div>

              <div className="tutor-week-legend">
                {legend.map((item) => (
                  <span key={item.label} className={`tutor-week-legend__item tutor-week-legend__item--${item.tone}`}>
                    {item.label}
                  </span>
                ))}
              </div>
            </div>

            <div className="tutor-week-scroll">
              {isLoading && <div className="tutor-calendar-loading">Загрузка...</div>}
              <div className="tutor-week-grid">
                <div className="tutor-week-grid__corner" />
                {calendarWeekDays.map((day) => (
                  <div key={day.date} className={cn('tutor-week-grid__head', day.active && 'is-active')}>
                    <span>{day.day}</span>
                    <strong>{day.date}</strong>
                  </div>
                ))}

                {pageData.timeSlots.map((slot, slotIndex) => (
                  <div
                    key={slot}
                    className="tutor-week-grid__time"
                    style={{ gridColumn: '1', gridRow: String(slotIndex + 2) }}
                  >
                    {slot}
                  </div>
                ))}

                {pageData.timeSlots.flatMap((slot, slotIndex) =>
                  calendarWeekDays.map((day, dayIndex) => {
                    const isUnavailable = isCalendarCellInPast(day.isoDate, slot, calendarNow);

                    return (
                      <div
                        key={`${slot}-${day.isoDate}`}
                        className={cn('tutor-week-grid__cell', isUnavailable && 'is-unavailable')}
                        style={{ gridColumn: String(dayIndex + 2), gridRow: String(slotIndex + 2) }}
                        onClick={() => handleCellClick(day.isoDate, slot)}
                        role="button"
                        aria-disabled={isUnavailable}
                        tabIndex={isUnavailable ? -1 : 0}
                        title={isUnavailable ? 'В это время уже нельзя записать ученика' : undefined}
                        onKeyDown={(e) => {
                          if (!isUnavailable && (e.key === 'Enter' || e.key === ' ')) {
                            e.preventDefault();
                            handleCellClick(day.isoDate, slot);
                          }
                        }}
                      />
                    );
                  }),
                )}

                {pageData.events.map((event) => (
                  <article
                    key={event.id}
                    className={cn('tutor-week-event', `tutor-week-event--${event.tone}`)}
                    style={{
                      gridColumn: String(event.column + 1),
                      gridRow: `${event.row + 2} / span ${event.rows ?? 1}`,
                    }}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleEventClick(event);
                    }}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); handleEventClick(event); } }}
                  >
                    <strong>{event.title}</strong>
                    <span>{event.subtitle}</span>
                  </article>
                ))}

                {pageData.slots
                  .filter((slot) => slot.available)
                  .map((slot, index) => {
                    const slotDate = new Date(slot.date + 'T00:00:00');
                    const dayDiff = Math.round((slotDate.getTime() - currentWeekStart.getTime()) / (24 * 60 * 60 * 1000));
                    const column = dayDiff + 1;
                    const position = getTutorCalendarGridPosition(slot.startTime, slot.endTime);
                    const row = position.row;
                    if (column < 1 || column > calendarWeekDays.length || row < 0 || row >= pageData.timeSlots.length) return null;
                    return (
                      <article
                        key={`slot-${index}`}
                        className="tutor-week-event tutor-week-event--slate"
                        style={{
                          gridColumn: String(column + 1),
                          gridRow: `${row + 2} / span ${position.rowSpan}`,
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedSlot(slot);
                          setShowBookSlotModal(true);
                        }}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setSelectedSlot(slot); setShowBookSlotModal(true); } }}
                      >
                        <strong>Свободно</strong>
                        <span>{slot.startTime} – {slot.endTime}</span>
                      </article>
                    );
                  })}
              </div>
            </div>
            {calendarEmptyState ? (
              <div className="tutor-calendar-empty">
                <strong>{calendarEmptyState.title}</strong>
                <p>{calendarEmptyState.text}</p>
                <Button type="button" variant="ghost" onClick={() => openLessonModalForApplication()}>
                  {calendarEmptyState.action}
                </Button>
              </div>
            ) : null}
          </section>
        )}

        {viewMode === 'availability' && (
          <section className="tutor-week-card" aria-label="Доступные слоты">
            <div className="tutor-week-card__heading">
              <div>
                <h2>Моя доступность</h2>
                <p>Свободные и занятые слоты на ближайшие две недели</p>
              </div>
            </div>

            <div className="availability-actions" style={{ marginBottom: 16 }}>
              <Button onClick={() => setShowScheduleModal(true)}>Добавить правило</Button>
            </div>

            {schedules.length > 0 && (
              <div className="availability-schedules-list" style={{ marginBottom: 24 }}>
                <h3 style={{ fontSize: 15, marginBottom: 12, color: 'var(--ink)' }}>Правила</h3>
                {schedules.map((schedule) => (
                  <div key={schedule.id} className="availability-schedule-row">
                    <div>
                      <strong>{['Пн','Вт','Ср','Чт','Пт','Сб','Вс'][schedule.dayOfWeek - 1]}</strong>
                      <span>{schedule.startTime}–{schedule.endTime} · {schedule.slotDurationMinutes} мин</span>
                      {schedule.validUntil && <span> до {schedule.validUntil}</span>}
                    </div>
                    <div className="availability-schedule-actions">
                      <Button variant="ghost" onClick={() => setEditingSchedule(schedule)}>Редактировать</Button>
                       <Button
                         variant="ghost"
                         onClick={() => void handleDeleteSchedule(schedule.id)}
                         disabled={deletingScheduleId === schedule.id}
                       >
                         Удалить
                       </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {Object.keys(slotsByDate).length === 0 && (
              <p style={{ color: 'var(--muted)' }}>Нет созданных слотов. Добавьте правило доступности.</p>
            )}

            {Object.entries(slotsByDate).map(([date, slots]) => (
              <div key={date} style={{ marginBottom: 24 }}>
                <h3 style={{ fontSize: 15, marginBottom: 10, color: 'var(--ink)' }}>
                  {new Date(date).toLocaleDateString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' })}
                </h3>
                <div className="availability-grid">
                  {slots.map((slot) => (
                    <div
                      key={`${slot.date}-${slot.startTime}`}
                      className={`availability-slot availability-slot--${slot.available ? 'open' : 'booked'} availability-slot--${slot.source.toLowerCase()}`}
                    >
                      <span className="availability-slot__time">{slot.startTime} – {slot.endTime}</span>
                      <span className="availability-slot__status">{slot.available ? 'Свободно' : 'Занято'}</span>
                      {slot.source === 'EXTRA' && <span className="availability-slot__badge">EXTRA</span>}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </section>
        )}

        <aside className="tutor-calendar-side" aria-label="Контекст календаря">
          <section className="tutor-next-lesson-card">
            <span>Следующее занятие</span>
            <strong>{pageData.nextLesson.time}</strong>
            <h2>{pageData.nextLesson.title}</h2>
            <p>{pageData.nextLesson.description}</p>
          </section>

          {(pageData.pendingRequests?.length ?? 0) > 0 ? (
            <section className="tutor-calendar-events-card">
              <h2>Ожидают записи</h2>
              <div className="tutor-calendar-events-card__list">
                {pageData.pendingRequests.map((request) => (
                  <article key={request.applicationId} className="tutor-calendar-event-row">
                    <span className="tutor-calendar-event-row__line tutor-calendar-event-row__line--amber" />
                    <strong>{formatLessonRequestStartTime(request.desiredStartTime)}</strong>
                    <p>{request.subject} · {request.studentName}</p>
                    <Button type="button" variant="ghost" onClick={() => openLessonModalForApplication(request.applicationId)}>
                      Назначить урок
                    </Button>
                  </article>
                ))}
              </div>
            </section>
          ) : null}

          <section className="tutor-calendar-events-card">
            <h2>Ближайшие события</h2>
            <div className="tutor-calendar-events-card__list">
              {pageData.upcomingEvents.map((event) => (
                <article key={`${event.time}-${event.text}`} className="tutor-calendar-event-row">
                  <span className={cn('tutor-calendar-event-row__line', `tutor-calendar-event-row__line--${event.tone}`)} />
                  <strong>{event.time}</strong>
                  <p>{event.text}</p>
                </article>
              ))}
            </div>
          </section>

        </aside>
      </div>

      <section className="tutor-calendar-automation" aria-label="Автоматизация свободных окон">
        <strong>Автоматизация</strong>
        <p>Свободные окна можно открыть для новых заявок — ученики увидят их в каталоге и смогут выбрать удобное время.</p>
        <LinkButton to="/app/tutor/requests" variant="ghost">
          Открыть слоты
        </LinkButton>
      </section>

      {showScheduleModal && (
        <dialog className="schedule-modal" open>
          <h2>Новое правило доступности</h2>
          <form className="schedule-modal__form" onSubmit={handleCreateSchedule}>
            <label className="form-label">Дни недели</label>
            <div className="day-checkboxes">
              {['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'].map((day, i) => (
                <label key={day} className="checkbox-row">
                  <input type="checkbox" value={i + 1} name="days" /> {day}
                </label>
              ))}
            </div>
            <label className="form-label">
              Время начала
              <input className="control" type="time" name="startTime" required defaultValue="09:00" />
            </label>
            <label className="form-label">
              Время окончания
              <input className="control" type="time" name="endTime" required defaultValue="17:00" />
            </label>
            <label className="form-label">
              Длительность слота
              <select className="control" name="duration" defaultValue="60">
                <option value="30">30 мин</option>
                <option value="45">45 мин</option>
                <option value="60">60 мин</option>
                <option value="90">90 мин</option>
                <option value="120">120 мин</option>
              </select>
            </label>
            <label className="form-label">
              Действует до
              <input className="control" type="date" name="validUntil" />
            </label>
            <label className="checkbox-row">
              <input type="checkbox" name="indefinite" /> Бессрочно
            </label>
            <div className="schedule-modal__actions">
              <Button type="submit">Сохранить</Button>
              <Button type="button" variant="ghost" onClick={() => setShowScheduleModal(false)}>
                Отмена
              </Button>
            </div>
          </form>
        </dialog>
      )}

      {editingSchedule && (
        <div className="lesson-modal-overlay" onClick={() => setEditingSchedule(null)}>
          <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
            <h2>Редактировать правило</h2>
            <form onSubmit={handleUpdateSchedule}>
              <label>
                Время начала
                <input type="time" name="startTime" required defaultValue={editingSchedule.startTime} />
              </label>
              <label>
                Время окончания
                <input type="time" name="endTime" required defaultValue={editingSchedule.endTime} />
              </label>
              <label>
                Длительность слота
                <select name="duration" defaultValue={String(editingSchedule.slotDurationMinutes)}>
                  <option value="30">30 мин</option>
                  <option value="45">45 мин</option>
                  <option value="60">60 мин</option>
                  <option value="90">90 мин</option>
                  <option value="120">120 мин</option>
                </select>
              </label>
              <label>
                Действует до
                <input type="date" name="validUntil" defaultValue={editingSchedule.validUntil || ''} />
              </label>
              <label className="checkbox-row">
                <input type="checkbox" name="active" defaultChecked={editingSchedule.active} /> Активно
              </label>
              <div className="modal-actions">
                <Button type="submit">Сохранить</Button>
                <Button type="button" variant="ghost" onClick={() => setEditingSchedule(null)}>Отмена</Button>
              </div>
            </form>
          </dialog>
        </div>
      )}

      {showLessonModal && (
        <div className="lesson-modal-overlay" onClick={closeLessonModal}>
          <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
            <h2>Новое занятие</h2>
            <form
              key={`${lessonMode}-${selectedApplicationId}-${lessonModalDefaults.date}-${lessonModalDefaults.time}`}
              onSubmit={handleCreateLessonSubmit}
            >
              <fieldset style={{ border: 0, padding: 0, margin: '0 0 16px' }}>
                <legend className="form-label">Источник</legend>
                <div className="day-checkboxes" role="radiogroup" aria-label="Источник занятия">
                  <label className="checkbox-row">
                    <input
                      type="radio"
                      name="lessonMode"
                      value="platform"
                      checked={lessonMode === 'platform'}
                      onChange={() => {
                        setLessonMode('platform');
                        setSelectedApplicationId(lessonRequestOptions[0]?.applicationId ?? '');
                      }}
                      disabled={lessonRequestOptions.length === 0}
                    />
                    Из принятых заявок
                  </label>
                  <label className="checkbox-row">
                    <input
                      type="radio"
                      name="lessonMode"
                      value="manual"
                      checked={lessonMode === 'manual'}
                      onChange={() => {
                        setLessonMode('manual');
                        setSelectedApplicationId('');
                      }}
                    />
                    Вручную
                  </label>
                </div>
              </fieldset>
              <label>
                Дата
                <input type="date" name="date" required min={formatDateISO(new Date())} defaultValue={lessonModalDefaults.date} />
              </label>
              <label>
                Время
                <input type="time" name="time" required defaultValue={lessonModalDefaults.time} />
              </label>
              <label>
                Длительность
                <select name="duration" defaultValue={String(lessonModalDefaults.durationMinutes)}>
                  <option value="30">30 мин</option>
                  <option value="45">45 мин</option>
                  <option value="60">60 мин</option>
                  <option value="90">90 мин</option>
                  <option value="120">120 мин</option>
                </select>
              </label>
              <fieldset className="lesson-modal__fieldset">
                <legend className="form-label">Повтор</legend>
                <div className="lesson-repeat-options">
                  <label className="checkbox-row">
                    <input type="radio" name="recurrenceType" value="NONE" defaultChecked />
                    Одноразовый урок
                  </label>
                  <label className="checkbox-row">
                    <input type="radio" name="recurrenceType" value="WEEKLY" />
                    Еженедельно
                  </label>
                </div>
                <label>
                  Количество уроков в серии
                  <select name="recurrenceCount" defaultValue="8">
                    <option value="4">4 урока</option>
                    <option value="8">8 уроков</option>
                    <option value="12">12 уроков</option>
                    <option value="16">16 уроков</option>
                  </select>
                </label>
              </fieldset>
              {lessonMode === 'platform' ? (
                <label>
                  Принятая заявка
                  <select
                    name="applicationId"
                    value={selectedApplicationId}
                    onChange={(event) => setSelectedApplicationId(event.target.value)}
                    required
                    disabled={lessonRequestOptions.length === 0}
                  >
                    {lessonRequestOptions.length === 0 ? (
                      <option value="">Нет принятых заявок</option>
                    ) : (
                      <>
                        <option value="">Выберите заявку</option>
                        {lessonRequestOptions.map((candidate) => (
                          <option key={candidate.applicationId} value={candidate.applicationId}>
                            {candidate.studentName} · {candidate.subject} · {candidate.format} · {formatLessonRequestStartTime(candidate.desiredStartTime)}
                          </option>
                        ))}
                      </>
                    )}
                  </select>
                  {selectedLessonRequest ? (
                    <small style={{ color: 'var(--muted)' }}>
                      {selectedLessonRequest.studentName} · {selectedLessonRequest.subject} · {selectedLessonRequest.format} · {formatLessonRequestStartTime(selectedLessonRequest.desiredStartTime)}
                    </small>
                  ) : null}
                </label>
              ) : (
                <>
                  <fieldset className="lesson-modal__fieldset">
                    <legend className="form-label">Ученик</legend>
                    <div className="lesson-repeat-options">
                      <label className="checkbox-row">
                        <input
                          type="radio"
                          name="studentInputMode"
                          value="existing"
                          checked={studentInputMode === 'existing' && studentOptions.length > 0}
                          onChange={() => setStudentInputMode('existing')}
                          disabled={studentOptions.length === 0}
                        />
                        Из моих учеников
                      </label>
                      <label className="checkbox-row">
                        <input
                          type="radio"
                          name="studentInputMode"
                          value="new"
                          checked={studentInputMode === 'new' || studentOptions.length === 0}
                          onChange={() => setStudentInputMode('new')}
                        />
                        Новый ученик
                      </label>
                    </div>
                    {studentInputMode === 'existing' && studentOptions.length > 0 ? (
                      <>
                        <label>
                          Ученик
                          <select name="studentId" required defaultValue={studentOptions[0]?.id}>
                            {studentOptions.map((student) => (
                              <option key={student.id} value={student.id}>
                                {student.name} · {student.subject}
                              </option>
                            ))}
                          </select>
                        </label>
                        <input type="hidden" name="studentName" value="" />
                      </>
                    ) : (
                      <label>
                        Имя ученика
                        <input type="text" name="studentName" required placeholder="Иван Иванов" />
                      </label>
                    )}
                  </fieldset>
                  <label>
                    Предмет
                    <input type="text" name="subject" required placeholder="Математика" />
                  </label>
                  <label>
                    Формат
                    <select name="format" defaultValue="Онлайн">
                      <option value="Онлайн">Онлайн</option>
                      <option value="Офлайн">Офлайн</option>
                    </select>
                  </label>
                </>
              )}
              <div className="modal-actions">
                <Button type="submit">Создать</Button>
                <Button type="button" variant="ghost" onClick={closeLessonModal}>Отмена</Button>
              </div>
            </form>
          </dialog>
        </div>
      )}

      {showLessonDetailModal && selectedLesson && (
        <div className="lesson-modal-overlay" onClick={() => setShowLessonDetailModal(false)}>
          <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
            <h2>{selectedLesson.title}</h2>
            <div className="lesson-detail">
              <p><strong>Время:</strong> {selectedLesson.subtitle}</p>
              <p><strong>Тип:</strong> {selectedLesson.recurrenceLabel ?? 'одноразовый урок'}</p>
              <p><strong>Статус:</strong> Активно</p>
            </div>
            <form onSubmit={handleRescheduleLesson}>
              <fieldset className="lesson-modal__fieldset">
                <legend className="form-label">Перенос</legend>
                <label>
                  Новая дата
                  <input type="date" name="date" required min={formatDateISO(new Date())} defaultValue={selectedLesson.startsAt?.slice(0, 10) ?? formatDateISO(new Date())} />
                </label>
                <label>
                  Новое время
                  <input type="time" name="time" required defaultValue={selectedLesson.startsAt?.slice(11, 16) ?? '09:00'} />
                </label>
                <label>
                  Что переносим
                  <select name="scope" defaultValue="SINGLE">
                    <option value="SINGLE">Только этот урок</option>
                    <option value="SERIES">Эту и будущие недели серии</option>
                  </select>
                </label>
                <Button type="submit">Перенести</Button>
              </fieldset>
            </form>
            <div className="modal-actions">
              <Button variant="ghost" onClick={() => setShowLessonDetailModal(false)}>Закрыть</Button>
            </div>
          </dialog>
        </div>
      )}

      {showCreateSlotModal && (
        <div className="lesson-modal-overlay" onClick={() => setShowCreateSlotModal(false)}>
          <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
            <h2>Создать окно</h2>
            <form onSubmit={handleCreateSlot}>
              <label>
                Дата
                <input type="date" name="date" required min={formatDateISO(new Date())} defaultValue={formatDateISO(new Date())} />
              </label>
              <label>
                Время начала
                <input type="time" name="startTime" required defaultValue="09:00" />
              </label>
              <label>
                Длительность
                <select name="duration" defaultValue="60">
                  <option value="30">30 мин</option>
                  <option value="45">45 мин</option>
                  <option value="60">60 мин</option>
                  <option value="90">90 мин</option>
                  <option value="120">120 мин</option>
                </select>
              </label>
              <div className="modal-actions">
                <Button type="submit">Создать</Button>
                <Button type="button" variant="ghost" onClick={() => setShowCreateSlotModal(false)}>Отмена</Button>
              </div>
            </form>
          </dialog>
        </div>
      )}

      {showBookSlotModal && selectedSlot && (
        <div className="lesson-modal-overlay" onClick={() => setShowBookSlotModal(false)}>
          <dialog className="lesson-modal" open onClick={(e) => e.stopPropagation()}>
            <h2>Записать ученика</h2>
            <p style={{ color: 'var(--muted)', fontSize: 14, marginBottom: 16 }}>
              {selectedSlot.date} · {selectedSlot.startTime} – {selectedSlot.endTime}
            </p>
            <form onSubmit={handleBookSlot}>
              <fieldset className="lesson-modal__fieldset">
                <legend className="form-label">Ученик</legend>
                <div className="lesson-repeat-options">
                  <label className="checkbox-row">
                    <input
                      type="radio"
                      name="studentInputMode"
                      value="existing"
                      checked={studentInputMode === 'existing' && studentOptions.length > 0}
                      onChange={() => setStudentInputMode('existing')}
                      disabled={studentOptions.length === 0}
                    />
                    Из моих учеников
                  </label>
                  <label className="checkbox-row">
                    <input
                      type="radio"
                      name="studentInputMode"
                      value="new"
                      checked={studentInputMode === 'new' || studentOptions.length === 0}
                      onChange={() => setStudentInputMode('new')}
                    />
                    Новый ученик
                  </label>
                </div>
                {studentInputMode === 'existing' && studentOptions.length > 0 ? (
                  <>
                    <label>
                      Ученик
                      <select name="studentId" required defaultValue={studentOptions[0]?.id}>
                        {studentOptions.map((student) => (
                          <option key={student.id} value={student.id}>
                            {student.name} · {student.subject}
                          </option>
                        ))}
                      </select>
                    </label>
                    <input type="hidden" name="studentName" value="" />
                  </>
                ) : (
                  <label>
                    Имя ученика
                    <input type="text" name="studentName" required placeholder="Иван Иванов" />
                  </label>
                )}
              </fieldset>
              <label>
                Предмет
                <input type="text" name="subject" required placeholder="Математика" />
              </label>
              <label>
                Формат
                <select name="format" defaultValue="Онлайн">
                  <option value="Онлайн">Онлайн</option>
                  <option value="Офлайн">Офлайн</option>
                </select>
              </label>
              <fieldset className="lesson-modal__fieldset">
                <legend className="form-label">Повтор</legend>
                <div className="lesson-repeat-options">
                  <label className="checkbox-row">
                    <input type="radio" name="recurrenceType" value="NONE" defaultChecked />
                    Одноразовый урок
                  </label>
                  <label className="checkbox-row">
                    <input type="radio" name="recurrenceType" value="WEEKLY" />
                    Еженедельно
                  </label>
                </div>
                <label>
                  Количество уроков в серии
                  <select name="recurrenceCount" defaultValue="8">
                    <option value="4">4 урока</option>
                    <option value="8">8 уроков</option>
                    <option value="12">12 уроков</option>
                    <option value="16">16 уроков</option>
                  </select>
                </label>
              </fieldset>
              <div className="modal-actions">
                <Button type="submit">Записать</Button>
                <Button type="button" variant="ghost" onClick={() => { setShowBookSlotModal(false); setSelectedSlot(null); }}>Отмена</Button>
              </div>
            </form>
          </dialog>
        </div>
      )}
    </div>
  );
}
