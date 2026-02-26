import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { CheckCircle, SkipForward } from 'lucide-react';
import {
  resolveReviewItem,
  skipReviewItem,
  type ReviewQueueItem,
} from '../../api/review';
import { Badge } from '../common/Badge';

// ── Resolution form fields ──────────────────────────────────────────────────

interface ResolutionFields {
  itemDescription: string;
  category: string;
  partNumber: string;
  quantity: string;
  price: string;
  intent: 'sell' | 'want' | 'unknown';
}

function parseSuggestedValues(raw: string | null): Record<string, unknown> {
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw);
    return typeof parsed === 'object' && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

function buildInitialFields(item: ReviewQueueItem): ResolutionFields {
  const sv = parseSuggestedValues(item.suggestedValues);
  return {
    itemDescription: String(sv['itemDescription'] ?? ''),
    category: String(sv['category'] ?? ''),
    partNumber: String(sv['partNumber'] ?? ''),
    quantity: sv['quantity'] !== undefined && sv['quantity'] !== null ? String(sv['quantity']) : '',
    price: sv['price'] !== undefined && sv['price'] !== null ? String(sv['price']) : '',
    intent: (['sell', 'want', 'unknown'].includes(String(sv['intent']))
      ? String(sv['intent'])
      : 'unknown') as 'sell' | 'want' | 'unknown',
  };
}

// ── Resolve Form ────────────────────────────────────────────────────────────

