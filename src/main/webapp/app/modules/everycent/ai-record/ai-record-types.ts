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

export type AssistantResponseCard = {
  type?: string;
  title?: string;
  message?: string;
  data?: unknown;
};

export type AssistantSkillResult = {
  actionName?: string;
  success?: boolean;
  errorCode?: string;
  message?: string;
  needUserConfirmation?: boolean;
  blockedByPolicy?: boolean;
};

export type AssistantChatResponse = {
  conversationId?: number;
  messageId?: number;
  assistantMessage?: string;
  responseType?: string;
  cards?: AssistantResponseCard[];
  skillResults?: AssistantSkillResult[];
  accountingCapture?: {
    captured?: boolean;
    created?: boolean;
    needConfirmation?: boolean;
    transactionId?: number;
    amount?: number | string;
    type?: string;
    transactionDate?: string;
  };
};

export type AiChatMessage = {
  id: string;
  role: AiChatRole;
  content: string;
  createdAt: string;
  preview?: TransactionParsePreview;
  cards?: AssistantResponseCard[];
};

export type AssistantChatHistory = {
  conversationId?: number;
  ledgerId?: number;
  messages?: AiChatMessage[];
};
