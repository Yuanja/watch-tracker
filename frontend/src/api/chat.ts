import { apiClient } from './client';
import type {
  ChatSession,
  ChatMessage,
  SendMessageRequest,
  CostSummary,
  SessionDetail,
  SendMessageResponse,
} from '../types/chat';

/** Backend page wrapper returned by GET /chat/sessions */
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  last: boolean;
}

/**
 * Create a new chat session.
 */
export async function createSession(): Promise<ChatSession> {
  const response = await apiClient.post<ChatSession>('/chat/sessions');
  return response.data;
}

/**
 * List all chat sessions for the current user.
 * The backend returns a Spring Page wrapper; we unwrap the content array.
 */
export async function getSessions(): Promise<ChatSession[]> {
  const response = await apiClient.get<PageResponse<ChatSession> | ChatSession[]>(
    '/chat/sessions'
  );
  const data = response.data;
  // Handle both paginated and plain-array responses defensively
  if (Array.isArray(data)) return data;
  return data.content ?? [];
}

/**
 * Fetch a chat session with all its messages.
 * The backend returns { id, title, messages: [...] }.
 */
export async function getSessionMessages(
  sessionId: string
): Promise<ChatMessage[]> {
  const response = await apiClient.get<SessionDetail>(
    `/chat/sessions/${sessionId}`
  );
  return response.data.messages ?? [];
}

/**
 * Send a message to a chat session and get the AI response.
 * The backend returns { message: {...}, toolResults: [...] }.
 */
export async function sendMessage(
  sessionId: string,
  request: SendMessageRequest
): Promise<SendMessageResponse> {
  const response = await apiClient.post<SendMessageResponse>(
    `/chat/sessions/${sessionId}/messages`,
    request
  );
  return response.data;
}

/**
 * Fetch the current user's cost summary.
 */
export async function getCostSummary(): Promise<CostSummary> {
  const response = await apiClient.get<CostSummary>('/chat/cost');
  return response.data;
}
