import { apiClient } from './client';
import type {
  WhatsappGroup,
  ReplayMessage,
  ExtractedListingRef,
  MessageSearchRequest,
  PagedResponse,
} from '../types/message';

/**
 * Fetch all monitored WhatsApp groups.
 */
export async function getGroups(): Promise<WhatsappGroup[]> {
  const response = await apiClient.get<WhatsappGroup[]>('/messages/groups');
  return response.data;
}

export interface GetGroupMessagesParams {
  page?: number;
  size?: number;
}

/**
 * Fetch paginated messages for a specific group.
 */
export async function getGroupMessages(
  groupId: string,
  params: GetGroupMessagesParams = {}
): Promise<PagedResponse<ReplayMessage>> {
  const response = await apiClient.get<PagedResponse<ReplayMessage>>(
    `/messages/groups/${groupId}/messages`,
    { params }
  );
  return response.data;
}

/**
 * Search messages across all groups or within a specific group.
 * Supports both text (trigram) and semantic (vector) search modes.
 */
export async function searchMessages(
  params: MessageSearchRequest
): Promise<PagedResponse<ReplayMessage>> {
  const response = await apiClient.get<PagedResponse<ReplayMessage>>(
    '/messages/search',
    { params }
  );
  return response.data;
}

/**
 * Fetch a single raw message by ID.
 */
export async function getMessage(messageId: string): Promise<ReplayMessage> {
  const response = await apiClient.get<ReplayMessage>(`/messages/${messageId}`);
  return response.data;
}

/**
 * Trigger extraction on a single message (admin only).
 */
export async function extractMessage(
  messageId: string
): Promise<ExtractedListingRef | null> {
  const response = await apiClient.post<ExtractedListingRef>(
    `/messages/${messageId}/extract`
  );
  // 204 No Content means no listing was created
  if (response.status === 204) return null;
  return response.data;
}

/**
 * Fetch paginated messages across all groups.
 */
export async function getMessages(
  params: { page?: number; size?: number } = {}
): Promise<PagedResponse<ReplayMessage>> {
  const response = await apiClient.get<PagedResponse<ReplayMessage>>(
    '/messages',
    { params }
  );
  return response.data;
}
