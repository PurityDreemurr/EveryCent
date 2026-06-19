import axios from 'axios';

export type DashboardPeriod = 'WEEK' | 'MONTH' | 'YEAR';

export interface DashboardSummary {
  totalIncome?: string;
  incomeTotal?: string;
  totalExpense?: string;
  expenseTotal?: string;
  balance?: string;
  transactionCount?: number;
  budgetUsedRatio?: string;
  budgetUsedRate?: string;
  budgetAlertLevel?: string;
}

export interface TrendPoint {
  date: string;
  income: string;
  expense: string;
}

export interface TagStat {
  tagId: number;
  tagName: string;
  amount: string;
  count: number;
  ratio?: string;
  percentage?: string;
}

export interface PeriodQuery {
  period?: DashboardPeriod;
  date?: string;
}

export interface TrendQuery {
  startDate?: string;
  endDate?: string;
}

const dashboardUrl = (ledgerId: number, path: string) => `api/ledgers/${ledgerId}/dashboard/${path}`;

export const getDashboardSummary = async (ledgerId: number, query: PeriodQuery = {}) => {
  const response = await axios.get<DashboardSummary>(dashboardUrl(ledgerId, 'summary'), { params: query });
  return response.data;
};

export const getDashboardTrend = async (ledgerId: number, query: TrendQuery = {}) => {
  const response = await axios.get<TrendPoint[]>(dashboardUrl(ledgerId, 'trend'), { params: query });
  return response.data;
};

export const getBehaviorTagStats = async (ledgerId: number, query: PeriodQuery = {}) => {
  const response = await axios.get<TagStat[]>(dashboardUrl(ledgerId, 'behavior-tags'), { params: query });
  return response.data;
};

export const getEmotionTagStats = async (ledgerId: number, query: PeriodQuery = {}) => {
  const response = await axios.get<TagStat[]>(dashboardUrl(ledgerId, 'emotion-tags'), { params: query });
  return response.data;
};
