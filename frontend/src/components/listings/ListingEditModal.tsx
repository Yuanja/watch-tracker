import { useState } from 'react';
import { X } from 'lucide-react';
import type { Listing } from '../../types/listing';

interface ListingEditModalProps {
  listing: Listing;
  isOpen: boolean;
  onClose: () => void;
  onSave: (updates: Record<string, unknown>) => void;
  isSaving: boolean;
}

export function ListingEditModal({
  listing,
  isOpen,
  onClose,
  onSave,
  isSaving,
}: ListingEditModalProps) {
  const [description, setDescription] = useState(listing.itemDescription);
  const [partNumber, setPartNumber] = useState(listing.partNumber ?? '');
  const [price, setPrice] = useState(listing.price?.toString() ?? '');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      itemDescription: description,
      partNumber: partNumber || null,
      price: price ? parseFloat(price) : null,
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Edit Listing</h2>
          <button
            onClick={onClose}
            className="rounded-md p-1 text-gray-400 hover:text-gray-600"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="edit-desc" className="block text-sm font-medium text-gray-700">
              Description
            </label>
            <input
              id="edit-desc"
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>

          <div>
            <label htmlFor="edit-pn" className="block text-sm font-medium text-gray-700">
              Part Number
            </label>
            <input
              id="edit-pn"
              type="text"
              value={partNumber}
              onChange={(e) => setPartNumber(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <div>
            <label htmlFor="edit-price" className="block text-sm font-medium text-gray-700">
              Price (USD)
            </label>
            <input
              id="edit-price"
              type="number"
              step="0.01"
              min={0}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {isSaving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
