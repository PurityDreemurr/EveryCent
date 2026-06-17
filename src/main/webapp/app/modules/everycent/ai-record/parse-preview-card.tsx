import React from 'react';

import { TransactionParsePreview } from './ai-record-types';

type ParsePreviewCardProps = {
  preview: TransactionParsePreview;
};

const formatAmount = (amount?: number) => {
  if (amount === undefined) return '-';
  return amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const formatConfidence = (confidence?: number) => {
  if (confidence === undefined) return '待确认';
  return `${Math.round(confidence * 100)}%`;
};

const formatType = (type?: string) => {
  if (type === 'income') return '收入';
  if (type === 'expense') return '支出';
  return '-';
};

const ParsePreviewCard = ({ preview }: ParsePreviewCardProps) => (
  <section className="everycent-parse-card">
    <header>
      <div>
        <span className="everycent-parse-card__label">解析预览</span>
        <h2>{preview.type === 'income' ? '收入记录' : '支出记录'}</h2>
      </div>
      <strong>{formatConfidence(preview.confidence)}</strong>
    </header>

    <dl>
      <div>
        <dt>金额</dt>
        <dd>{formatAmount(preview.amount)}</dd>
      </div>
      <div>
        <dt>类型</dt>
        <dd>{formatType(preview.type)}</dd>
      </div>
      <div>
        <dt>行为</dt>
        <dd>{preview.behaviorTag ?? '-'}</dd>
      </div>
      <div>
        <dt>情绪</dt>
        <dd>{preview.moodTag ?? '-'}</dd>
      </div>
    </dl>

    {preview.remark && <p>{preview.remark}</p>}

    <footer>
      <button type="button" className="everycent-primary-button">
        确认入账
      </button>
      <button type="button" className="everycent-secondary-button">
        继续编辑
      </button>
      {preview.source === 'fallback' && <span>本地预览，后端解析接口暂未连接。</span>}
    </footer>
  </section>
);

export default ParsePreviewCard;
