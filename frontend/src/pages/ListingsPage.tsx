import { useState, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Search,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  ChevronUp,
  List,
  TrendingUp,
  TrendingDown,
  Clock,
  Activity,
} from 'lucide-react';
import { getListings, getListingStats } from '../api/listings';
import type { ListingSearchRequest, Listing, ListingIntent, ListingStatus } from '../types/listing';
import { Badge, IntentBadge, StatusBadge } from '../components/common/Badge';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

// ── Helpers ──────────────────────────────────────────────────────────────────

function formatPrice(price: number | null, currency: string): string {
  if (price === null) return '—';
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

function confidenceColor(score: number): string {
  if (score >= 0.8) return 'text-green-700';
  if (score >= 0.5) return 'text-yellow-700';
  return 'text-red-700';
}

// ── Stat Card ────────────────────────────────────────────────────────────────

interface StatCardProps {
  label: string;
  value: number | string;
  icon: React.ReactNode;
  iconBg: string;
}

function StatCard({ label, value, icon, iconBg }: StatCardProps) {
  return (
    <div className="flex items-center gap-4 rounded-xl border border-gray-200 bg-white px-5 py-4 shadow-sm">
      <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${iconBg}`}>
        {icon}
      </div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className="text-2xl font-semibold text-gray-900">{value}</p>
      </div>
    </div>
  );
}

// ── Expanded Detail Row ───────────────────────────────────────────────────────

function ListingDetailPanel({ listing }: { listing: Listing }) {
  return (
    <div className="bg-gray-50 border-t border-gray-200 px-6 py-4">
      <div className="grid grid-cols-1 gap-y-3 gap-x-8 sm:grid-cols-2 lg:grid-cols-3">
        {/* Original text */}
        <div className="sm:col-span-2 lg:col-span-3">
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400">
            Original Message
          </p>
          <p className="mt-1 whitespace-pre-wrap text-sm text-gray-700 bg-white border border-gray-200 rounded-lg px-3 py-2">
            {listing.originalText}
          </p>
        </div>

        <DetailField label="Confidence Score">
          <span className={`font-semibold ${confidenceColor(listing.confidenceScore)}`}>
            {(listing.confidenceScore * 100).toFixed(1)}%
          </span>
        </DetailField>

        <DetailField label="Category">
          {listing.categoryName ? (
            <Badge variant="teal">{listing.categoryName}</Badge>
          ) : (
            <span className="text-gray-400">—</span>
          )}
        </DetailField>

        <DetailField label="Manufacturer">
          {listing.manufacturerName ?? <span className="text-gray-400">—</span>}
        </DetailField>

        <DetailField label="Part Number">
          {listing.partNumber ? (
            <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs font-mono text-gray-700">
              {listing.partNumber}
            </code>
          ) : (
            <span className="text-gray-400">—</span>
          )}
        </DetailField>

        <DetailField label="Quantity">
          {listing.quantity !== null
            ? `${listing.quantity}${listing.unitAbbr ? ` ${listing.unitAbbr}` : ''}`
            : <span className="text-gray-400">—</span>}
        </DetailField>

        <DetailField label="Condition">
          {listing.conditionName ?? <span className="text-gray-400">—</span>}
        </DetailField>

        <DetailField label="Sender">
          {listing.senderName ?? listing.senderPhone ?? <span className="text-gray-400">Unknown</span>}
        </DetailField>

        <DetailField label="Group">
          {listing.groupName ?? <span className="text-gray-400">—</span>}
        </DetailField>

        <DetailField label="Needs Review">
          {listing.needsHumanReview ? (
            <Badge variant="yellow">Yes</Badge>
          ) : (
            <Badge variant="green">No</Badge>
          )}
        </DetailField>

        {listing.expiresAt && (
          <DetailField label="Expires">
            {formatDate(listing.expiresAt)}
          </DetailField>
        )}

        {listing.reviewedBy && (
          <DetailField label="Reviewed By">
            {listing.reviewedBy}
            {listing.reviewedAt ? ` on ${formatDate(listing.reviewedAt)}` : ''}
          </DetailField>
        )}
      </div>
    </div>
  );
}

function DetailField({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-gray-400">{label}</p>
      <div className="mt-1 text-sm text-gray-800">{children}</div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

const PAGE_SIZE = 20;

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

export function ListingsPage() {
  const [page, setPage] = useState(0);
  const [textQuery, setTextQuery] = useState('');
  const [draftQuery, setDraftQuery] = useState('');
  const [intent, setIntent] = useState<ListingIntent | ''>('');
  const [status, setStatus] = useState<ListingStatus | ''>('');
  const [priceMin, setPriceMin] = useState('');
  const [priceMax, setPriceMax] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // Build query params from current filter state
  const searchParams: ListingSearchRequest = {
    page,
    size: PAGE_SIZE,
    ...(textQuery && { textQuery }),
    ...(intent && { intent }),
    ...(status && { status: status as ListingStatus }),
    ...(priceMin && { priceMin: parseFloat(priceMin) }),
    ...(priceMax && { priceMax: parseFloat(priceMax) }),
  };

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['listings', searchParams],
    queryFn: () => getListings(searchParams),
    placeholderData: (prev) => prev,
  });

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['listing-stats'],
    queryFn: getListingStats,
    staleTime: 60_000,
  });

  const handleSearch = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      setTextQuery(draftQuery.trim());
      setPage(0);
    },
    [draftQuery]
  );

  const handleFilterChange = useCallback(() => {
    setPage(0);
  }, []);

  const toggleExpand = useCallback((id: string) => {
    setExpandedId((prev) => (prev === id ? null : id));
  }, []);

  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className="flex h-full flex-col gap-6 overflow-y-auto p-6">
      {/* Page header */}
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-green-100">
          <List className="h-5 w-5 text-green-600" />
        </div>
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Listings</h1>
          <p className="text-sm text-gray-500">Browse and search extracted trade listings</p>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {statsLoading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="h-20 animate-pulse rounded-xl bg-gray-100"
            />
          ))
        ) : stats ? (
          <>
            <StatCard
              label="Total Active"
              value={stats.totalActive}
              icon={<Activity className="h-5 w-5 text-green-600" />}
              iconBg="bg-green-100"
            />
            <StatCard
              label="Sell Listings"
              value={stats.sellCount}
              icon={<TrendingUp className="h-5 w-5 text-blue-600" />}
              iconBg="bg-blue-100"
            />
            <StatCard
              label="Want Listings"
              value={stats.wantCount}
              icon={<TrendingDown className="h-5 w-5 text-purple-600" />}
              iconBg="bg-purple-100"
            />
            <StatCard
              label="Pending Review"
              value={stats.pendingReview}
              icon={<Clock className="h-5 w-5 text-yellow-600" />}
              iconBg="bg-yellow-100"
            />
          </>
        ) : null}
      </div>

      {/* Search + filters */}
      <div className="flex flex-col gap-3 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        {/* Search bar */}
        <form onSubmit={handleSearch} className="flex gap-2">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              value={draftQuery}
              onChange={(e) => setDraftQuery(e.target.value)}
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
          {(textQuery || intent || status || priceMin || priceMax) && (
            <button
              type="button"
              onClick={() => {
                setDraftQuery('');
                setTextQuery('');
                setIntent('');
                setStatus('');
                setPriceMin('');
                setPriceMax('');
                setPage(0);
              }}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50"
            >
              Clear
            </button>
          )}
        </form>

        {/* Filter row */}
        <div className="flex flex-wrap gap-3">
          {/* Intent filter */}
          <select
            value={intent}
            onChange={(e) => {
              setIntent(e.target.value as ListingIntent | '');
              handleFilterChange();
            }}
            className="rounded-lg border border-gray-300 py-1.5 pl-3 pr-8 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            aria-label="Filter by intent"
          >
            {INTENT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          {/* Status filter */}
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as ListingStatus | '');
              handleFilterChange();
            }}
            className="rounded-lg border border-gray-300 py-1.5 pl-3 pr-8 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            aria-label="Filter by status"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          {/* Price range */}
          <div className="flex items-center gap-2">
            <input
              type="number"
              value={priceMin}
              onChange={(e) => {
                setPriceMin(e.target.value);
                handleFilterChange();
              }}
              placeholder="Min price"
              min={0}
              className="w-28 rounded-lg border border-gray-300 py-1.5 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              aria-label="Minimum price"
            />
            <span className="text-sm text-gray-400">–</span>
            <input
              type="number"
              value={priceMax}
              onChange={(e) => {
                setPriceMax(e.target.value);
                handleFilterChange();
              }}
              placeholder="Max price"
              min={0}
              className="w-28 rounded-lg border border-gray-300 py-1.5 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              aria-label="Maximum price"
            />
          </div>
        </div>
      </div>

      {/* Results */}
      <div className="min-h-0 flex-1 rounded-xl border border-gray-200 bg-white shadow-sm">
        {isLoading ? (
          <LoadingOverlay message="Loading listings..." />
        ) : isError ? (
          <div className="flex flex-col items-center justify-center gap-3 py-16">
            <p className="text-sm font-medium text-red-600">
              Failed to load listings
            </p>
            <p className="text-xs text-gray-500">
              {error instanceof Error ? error.message : 'Unknown error'}
            </p>
          </div>
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            title="No listings found"
            description={
              textQuery
                ? `No listings match "${textQuery}". Try adjusting your search or filters.`
                : 'No listings match the current filters.'
            }
          />
        ) : (
          <>
            {/* Result count */}
            <div className="border-b border-gray-100 px-4 py-3">
              <p className="text-sm text-gray-500">
                {totalElements.toLocaleString()} listing
                {totalElements !== 1 ? 's' : ''} found
                {textQuery && (
                  <span>
                    {' '}
                    for{' '}
                    <span className="font-medium text-gray-700">
                      &ldquo;{textQuery}&rdquo;
                    </span>
                  </span>
                )}
              </p>
            </div>

            {/* Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50 text-left">
                    <th className="px-4 py-3 font-medium text-gray-600">Description</th>
                    <th className="px-4 py-3 font-medium text-gray-600">Intent</th>
                    <th className="hidden px-4 py-3 font-medium text-gray-600 sm:table-cell">
                      Sender
                    </th>
                    <th className="hidden px-4 py-3 font-medium text-gray-600 md:table-cell">
                      Price
                    </th>
                    <th className="px-4 py-3 font-medium text-gray-600">Status</th>
                    <th className="hidden px-4 py-3 font-medium text-gray-600 lg:table-cell">
                      Date
                    </th>
                    <th className="w-8 px-2 py-3" aria-hidden="true" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {data.content.map((listing: Listing) => (
                    <>
                      <tr
                        key={listing.id}
                        className="cursor-pointer transition-colors hover:bg-gray-50"
                        onClick={() => toggleExpand(listing.id)}
                        aria-expanded={expandedId === listing.id}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            toggleExpand(listing.id);
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
                          {expandedId === listing.id ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          )}
                        </td>
                      </tr>
                      {expandedId === listing.id && (
                        <tr key={`${listing.id}-detail`}>
                          <td colSpan={7} className="p-0">
                            <ListingDetailPanel listing={listing} />
                          </td>
                        </tr>
                      )}
                    </>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-gray-100 px-4 py-3">
                <p className="text-sm text-gray-500">
                  Page {page + 1} of {totalPages}
                </p>
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                    aria-label="Previous page"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>

                  {/* Page numbers — show a window around current page */}
                  {Array.from({ length: totalPages }).map((_, i) => {
                    // Always show first, last, and a window of 2 around current
                    const show =
                      i === 0 ||
                      i === totalPages - 1 ||
                      Math.abs(i - page) <= 1;
                    if (!show) {
                      // Show ellipsis only at boundary positions
                      if (i === 1 && page > 3) {
                        return (
                          <span key={`ellipsis-start`} className="px-1 text-gray-400">
                            …
                          </span>
                        );
                      }
                      if (i === totalPages - 2 && page < totalPages - 4) {
                        return (
                          <span key={`ellipsis-end`} className="px-1 text-gray-400">
                            …
                          </span>
                        );
                      }
                      return null;
                    }
                    return (
                      <button
                        key={i}
                        onClick={() => setPage(i)}
                        className={`inline-flex h-8 w-8 items-center justify-center rounded-md text-sm font-medium transition-colors ${
                          i === page
                            ? 'bg-blue-600 text-white'
                            : 'border border-gray-300 text-gray-600 hover:bg-gray-50'
                        }`}
                        aria-label={`Go to page ${i + 1}`}
                        aria-current={i === page ? 'page' : undefined}
                      >
                        {i + 1}
                      </button>
                    );
                  })}

                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                    aria-label="Next page"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Inline spinner overlay for page changes (non-initial loads) */}
      {!isLoading && data && (
        <div aria-live="polite" className="sr-only">
          Showing page {page + 1} of {totalPages}
        </div>
      )}
    </div>
  );
}
