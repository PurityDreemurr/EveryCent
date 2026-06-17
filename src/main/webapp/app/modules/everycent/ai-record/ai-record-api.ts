import axios from 'axios';

import { TransactionParsePreview } from './ai-record-types';

type ParseTransactionResponse = {
  amount?: number | string;
  type?: string;
  behaviorTag?: string;
  moodTag?: string;
  remark?: string;
  confidence?: number;
  needsManualReview?: boolean;
};

const normalizeAmount = (amount: ParseTransactionResponse['amount']) => {
  if (typeof amount === 'number') return amount;
  if (typeof amount === 'string') {
    const parsed = Number(amount);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  return undefined;
};

const createFallbackPreview = (text: string): TransactionParsePreview => {
  const amountMatch = text.match(/(?:￥|\$)?\s*(\d+(?:\.\d{1,2})?)/);
  const amount = amountMatch ? Number(amountMatch[1]) : undefined;
  const lowerText = text.toLowerCase();
  const isIncome = /salary|income|paid|bonus|工资|收入|到账|奖金/.test(lowerText);

  return {
    amount,
    type: isIncome ? 'income' : 'expense',
    behaviorTag: isIncome ? '收入' : '普通',
    moodTag: '平静',
    remark: text,
    confidence: amount ? 0.62 : 0.28,
    needsManualReview: true,
    source: 'fallback',
  };
};

export const parseTransactionText = async (text: string): Promise<TransactionParsePreview> => {
  try {
    const response = await axios.post<ParseTransactionResponse>('/api/ai/transaction/parse', {
      text,
      naturalLanguageText: text,
    });

    return {
      amount: normalizeAmount(response.data.amount),
      type: response.data.type,
      behaviorTag: response.data.behaviorTag,
      moodTag: response.data.moodTag,
      remark: response.data.remark ?? text,
      confidence: response.data.confidence,
      needsManualReview: response.data.needsManualReview,
      source: 'api',
    };
  } catch {
    return createFallbackPreview(text);
  }
};
