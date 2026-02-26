import { useState, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { List, TrendingUp, TrendingDown, Clock, Activity } from 'lucide-react';
import { getListings, getListingStats } from '../api/listings';
import { useWebSocket } from '../hooks/useWebSocket';
import type { ListingSearchRequest, ListingIntent, ListingStatus } from '../types/listing';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import { ListingFilters } from '../components/listings/ListingFilters';
import { ListingsView } from '../components/listings/ListingsView';

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

// ── Main Page ─────────────────────────────────────────────────────────────────

const PAGE_SIZE = 20;

export function ListingsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [textQuery, setTextQuery] = useState('');
  const [draftQuery, setDraftQuery] = useState('');
  const [intent, setIntent] = useState<ListingIntent | ''>('');
  const [status, setStatus] = useState<ListingStatus | ''>('');
  const [priceMin, setPriceMin] = useState('');
  const [priceMax, setPriceMax] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // Real-time updates: refresh listings when new ones arrive via WebSocket
  useWebSocket('/topic/listings', useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['listings'] });
    queryClient.invalidateQueries({ queryKey: ['listing-stats'] });
  }, [queryClient]));

  const searchParams: ListingSearchRequest = {
    page,
    size: PAGE_SIZE,
    ...(textQuery && { q: textQuery }),
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

  const handleFilterChange = useCallback((setter: (v: string) => void, value: string) => {
    setter(value);
    setPage(0);
  }, []);

  const toggleExpand = useCallback((id: string) => {
    setExpandedId((prev) => (prev === id ? null : id));
  }, []);

  const hasActiveFilters = !!(textQuery || intent || status || priceMin || priceMax);

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
      <ListingFilters
        draftQuery={draftQuery}
        onDraftQueryChange={setDraftQuery}
        onSearch={handleSearch}
        intent={intent}
        onIntentChange={(v) => handleFilterChange(setIntent as (v: string) => void, v)}
        status={status}
        onStatusChange={(v) => handleFilterChange(setStatus as (v: string) => void, v)}
        priceMin={priceMin}
        onPriceMinChange={(v) => handleFilterChange(setPriceMin, v)}
        priceMax={priceMax}
        onPriceMaxChange={(v) => handleFilterChange(setPriceMax, v)}
        hasActiveFilters={hasActiveFilters}
        onClearAll={() => {
          setDraftQuery('');
          setTextQuery('');
          setIntent('');
          setStatus('');
          setPriceMin('');
          setPriceMax('');
          setPage(0);
        }}
      />

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
        ) : (
          <ListingsView
            listings={data?.content ?? []}
            totalPages={data?.totalPages ?? 0}
            totalElements={data?.totalElements ?? 0}
            currentPage={page}
            textQuery={textQuery}
            expandedId={expandedId}
            onToggleExpand={toggleExpand}
            onPageChange={setPage}
          />
        )}
      </div>
    </div>
  );
}
