import React, { useEffect, useRef } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { AiChatMessage } from './ai-record-types';
import ParsePreviewCard from './parse-preview-card';

const formatCardData = (data: unknown) => {
  if (data === undefined || data === null) return '';
  if (Array.isArray(data)) return `共 ${data.length} 条`;
  if (typeof data === 'string' || typeof data === 'number' || typeof data === 'boolean') return String(data);
  if (typeof data === 'object') return JSON.stringify(data, null, 2);
  return '';
};

const cardClassName = (type?: string) => `everycent-result-card everycent-result-card--${type ?? 'result'}`;

type ChatMessageListProps = {
  confirmingMessageId?: string;
  loading?: boolean;
  messages: AiChatMessage[];
  onConfirmPreview?: (message: AiChatMessage) => void;
};

const ChatMessageList = ({ confirmingMessageId, loading, messages, onConfirmPreview }: ChatMessageListProps) => {
  const endRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [loading, messages]);

  return (
    <div className="everycent-chat__messages">
      {messages.map(message => (
        <article key={message.id} className={`everycent-chat-message everycent-chat-message--${message.role}`}>
          <div className="everycent-chat-message__avatar">{message.role === 'assistant' ? <FontAwesomeIcon icon="pencil-alt" /> : 'U'}</div>
          <div className="everycent-chat-message__body">
            <strong>{message.role === 'assistant' ? 'EveryCent AI' : '你'}</strong>
            <p>{message.content}</p>
            {message.preview && (
              <ParsePreviewCard
                confirming={confirmingMessageId === message.id}
                onConfirm={onConfirmPreview ? () => onConfirmPreview(message) : undefined}
                preview={message.preview}
              />
            )}
            {message.cards?.map((card, index) => (
              <section key={`${message.id}-card-${index}`} className={cardClassName(card.type)}>
                <header>
                  <span>{card.type ?? 'result'}</span>
                  <h2>{card.title ?? '结果'}</h2>
                </header>
                {card.message && <p>{card.message}</p>}
                {formatCardData(card.data) && <pre>{formatCardData(card.data)}</pre>}
              </section>
            ))}
          </div>
        </article>
      ))}

      {loading && (
        <article className="everycent-chat-message everycent-chat-message--assistant">
          <div className="everycent-chat-message__avatar">
            <FontAwesomeIcon icon="pencil-alt" />
          </div>
          <div className="everycent-chat-message__body">
            <strong>EveryCent AI</strong>
            <p>正在处理你的请求...</p>
          </div>
        </article>
      )}
      <div ref={endRef} />
    </div>
  );
};

export default ChatMessageList;
