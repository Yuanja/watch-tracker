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
  const [modelName, setModelName] = useState(listing.modelName ?? '');
  const [dialColor, setDialColor] = useState(listing.dialColor ?? '');
  const [caseMaterial, setCaseMaterial] = useState(listing.caseMaterial ?? '');
  const [year, setYear] = useState(listing.year?.toString() ?? '');
  const [caseSizeMm, setCaseSizeMm] = useState(listing.caseSizeMm?.toString() ?? '');
  const [setComposition, setSetComposition] = useState(listing.setComposition ?? '');
  const [braceletStrap, setBraceletStrap] = useState(listing.braceletStrap ?? '');
  const [price, setPrice] = useState(listing.price?.toString() ?? '');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      itemDescription: description,
      partNumber: partNumber || null,
      modelName: modelName || null,
      dialColor: dialColor || null,
      caseMaterial: caseMaterial || null,
      year: year ? parseInt(year, 10) : null,
      caseSizeMm: caseSizeMm ? parseInt(caseSizeMm, 10) : null,
      setComposition: setComposition || null,
      braceletStrap: braceletStrap || null,
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

          <div className="grid grid-cols-2 gap-4">
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
              <label htmlFor="edit-model" className="block text-sm font-medium text-gray-700">
                Model Name
              </label>
              <input
                id="edit-model"
                type="text"
                value={modelName}
                onChange={(e) => setModelName(e.target.value)}
                placeholder="e.g. Submariner"
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="edit-dial" className="block text-sm font-medium text-gray-700">
                Dial Color
              </label>
              <input
                id="edit-dial"
                type="text"
                value={dialColor}
                onChange={(e) => setDialColor(e.target.value)}
                placeholder="e.g. black, blue, green"
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="edit-material" className="block text-sm font-medium text-gray-700">
                Case Material
              </label>
              <input
                id="edit-material"
                type="text"
                value={caseMaterial}
                onChange={(e) => setCaseMaterial(e.target.value)}
                placeholder="e.g. steel, rose gold"
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="edit-year" className="block text-sm font-medium text-gray-700">
                Year
              </label>
              <input
                id="edit-year"
                type="number"
                min={1900}
                max={2100}
                value={year}
                onChange={(e) => setYear(e.target.value)}
                placeholder="e.g. 2023"
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="edit-size" className="block text-sm font-medium text-gray-700">
                Case Size (mm)
              </label>
              <input
                id="edit-size"
                type="number"
                min={10}
                max={60}
                value={caseSizeMm}
                onChange={(e) => setCaseSizeMm(e.target.value)}
                placeholder="e.g. 41"
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="edit-set" className="block text-sm font-medium text-gray-700">
                Set Composition
              </label>
              <input
                id="edit-set"
                type="text"
                value={setComposition}
                onChange={(e) => setSetComposition(e.target.value)}
                placeholder="e.g. full set, watch only"
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="edit-bracelet" className="block text-sm font-medium text-gray-700">
                Bracelet/Strap
              </label>
              <input
                id="edit-bracelet"
                type="text"
                value={braceletStrap}
                onChange={(e) => setBraceletStrap(e.target.value)}
                placeholder="e.g. Oyster, Jubilee, rubber"
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="edit-price" className="block text-sm font-medium text-gray-700">
                Price
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
