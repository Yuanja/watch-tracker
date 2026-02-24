import { apiClient } from './client';
import type {
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
