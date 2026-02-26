import { useState } from 'react';
import { CheckCircle, XCircle, BookOpen } from 'lucide-react';

interface JargonReviewItem {
  id: string;
  acronym: string;
  expansion: string;
  contextExample?: string;
  confidence: number;
}

interface JargonReviewCardProps {
  item: JargonReviewItem;
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
  isWorking: boolean;
}

export function JargonReviewCard({
  item,
  onApprove,
  onReject,
  isWorking,
}: JargonReviewCardProps) {
  const [editedExpansion, setEditedExpansion] = useState(item.expansion);

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-purple-100">
          <BookOpen className="h-4 w-4 text-purple-600" />
        </div>

        <div className="flex-1 min-w-0 space-y-2">
          <div className="flex items-center gap-2">
            <code className="rounded bg-gray-100 px-2 py-0.5 text-sm font-mono font-semibold text-gray-900">
              {item.acronym}
            </code>
            <span className="text-xs text-gray-400">
              ({(item.confidence * 100).toFixed(0)}% confidence)
            </span>
          </div>

          <div>
            <label
              htmlFor={`expansion-${item.id}`}
              className="block text-xs font-medium text-gray-500"
            >
              Expansion
            </label>
            <input
              id={`expansion-${item.id}`}
              type="text"
              value={editedExpansion}
              onChange={(e) => setEditedExpansion(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {item.contextExample && (
            <p className="text-xs text-gray-500 italic">
              Context: &ldquo;{item.contextExample}&rdquo;
            </p>
          )}
        </div>
      </div>

      <div className="mt-3 flex gap-2 pl-11">
        <button
          onClick={() => onApprove(item.id)}
          disabled={isWorking}
          className="flex items-center gap-1 rounded-md bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
        >
          <CheckCircle className="h-3.5 w-3.5" />
          Approve
        </button>
        <button
          onClick={() => onReject(item.id)}
          disabled={isWorking}
          className="flex items-center gap-1 rounded-md border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-50"
        >
          <XCircle className="h-3.5 w-3.5" />
          Reject
        </button>
      </div>
    </div>
  );
}
