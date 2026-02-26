export type ListingIntent = 'sell' | 'want' | 'unknown';
export type ListingStatus = 'active' | 'expired' | 'deleted' | 'pending_review';

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
  categoryName: string | null;
  manufacturerId: string | null;
  manufacturerName: string | null;
  partNumber: string | null;
  quantity: number | null;
  unitId: string | null;
  unitAbbr: string | null;
  price: number | null;
  priceCurrency: string;
  conditionId: string | null;
  conditionName: string | null;

  // Provenance
  originalText: string;
  senderName: string | null;
  senderPhone: string | null;

  // Review state
  status: ListingStatus;
  needsHumanReview: boolean;
  reviewedBy: string | null;
  reviewedAt: string | null;

  // Lifecycle
  createdAt: string;
  updatedAt: string;
  expiresAt: string | null;
  deletedAt: string | null;
  deletedBy: string | null;
}

export interface ListingSearchRequest {
  intent?: ListingIntent;
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
  totalActive: number;
  sellCount: number;
  wantCount: number;
  pendingReview: number;
  todayCount: number;
}
