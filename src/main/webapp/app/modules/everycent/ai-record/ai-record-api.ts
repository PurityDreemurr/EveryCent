import axios from 'axios';

import { TransactionParsePreview } from './ai-record-types';

type ParseTransactionResponse = {
  amount?: number | string;
  type?: string;
  behaviorTagCode?: string;
  behaviorTagName?: string;
  behaviorTagId?: number;
  emotionTagCode?: string;
  emotionTagName?: string;
  emotionTagId?: number;
  transactionDate?: string;
  description?: string;
  confidence?: number;
  needUserConfirm?: boolean;
  rawInput?: string;
};

type NaturalLanguageCreateResponse = {
  transactionId?: number;
  amount?: number | string;
  type?: string;
  behaviorTagName?: string;
  emotionTagName?: string;
  parsedResult?: {
    amount?: number | string;
    type?: string;
    behaviorTag?: string;
    emotionTag?: string;
  };
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

const normalizeType = (type?: string) => {
  if (type === 'INCOME') return 'income';
  if (type === 'EXPENSE') return 'expense';
  return type?.toLowerCase();
};

export const parseTransactionText = async (ledgerId: number, text: string): Promise<TransactionParsePreview> => {
  try {
    const response = await axios.post<ParseTransactionResponse>('/api/ai/transaction/parse', {
      ledgerId,
      text,
    });

    return {
      amount: normalizeAmount(response.data.amount),
      type: normalizeType(response.data.type),
      behaviorTagId: response.data.behaviorTagId,
      behaviorTag: response.data.behaviorTagName ?? response.data.behaviorTagCode,
      emotionTagId: response.data.emotionTagId,
      moodTag: response.data.emotionTagName ?? response.data.emotionTagCode,
      transactionDate: response.data.transactionDate,
      remark: response.data.description ?? text,
      rawInput: response.data.rawInput ?? text,
      confidence: response.data.confidence,
      needsManualReview: response.data.needUserConfirm,
      source: 'api',
    };
  } catch {
    return createFallbackPreview(text);
  }
};

export const createTransactionFromNaturalLanguage = async (ledgerId: number, text: string, transactionDate?: string) => {
  const response = await axios.post<NaturalLanguageCreateResponse>(`/api/ledgers/${ledgerId}/transactions/natural-language`, {
    text,
    transactionDate,
    confirm: true,
  });

  return {
    transactionId: response.data.transactionId,
    amount: normalizeAmount(response.data.amount ?? response.data.parsedResult?.amount),
    type: normalizeType(response.data.type ?? response.data.parsedResult?.type),
    behaviorTag: response.data.behaviorTagName ?? response.data.parsedResult?.behaviorTag,
    moodTag: response.data.emotionTagName ?? response.data.parsedResult?.emotionTag,
  };
};
