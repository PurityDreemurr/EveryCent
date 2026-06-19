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

const normalizeLedgerDetail = (data: Ledger | Ledger[]) => (Array.isArray(data) ? data[0] : data);

export const getLedgers = async () => {
  const response = await axios.get<Ledger[]>(apiUrl);
  return response.data;
};

export const getLedger = async (ledgerId: number | string) => {
  const response = await axios.get<Ledger | Ledger[]>(`${apiUrl}/${ledgerId}`);
  return normalizeLedgerDetail(response.data);
};

export const createLedger = async (payload: LedgerPayload) => {
  const response = await axios.post<Ledger>(apiUrl, payload);
  return response.data;
};

export const updateLedger = async (ledgerId: number | string, payload: LedgerPayload) => {
  const response = await axios.put<Ledger>(`${apiUrl}/${ledgerId}`, payload);
  return response.data;
};

export const deleteLedger = async (ledgerId: number | string) => {
  await axios.delete(`${apiUrl}/${ledgerId}`);
};
