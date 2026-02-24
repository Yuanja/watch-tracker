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

export interface SendMessageRequest {
  content: string;
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
