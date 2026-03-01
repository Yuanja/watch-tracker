import { useState, useRef, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bot, CheckCircle, Loader2, Send, SkipForward, Trash2 } from 'lucide-react';
import {
  resolveReviewItem,
  skipReviewItem,
  type ReviewQueueItem,
} from '../../api/review';
import { getMessage } from '../../api/messages';
import { retryExtraction, deleteListing } from '../../api/listings';
import { Badge } from '../common/Badge';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { MessageBubble } from '../replay/MessageBubble';

// ── Helpers ────────────────────────────────────────────────────────────────

function parseSuggestedValues(raw: string | null): Record<string, unknown> {
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw);
    return typeof parsed === 'object' && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

function DetailField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-gray-400">{label}</p>
      <div className="mt-1 text-sm text-gray-800">{children}</div>
    </div>
  );
}

// ── Resolution form fields ──────────────────────────────────────────────────

interface ResolutionFields {
  itemDescription: string;
  category: string;
  partNumber: string;
  quantity: string;
  price: string;
  intent: 'sell' | 'want' | 'unknown';
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

function ConfidencePill({ score }: { score: number | null }) {
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
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Agent popover state (like ListingDetail)
  const [showAgentPopover, setShowAgentPopover] = useState(false);
  const [hint, setHint] = useState('');
  const [retrying, setRetrying] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);
  const [retrySuccess, setRetrySuccess] = useState(false);
  const hideTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const sv = parseSuggestedValues(item.suggestedValues);
  const confidenceScore = typeof sv['confidenceScore'] === 'number' ? sv['confidenceScore'] : null;

  // Fetch the raw message for MessageBubble rendering
  const { data: rawMessage, isLoading: isLoadingMessage } = useQuery({
    queryKey: ['rawMessage', item.rawMessageId],
    queryFn: () => getMessage(item.rawMessageId),
    enabled: !!item.rawMessageId,
    staleTime: 5 * 60_000,
  });

  // ── Mutations ──────────────────────────────────────────────────────────

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

  const deleteListingMutation = useMutation({
    mutationFn: async () => {
      if (item.listingId) {
        await deleteListing(item.listingId);
      }
      await skipReviewItem(item.id);
    },
    onSuccess: () => {
      setShowDeleteConfirm(false);
      onSuccess('Listing deleted and review item removed.');
      onSkipSuccess();
      queryClient.invalidateQueries({ queryKey: ['listings'] });
      queryClient.invalidateQueries({ queryKey: ['listing-stats'] });
    },
    onError: () => {
      setShowDeleteConfirm(false);
      onError('Failed to delete listing.');
    },
  });

  const isWorking = resolveMutation.isPending || skipMutation.isPending || deleteListingMutation.isPending;

  // ── Agent popover handlers (like ListingDetail) ────────────────────────

  const handleRetry = async () => {
    if (!item.listingId) return;
    setRetrying(true);
    setRetryError(null);
    setRetrySuccess(false);
    try {
      await retryExtraction(item.listingId, hint || undefined);
      setHint('');
      setRetrySuccess(true);
      queryClient.invalidateQueries({ queryKey: ['review-queue'] });
      queryClient.invalidateQueries({ queryKey: ['listings'] });
      setTimeout(() => {
        setRetrySuccess(false);
        setShowAgentPopover(false);
      }, 2000);
    } catch (err) {
      setRetryError(err instanceof Error ? err.message : 'Retry failed');
    } finally {
      setRetrying(false);
    }
  };

  const handleMouseEnter = () => {
    if (hideTimeout.current) {
      clearTimeout(hideTimeout.current);
      hideTimeout.current = null;
    }
    setShowAgentPopover(true);
    setTimeout(() => inputRef.current?.focus(), 50);
  };

  const handleMouseLeave = () => {
    if (retrying) return;
    hideTimeout.current = setTimeout(() => {
      setShowAgentPopover(false);
      setRetryError(null);
    }, 300);
  };

  // ── Render ─────────────────────────────────────────────────────────────

