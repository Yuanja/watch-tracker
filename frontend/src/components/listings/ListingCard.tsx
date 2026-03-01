import { Bot, ChevronDown, ChevronUp } from 'lucide-react';
import type { Listing } from '../../types/listing';
import { Badge, IntentBadge, StatusBadge } from '../common/Badge';

function formatPrice(price: number | null, currency: string): string {
  if (price === null) return '\u2014';
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD',
      maximumFractionDigits: 2,
    }).format(price);
  } catch {
    return `${currency || ''} ${price.toLocaleString()}`.trim();
  }
}

function formatTimestamp(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  const tz = d.toLocaleTimeString('en-US', { timeZoneName: 'short' }).split(' ').pop();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${tz}`;
}

function formatUsdPrice(priceUsd: number | null): string {
  if (priceUsd === null) return '';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(priceUsd);
}

interface ListingCardProps {
  listing: Listing;
  isExpanded: boolean;
  onToggle: () => void;
  onAssist?: () => void;
}

export function ListingCard({ listing, isExpanded, onToggle, onAssist }: ListingCardProps) {
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
        <td className="whitespace-nowrap px-4 py-3 text-xs text-gray-500">
          {formatTimestamp(listing.messageTimestamp ?? listing.createdAt)}
        </td>
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
        <td className="hidden px-4 py-3 text-gray-600 sm:table-cell">
          {listing.groupName ?? <span className="text-gray-400">&mdash;</span>}
        </td>
        <td className="hidden px-4 py-3 text-gray-700 md:table-cell">
          {formatPrice(listing.price, listing.priceCurrency)}
        </td>
        <td className="hidden whitespace-nowrap px-4 py-3 text-xs text-gray-500 md:table-cell">
          {listing.exchangeRateToUsd != null && listing.priceCurrency !== 'USD'
            ? listing.exchangeRateToUsd.toFixed(4)
            : <span className="text-gray-300">&mdash;</span>}
        </td>
        <td className="hidden whitespace-nowrap px-4 py-3 text-gray-700 md:table-cell">
          {listing.priceUsd != null
            ? formatUsdPrice(listing.priceUsd)
            : <span className="text-gray-300">&mdash;</span>}
        </td>
        <td className="px-4 py-3">
          <span className="inline-flex items-center gap-1.5">
            <StatusBadge status={listing.status} />
            {listing.crossPostCount > 0 && (
              <Badge variant="orange" size="sm">
                Repost ({listing.crossPostCount})
              </Badge>
            )}
            {listing.status === 'pending_review' && onAssist && (
              <button
                type="button"
                aria-label="Agent-assisted review"
                onClick={(e) => { e.stopPropagation(); onAssist(); }}
                className="rounded p-0.5 text-purple-500 hover:bg-purple-50 hover:text-purple-700"
              >
                <Bot className="h-4 w-4" />
              </button>
            )}
          </span>
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
