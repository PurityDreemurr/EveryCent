export type AiChatRole = 'assistant' | 'user';

export type TransactionParsePreview = {
  amount?: number;
  type?: string;
  behaviorTag?: string;
  moodTag?: string;
  remark?: string;
  confidence?: number;
  needsManualReview?: boolean;
  source?: 'api' | 'fallback';
};

export type AiChatMessage = {
  id: string;
  role: AiChatRole;
  content: string;
  createdAt: string;
  preview?: TransactionParsePreview;
};
