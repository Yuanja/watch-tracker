import { ChevronDown, ChevronUp } from 'lucide-react';
import type { Listing } from '../../types/listing';
import { IntentBadge, StatusBadge } from '../common/Badge';

function formatPrice(price: number | null, currency: string): string {
  if (price === null) return '\u2014';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency || 'USD',
    maximumFractionDigits: 2,
  }).format(price);
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

interface ListingCardProps {
  listing: Listing;
  isExpanded: boolean;
  onToggle: () => void;
}

export function ListingCard({ listing, isExpanded, onToggle }: ListingCardProps) {
  return (
    <>
      <tr
        className="cursor-pointer transition-colors hover:bg-gray-50"
        onClick={onToggle}
        aria-expanded={isExpanded}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onToggle();
          }
        }}
      >
        <td className="px-4 py-3">
          <span className="line-clamp-2 max-w-xs text-gray-900">
            {listing.itemDescription}
          </span>
        </td>
        <td className="px-4 py-3">
          <IntentBadge intent={listing.intent} />
        </td>
        <td className="hidden px-4 py-3 text-gray-600 sm:table-cell">
          {listing.senderName ?? listing.senderPhone ?? (
            <span className="text-gray-400">Unknown</span>
          )}
        </td>
        <td className="hidden px-4 py-3 text-gray-700 md:table-cell">
          {formatPrice(listing.price, listing.priceCurrency)}
        </td>
        <td className="px-4 py-3">
          <StatusBadge status={listing.status} />
        </td>
        <td className="hidden px-4 py-3 text-gray-500 lg:table-cell">
          {formatDate(listing.createdAt)}
        </td>
        <td className="px-2 py-3 text-gray-400">
          {isExpanded ? (
            <ChevronUp className="h-4 w-4" />
          ) : (
            <ChevronDown className="h-4 w-4" />
          )}
        </td>
      </tr>
    </>
  );
}
