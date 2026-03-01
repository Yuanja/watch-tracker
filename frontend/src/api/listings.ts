import { apiClient } from './client';
import type {
  CrossPost,
  Listing,
  ListingSearchRequest,
  ListingStats,
} from '../types/listing';
import type { PagedResponse } from '../types/message';

/**
 * Search and filter listings with pagination.
 */
export async function getListings(
  params: ListingSearchRequest = {}
): Promise<PagedResponse<Listing>> {
  const response = await apiClient.get<PagedResponse<Listing>>('/listings', {
    params,
  });
  return response.data;
}

/**
 * Fetch a single listing by ID.
 */
export async function getListing(id: string): Promise<Listing> {
  const response = await apiClient.get<Listing>(`/listings/${id}`);
  return response.data;
}

/**
 * Fetch aggregate stats for the listings dashboard.
 */
export async function getListingStats(): Promise<ListingStats> {
  const response = await apiClient.get<ListingStats>('/listings/stats');
  return response.data;
}

/**
 * Update a listing. Requires admin role.
 */
export async function updateListing(
  id: string,
  data: Partial<Listing>
): Promise<Listing> {
  const response = await apiClient.put<Listing>(`/listings/${id}`, data);
  return response.data;
}

/**
 * Soft-delete a listing. Requires uber_admin role.
 */
export async function deleteListing(id: string): Promise<void> {
  await apiClient.delete(`/listings/${id}`);
}

/**
 * Fetch cross-posts for a listing.
 */
export async function getCrossPosts(id: string): Promise<CrossPost[]> {
  const response = await apiClient.get<CrossPost[]>(`/listings/${id}/cross-posts`);
  return response.data;
}

/**
 * Retry AI extraction on a listing with an optional hint.
 */
export async function retryExtraction(
  id: string,
  hint?: string
): Promise<Listing> {
  const response = await apiClient.post<Listing>(
    `/listings/${id}/retry-extraction`,
    hint ? { hint } : {}
  );
  return response.data;
}
