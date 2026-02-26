export type ChatRole = 'user' | 'assistant' | 'system';

export interface ChatSession {
  id: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
  messageCount?: number;
}

export interface ToolCall {
  tool: string;
  params: Record<string, unknown>;
  result?: unknown;
}

export interface ChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  modelUsed: string | null;
  inputTokens: number;
  outputTokens: number;
  costUsd: number;
  toolCalls: string | null;
  createdAt: string;
}

/**
 * Parse the raw JSON toolCalls string from the backend into typed ToolCall[].
 */
export function parseToolCalls(raw: string | null): ToolCall[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

/**
 * Backend response shape for GET /chat/sessions/:id
 */
export interface SessionDetail {
  id: string;
  title: string | null;
  messages: ChatMessage[];
}

/**
 * Backend response shape for POST /chat/sessions/:id/messages
 */
export interface SendMessageResponse {
  message: ChatMessage;
  toolResults: string[];
}

export interface SendMessageRequest {
  message: string;
}

export interface CostSummary {
  totalInputTokens: number;
  totalOutputTokens: number;
  totalCostUsd: number;
  sessionCount: number;
  dailyBreakdown: DailyUsage[];
}

export interface DailyUsage {
  date: string;
  inputTokens: number;
  outputTokens: number;
  costUsd: number;
}