  return (
    <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
      {/* Action buttons row */}
      <div className="flex items-center justify-between border-b border-gray-100 px-5 py-3">
        <div className="flex items-center gap-2">
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400">
            Review Item
          </p>
          <ConfidencePill score={confidenceScore} />
        </div>

        <div className="flex items-center gap-2">
          {/* Agent popover button */}
          {item.listingId && (
            <div
              className="relative"
              onMouseEnter={handleMouseEnter}
              onMouseLeave={handleMouseLeave}
            >
              <button
                type="button"
                className="flex items-center gap-1.5 rounded-md border border-purple-300 bg-white px-3 py-1.5 text-xs font-medium text-purple-700 hover:bg-purple-50"
              >
                {retrying ? (
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                ) : (
                  <Bot className="h-3.5 w-3.5" />
                )}
                Agent
              </button>

              {showAgentPopover && (
                <div className="absolute right-0 top-full z-20 mt-1 w-80 rounded-lg border border-purple-200 bg-white p-3 shadow-lg">
                  <p className="mb-2 text-xs text-gray-500">
                    Re-parse with AI. Add an optional hint below.
                  </p>
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      handleRetry();
                    }}
                    className="flex items-center gap-1.5"
                  >
                    <input
                      ref={inputRef}
                      type="text"
                      value={hint}
                      onChange={(e) => setHint(e.target.value)}
                      placeholder="e.g. 'this is a Rolex Submariner'"
                      disabled={retrying}
                      className="flex-1 rounded-md border border-gray-300 px-2.5 py-1.5 text-xs placeholder:text-gray-400 focus:border-purple-500 focus:outline-none focus:ring-1 focus:ring-purple-500 disabled:bg-gray-50"
                    />
                    <button
                      type="submit"
                      disabled={retrying}
                      className="rounded-md bg-purple-600 p-1.5 text-white hover:bg-purple-700 disabled:opacity-50"
                      aria-label="Submit"
                    >
                      {retrying ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <Send className="h-3.5 w-3.5" />
                      )}
                    </button>
                  </form>
                  {retryError && (
                    <p className="mt-1.5 text-xs text-red-600">{retryError}</p>
                  )}
                  {retrySuccess && (
                    <p className="mt-1.5 text-xs text-green-600">Updated!</p>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Delete button */}
          {item.listingId && (
            <button
              type="button"
              onClick={() => setShowDeleteConfirm(true)}
              disabled={isWorking}
              className="flex items-center gap-1.5 rounded-md border border-red-300 bg-white px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Delete
            </button>
          )}
        </div>
      </div>

      <div className="p-5">
        {/* Source message rendered as MessageBubble */}
        <div className="mb-4">
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-2">
            Source Message
          </p>
          {isLoadingMessage ? (
            <div className="flex items-center gap-2 text-xs text-gray-400 py-2">
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
              Loading source message...
            </div>
          ) : rawMessage ? (
            <div className="rounded-lg border border-gray-200 bg-white p-4">
              <MessageBubble message={rawMessage} />
            </div>
          ) : (
            <p className="whitespace-pre-wrap text-sm text-gray-800 bg-gray-50 rounded-lg border border-gray-200 px-3 py-2">
              {item.originalMessageBody ?? (
                <span className="text-gray-400 italic">No original text available</span>
              )}
            </p>
          )}
        </div>

        {/* Metadata row */}
        <div className="mb-4 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500">
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

        {/* LLM explanation */}
        {item.llmExplanation && (
          <div className="mb-4">
            <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-1">
              LLM Explanation
            </p>
            <p className="text-sm text-gray-600 italic">{item.llmExplanation}</p>
          </div>
        )}

        {/* Structured suggested values grid (like ListingDetail) */}
        {(() => {
          const entries = Object.entries(sv).filter(
            ([key, val]) => val !== null && val !== undefined && val !== '' && key !== 'confidenceScore'
          );
          if (entries.length === 0) return null;

          // Map well-known keys to nice labels
          const labelMap: Record<string, string> = {
            itemDescription: 'Description',
            intent: 'Intent',
            category: 'Category',
            manufacturer: 'Manufacturer',
            partNumber: 'Part Number',
            modelName: 'Model',
            dialColor: 'Dial Color',
            caseMaterial: 'Case Material',
            year: 'Year',
            caseSizeMm: 'Case Size',
            setComposition: 'Set Composition',
            braceletStrap: 'Bracelet/Strap',
            quantity: 'Quantity',
            unit: 'Unit',
            price: 'Price',
            currency: 'Currency',
            condition: 'Condition',
            priceCurrency: 'Currency',
          };

          return (
            <div className="mb-4">
              <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-2">
                Extracted Fields
              </p>
              <div className="grid grid-cols-1 gap-y-2 gap-x-6 sm:grid-cols-2 lg:grid-cols-3">
                {entries.map(([key, val]) => {
                  const label = labelMap[key] ?? key.replace(/([A-Z])/g, ' $1').trim();
                  let displayVal = String(val);
                  if (key === 'caseSizeMm' && val) displayVal = `${val}mm`;
                  return (
                    <DetailField key={key} label={label}>
                      {displayVal}
                    </DetailField>
                  );
                })}
              </div>
            </div>
          );
        })()}

        {/* Action buttons: Resolve / Skip */}
        {!showForm && (
          <div className="flex gap-2">
            <button
              onClick={() => setShowForm(true)}
              disabled={isWorking}
              className="flex items-center gap-1.5 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 disabled:opacity-50"
              aria-label={`Resolve review item for: ${item.originalMessageBody?.slice(0, 50) ?? item.id}`}
            >
              <CheckCircle className="h-4 w-4" />
              Edit &amp; Resolve
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

      {/* Delete confirmation */}
      <ConfirmDialog
        isOpen={showDeleteConfirm}
        title="Delete listing"
        message="Delete this listing and remove it from the review queue? This action is a soft delete and can be reversed."
        confirmLabel="Delete"
        variant="danger"
        onConfirm={() => deleteListingMutation.mutate()}
        onCancel={() => setShowDeleteConfirm(false)}
        isLoading={deleteListingMutation.isPending}
      />
    </div>
  );
}
