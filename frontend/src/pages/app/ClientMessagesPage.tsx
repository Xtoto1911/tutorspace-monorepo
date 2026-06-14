import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Button, LinkButton } from '../../components/primitives';
import { getClientMessagesPage, sendClientMessage } from '../../shared/api/client';
import { cn } from '../../shared/lib/cn';
import type { ClientMessagesPageData } from '../../shared/types';

export function ClientMessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [pageData, setPageData] = useState<ClientMessagesPageData | null>(null);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const selectedThreadId = searchParams.get('threadId');

  const activeConversation = pageData?.conversations.find((c) => c.active) ?? pageData?.conversations[0];
  const activeThreadId = pageData?.activeDialog.threadId || activeConversation?.id || selectedThreadId;
  const hasActiveDialog = Boolean(activeThreadId && pageData && pageData.conversations.length > 0);

  const loadMessages = async (threadId?: string | null) => {
    setIsLoading(true);
    setError('');
    try {
      setPageData(await getClientMessagesPage(threadId));
    } catch {
      setPageData(null);
      setError('Не удалось загрузить сообщения.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!draft.trim() || !activeThreadId) return;
    setIsSending(true);
    setError('');
    try {
      await sendClientMessage(activeThreadId, draft.trim());
      setDraft('');
      await loadMessages(activeThreadId);
    } catch {
      setError('Не удалось отправить сообщение.');
    } finally {
      setIsSending(false);
    }
  };

  const handleSelectConversation = (threadId: string) => {
    const next = new URLSearchParams(searchParams);
    next.set('threadId', threadId);
    setSearchParams(next);
  };

  useEffect(() => {
    void loadMessages(selectedThreadId);
  }, [selectedThreadId]);

  if (isLoading) {
    return <div className="client-messages">Загрузка сообщений...</div>;
  }

  if (error && !pageData) {
    return (
      <div className="client-messages">
        <p role="alert">{error}</p>
        <Button type="button" onClick={() => void loadMessages(selectedThreadId)}>Повторить</Button>
      </div>
    );
  }

  if (!pageData) {
    return null;
  }

  return (
    <div className="client-messages">
      <header className="client-messages__header">
        <div>
          <p className="client-messages__eyebrow">CLIENT AREA</p>
          <h1>Сообщения</h1>
          <p>Диалоги с репетиторами, поддержкой и быстрый контекст по заявкам и занятиям.</p>
        </div>

        <div className="client-messages__actions">
          <Button variant="ghost">Настроить уведомления</Button>
          <Button>Новый диалог</Button>
        </div>
      </header>

      <section className="messages-metrics" aria-label="Сводка сообщений">
        {error ? <p role="alert">{error}</p> : null}
        {pageData.metrics.map((metric) => (
          <article key={metric.label} className={cn('messages-metric', `messages-metric--${metric.tone}`)}>
            <strong>{metric.value}</strong>
            <span>{metric.label}</span>
          </article>
        ))}
      </section>

      <section className="messages-workspace" aria-label="Рабочая область сообщений">
        <aside className="messages-list-panel" aria-label="Список диалогов">
          <div className="messages-list-panel__top">
            <h2>Диалоги</h2>
          </div>

          <label className="messages-search">
            <span>⌕</span>
            <input
              id="client-message-search"
              name="client-message-search"
              aria-label="Поиск по сообщениям"
              placeholder="Поиск по сообщениям"
            />
          </label>

          <div className="messages-filter" role="tablist" aria-label="Фильтр диалогов">
            {pageData.filters.map((filter, index) => (
              <button key={filter} className={index === 0 ? 'is-active' : undefined} type="button">
                {filter}
              </button>
            ))}
          </div>

          <div className="conversation-list">
            {pageData.conversations.length > 0 ? (
              pageData.conversations.map((conversation) => (
                <button
                  key={conversation.id}
                  className={cn('conversation-card', conversation.active && 'is-active')}
                  type="button"
                  aria-pressed={conversation.active ? 'true' : 'false'}
                  onClick={() => handleSelectConversation(conversation.id)}
                >
                  <span className="conversation-card__avatar">{conversation.initials}</span>
                  <span className="conversation-card__body">
                    <span className="conversation-card__line">
                      <strong>{conversation.name}</strong>
                      <time>{conversation.time}</time>
                    </span>
                    <span className="conversation-card__subject">{conversation.subject}</span>
                    <span className="conversation-card__preview">{conversation.preview}</span>
                  </span>
                  {conversation.unread ? <span className="conversation-card__badge">{conversation.unread}</span> : null}
                </button>
              ))
            ) : (
              <div className="conversation-list__empty">
                <strong>Пока пусто</strong>
                <span>Диалоги появятся после заявки репетитору.</span>
              </div>
            )}
          </div>
        </aside>

        <main className="messages-chat-panel" aria-label={`Диалог с ${pageData.activeDialog.name}`}>
          <header className="chat-header">
            <div className="chat-header__identity">
              <span>{pageData.activeDialog.initials}</span>
              <div>
                <h2>{pageData.activeDialog.name}</h2>
                <p>{pageData.activeDialog.subtitle}</p>
              </div>
            </div>
            <div className="chat-header__actions">
              {pageData.activeDialog.tutorSlug ? (
                <LinkButton to={`/tutors/${pageData.activeDialog.tutorSlug}`} variant="ghost">
                  Профиль
                </LinkButton>
              ) : null}
              {pageData.activeDialog.applicationId ? (
                <LinkButton to={`/app/client/applications/${pageData.activeDialog.applicationId}`} variant="ghost">
                  Заявка
                </LinkButton>
              ) : null}
            </div>
          </header>

          {hasActiveDialog ? (
            <div className="chat-day-divider">
              <span>Сегодня</span>
            </div>
          ) : null}

          <div className="chat-thread">
            {pageData.messages.length > 0 ? (
              pageData.messages.map((message) => (
                <article key={message.id ?? `${message.author}-${message.time}`} className={cn('chat-message', message.own && 'is-own')}>
                  <div className="chat-message__bubble">
                    <header>
                      <strong>{message.author}</strong>
                      <time>{message.time}</time>
                    </header>
                    <p>{message.text}</p>
                  </div>
                </article>
              ))
            ) : (
              <div className="chat-empty-state">
                <strong>Диалогов пока нет</strong>
                <p>Когда репетитор ответит на заявку, переписка появится здесь.</p>
              </div>
            )}
          </div>

          <form className="message-composer" onSubmit={(event) => void handleSubmit(event)}>
            <button type="button" aria-label="Прикрепить файл" disabled={!activeThreadId}>
              +
            </button>
            <input
              id="client-message-input"
              name="message"
              placeholder="Напишите сообщение репетитору"
              value={draft}
              disabled={!activeThreadId}
              onChange={(event) => setDraft(event.target.value)}
            />
            <Button type="submit" disabled={!activeThreadId || isSending}>
              {isSending ? 'Отправляем...' : 'Отправить'}
            </Button>
          </form>
        </main>

        <aside className="messages-context-panel" aria-label="Контекст диалога">
          <section className="messages-context-card messages-context-card--lesson">
            <div className="messages-context-card__title">
              <h2>Карточка занятия</h2>
              <span>активно</span>
            </div>
            <dl className="message-lesson-details">
              {pageData.lessonDetails.map((detail) => (
                <div key={detail.label}>
                  <dt>{detail.label}</dt>
                  <dd>{detail.value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="messages-context-card">
            <h2>Быстрые действия</h2>
            <div className="message-quick-actions">
              {pageData.quickActions.map((action) => (
                <button key={action} type="button">
                  {action}
                </button>
              ))}
            </div>
          </section>

          <section className="messages-context-card">
            <h2>Материалы</h2>
            <div className="message-attachments">
              {pageData.attachments.map((attachment) => (
                <article key={attachment.name}>
                  <span>□</span>
                  <div>
                    <strong>{attachment.name}</strong>
                    <p>{attachment.meta}</p>
                  </div>
                </article>
              ))}
            </div>
          </section>

          <section className="messages-context-card messages-context-card--safety">
            <h2>Безопасность</h2>
            <p>Оплата и переносы фиксируются в заявке, чтобы история была доступна клиенту и модерации.</p>
          </section>
        </aside>
      </section>
    </div>
  );
}
