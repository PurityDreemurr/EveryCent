export type AiChatRole = 'assistant' | 'user';

export type TransactionParsePreview = {
  amount?: number;
  type?: string;
  behaviorTagId?: number;
  behaviorTag?: string;
  emotionTagId?: number;
  moodTag?: string;
  transactionDate?: string;
  remark?: string;
  rawInput?: string;
  confidence?: number;
  needsManualReview?: boolean;
  source?: 'api' | 'fallback';
  transactionId?: number;
  created?: boolean;
};

export type AiChatMessage = {
  id: string;
  role: AiChatRole;
  content: string;
  createdAt: string;
  preview?: TransactionParsePreview;
};
