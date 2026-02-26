import { useState, useCallback } from 'react';
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

  const isAdmin = user?.role === 'admin' || user?.role === 'uber_admin';
  const isUberAdmin = user?.role === 'uber_admin';

  const editMutation = useMutation({
    mutationFn: ({ id, updates }: { id: string; updates: Record<string, unknown> }) =>
      updateListing(id, updates as Partial<Listing>),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['listings'] });
      setEditingListing(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteListing(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['listings'] });
      queryClient.invalidateQueries({ queryKey: ['listing-stats'] });
      setDeletingListing(null);
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
    </>
  );
}
