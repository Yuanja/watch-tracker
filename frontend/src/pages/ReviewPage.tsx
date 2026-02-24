import { useState, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ClipboardList,
  CheckCircle,
  SkipForward,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  X,
} from 'lucide-react';
import {
  getReviewQueue,
  resolveReviewItem,
  skipReviewItem,
} from '../api/review';
import type { ReviewQueueItem } from '../api/review';
import { Badge } from '../components/common/Badge';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

// ── Toast ─────────────────────────────────────────────────────────────────────

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error';
}

let toastCounter = 0;

function useToasts() {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback((message: string, type: Toast['type']) => {
    const id = ++toastCounter;
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return { toasts, addToast, removeToast };
}

function ToastContainer({
  toasts,
  onRemove,
}: {
  toasts: Toast[];
  onRemove: (id: number) => void;
}) {
  if (toasts.length === 0) return null;
  return (
    <div
      className="fixed bottom-6 right-6 z-50 flex flex-col gap-2"
      aria-live="polite"
      aria-atomic="false"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-medium shadow-lg ${
            t.type === 'success'
              ? 'bg-green-600 text-white'
              : 'bg-red-600 text-white'
          }`}
          role="status"
        >
          {t.type === 'success' ? (
            <CheckCircle className="h-4 w-4 shrink-0" />
          ) : (
            <AlertCircle className="h-4 w-4 shrink-0" />
          )}
          <span>{t.message}</span>
          <button
            onClick={() => onRemove(t.id)}
            className="ml-2 rounded p-0.5 opacity-80 hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-white"
            aria-label="Dismiss notification"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      ))}
    </div>
  );
}

// ── Resolve Form ──────────────────────────────────────────────────────────────

interface ResolutionFields {
  itemDescription: string;
  category: string;
  partNumber: string;
  quantity: string;
  price: string;
  intent: 'sell' | 'want' | 'unknown';
}

function buildInitialFields(item: ReviewQueueItem): ResolutionFields {
  const sv = (item.suggestedValues ?? {}) as Record<string, unknown>;
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

interface ResolveFormProps {
  item: ReviewQueueItem;
  onSubmit: (fields: ResolutionFields) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

function ResolveForm({ item, onSubmit, onCancel, isSubmitting }: ResolveFormProps) {
  const [fields, setFields] = useState<ResolutionFields>(() =>
    buildInitialFields(item)
  );

  const set = (key: keyof ResolutionFields) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setFields((prev) => ({ ...prev, [key]: e.target.value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(fields);
  };

  return (
    <form
      onSubmit={handleSubmit}
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

// ── Review Item Card ──────────────────────────────────────────────────────────

interface ReviewItemCardProps {
  item: ReviewQueueItem;
  onResolveSuccess: () => void;
  onSkipSuccess: () => void;
  onError: (msg: string) => void;
  onSuccess: (msg: string) => void;
}

function ReviewItemCard({
  item,
  onResolveSuccess,
  onSkipSuccess,
  onError,
  onSuccess,
}: ReviewItemCardProps) {
  const [showForm, setShowForm] = useState(false);

  const resolveMutation = useMutation({
    mutationFn: (fields: ResolutionFields) => {
      const resolution: Record<string, unknown> = {
        itemDescription: fields.itemDescription,
        intent: fields.intent,
      };
      if (fields.category) resolution['category'] = fields.category;
      if (fields.partNumber) resolution['partNumber'] = fields.partNumber;
      if (fields.quantity) resolution['quantity'] = parseFloat(fields.quantity);
      if (fields.price) resolution['price'] = parseFloat(fields.price);
      return resolveReviewItem(item.id, { resolution });
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
      {/* Header row */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          {/* Original message */}
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-1">
            Original Message
          </p>
          <p className="whitespace-pre-wrap text-sm text-gray-800 bg-gray-50 rounded-lg border border-gray-200 px-3 py-2">
            {item.originalText ?? (
              <span className="text-gray-400 italic">No original text available</span>
            )}
          </p>
        </div>

        {/* Confidence badge */}
        <div className="shrink-0 text-right">
          <p className="text-xs text-gray-400 mb-1">Confidence</p>
          <ConfidencePill item={item} />
        </div>
      </div>

      {/* Metadata strip */}
      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500">
        {item.senderName && (
          <span>
            <span className="font-medium text-gray-600">Sender:</span> {item.senderName}
          </span>
        )}
        {item.groupName && (
          <span>
            <span className="font-medium text-gray-600">Group:</span> {item.groupName}
          </span>
        )}
        <span>
          <span className="font-medium text-gray-600">Queued:</span>{' '}
          {new Date(item.createdAt).toLocaleString()}
        </span>
      </div>

      {/* LLM reason */}
      {item.llmExplanation && (
        <div className="mt-3">
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-1">
            LLM Explanation
          </p>
          <p className="text-sm text-gray-600 italic">{item.llmExplanation}</p>
        </div>
      )}

      {/* Suggested values */}
      {item.suggestedValues && Object.keys(item.suggestedValues).length > 0 && (
        <div className="mt-3">
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-2">
            Suggested Values
          </p>
          <div className="flex flex-wrap gap-2">
            {Object.entries(item.suggestedValues).map(([key, val]) => {
              if (val === null || val === undefined || val === '') return null;
              return (
                <div
                  key={key}
                  className="flex items-center gap-1.5 rounded-md border border-gray-200 bg-gray-50 px-2.5 py-1"
                >
                  <span className="text-xs font-medium text-gray-500 capitalize">
                    {key.replace(/([A-Z])/g, ' $1').trim()}:
                  </span>
                  <span className="text-xs text-gray-800">{String(val)}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Action buttons */}
      {!showForm && (
        <div className="mt-4 flex gap-2">
          <button
            onClick={() => setShowForm(true)}
            disabled={isWorking}
            className="flex items-center gap-1.5 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 disabled:opacity-50"
            aria-label={`Resolve review item for: ${item.originalText?.slice(0, 50) ?? item.id}`}
          >
            <CheckCircle className="h-4 w-4" />
            Resolve
          </button>
          <button
            onClick={() => skipMutation.mutate()}
            disabled={isWorking}
            className="flex items-center gap-1.5 rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-400 disabled:opacity-50"
            aria-label={`Skip review item for: ${item.originalText?.slice(0, 50) ?? item.id}`}
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

      {/* Inline resolve form */}
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

function ConfidencePill({ item }: { item: ReviewQueueItem }) {
  // confidence may live in suggestedValues or as a root field
  const sv = (item.suggestedValues ?? {}) as Record<string, unknown>;
  const score =
    typeof sv['confidenceScore'] === 'number'
      ? sv['confidenceScore']
      : null;

  if (score === null) {
    return <Badge variant="gray">—</Badge>;
  }

  const pct = (score * 100).toFixed(0);
  const variant =
    score >= 0.8 ? 'green' : score >= 0.5 ? 'yellow' : 'red';

  return <Badge variant={variant}>{pct}%</Badge>;
}

// ── Main Page ─────────────────────────────────────────────────────────────────

const PAGE_SIZE = 10;

export function ReviewPage() {
  const [page, setPage] = useState(0);
  const { toasts, addToast, removeToast } = useToasts();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['review-queue', page],
    queryFn: () => getReviewQueue({ page, size: PAGE_SIZE }),
    placeholderData: (prev) => prev,
  });

  const invalidateQueue = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['review-queue'] });
  }, [queryClient]);

  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  return (
    <>
      <div className="flex h-full flex-col gap-6 overflow-y-auto p-6">
        {/* Page header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-100">
              <ClipboardList className="h-5 w-5 text-orange-600" />
            </div>
            <div>
              <h1 className="text-xl font-semibold text-gray-900">Review Queue</h1>
              <p className="text-sm text-gray-500">
                Review and correct low-confidence LLM extractions
              </p>
            </div>
          </div>

          {totalElements > 0 && (
            <div className="text-sm text-gray-500">
              <span className="font-semibold text-orange-600">{totalElements}</span>{' '}
              pending item{totalElements !== 1 ? 's' : ''}
            </div>
          )}
        </div>

        {/* Content area */}
        {isLoading ? (
          <LoadingOverlay message="Loading review queue..." />
        ) : isError ? (
          <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-red-200 bg-red-50 py-12">
            <AlertCircle className="h-8 w-8 text-red-500" />
            <p className="text-sm font-medium text-red-700">
              Failed to load review queue
            </p>
            <p className="text-xs text-red-500">
              {error instanceof Error ? error.message : 'Unknown error'}
            </p>
          </div>
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={<ClipboardList className="h-8 w-8 text-gray-400" />}
            title="Queue is empty"
            description="All extraction items have been reviewed. New items will appear here when the pipeline flags them for human review."
          />
        ) : (
          <>
            {/* Items list */}
            <div className="flex flex-col gap-4">
              {data.content.map((item: ReviewQueueItem) => (
                <ReviewItemCard
                  key={item.id}
                  item={item}
                  onResolveSuccess={invalidateQueue}
                  onSkipSuccess={invalidateQueue}
                  onSuccess={(msg) => addToast(msg, 'success')}
                  onError={(msg) => addToast(msg, 'error')}
                />
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between rounded-xl border border-gray-200 bg-white px-4 py-3 shadow-sm">
                <p className="text-sm text-gray-500">
                  Page {page + 1} of {totalPages} &mdash; {totalElements} total
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
                  <span className="px-2 text-sm text-gray-700">
                    {page + 1} / {totalPages}
                  </span>
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

      {/* Toast notifications */}
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
}
