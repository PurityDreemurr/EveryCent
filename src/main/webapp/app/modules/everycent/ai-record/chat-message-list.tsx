import React, { useEffect, useRef } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { AiChatMessage } from './ai-record-types';
import EmotionTagBadge from '../shared/emotion-tag-badge';
import { exportLedgerTransactions } from '../export/export-api';
import ParsePreviewCard from './parse-preview-card';

type AnyRecord = Record<string, unknown>;

const isObject = (value: unknown): value is AnyRecord => typeof value === 'object' && value !== null && !Array.isArray(value);

const asArray = (value: unknown): AnyRecord[] => (Array.isArray(value) ? value.filter(isObject) : []);

const primitiveText = (value: unknown, fallback = '-') => {
  if (typeof value === 'string') return value === '' ? fallback : value;
  if (typeof value === 'number' || typeof value === 'boolean') return `${value}`;
  return fallback;
};

const textValue = (value: unknown, fallback = '-') => {
  if (value === undefined || value === null || value === '') return fallback;
  return primitiveText(value, fallback);
};

const numberValue = (value: unknown) => {
  const parsed = typeof value === 'number' ? value : typeof value === 'string' ? Number(value) : 0;
  return Number.isFinite(parsed) ? parsed : 0;
};

const formatAmount = (value: unknown) => `¥${numberValue(value).toFixed(2)}`;

const transactionType = (item: AnyRecord) => textValue(item.type).toUpperCase();

const transactionTypeLabel = (type: unknown) => (textValue(type).toUpperCase() === 'INCOME' ? '收入' : '支出');

const signedAmount = (item: AnyRecord) => {
  const amount = formatAmount(item.amount);
  return `${transactionType(item) === 'INCOME' ? '+' : '-'}${amount}`;
};

const transactionName = (item: AnyRecord) =>
  textValue(item.description, textValue(item.rawInput, textValue(item.behaviorTagName, '未命名记录')));

const isTransactionPage = (data: unknown): data is AnyRecord => isObject(data) && Array.isArray(data.content);

const isTransactionRecord = (data: unknown): data is AnyRecord =>
  isObject(data) && data.amount !== undefined && (data.recordDate !== undefined || data.transactionDate !== undefined);

const isDownloadResult = (data: unknown): data is AnyRecord => isObject(data) && typeof data.downloadUrl === 'string';

const humanKey = (key: string) =>
  ({
    totalElements: '总条数',
    page: '页码',
    size: '每页数量',
    totalIncome: '收入合计',
    incomeTotal: '收入合计',
    totalExpense: '支出合计',
    expenseTotal: '支出合计',
    balance: '结余',
    transactionCount: '流水数',
    budgetUsedRatio: '预算使用率',
    budgetUsedRate: '预算使用率',
    budgetAlertLevel: '预算状态',
    limitAmount: '预算额度',
    amount: '金额',
    usedAmount: '已使用',
    remainingAmount: '剩余额度',
    usedRatio: '使用率',
    usageRate: '使用率',
    periodStart: '开始日期',
    periodEnd: '结束日期',
    cycle: '周期',
    status: '状态',
    overBudget: '是否超预算',
    enabled: '是否启用',
    tagName: '标签',
    count: '笔数',
    ratio: '占比',
    percentage: '占比',
    byteLength: '文件大小',
  })[key] ?? key;

const formatReadableValue = (key: string, value: unknown) => {
  if (value === undefined || value === null || value === '') return '-';
  if (/emotionTagName|emotionTag|moodTag/i.test(key)) return <EmotionTagBadge name={primitiveText(value)} size="sm" />;
  if (/amount|income|expense|balance/i.test(key)) return formatAmount(value);
  if (/ratio|rate|percentage/i.test(key)) return `${(numberValue(value) * 100).toFixed(1)}%`;
  if (typeof value === 'boolean') return value ? '是' : '否';
  return primitiveText(value);
};

const formatBytes = (value: unknown) => {
  const bytes = numberValue(value);
  if (bytes <= 0) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
};

const downloadBlob = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

const renderTransactionRows = (records: AnyRecord[]) => (
  <ul className="everycent-result-list">
    {records.map((item, index) => (
      <li key={`${textValue(item.id, 'transaction')}-${index}`}>
        <div>
          <strong>{transactionName(item)}</strong>
          <span>
            {textValue(item.recordDate ?? item.transactionDate)} · {transactionTypeLabel(item.type)} ·{' '}
            {textValue(item.behaviorTagName, '未分类')}
          </span>
        </div>
        <b className={transactionType(item) === 'INCOME' ? 'is-income' : 'is-expense'}>{signedAmount(item)}</b>
      </li>
    ))}
  </ul>
);

