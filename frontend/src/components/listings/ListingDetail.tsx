import { useState, useRef, type ReactNode } from 'react';
import { Bot, Loader2, Pencil, Send, Trash2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import type { Listing } from '../../types/listing';
import { Badge, StatusBadge } from '../common/Badge';
import { getCrossPosts, retryExtraction } from '../../api/listings';
import { getMessage } from '../../api/messages';
import { useQueryClient } from '@tanstack/react-query';
import { MessageBubble } from '../replay/MessageBubble';

function confidenceColor(score: number): string {
  if (score >= 0.8) return 'text-green-700';
  if (score >= 0.5) return 'text-yellow-700';
  return 'text-red-700';
}

function formatPrice(price: number | null, currency: string): string {
  if (price === null) return '\u2014';
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD',
      maximumFractionDigits: 2,
    }).format(price);
  } catch {
    return `${currency || ''} ${price.toLocaleString()}`.trim();
  }
}

function formatTimestamp(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  const tz = d.toLocaleTimeString('en-US', { timeZoneName: 'short' }).split(' ').pop();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${tz}`;
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
  const queryClient = useQueryClient();
  const [hint, setHint] = useState('');
  const [retrying, setRetrying] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);
  const [retrySuccess, setRetrySuccess] = useState(false);
  const [showAgentPopover, setShowAgentPopover] = useState(false);
  const hideTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Fetch cross-posts when the listing has cross-post count > 0
  const { data: crossPosts, isLoading: isLoadingCrossPosts } = useQuery({
    queryKey: ['crossPosts', listing.id],
    queryFn: () => getCrossPosts(listing.id),
    enabled: listing.crossPostCount > 0,
    staleTime: 5 * 60_000,
  });

  // Fetch the raw message for MessageBubble rendering
  const { data: rawMessage, isLoading: isLoadingMessage, error: messageError } = useQuery({
    queryKey: ['rawMessage', listing.rawMessageId],
    queryFn: () => getMessage(listing.rawMessageId),
    enabled: !!listing.rawMessageId,
    staleTime: 5 * 60_000,
  });

  const handleRetry = async () => {
    setRetrying(true);
    setRetryError(null);
    setRetrySuccess(false);
    try {
      await retryExtraction(listing.id, hint || undefined);
      setHint('');
      setRetrySuccess(true);
      queryClient.invalidateQueries({ queryKey: ['listings'] });
      queryClient.invalidateQueries({ queryKey: ['listing-stats'] });
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
    // Don't close while retrying or if there's input text
    if (retrying) return;
    hideTimeout.current = setTimeout(() => {
      setShowAgentPopover(false);
      setRetryError(null);
    }, 300);
  };

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
          {canEdit && (
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
                      placeholder="e.g. 'manufacturer is Siemens'"
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
        {/* Source message rendered as MessageBubble */}
        <div className="sm:col-span-2 lg:col-span-3">
          <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-2">
            Source Message
          </p>
          {isLoadingMessage ? (
            <div className="flex items-center gap-2 text-xs text-gray-400 py-2">
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
              Loading source message...
            </div>
          ) : rawMessage ? (
            <div className="rounded-lg border border-gray-200 bg-white p-4" onClick={(e) => e.stopPropagation()}>
              <MessageBubble message={rawMessage} />
            </div>
          ) : (
            <>
              {messageError && (
                <p className="mb-1 text-xs text-red-500">
                  Failed to load source message: {messageError instanceof Error ? messageError.message : 'unknown error'}
                </p>
              )}
              <p className="whitespace-pre-wrap text-sm text-gray-700 bg-white border border-gray-200 rounded-lg px-3 py-2">
                {listing.originalText}
              </p>
            </>
          )}
        </div>

        <DetailField label="AI Parsing Confidence">
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

        <DetailField label="Price">
          <span className="font-semibold">
            {formatPrice(listing.price, listing.priceCurrency)}
          </span>
          {listing.price !== null && listing.priceCurrency && (
            <span className="ml-1.5 text-xs text-gray-400">{listing.priceCurrency}</span>
          )}
          {listing.priceUsd != null && listing.priceCurrency !== 'USD' && (
            <span className="ml-1.5 text-xs text-gray-500">
              &asymp; {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(listing.priceUsd)} USD
            </span>
          )}
        </DetailField>

        {listing.exchangeRateToUsd != null && listing.priceCurrency && listing.priceCurrency !== 'USD' && (
          <DetailField label="Exchange Rate">
            1 {listing.priceCurrency} = {listing.exchangeRateToUsd.toFixed(6)} USD (at listing date)
          </DetailField>
        )}

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

        {listing.soldAt && (
          <DetailField label="Sold Date">
            {formatTimestamp(listing.soldAt)}
          </DetailField>
        )}

        {listing.buyerName && (
          <DetailField label="Buyer">
            {listing.buyerName}
          </DetailField>
        )}

        {listing.expiresAt && (
          <DetailField label="Expires">
            {formatTimestamp(listing.expiresAt)}
          </DetailField>
        )}

        {listing.reviewedByName && (
          <DetailField label="Reviewed By">
            {listing.reviewedByName}
            {listing.reviewedAt ? ` on ${formatTimestamp(listing.reviewedAt)}` : ''}
          </DetailField>
        )}
      </div>

      {/* Cross-posts section */}
      {listing.crossPostCount > 0 && (
        <div className="mt-4 rounded-lg border border-orange-200 bg-orange-50/50 p-4">
          <p className="text-xs font-medium uppercase tracking-wide text-orange-700 mb-2">
            Cross-Posts ({listing.crossPostCount})
          </p>
          {isLoadingCrossPosts ? (
            <div className="flex items-center gap-2 text-xs text-gray-400 py-2">
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
              Loading cross-posts...
            </div>
          ) : crossPosts && crossPosts.length > 0 ? (
            <div className="space-y-2">
              {crossPosts.map((cp) => (
                <div
                  key={cp.id}
                  className="flex items-center justify-between rounded-md bg-white px-3 py-2 text-sm border border-orange-100"
                >
                  <div className="flex items-center gap-3">
                    <span className="font-medium text-gray-800">
                      {cp.groupName ?? 'Unknown group'}
                    </span>
                    <span className="text-gray-500">
                      {cp.senderName ?? 'Unknown sender'}
                    </span>
                    <span className="font-medium text-gray-700">
                      {formatPrice(cp.price, cp.priceCurrency ?? 'USD')}
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <StatusBadge status={cp.status} />
                    <span className="text-xs text-gray-400">
                      {cp.messageTimestamp
                        ? formatTimestamp(cp.messageTimestamp)
                        : formatTimestamp(cp.createdAt)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-gray-500">No cross-posts found.</p>
          )}
        </div>
      )}
    </div>
  );
}
