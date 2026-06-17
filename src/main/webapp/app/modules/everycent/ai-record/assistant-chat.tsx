import React, { useState } from 'react';
import { v4 as uuidv4 } from 'uuid';

import { parseTransactionText } from './ai-record-api';
import { AiChatMessage } from './ai-record-types';
import ChatComposer from './chat-composer';
import ChatMessageList from './chat-message-list';

const createMessage = (role: AiChatMessage['role'], content: string, preview?: AiChatMessage['preview']): AiChatMessage => ({
  id: uuidv4(),
  role,
  content,
  createdAt: new Date().toISOString(),
  preview,
});

const AssistantChat = () => {
  const [messages, setMessages] = useState<AiChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const isEmpty = messages.length === 0;

  const submitMessage = async (text: string) => {
    setMessages(current => [...current, createMessage('user', text)]);
    setLoading(true);

    const preview = await parseTransactionText(text);
    const responseText =
      preview.source === 'api'
        ? 'I parsed this into a transaction. Please review it before saving.'
        : 'I made a local preview because the backend parse API is not available yet.';

    setMessages(current => [...current, createMessage('assistant', responseText, preview)]);
    setLoading(false);
  };

  return (
    <section className={`everycent-chat${isEmpty ? ' everycent-chat--empty' : ' everycent-chat--thread'}`} aria-label="AI transaction chat">
      {isEmpty ? (
        <div className="everycent-chat-home">
          <h2>Where should we begin?</h2>
          <ChatComposer disabled={loading} onSubmit={submitMessage} variant="hero" />
        </div>
      ) : (
        <>
          <ChatMessageList messages={messages} loading={loading} />
          <ChatComposer disabled={loading} onSubmit={submitMessage} />
        </>
      )}
    </section>
  );
};

export default AssistantChat;
