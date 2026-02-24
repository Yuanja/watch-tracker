export interface Category {
  id: string;
  name: string;
  parentId: string | null;
  parentName?: string | null;
  sortOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  listingCount?: number;
}

export interface Manufacturer {
  id: string;
  name: string;
  aliases: string[];
  website: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  listingCount?: number;
}

export interface Unit {
  id: string;
  name: string;
  abbreviation: string;
  isActive: boolean;
  createdAt: string;
}

export interface Condition {
  id: string;
  name: string;
  abbreviation: string | null;
  sortOrder: number;
  isActive: boolean;
  createdAt: string;
}

export interface JargonEntry {
  id: string;
  acronym: string;
  expansion: string;
  industry: string | null;
  contextExample: string | null;
  source: 'llm' | 'human' | 'seed';
  confidence: number;
  usageCount: number;
  verified: boolean;
  createdAt: string;
  updatedAt: string;
}