const renderTransactionPage = (data: AnyRecord) => {
  const records = asArray(data.content);
  const incomeTotal = records
    .filter(item => transactionType(item) === 'INCOME')
    .reduce((total, item) => total + numberValue(item.amount), 0);
  const expenseTotal = records
    .filter(item => transactionType(item) !== 'INCOME')
    .reduce((total, item) => total + numberValue(item.amount), 0);

  return (
    <div className="everycent-result-readable">
      <dl className="everycent-result-summary">
        <div>
          <dt>匹配记录</dt>
          <dd>{textValue(data.totalElements, '0')} 条</dd>
        </div>
        <div>
          <dt>当前展示</dt>
          <dd>{records.length} 条</dd>
        </div>
        <div>
          <dt>收入合计</dt>
          <dd>{formatAmount(incomeTotal)}</dd>
        </div>
        <div>
          <dt>支出合计</dt>
          <dd>{formatAmount(expenseTotal)}</dd>
        </div>
      </dl>
      {records.length > 0 ? renderTransactionRows(records) : <p>没有查到符合条件的账单记录。</p>}
    </div>
  );
};

const renderKeyValueData = (data: AnyRecord) => {
  const hiddenKeys = new Set(['content', 'id', 'ledgerId']);
  const entries = Object.entries(data).filter(([, value]) => !Array.isArray(value) && !isObject(value));
  const visibleEntries = entries.filter(([key]) => !hiddenKeys.has(key));
  if (visibleEntries.length === 0) return null;

  return (
    <dl className="everycent-result-summary">
      {visibleEntries.map(([key, value]) => (
        <div key={key}>
          <dt>{humanKey(key)}</dt>
          <dd>{formatReadableValue(key, value)}</dd>
        </div>
      ))}
    </dl>
  );
};

const renderReadableData = (data: unknown) => {
  if (data === undefined || data === null) return null;
  if (isTransactionPage(data)) return renderTransactionPage(data);
  if (isTransactionRecord(data)) return renderTransactionRows([data]);
  if (Array.isArray(data)) {
    const records = asArray(data);
    if (records.length > 0 && records.every(isTransactionRecord)) return renderTransactionRows(records);
    return <p>共 {data.length} 条结果。</p>;
  }
  if (isObject(data)) return renderKeyValueData(data);
  return <p>{primitiveText(data, '')}</p>;
};

const renderDownloadData = (data: unknown, onDownload: (data: AnyRecord) => void) => {
  if (!isDownloadResult(data)) return renderReadableData(data);

  return (
    <div className="everycent-download-card">
      <dl className="everycent-result-summary">
        <div>
          <dt>导出范围</dt>
          <dd>
            {textValue(data.startDate)} 至 {textValue(data.endDate)}
          </dd>
        </div>
        <div>
          <dt>文件大小</dt>
          <dd>{formatBytes(data.byteLength)}</dd>
        </div>
      </dl>
      <button type="button" className="everycent-download-card__button" onClick={() => onDownload(data)}>
        <FontAwesomeIcon icon="save" />
        <span>下载账单</span>
      </button>
    </div>
  );
};

const cardClassName = (type?: string) => `everycent-result-card everycent-result-card--${type ?? 'result'}`;

const displayMessage = (content: string) => content.replace(/\s*\{\s*"mood"\s*:\s*\d+\s*,\s*"emoji"\s*:\s*"[^"]+"\s*\}\s*$/u, '').trim();

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

  const downloadExport = async (data: AnyRecord) => {
    const ledgerId = Number(data.ledgerId);
    const startDate = textValue(data.startDate, '');
    const endDate = textValue(data.endDate, '');
    if (!Number.isFinite(ledgerId) || !startDate || !endDate) return;

    const result = await exportLedgerTransactions(ledgerId, { startDate, endDate });
    downloadBlob(result.blob, textValue(data.fileName, result.filename));
  };

  return (
    <div className="everycent-chat__messages">
      {messages.map(message => (
        <article key={message.id} className={`everycent-chat-message everycent-chat-message--${message.role}`}>
          <div className="everycent-chat-message__avatar">
            {message.role === 'assistant' ? <img src="/content/images/emotions/surprised.png" alt="喵喵" /> : 'U'}
          </div>
          <div className="everycent-chat-message__body">
            <strong>{message.role === 'assistant' ? '喵喵' : '你'}</strong>
            <p>{message.role === 'assistant' ? displayMessage(message.content) : message.content}</p>
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
                {card.type === 'download_result' ? renderDownloadData(card.data, downloadExport) : renderReadableData(card.data)}
              </section>
            ))}
          </div>
        </article>
      ))}

      {loading && (
        <article className="everycent-chat-message everycent-chat-message--assistant">
          <div className="everycent-chat-message__avatar">
            <img src="/content/images/emotions/surprised.png" alt="喵喵" />
          </div>
          <div className="everycent-chat-message__body">
            <strong>喵喵</strong>
            <p>正在处理你的请求...</p>
          </div>
        </article>
      )}
      <div ref={endRef} />
    </div>
  );
};

export default ChatMessageList;
