import axios from 'axios';

export type LedgerPermission = string;

export interface Ledger {
  id: number;
  name: string;
  description: string;
  defaultCurrency?: string;
  currentMonthBalance?: number;
  creatorId?: number;
  creatorLogin?: string;
  permissionLevel?: LedgerPermission;
  permissionType?: LedgerPermission;
  createdDate?: string;
  lastModifiedDate?: string;
}

export interface LedgerPayload {
  name: string;
  description: string;
}

const apiUrl = 'api/ledgers';

export const getLedgers = async () => {
  const response = await axios.get<Ledger[]>(apiUrl);
  return response.data;
};

export const getLedger = async (ledgerId: number) => {
  const response = await axios.get<Ledger>(`${apiUrl}/${ledgerId}`);
  return response.data;
};

export const createLedger = async (payload: LedgerPayload) => {
  const response = await axios.post<Ledger>(apiUrl, payload);
  return response.data;
};

export const updateLedger = async (ledgerId: number, payload: LedgerPayload) => {
  const response = await axios.put<Ledger>(`${apiUrl}/${ledgerId}`, payload);
  return response.data;
};

export const deleteLedger = async (ledgerId: number) => {
  await axios.delete(`${apiUrl}/${ledgerId}`);
};
