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
      preview.source === 'api' ? '我已把这段内容解析成一条记账记录，请确认后再保存。' : '后端解析接口暂不可用，我先生成了一条本地预览。';

    setMessages(current => [...current, createMessage('assistant', responseText, preview)]);
    setLoading(false);
  };

  return (
    <section className={`everycent-chat${isEmpty ? ' everycent-chat--empty' : ' everycent-chat--thread'}`} aria-label="AI transaction chat">
      {isEmpty ? (
        <div className="everycent-chat-home">
          <h2>今天从哪里开始记？</h2>
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
