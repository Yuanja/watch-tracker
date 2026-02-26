import type { ReactNode } from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import type { Listing } from '../../types/listing';
import { Badge } from '../common/Badge';

function confidenceColor(score: number): string {
  if (score >= 0.8) return 'text-green-700';
  if (score >= 0.5) return 'text-yellow-700';
  return 'text-red-700';
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function DetailField({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-gray-400">{label}</p>
      <div className="mt-1 text-sm text-gray-800">{children}</div>
    </div>
  );
}

interface ListingDetailProps {
  listing: Listing;
  canEdit?: boolean;
  canDelete?: boolean;
  onEdit?: () => void;
  onDelete?: () => void;
}

export function ListingDetail({ listing, canEdit, canDelete, onEdit, onDelete }: ListingDetailProps) {
  return (
    <div className="bg-gray-50 border-t border-gray-200 px-6 py-4">
      {/* Admin actions */}
      {(canEdit || canDelete) && (
        <div className="flex justify-end gap-2 mb-3">
          {canEdit && (
            <button
              onClick={onEdit}
              className="flex items-center gap-1.5 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
            >
              <Pencil className="h-3.5 w-3.5" />
              Edit
            </button>
          )}
          {canDelete && (
            <button
              onClick={onDelete}
              className="flex items-center gap-1.5 rounded-md border border-red-300 bg-white px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Delete
            </button>
          )}
        </div>
      )}

      <div className="grid grid-cols-1 gap-y-3 gap-x-8 sm:grid-cols-2 lg:grid-cols-3">
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
          {listing.itemCategoryName ? (
            <Badge variant="teal">{listing.itemCategoryName}</Badge>
          ) : (
            <span className="text-gray-400">&mdash;</span>
          )}
        </DetailField>

        <DetailField label="Manufacturer">
          {listing.manufacturerName ?? <span className="text-gray-400">&mdash;</span>}
        </DetailField>

        <DetailField label="Part Number">
          {listing.partNumber ? (
            <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs font-mono text-gray-700">
              {listing.partNumber}
            </code>
          ) : (
            <span className="text-gray-400">&mdash;</span>
          )}
        </DetailField>

        <DetailField label="Quantity">
          {listing.quantity !== null
            ? `${listing.quantity}${listing.unitAbbreviation ? ` ${listing.unitAbbreviation}` : ''}`
            : <span className="text-gray-400">&mdash;</span>}
        </DetailField>

        <DetailField label="Condition">
          {listing.conditionName ?? <span className="text-gray-400">&mdash;</span>}
        </DetailField>

        <DetailField label="Sender">
          {listing.senderName ?? listing.senderPhone ?? <span className="text-gray-400">Unknown</span>}
        </DetailField>

        <DetailField label="Group">
          {listing.groupName ?? <span className="text-gray-400">&mdash;</span>}
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

        {listing.reviewedByName && (
          <DetailField label="Reviewed By">
            {listing.reviewedByName}
            {listing.reviewedAt ? ` on ${formatDate(listing.reviewedAt)}` : ''}
          </DetailField>
        )}
      </div>
    </div>
  );
}
