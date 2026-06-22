import axios from 'axios';

export type BudgetCycle = 'WEEKLY' | 'MONTHLY';

export interface Budget {
  id: number;
  ledgerId?: number;
  cycle: BudgetCycle;
  periodStart: string;
  periodEnd: string;
  limitAmount?: string;
  amount?: string;
  budgetAmount?: string;
  alertThreshold: string;
  enabled: boolean;
  usedAmount?: string;
  remainingAmount?: string;
  usedRatio?: string;
  usageRate?: string;
  status?: string;
  overBudget?: boolean;
}

export interface BudgetPayload {
  cycle: BudgetCycle;
  periodStart: string;
  periodEnd: string;
  limitAmount: string;
  alertThreshold: string;
  enabled: boolean;
}

export interface BudgetStatusQuery {
  cycle?: BudgetCycle;
  date?: string;
}

export interface BudgetAlertResult {
  title?: string;
  content?: string;
  analysisSummary?: string;
  majorExpenses?: string[];
  unnecessaryExpenses?: string[];
  suggestions?: string[];
  level?: 'INFO' | 'WARNING' | 'DANGER';
  overBudget?: boolean;
  usedAmount?: string;
  limitAmount?: string;
  usedRatio?: string;
  needNotification?: boolean;
  notificationId?: number;
}

const ledgerBudgetsUrl = (ledgerId: number) => `api/ledgers/${ledgerId}/budgets`;
const budgetUrl = (budgetId: number) => `api/budgets/${budgetId}`;

export const budgetLimit = (budget: Budget) => budget.limitAmount || budget.amount || budget.budgetAmount || '0';

const formatBudgetRate = (value?: string) => {
  const numericValue = Number(value || 0);
  const percentValue = numericValue > 1 ? numericValue : numericValue * 100;
  return `${percentValue.toLocaleString('zh-CN', { maximumFractionDigits: 2 })}%`;
};

export const budgetUsageRate = (budget: Budget) => formatBudgetRate(budget.usedRatio || budget.usageRate);

export const getBudgets = async (ledgerId: number) => {
  const response = await axios.get<Budget[]>(ledgerBudgetsUrl(ledgerId));
  return response.data;
};

export const createBudget = async (ledgerId: number, payload: BudgetPayload) => {
  const response = await axios.post<Budget>(ledgerBudgetsUrl(ledgerId), payload);
  return response.data;
};

export const updateBudget = async (budgetId: number, payload: BudgetPayload) => {
  const response = await axios.put<Budget>(budgetUrl(budgetId), payload);
  return response.data;
};

export const deleteBudget = async (budgetId: number) => {
  await axios.delete(budgetUrl(budgetId));
};

export const getBudgetStatus = async (ledgerId: number, query: BudgetStatusQuery = {}) => {
  const response = await axios.get<Budget>(`${ledgerBudgetsUrl(ledgerId)}/status`, {
    params: query,
  });
  return response.data;
};

export const generateBudgetAlert = async (ledgerId: number, budgetId: number, saveAsNotification = false) => {
  const response = await axios.post<BudgetAlertResult>('api/ai/budget-alert/generate', {
    ledgerId,
    budgetId,
    saveAsNotification,
  });
  return response.data;
};
