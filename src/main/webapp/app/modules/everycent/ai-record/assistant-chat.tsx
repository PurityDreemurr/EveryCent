import React, { useEffect, useMemo, useState } from 'react';
import { v4 as uuidv4 } from 'uuid';

import { getLedgers, Ledger } from '../ledger/ledger-api';
import { createTransactionFromNaturalLanguage, parseTransactionText } from './ai-record-api';
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
  const [confirmingMessageId, setConfirmingMessageId] = useState<string>();
  const [ledgerError, setLedgerError] = useState('');
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [messages, setMessages] = useState<AiChatMessage[]>([]);
  const [selectedLedgerId, setSelectedLedgerId] = useState<number>();
  const [loading, setLoading] = useState(false);
  const isEmpty = messages.length === 0;
  const selectedLedger = useMemo(() => ledgers.find(ledger => ledger.id === selectedLedgerId), [selectedLedgerId, ledgers]);

  useEffect(() => {
    let mounted = true;

    getLedgers()
      .then(data => {
        if (!mounted) return;
        setLedgers(data);
        setSelectedLedgerId(current => current ?? data[0]?.id);
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

    const preview = await parseTransactionText(selectedLedgerId, text);
    const responseText =
      preview.source === 'api'
        ? `已解析到 ${selectedLedger?.name ?? '当前账本'}，请确认后入账。`
        : '后端解析接口暂不可用，我先生成了一条本地预览。';

    setMessages(current => [...current, createMessage('assistant', responseText, preview)]);
    setLoading(false);
  };

  const confirmPreview = async (message: AiChatMessage) => {
    if (!selectedLedgerId || !message.preview || message.preview.source !== 'api') return;

    const rawInput = message.preview.rawInput ?? message.preview.remark;
    if (!rawInput) return;

    setConfirmingMessageId(message.id);
    try {
      const created = await createTransactionFromNaturalLanguage(selectedLedgerId, rawInput, message.preview.transactionDate);

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
