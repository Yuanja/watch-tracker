import { Search } from 'lucide-react';
import type { ListingIntent, ListingStatus } from '../../types/listing';

const INTENT_OPTIONS: { value: ListingIntent | ''; label: string }[] = [
  { value: '', label: 'All Intents' },
  { value: 'sell', label: 'Sell' },
  { value: 'want', label: 'Want' },
  { value: 'unknown', label: 'Unknown' },
];

const STATUS_OPTIONS: { value: ListingStatus | ''; label: string }[] = [
  { value: '', label: 'All Statuses' },
  { value: 'active', label: 'Active' },
  { value: 'pending_review', label: 'Pending Review' },
  { value: 'expired', label: 'Expired' },
  { value: 'deleted', label: 'Deleted' },
];

interface ListingFiltersProps {
  draftQuery: string;
  onDraftQueryChange: (value: string) => void;
  onSearch: (e: React.FormEvent) => void;
  intent: ListingIntent | '';
  onIntentChange: (value: ListingIntent | '') => void;
  status: ListingStatus | '';
  onStatusChange: (value: ListingStatus | '') => void;
  priceMin: string;
  onPriceMinChange: (value: string) => void;
  priceMax: string;
  onPriceMaxChange: (value: string) => void;
  hasActiveFilters: boolean;
  onClearAll: () => void;
}

export function ListingFilters({
  draftQuery,
  onDraftQueryChange,
  onSearch,
  intent,
  onIntentChange,
  status,
  onStatusChange,
  priceMin,
  onPriceMinChange,
  priceMax,
  onPriceMaxChange,
  hasActiveFilters,
  onClearAll,
}: ListingFiltersProps) {
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
      {/* Search bar */}
      <form onSubmit={onSearch} className="flex gap-2">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            value={draftQuery}
            onChange={(e) => onDraftQueryChange(e.target.value)}
            placeholder="Search descriptions, part numbers, senders..."
            className="w-full rounded-lg border border-gray-300 py-2 pl-9 pr-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            aria-label="Search listings"
          />
        </div>
        <button
          type="submit"
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1"
        >
          Search
        </button>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={onClearAll}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50"
          >
            Clear
          </button>
        )}
      </form>

      {/* Filter row */}
      <div className="flex flex-wrap gap-3">
        <select
          value={intent}
          onChange={(e) => onIntentChange(e.target.value as ListingIntent | '')}
          className="rounded-lg border border-gray-300 py-1.5 pl-3 pr-8 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          aria-label="Filter by intent"
        >
          {INTENT_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <select
          value={status}
          onChange={(e) => onStatusChange(e.target.value as ListingStatus | '')}
          className="rounded-lg border border-gray-300 py-1.5 pl-3 pr-8 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          aria-label="Filter by status"
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <div className="flex items-center gap-2">
          <input
            type="number"
            value={priceMin}
            onChange={(e) => onPriceMinChange(e.target.value)}
            placeholder="Min price"
            min={0}
            className="w-28 rounded-lg border border-gray-300 py-1.5 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            aria-label="Minimum price"
          />
          <span className="text-sm text-gray-400">&ndash;</span>
          <input
            type="number"
            value={priceMax}
            onChange={(e) => onPriceMaxChange(e.target.value)}
            placeholder="Max price"
            min={0}
            className="w-28 rounded-lg border border-gray-300 py-1.5 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            aria-label="Maximum price"
          />
        </div>
      </div>
    </div>
  );
}
