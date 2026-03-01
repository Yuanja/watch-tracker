export type ListingIntent = 'sell' | 'want' | 'unknown';
export type ListingStatus = 'active' | 'expired' | 'deleted' | 'pending_review' | 'sold';

export interface Listing {
  id: string;
  rawMessageId: string;
  groupId: string;
  groupName?: string;

  // Classification
  intent: ListingIntent;
  confidenceScore: number;

  // Normalized fields
  itemDescription: string;
  itemCategoryId: string | null;
  itemCategoryName: string | null;
  manufacturerId: string | null;
  manufacturerName: string | null;
  partNumber: string | null;
  quantity: number | null;
  unitId: string | null;
  unitAbbreviation: string | null;
  price: number | null;
  priceCurrency: string;
  exchangeRateToUsd: number | null;
  priceUsd: number | null;
  conditionId: string | null;
  conditionName: string | null;

  // Provenance
  originalText: string;
  senderName: string | null;
  senderPhone: string | null;

  // Review state
  status: ListingStatus;
  needsHumanReview: boolean;
  reviewedByName: string | null;
  reviewedAt: string | null;

  // Sold tracking
  soldAt: string | null;
  soldMessageId: string | null;
  buyerName: string | null;

  // Message timestamp (when WhatsApp message was sent)
  messageTimestamp: string | null;

  // Cross-post detection
  crossPostCount: number;

  // Lifecycle
  createdAt: string;
  updatedAt: string;
  expiresAt: string | null;
  deletedAt: string | null;
  deletedById: string | null;
}

export interface CrossPost {
  id: string;
  groupName: string | null;
  senderName: string | null;
  price: number | null;
  priceCurrency: string | null;
  status: ListingStatus;
  messageTimestamp: string | null;
  createdAt: string;
}

export interface ListingSearchRequest {
  intent?: ListingIntent;
  status?: ListingStatus;
  categoryId?: string;
  manufacturerId?: string;
  conditionId?: string;
  partNumber?: string;
  priceMin?: number;
  priceMax?: number;
  createdAfter?: string;
  createdBefore?: string;
  q?: string;
  semanticQuery?: string;
  page?: number;
  size?: number;
}

export interface ListingStats {
  total: number;
  byIntent: Record<string, number>;
  byStatus: Record<string, number>;
}
