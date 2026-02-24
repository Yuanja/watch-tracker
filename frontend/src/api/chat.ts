import { apiClient } from './client';
import type {
  ChatSession,
  ChatMessage,
  SendMessageRequest,
  CostSummary,
} from '../types/chat';

/**
 * Create a new chat session.
 */
export async function createSession(): Promise<ChatSession> {
  const response = await apiClient.post<ChatSession>('/chat/sessions');
  return response.data;
}

/**
 * List all chat sessions for the current user.
 */
export async function getSessions(): Promise<ChatSession[]> {
  const response = await apiClient.get<ChatSession[]>('/chat/sessions');
  return response.data;
}

/**
 * Fetch all messages for a chat session.
 */
export async function getSessionMessages(
  sessionId: string
): Promise<ChatMessage[]> {
  const response = await apiClient.get<ChatMessage[]>(
    `/chat/sessions/${sessionId}`
  );
  return response.data;
}

/**
 * Send a message to a chat session and get the AI response.
 */
export async function sendMessage(
  sessionId: string,
  request: SendMessageRequest
): Promise<ChatMessage> {
  const response = await apiClient.post<ChatMessage>(
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
