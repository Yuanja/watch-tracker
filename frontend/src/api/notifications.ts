import { apiClient } from './client';

export interface NotificationRule {
  id: string;
  nlRule: string;
  parsedIntent: 'sell' | 'want' | 'unknown' | null;
  parsedKeywords: string[];
  parsedPriceMin: number | null;
  parsedPriceMax: number | null;
  notifyChannel: string;
  notifyEmail: string | null;
  isActive: boolean;
  lastTriggered: string | null;
  createdAt: string;
}

export interface CreateNotificationRuleRequest {
  nlRule: string;
  notifyChannel?: string;
  notifyEmail?: string;
}

export interface UpdateNotificationRuleRequest {
  nlRule?: string;
  notifyChannel?: string;
  notifyEmail?: string;
  isActive?: boolean;
}

export async function getNotificationRules(): Promise<NotificationRule[]> {
  const response = await apiClient.get<NotificationRule[]>('/notifications');
  return response.data;
}

export async function createNotificationRule(
  data: CreateNotificationRuleRequest
): Promise<NotificationRule> {
  const response = await apiClient.post<NotificationRule>('/notifications', data);
  return response.data;
}

export async function updateNotificationRule(
  id: string,
  data: UpdateNotificationRuleRequest
): Promise<NotificationRule> {
  const response = await apiClient.put<NotificationRule>(
    `/notifications/${id}`,
    data
  );
  return response.data;
}

export async function deleteNotificationRule(id: string): Promise<void> {
  await apiClient.delete(`/notifications/${id}`);
}
