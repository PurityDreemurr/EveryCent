import axios from 'axios';

export type NotificationReadFilter = 'all' | 'unread' | 'read';

export type NotificationType = string;

export type NotificationLevel = string;

export interface NotificationMessage {
  id: number;
  userId?: number;
  ledgerId?: number;
  budgetId?: number;
  title: string;
  content: string;
  type?: NotificationType;
  level?: NotificationLevel;
  read: boolean;
  createdDate?: string;
}

export interface NotificationPage {
  content: NotificationMessage[];
  totalElements: number;
  page: number;
  size: number;
}

export interface NotificationQuery {
  read?: boolean;
  page?: number;
  size?: number;
}

const apiUrl = 'api/notifications';

export const getNotifications = async (query: NotificationQuery = {}) => {
  const response = await axios.get<NotificationPage>(apiUrl, { params: query });
  return response.data;
};

export const markNotificationAsRead = async (notificationId: number) => {
  const response = await axios.patch<NotificationMessage>(`${apiUrl}/${notificationId}/read`);
  return response.data;
};

export const deleteNotification = async (notificationId: number) => {
  await axios.delete(`${apiUrl}/${notificationId}`);
};
