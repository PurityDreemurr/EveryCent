import React, { useEffect, useRef, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

import { getLedgers, Ledger } from '../ledger/ledger-api';
import { readSelectedLedgerId, subscribeSelectedLedgerChange, writeSelectedLedgerId } from '../ledger/ledger-selection';
import {
  clearAssistantChatHistory,
  createTransactionFromPreview,
  getAssistantChatHistory,
  sendAssistantChatMessage,
} from './ai-record-api';
import { AiChatMessage } from './ai-record-types';
import ChatComposer from './chat-composer';
import ChatMessageList from './chat-message-list';

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

const assistantErrorMessage = (error: unknown, fallback: string) => {
  if (!axios.isAxiosError(error)) return fallback;
  const data = error.response?.data;
  if (data && typeof data === 'object') {
    const response = data as Record<string, unknown>;
    for (const key of ['detail', 'message', 'title']) {
      const value = response[key];
      if (typeof value === 'string' && value.trim()) return value;
    }
  }
  return fallback;
};

const AssistantChat = () => {
  const [confirmingMessageId, setConfirmingMessageId] = useState<string>();
  const [ledgerError, setLedgerError] = useState('');
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [messages, setMessages] = useState<AiChatMessage[]>([]);
  const [conversationId, setConversationId] = useState<number>();
  const [selectedLedgerId, setSelectedLedgerId] = useState<number | undefined>(readSelectedLedgerId);
  const [clearing, setClearing] = useState(false);
  const [loading, setLoading] = useState(false);
  const submittingRef = useRef(false);
  const isEmpty = messages.length === 0;

  useEffect(() => {
    if (!selectedLedgerId) {
      setMessages([]);
      setConversationId(undefined);
      return;
    }

    let mounted = true;
    getAssistantChatHistory(selectedLedgerId)
      .then(history => {
        if (!mounted) return;
        setConversationId(history.conversationId);
        setMessages(Array.isArray(history.messages) ? history.messages : []);
      })
      .catch(() => {
        if (!mounted) return;
        setMessages([]);
        setConversationId(undefined);
      });

    return () => {
      mounted = false;
    };
  }, [selectedLedgerId]);

  useEffect(() => {
    let mounted = true;

    getLedgers()
      .then(data => {
        if (!mounted) return;
        setLedgers(data);
        setSelectedLedgerId(current => {
          if (current && data.some(ledger => ledger.id === current)) {
            return current;
          }
          const defaultLedgerId = data[0]?.id;
          if (defaultLedgerId) {
            writeSelectedLedgerId(defaultLedgerId);
          }
          return defaultLedgerId;
        });
      })
      .catch(() => {
        if (!mounted) return;
        setLedgerError('账本列表加载失败，请确认已经登录并且后端服务可用。');
      });

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(
    () =>
      subscribeSelectedLedgerChange(ledgerId => {
        setSelectedLedgerId(ledgerId);
      }),
    [],
  );

  const handleLedgerChange = (ledgerId?: number) => {
    setSelectedLedgerId(ledgerId);
    writeSelectedLedgerId(ledgerId);
  };

  const submitMessage = async (text: string) => {
    if (submittingRef.current) return;
    submittingRef.current = true;

    if (!selectedLedgerId) {
      setMessages(current => [
        ...current,
        createMessage('user', text),
        createMessage('assistant', ledgers.length === 0 ? '还没有可用账本，请先创建账本后再使用 AI 记账。' : '请先选择要记入的账本。'),
      ]);
      submittingRef.current = false;
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
    } catch (error) {
      setMessages(current => [
        ...current,
        createMessage('assistant', assistantErrorMessage(error, '暂时无法连接 Assistant Chat，请稍后再试。')),
      ]);
    } finally {
      submittingRef.current = false;
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

  const clearCurrentHistory = async () => {
    if (!selectedLedgerId || clearing || loading || messages.length === 0) return;

    setClearing(true);
    try {
      await clearAssistantChatHistory(selectedLedgerId);
      setMessages([]);
      setConversationId(undefined);
    } catch {
      setMessages(current => [...current, createMessage('assistant', '暂时没有清除成功，请稍后再试。')]);
    } finally {
      setClearing(false);
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
          onChange={event => handleLedgerChange(event.target.value ? Number(event.target.value) : undefined)}
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
        <button
          type="button"
          className="everycent-chat__clear"
          disabled={!selectedLedgerId || loading || clearing || messages.length === 0}
          onClick={clearCurrentHistory}
          title="清除当前账本对话"
          aria-label="清除当前账本对话"
        >
          <FontAwesomeIcon icon="trash" />
          <span>{clearing ? '清除中' : '清除'}</span>
        </button>
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
