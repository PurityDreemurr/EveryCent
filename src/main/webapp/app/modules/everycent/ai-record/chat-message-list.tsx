import React, { useEffect, useRef } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { AiChatMessage } from './ai-record-types';
import ParsePreviewCard from './parse-preview-card';

type ChatMessageListProps = {
  loading?: boolean;
  messages: AiChatMessage[];
};

const ChatMessageList = ({ loading, messages }: ChatMessageListProps) => {
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
            {message.preview && <ParsePreviewCard preview={message.preview} />}
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
            <p>正在解析你的记账内容...</p>
          </div>
        </article>
      )}
      <div ref={endRef} />
    </div>
  );
};

export default ChatMessageList;