function ResolveForm({
  item,
  onSubmit,
  onCancel,
  isSubmitting,
}: {
  item: ReviewQueueItem;
  onSubmit: (fields: ResolutionFields) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}) {
  const [fields, setFields] = useState<ResolutionFields>(() =>
    buildInitialFields(item)
  );

  const set = (key: keyof ResolutionFields) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setFields((prev) => ({ ...prev, [key]: e.target.value }));

  return (
    <form
      onSubmit={(e) => { e.preventDefault(); onSubmit(fields); }}
      className="mt-4 rounded-lg border border-blue-200 bg-blue-50 p-4"
      aria-label="Resolve listing form"
    >
      <h4 className="mb-3 text-sm font-semibold text-blue-900">
        Edit Extraction Fields
      </h4>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <label className="block text-xs font-medium text-gray-600" htmlFor={`desc-${item.id}`}>
            Item Description
          </label>
          <input
            id={`desc-${item.id}`}
            type="text"
            value={fields.itemDescription}
            onChange={set('itemDescription')}
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            required
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-600" htmlFor={`intent-${item.id}`}>
            Intent
          </label>
          <select
            id={`intent-${item.id}`}
            value={fields.intent}
            onChange={set('intent')}
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="sell">Sell</option>
            <option value="want">Want</option>
            <option value="unknown">Unknown</option>
          </select>
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-600" htmlFor={`category-${item.id}`}>
            Category
          </label>
          <input
            id={`category-${item.id}`}
            type="text"
            value={fields.category}
            onChange={set('category')}
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-600" htmlFor={`pn-${item.id}`}>
            Part Number
          </label>
          <input
            id={`pn-${item.id}`}
            type="text"
            value={fields.partNumber}
            onChange={set('partNumber')}
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-600" htmlFor={`qty-${item.id}`}>
            Quantity
          </label>
          <input
            id={`qty-${item.id}`}
            type="number"
            value={fields.quantity}
            onChange={set('quantity')}
            min={0}
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-600" htmlFor={`price-${item.id}`}>
            Price (USD)
          </label>
          <input
            id={`price-${item.id}`}
            type="number"
            value={fields.price}
            onChange={set('price')}
            min={0}
            step="0.01"
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
      </div>

      <div className="mt-4 flex gap-2">
        <button
          type="submit"
          disabled={isSubmitting}
          className="flex items-center gap-1.5 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 disabled:opacity-50"
        >
          <CheckCircle className="h-4 w-4" />
          {isSubmitting ? 'Saving...' : 'Confirm Resolution'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={isSubmitting}
          className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-400 disabled:opacity-50"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}

// ── Confidence Pill ─────────────────────────────────────────────────────────

function ConfidencePill({ item }: { item: ReviewQueueItem }) {
  const sv = parseSuggestedValues(item.suggestedValues);
  const score =
    typeof sv['confidenceScore'] === 'number'
      ? sv['confidenceScore']
      : null;

  if (score === null) {
    return <Badge variant="gray">&mdash;</Badge>;
  }

  const pct = (score * 100).toFixed(0);
  const variant =
    score >= 0.8 ? 'green' : score >= 0.5 ? 'yellow' : 'red';

  return <Badge variant={variant}>{pct}%</Badge>;
}

// ── Review Card ─────────────────────────────────────────────────────────────

interface ReviewCardProps {
  item: ReviewQueueItem;
  onResolveSuccess: () => void;
  onSkipSuccess: () => void;
  onError: (msg: string) => void;
  onSuccess: (msg: string) => void;
}

export function ReviewCard({
  item,
  onResolveSuccess,
  onSkipSuccess,
  onError,
  onSuccess,
}: ReviewCardProps) {
  const [showForm, setShowForm] = useState(false);

  const resolveMutation = useMutation({
    mutationFn: (fields: ResolutionFields) => {
      const request: Record<string, unknown> = {
        itemDescription: fields.itemDescription,
        intent: fields.intent,
      };
      if (fields.category) request['categoryName'] = fields.category;
      if (fields.partNumber) request['partNumber'] = fields.partNumber;
      if (fields.quantity) request['quantity'] = parseFloat(fields.quantity);
      if (fields.price) request['price'] = parseFloat(fields.price);
      return resolveReviewItem(item.id, request as import('../../api/review').ResolveRequest);
    },
    onSuccess: () => {
      onSuccess('Item resolved successfully.');
      onResolveSuccess();
    },
    onError: () => {
      onError('Failed to resolve item. Please try again.');
    },
  });

  const skipMutation = useMutation({
    mutationFn: () => skipReviewItem(item.id),
    onSuccess: () => {
      onSuccess('Item skipped.');
      onSkipSuccess();
    },
    onError: () => {
      onError('Failed to skip item. Please try again.');
    },
  });

  const isWorking = resolveMutation.isPending || skipMutation.isPending;

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-1">
            Original Message
          </p>
          <p className="whitespace-pre-wrap text-sm text-gray-800 bg-gray-50 rounded-lg border border-gray-200 px-3 py-2">
            {item.originalMessageBody ?? (
              <span className="text-gray-400 italic">No original text available</span>
            )}
          </p>
        </div>

        <div className="shrink-0 text-right">
          <p className="text-xs text-gray-400 mb-1">Confidence</p>
          <ConfidencePill item={item} />
        </div>
      </div>

      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500">
        {item.senderName && (
          <span>
            <span className="font-medium text-gray-600">Sender:</span> {item.senderName}
          </span>
        )}
        <span>
          <span className="font-medium text-gray-600">Queued:</span>{' '}
          {new Date(item.createdAt).toLocaleString()}
        </span>
      </div>

      {item.llmExplanation && (
        <div className="mt-3">
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-1">
            LLM Explanation
          </p>
          <p className="text-sm text-gray-600 italic">{item.llmExplanation}</p>
        </div>
      )}

      {(() => {
        const sv = parseSuggestedValues(item.suggestedValues);
        const entries = Object.entries(sv).filter(([, val]) => val !== null && val !== undefined && val !== '');
        if (entries.length === 0) return null;
        return (
          <div className="mt-3">
            <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-2">
              Suggested Values
            </p>
            <div className="flex flex-wrap gap-2">
              {entries.map(([key, val]) => (
                <div
                  key={key}
                  className="flex items-center gap-1.5 rounded-md border border-gray-200 bg-gray-50 px-2.5 py-1"
                >
                  <span className="text-xs font-medium text-gray-500 capitalize">
                    {key.replace(/([A-Z])/g, ' $1').trim()}:
                  </span>
                  <span className="text-xs text-gray-800">{String(val)}</span>
                </div>
              ))}
            </div>
          </div>
        );
      })()}

      {!showForm && (
        <div className="mt-4 flex gap-2">
          <button
            onClick={() => setShowForm(true)}
            disabled={isWorking}
            className="flex items-center gap-1.5 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 disabled:opacity-50"
            aria-label={`Resolve review item for: ${item.originalMessageBody?.slice(0, 50) ?? item.id}`}
          >
            <CheckCircle className="h-4 w-4" />
            Resolve
          </button>
          <button
            onClick={() => skipMutation.mutate()}
            disabled={isWorking}
            className="flex items-center gap-1.5 rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-400 disabled:opacity-50"
            aria-label={`Skip review item for: ${item.originalMessageBody?.slice(0, 50) ?? item.id}`}
          >
            {skipMutation.isPending ? (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-gray-400 border-t-transparent" />
            ) : (
              <SkipForward className="h-4 w-4" />
            )}
            {skipMutation.isPending ? 'Skipping...' : 'Skip'}
          </button>
        </div>
      )}

      {showForm && (
        <ResolveForm
          item={item}
          onSubmit={(fields) => resolveMutation.mutate(fields)}
          onCancel={() => setShowForm(false)}
          isSubmitting={resolveMutation.isPending}
        />
      )}
    </div>
  );
}
