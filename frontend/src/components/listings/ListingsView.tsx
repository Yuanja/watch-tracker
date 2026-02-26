import type { Listing } from '../../types/listing';
import { EmptyState } from '../common/EmptyState';
import { Pagination } from '../common/Pagination';
import { ListingCard } from './ListingCard';
import { ListingDetail } from './ListingDetail';

interface ListingsViewProps {
  listings: Listing[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  textQuery: string;
  expandedId: string | null;
  onToggleExpand: (id: string) => void;
  onPageChange: (page: number) => void;
}

export function ListingsView({
  listings,
  totalPages,
  totalElements,
  currentPage,
  textQuery,
  expandedId,
  onToggleExpand,
  onPageChange,
}: ListingsViewProps) {
  if (listings.length === 0) {
    return (
      <EmptyState
        title="No listings found"
        description={
          textQuery
            ? `No listings match "${textQuery}". Try adjusting your search or filters.`
            : 'No listings match the current filters.'
        }
      />
    );
  }

  return (
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
            {listings.map((listing) => (
              <>
                <ListingCard
                  key={listing.id}
                  listing={listing}
                  isExpanded={expandedId === listing.id}
                  onToggle={() => onToggleExpand(listing.id)}
                />
                {expandedId === listing.id && (
                  <tr key={`${listing.id}-detail`}>
                    <td colSpan={7} className="p-0">
                      <ListingDetail listing={listing} />
                    </td>
                  </tr>
                )}
              </>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={onPageChange}
      />

      <div aria-live="polite" className="sr-only">
        Showing page {currentPage + 1} of {totalPages}
      </div>
    </>
  );
}
