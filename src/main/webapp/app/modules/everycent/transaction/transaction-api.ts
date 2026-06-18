import axios from 'axios';

export type TransactionType = string;

export interface TransactionRecord {
  id: number;
  ledgerId: number;
  type: TransactionType;
  amount: string;
  description: string;
  recordDate: string;
  behaviorTagId: number;
  behaviorTagName?: string;
  emotionTagId: number;
  emotionTagName?: string;
  source: string;
  rawInput?: null | string;
  creatorId?: number;
  createdBy?: number;
  createdDate?: string;
  lastModifiedDate?: string;
}

export interface TransactionPage {
  content: TransactionRecord[];
  totalElements: number;
  page: number;
  size: number;
}

export interface TransactionQuery {
  page?: number;
  size?: number;
  type?: string;
  startDate?: string;
  endDate?: string;
  behaviorTagId?: string;
  emotionTagId?: string;
}

export interface TransactionPayload {
  type: string;
  amount: string;
  description: string;
  recordDate: string;
  behaviorTagId: number;
  emotionTagId: number;
  source: string;
}

const ledgerTransactionsUrl = (ledgerId: number) => `api/ledgers/${ledgerId}/transactions`;
const transactionUrl = (transactionId: number) => `api/transactions/${transactionId}`;

const compactQuery = (query: TransactionQuery) =>
  Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== null && value !== ''));

export const getTransactions = async (ledgerId: number, query: TransactionQuery = {}) => {
  const response = await axios.get<TransactionPage>(ledgerTransactionsUrl(ledgerId), {
    params: compactQuery({
      page: 0,
      size: 20,
      ...query,
    }),
  });
  return response.data;
};

export const createTransaction = async (ledgerId: number, payload: TransactionPayload) => {
  const response = await axios.post<TransactionRecord>(ledgerTransactionsUrl(ledgerId), payload);
  return response.data;
};

export const getTransaction = async (transactionId: number) => {
  const response = await axios.get<TransactionRecord>(transactionUrl(transactionId));
  return response.data;
};

export const updateTransaction = async (transactionId: number, payload: TransactionPayload) => {
  const response = await axios.put<TransactionRecord>(transactionUrl(transactionId), payload);
  return response.data;
};

export const deleteTransaction = async (transactionId: number) => {
  await axios.delete(transactionUrl(transactionId));
};
