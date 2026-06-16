import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Button, LinkButton } from '../../components/primitives';
import { getTutorMessages, sendTutorMessage } from '../../shared/api/tutor';
import { cn } from '../../shared/lib/cn';
import type { TutorMessagesPageData } from '../../shared/types';

export function TutorMessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [pageData, setPageData] = useState<TutorMessagesPageData | null>(null);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const selectedThreadId = searchParams.get('threadId');

  const loadMessages = async (threadId?: string | null) => {
    setIsLoading(true);
    setError('');
    try {
      setPageData(await getTutorMessages(threadId));
    } catch {
      setPageData(null);
      setError('Не удалось загрузить сообщения.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadMessages(selectedThreadId);
  }, [selectedThreadId]);

  const filteredConversations = useMemo(() => {
    const conversations = pageData?.conversations ?? [];
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return conversations;
    }
    return conversations.filter((conversation) =>
      conversation.name.toLowerCase().includes(query)
      || conversation.subject.toLowerCase().includes(query)
      || conversation.preview.toLowerCase().includes(query),
    );
  }, [pageData?.conversations, searchQuery]);

  useEffect(() => {
    if (!selectedThreadId) return;
    if (filteredConversations.some((conversation) => conversation.id === selectedThreadId)) return;
    const next = new URLSearchParams(searchParams);
    next.delete('threadId');
    setSearchParams(next, { replace: true });
  }, [filteredConversations, selectedThreadId, searchParams, setSearchParams]);

  const dialog = pageData?.activeDialog;
  const activeConversation = pageData?.conversations.find((conversation) => conversation.active) ?? pageData?.conversations[0];
  const activeThreadId = dialog?.threadId || activeConversation?.id || selectedThreadId;
  const hasActiveDialog = Boolean(activeThreadId && pageData && pageData.conversations.length > 0);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!draft.trim() || !activeThreadId) {
      return;
    }

    setIsSending(true);
    setError('');
    try {
      await sendTutorMessage(activeThreadId, draft.trim());
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

  if (isLoading) {
    return <div className="tutor-messages">Загрузка сообщений...</div>;
  }

  if (error && !pageData) {
    return (
      <div className="tutor-messages">
        <p role="alert">{error}</p>
        <Button type="button" onClick={() => void loadMessages(selectedThreadId)}>Повторить</Button>
      </div>
    );
  }

  if (!pageData || !dialog) {
    return null;
  }

  return (
    <div className="tutor-messages">
      <header className="tutor-messages__header">
        <div>
          <p className="tutor-messages__eyebrow">TUTOR AREA</p>
          <h1>Сообщения</h1>
          <p>Диалоги с родителями, учениками и поддержкой: заявки, занятия, материалы и быстрые действия рядом.</p>
        </div>
      </header>

      <section className="messages-metrics" aria-label="Сводка сообщений репетитора">
        {error ? <p role="alert">{error}</p> : null}
        {pageData.metrics.map((metric) => (
          <article key={metric.label} className={cn('messages-metric', `messages-metric--${metric.tone}`)}>
            <strong>{metric.value}</strong>
            <span>{metric.label}</span>
          </article>
        ))}
      </section>

      <section className="messages-workspace tutor-messages-workspace" aria-label="Рабочая область сообщений">
        <aside className="messages-list-panel" aria-label="Список диалогов">
          <div className="messages-list-panel__top">
            <h2>Диалоги</h2>
          </div>

          <label className="messages-search">
            <span>⌕</span>
            <input
              id="tutor-message-search"
              name="tutor-message-search"
              aria-label="Поиск по сообщениям"
              placeholder="Поиск по родителям и ученикам"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
            {searchQuery ? (
              <button
                type="button"
                className="messages-search__clear"
                onClick={() => setSearchQuery('')}
                aria-label="Очистить поиск"
              >
                ×
              </button>
            ) : null}
          </label>

          <div className="conversation-list">
            {pageData.conversations.length === 0 ? (
              <div className="conversation-list__empty">
                <strong>Пока пусто</strong>
                <span>Диалоги появятся после первой заявки.</span>
              </div>
            ) : filteredConversations.length === 0 ? (
              <div className="conversation-list__empty">
                <strong>Диалогов не найдено</strong>
                <span>Попробуйте изменить запрос.</span>
                <button
                  type="button"
                  className="conversation-list__reset"
                  onClick={() => setSearchQuery('')}
                >
                  Очистить поиск
                </button>
              </div>
            ) : (
              filteredConversations.map((conversation) => (
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
            )}
          </div>
        </aside>

        <main className="messages-chat-panel" aria-label={`Диалог с ${dialog.name}`}>
          <header className="chat-header">
            <div className="chat-header__identity">
              <span>{dialog.initials}</span>
              <div>
                <h2>{dialog.name}</h2>
                <p>{dialog.subtitle}</p>
              </div>
            </div>
            <div className="chat-header__actions">
              <LinkButton to="/app/tutor/requests" variant="ghost">
                Заявка
              </LinkButton>
              <LinkButton to="/app/tutor/students" variant="ghost">
                Ученик
              </LinkButton>
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
                <p>Примите заявку или дождитесь сообщения от клиента, чтобы начать переписку.</p>
              </div>
            )}
          </div>

          <form className="message-composer" onSubmit={(event) => void handleSubmit(event)}>
            <button type="button" aria-label="Прикрепить файл" disabled={!activeThreadId}>
              +
            </button>
            <input
              id="tutor-message-input"
              name="message"
              placeholder="Напишите сообщение родителю"
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
              <h2>Карточка заявки</h2>
              <span>активно</span>
            </div>
            <dl className="message-lesson-details">
              {pageData.details.map((detail) => (
                <div key={detail.label}>
                  <dt>{detail.label}</dt>
                  <dd>{detail.value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="messages-context-card">
            <h2>Материалы</h2>
            <div className="message-attachments">
              {pageData.materials.map((material) => (
                <article key={material.name}>
                  <span>□</span>
                  <div>
                    <strong>{material.name}</strong>
                    <p>{material.meta}</p>
                  </div>
                </article>
              ))}
            </div>
          </section>

          <section className="messages-context-card messages-context-card--safety">
            <h2>CRM-подсказка</h2>
            <p>После принятия заявки создайте занятие: диалог, ученик и календарь останутся связаны в истории CRM.</p>
          </section>
        </aside>
      </section>
    </div>
  );
}
