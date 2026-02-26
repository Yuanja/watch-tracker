export type MessageType = 'text' | 'image' | 'document' | 'video' | 'audio';

export interface WhatsappGroup {
  id: string;
  whapiGroupId: string;
  groupName: string;
  description: string | null;
  avatarUrl: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  messageCount?: number;
  lastMessageAt?: string | null;
}

export interface ExtractedListingRef {
  id: string;
  intent: 'sell' | 'want' | 'unknown';
  itemDescription: string;
  confidenceScore: number;
}

export interface ReplayMessage {
  id: string;
  groupId: string;
  groupName?: string;
  whapiMsgId: string;
  senderPhone: string | null;
  senderName: string | null;
  senderAvatar: string | null;
  messageBody: string | null;
  messageType: MessageType;
  mediaUrl: string | null;
  mediaMimeType: string | null;
  mediaLocalPath: string | null;
  replyToMsgId: string | null;
  isForwarded: boolean;
  timestampWa: string;
  receivedAt: string;
  processed: boolean;
  processingError: string | null;
  extractedListing?: ExtractedListingRef | null;
}

export interface MessageSearchRequest {
  groupId?: string;
  sender?: string;
  q?: string;
  semantic?: string;
  page?: number;
  size?: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
