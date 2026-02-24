import { apiClient } from './client';
import type { PagedResponse } from '../types/message';

export interface ReviewQueueItem {
  id: string;
  listingId: string | null;
  rawMessageId: string;
  reason: string;
  llmExplanation: string | null;
  suggestedValues: Record<string, unknown> | null;
  status: 'pending' | 'resolved' | 'skipped';
  resolvedBy: string | null;
  resolution: Record<string, unknown> | null;
  createdAt: string;
  resolvedAt: string | null;
  // Enriched fields
  originalText?: string;
  senderName?: string | null;
  groupName?: string;
}

export interface ResolveRequest {
  resolution: Record<string, unknown>;
}

export async function getReviewQueue(
  params: { page?: number; size?: number } = {}
): Promise<PagedResponse<ReviewQueueItem>> {
  const response = await apiClient.get<PagedResponse<ReviewQueueItem>>(
    '/review',
    { params }
  );
  return response.data;
}

export async function resolveReviewItem(
  id: string,
  data: ResolveRequest
): Promise<ReviewQueueItem> {
  const response = await apiClient.post<ReviewQueueItem>(
    `/review/${id}/resolve`,
    data
  );
  return response.data;
}

export async function skipReviewItem(id: string): Promise<ReviewQueueItem> {
  const response = await apiClient.post<ReviewQueueItem>(
    `/review/${id}/skip`
  );
  return response.data;
}
