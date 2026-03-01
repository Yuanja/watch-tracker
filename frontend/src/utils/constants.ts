export const APP_NAME = 'DialIntel.ai';

export const ROLES = {
  USER: 'user',
  ADMIN: 'admin',
  UBER_ADMIN: 'uber_admin',
} as const;

export const INTENT_LABELS = {
  sell: 'SELL',
  want: 'WANT',
  unknown: 'UNKNOWN',
} as const;

export const INTENT_COLORS = {
  sell: 'green',
  want: 'blue',
  unknown: 'gray',
} as const;

export const LISTING_STATUS_LABELS = {
  active: 'Active',
  expired: 'Expired',
  deleted: 'Deleted',
  pending_review: 'Pending Review',
} as const;

export const DEFAULT_PAGE_SIZE = 20;

export const CONFIDENCE_THRESHOLDS = {
  AUTO_ACCEPT: 0.8,
  REVIEW: 0.5,
} as const;
