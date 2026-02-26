export type ChatRole = 'user' | 'assistant' | 'system';

export interface ChatSession {
  id: string;
  userId: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
  messageCount?: number;
}

export interface ToolCall {
  id: string;
  name: string;
  arguments: Record<string, unknown>;
  result?: unknown;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: ChatRole;
  content: string;
  modelUsed: string | null;
  inputTokens: number;
  outputTokens: number;
  costUsd: number;
  toolCalls: ToolCall[] | null;
  createdAt: string;
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
  toolResults: ToolCall[];
}

export interface SendMessageRequest {
  message: string;
}

export interface CostSummary {
  totalInputTokens: number;
  totalOutputTokens: number;
  totalCostUsd: number;
  sessionCount: number;
  byPeriod: PeriodCost[];
}

export interface PeriodCost {
  periodDate: string;
  inputTokens: number;
  outputTokens: number;
  costUsd: number;
  sessionCount: number;
}
