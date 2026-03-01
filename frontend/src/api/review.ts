import { apiClient } from './client';
import type { PagedResponse } from '../types/message';

export interface ReviewQueueItem {
  id: string;
  listingId: string | null;
  rawMessageId: string;
  reason: string;
  llmExplanation: string | null;
  suggestedValues: string | null;
  status: string;
  createdAt: string;
  // Enriched fields from raw message
  originalMessageBody?: string;
  senderName?: string | null;
}

export interface ResolveRequest {
  itemDescription?: string;
  categoryName?: string;
  manufacturerName?: string;
  partNumber?: string;
  quantity?: number;
  unit?: string;
  price?: number;
  condition?: string;
  intent?: string;
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

export interface ExtractionItem {
  description?: string;
  category?: string;
  manufacturer?: string;
  part_number?: string;
  quantity?: number;
  unit?: string;
  price?: number;
  currency?: string;
  condition?: string;
}

export interface ExtractionResult {
  intent?: string;
  items?: ExtractionItem[];
  unknown_terms?: string[];
  confidence?: number;
}

export interface AssistResponse {
  extraction: ExtractionResult;
  originalText: string;
}

export async function assistByListing(
  listingId: string,
  hint: string
): Promise<AssistResponse> {
  const response = await apiClient.post<AssistResponse>(
    `/review/listing/${listingId}/assist`,
    { hint }
  );
  return response.data;
}
