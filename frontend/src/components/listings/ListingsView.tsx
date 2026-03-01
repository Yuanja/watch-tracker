import { useState, useCallback, Fragment } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { Listing } from '../../types/listing';
import { updateListing, deleteListing } from '../../api/listings';
import { useAuth } from '../../contexts/AuthContext';
import { EmptyState } from '../common/EmptyState';
import { Pagination } from '../common/Pagination';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { ListingCard } from './ListingCard';
import { ListingDetail } from './ListingDetail';
import { ListingEditModal } from './ListingEditModal';
import { AssistDialog } from '../review/AssistDialog';

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
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [editingListing, setEditingListing] = useState<Listing | null>(null);
  const [deletingListing, setDeletingListing] = useState<Listing | null>(null);
  const [assistListingId, setAssistListingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const isAdmin = user?.role === 'admin' || user?.role === 'uber_admin';
  const isUberAdmin = user?.role === 'uber_admin';

  const editMutation = useMutation({
    mutationFn: ({ id, updates }: { id: string; updates: Record<string, unknown> }) =>
      updateListing(id, updates as Partial<Listing>),
    onSuccess: () => {
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['listings'] });
      setEditingListing(null);
    },
    onError: (err) => {
      setActionError(err instanceof Error ? err.message : 'Failed to update listing');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteListing(id),
    onSuccess: () => {
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['listings'] });
      queryClient.invalidateQueries({ queryKey: ['listing-stats'] });
      setDeletingListing(null);
    },
    onError: (err) => {
      setDeletingListing(null);
      setActionError(err instanceof Error ? err.message : 'Failed to delete listing');
    },
  });

  const handleEdit = useCallback((listing: Listing) => setEditingListing(listing), []);
  const handleDelete = useCallback((listing: Listing) => setDeletingListing(listing), []);
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
      {actionError && (
        <div className="mx-4 mt-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {actionError}
          <button
            type="button"
            onClick={() => setActionError(null)}
            className="ml-2 font-medium underline hover:text-red-800"
          >
            Dismiss
          </button>
        </div>
      )}

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
              <th className="whitespace-nowrap px-2 py-3 font-medium text-gray-600">Date</th>
              <th className="px-4 py-3 font-medium text-gray-600">Description</th>
              <th className="px-4 py-3 font-medium text-gray-600">Intent</th>
              <th className="hidden px-4 py-3 font-medium text-gray-600 sm:table-cell">
                Sender
              </th>
              <th className="hidden px-4 py-3 font-medium text-gray-600 sm:table-cell">
                Group
              </th>
              <th className="px-4 py-3 font-medium text-gray-600">
                Price
              </th>
              <th className="hidden whitespace-nowrap px-4 py-3 font-medium text-gray-600 md:table-cell">
                Rate
              </th>
              <th className="whitespace-nowrap px-4 py-3 font-medium text-gray-600">
                USD
              </th>
              <th className="px-4 py-3 font-medium text-gray-600">Status</th>
              <th className="w-8 px-2 py-3" aria-hidden="true" />
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {listings.map((listing) => (
              <Fragment key={listing.id}>
                <ListingCard
                  listing={listing}
                  isExpanded={expandedId === listing.id}
                  onToggle={() => onToggleExpand(listing.id)}
                  onAssist={isAdmin ? () => setAssistListingId(listing.id) : undefined}
                />
                {expandedId === listing.id && (
                  <tr key={`${listing.id}-detail`}>
                    <td colSpan={10} className="p-0">
                      <ListingDetail
                        listing={listing}
                        canEdit={isAdmin}
                        canDelete={isUberAdmin}
                        onEdit={() => handleEdit(listing)}
                        onDelete={() => handleDelete(listing)}
                      />
                    </td>
                  </tr>
                )}
              </Fragment>
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

      {/* Edit modal */}
      {editingListing && (
        <ListingEditModal
          listing={editingListing}
          isOpen
          onClose={() => setEditingListing(null)}
          onSave={(updates) =>
            editMutation.mutate({ id: editingListing.id, updates })
          }
          isSaving={editMutation.isPending}
        />
      )}

      {/* Delete confirmation */}
      <ConfirmDialog
        isOpen={deletingListing !== null}
        title="Delete listing"
        message={`Delete "${deletingListing?.itemDescription ?? ''}"? This action is a soft delete and can be reversed.`}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={() => deletingListing && deleteMutation.mutate(deletingListing.id)}
        onCancel={() => setDeletingListing(null)}
        isLoading={deleteMutation.isPending}
      />

      {/* Agent-assisted review dialog */}
      {assistListingId && (
        <AssistDialog
          listingId={assistListingId}
          onClose={() => setAssistListingId(null)}
          onResolved={() => {
            setAssistListingId(null);
            queryClient.invalidateQueries({ queryKey: ['listings'] });
            queryClient.invalidateQueries({ queryKey: ['listing-stats'] });
          }}
        />
      )}
    </>
  );
}
