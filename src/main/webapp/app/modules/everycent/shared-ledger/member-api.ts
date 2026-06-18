import axios from 'axios';

export interface LedgerMember {
  ledgerId?: number;
  userId: number;
  login: string;
  email?: string;
  permissionType: string;
}

export interface InviteMemberPayload {
  userId: number;
  permissionType: string;
}

export interface UpdateMemberPermissionPayload {
  permissionType: string;
}

const memberUrl = (ledgerId: number) => `api/ledgers/${ledgerId}/members`;

export const getLedgerMembers = async (ledgerId: number) => {
  const response = await axios.get<LedgerMember[]>(memberUrl(ledgerId));
  return response.data;
};

export const inviteLedgerMember = async (ledgerId: number, payload: InviteMemberPayload) => {
  const response = await axios.post<LedgerMember>(memberUrl(ledgerId), payload);
  return response.data;
};

export const updateLedgerMemberPermission = async (ledgerId: number, userId: number, payload: UpdateMemberPermissionPayload) => {
  const response = await axios.put<UpdateMemberPermissionPayload>(`${memberUrl(ledgerId)}/${userId}`, payload);
  return response.data;
};

export const deleteLedgerMember = async (ledgerId: number, userId: number) => {
  await axios.delete(`${memberUrl(ledgerId)}/${userId}`);
};
