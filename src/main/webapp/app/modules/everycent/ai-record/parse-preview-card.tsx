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
  if (confidence === undefined) return 'Review';
  return `${Math.round(confidence * 100)}%`;
};

const ParsePreviewCard = ({ preview }: ParsePreviewCardProps) => (
  <section className="everycent-parse-card">
    <header>
      <div>
        <span className="everycent-parse-card__label">Transaction preview</span>
        <h2>{preview.type === 'income' ? 'Income record' : 'Expense record'}</h2>
      </div>
      <strong>{formatConfidence(preview.confidence)}</strong>
    </header>

    <dl>
      <div>
        <dt>Amount</dt>
        <dd>{formatAmount(preview.amount)}</dd>
      </div>
      <div>
        <dt>Type</dt>
        <dd>{preview.type ?? '-'}</dd>
      </div>
      <div>
        <dt>Behavior</dt>
        <dd>{preview.behaviorTag ?? '-'}</dd>
      </div>
      <div>
        <dt>Mood</dt>
        <dd>{preview.moodTag ?? '-'}</dd>
      </div>
    </dl>

    {preview.remark && <p>{preview.remark}</p>}

    <footer>
      <button type="button" className="everycent-primary-button">
        Confirm
      </button>
      <button type="button" className="everycent-secondary-button">
        Edit
      </button>
      {preview.source === 'fallback' && <span>Local preview. Backend API is not connected yet.</span>}
    </footer>
  </section>
);

export default ParsePreviewCard;
