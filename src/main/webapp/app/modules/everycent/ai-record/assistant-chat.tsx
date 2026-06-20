import React, { useEffect, useState } from 'react';
import { v4 as uuidv4 } from 'uuid';

import { getLedgers, Ledger } from '../ledger/ledger-api';
import { createTransactionFromPreview, sendAssistantChatMessage } from './ai-record-api';
import { AiChatMessage } from './ai-record-types';
import ChatComposer from './chat-composer';
import ChatMessageList from './chat-message-list';

const CHAT_MESSAGES_STORAGE_KEY = 'everycent.aiRecord.chat.messages';
const SELECTED_LEDGER_STORAGE_KEY = 'everycent.aiRecord.selectedLedgerId';

const createMessage = (
  role: AiChatMessage['role'],
  content: string,
  preview?: AiChatMessage['preview'],
  cards?: AiChatMessage['cards'],
): AiChatMessage => ({
  id: uuidv4(),
  role,
  content,
  createdAt: new Date().toISOString(),
  preview,
  cards,
});

const readStoredMessages = (): AiChatMessage[] => {
  try {
    const raw = window.sessionStorage.getItem(CHAT_MESSAGES_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const readStoredLedgerId = () => {
  try {
    const raw = window.sessionStorage.getItem(SELECTED_LEDGER_STORAGE_KEY);
    if (!raw) return undefined;
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : undefined;
  } catch {
    return undefined;
  }
};

const AssistantChat = () => {
  const [confirmingMessageId, setConfirmingMessageId] = useState<string>();
  const [ledgerError, setLedgerError] = useState('');
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [messages, setMessages] = useState<AiChatMessage[]>(readStoredMessages);
  const [conversationId, setConversationId] = useState<number>();
  const [selectedLedgerId, setSelectedLedgerId] = useState<number | undefined>(readStoredLedgerId);
  const [loading, setLoading] = useState(false);
  const isEmpty = messages.length === 0;

  useEffect(() => {
    try {
      window.sessionStorage.setItem(CHAT_MESSAGES_STORAGE_KEY, JSON.stringify(messages));
    } catch {
      // Ignore storage failures; chat still works for the current render.
    }
  }, [messages]);

  useEffect(() => {
    try {
      if (selectedLedgerId) {
        window.sessionStorage.setItem(SELECTED_LEDGER_STORAGE_KEY, String(selectedLedgerId));
      } else {
        window.sessionStorage.removeItem(SELECTED_LEDGER_STORAGE_KEY);
      }
    } catch {
      // Ignore storage failures; ledger selection can be restored from API defaults.
    }
  }, [selectedLedgerId]);

  useEffect(() => {
    let mounted = true;

    getLedgers()
      .then(data => {
        if (!mounted) return;
        setLedgers(data);
        setSelectedLedgerId(current => (current && data.some(ledger => ledger.id === current) ? current : data[0]?.id));
      })
      .catch(() => {
        if (!mounted) return;
        setLedgerError('账本列表加载失败，请确认已经登录并且后端服务可用。');
      });

    return () => {
      mounted = false;
    };
  }, []);

  const submitMessage = async (text: string) => {
    if (!selectedLedgerId) {
      setMessages(current => [
        ...current,
        createMessage('user', text),
        createMessage('assistant', ledgers.length === 0 ? '还没有可用账本，请先创建账本后再使用 AI 记账。' : '请先选择要记入的账本。'),
      ]);
      return;
    }

    setMessages(current => [...current, createMessage('user', text)]);
    setLoading(true);

    try {
      const response = await sendAssistantChatMessage(text, selectedLedgerId, conversationId);
      setConversationId(response.conversationId);
      setMessages(current => [
        ...current,
        createMessage('assistant', response.assistantMessage ?? '已处理。', undefined, response.cards ?? []),
      ]);
    } catch {
      setMessages(current => [...current, createMessage('assistant', '暂时无法连接 Assistant Chat，请稍后再试。')]);
    } finally {
      setLoading(false);
    }
  };

  const confirmPreview = async (message: AiChatMessage) => {
    if (!selectedLedgerId || !message.preview || message.preview.source !== 'api') return;

    setConfirmingMessageId(message.id);
    try {
      const created = await createTransactionFromPreview(selectedLedgerId, message.preview);

      setMessages(current =>
        current.map(item =>
          item.id === message.id
            ? {
                ...item,
                content: `已入账：${created.behaviorTag ?? item.preview?.behaviorTag ?? '记账记录'} ${created.amount ?? item.preview?.amount ?? ''} 元。`,
                preview: {
                  ...(item.preview ?? {}),
                  ...created,
                  created: true,
                  transactionId: created.transactionId,
                  transactionDate: created.transactionDate,
                },
              }
            : item,
        ),
      );
    } catch {
      setMessages(current => [...current, createMessage('assistant', '暂时没有入账成功，请稍后再试，或检查这条记录的信息是否完整。')]);
    } finally {
      setConfirmingMessageId(undefined);
    }
  };

  return (
    <section className={`everycent-chat${isEmpty ? ' everycent-chat--empty' : ' everycent-chat--thread'}`} aria-label="AI transaction chat">
      <div className="everycent-chat__ledger">
        <label htmlFor="ai-record-ledger">记入账本</label>
        <select
          id="ai-record-ledger"
          value={selectedLedgerId ?? ''}
          disabled={loading || ledgers.length === 0}
          onChange={event => setSelectedLedgerId(event.target.value ? Number(event.target.value) : undefined)}
        >
          {ledgers.length === 0 ? (
            <option value="">暂无账本</option>
          ) : (
            ledgers.map(ledger => (
              <option key={ledger.id} value={ledger.id}>
                {ledger.name}
              </option>
            ))
          )}
        </select>
        {ledgerError && <span>{ledgerError}</span>}
      </div>
      {isEmpty ? (
        <div className="everycent-chat-home">
          <h2>今天从哪里开始记？</h2>
          <ChatComposer disabled={loading} onSubmit={submitMessage} variant="hero" />
        </div>
      ) : (
        <>
          <ChatMessageList
            confirmingMessageId={confirmingMessageId}
            messages={messages}
            loading={loading}
            onConfirmPreview={confirmPreview}
          />
          <ChatComposer disabled={loading} onSubmit={submitMessage} />
        </>
      )}
    </section>
  );
};

export default AssistantChat;
