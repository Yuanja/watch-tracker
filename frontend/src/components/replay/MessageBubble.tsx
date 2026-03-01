import { useState } from 'react';
import { clsx } from 'clsx';
import { Code2, CornerUpRight, Share2, Sparkles, Loader2 } from 'lucide-react';
import type { ReplayMessage, ExtractedListingRef } from '../../types/message';
import { formatDateTime } from '../../utils/formatters';
import { getSenderColor, getSenderAvatarBg, getSenderInitials } from '../../utils/colors';
import { Badge } from '../common/Badge';
import { MediaPreview } from './MediaPreview';
import { useAuth } from '../../contexts/AuthContext';
import { extractMessage } from '../../api/messages';

interface MessageBubbleProps {
  message: ReplayMessage;
  quotedMessage?: ReplayMessage | null;
  isSearchResult?: boolean;
  highlightText?: string;
}

/**
 * Highlights occurrences of `highlight` within `text` by wrapping them in
 * a <mark> element. Returns an array of React nodes.
 */
function highlightMatches(text: string, highlight: string): React.ReactNode {
  if (!highlight.trim()) return text;
  const regex = new RegExp(
    `(${highlight.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`,
    'gi'
  );
  const parts = text.split(regex);
  return parts.map((part, i) =>
    regex.test(part) ? (
      <mark
        key={i}
        className="bg-yellow-200 text-yellow-900 rounded-sm px-0.5"
      >
        {part}
      </mark>
    ) : (
      part
    )
  );
}

function formatPrice(price: number | null | undefined): string {
  if (price == null) return '';
  return '$' + price.toLocaleString('en-US', { maximumFractionDigits: 0 });
}

function ListingCard({ listing }: { listing: ExtractedListingRef }) {
  return (
    <div className="mt-2 rounded-lg border border-gray-200 bg-gray-50 p-2.5">
      {/* Header: intent + status + confidence */}
      <div className="flex flex-wrap items-center gap-1.5 mb-2">
        <Badge
          variant={
            listing.intent === 'sell'
              ? 'green'
              : listing.intent === 'want'
              ? 'blue'
              : 'gray'
          }
          size="sm"
        >
          {listing.intent === 'sell'
            ? 'SELL'
            : listing.intent === 'want'
            ? 'WANT'
            : 'UNKNOWN'}
        </Badge>
        {listing.status && (
          <Badge
            variant={
              listing.status === 'active'
                ? 'teal'
                : listing.status === 'sold'
                ? 'red'
                : listing.status === 'pending_review'
                ? 'yellow'
                : 'gray'
            }
            size="sm"
          >
            {listing.status === 'pending_review' ? 'Review' : listing.status === 'sold' ? 'SOLD' : listing.status}
          </Badge>
        )}
        <span className="text-xs text-gray-400">
          {Math.round(listing.confidenceScore * 100)}% confidence
        </span>
      </div>

      {/* 2-column detail grid */}
      <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs">
        {listing.manufacturerName && (
          <>
            <span className="text-gray-400">Brand</span>
            <span className="text-gray-700 font-medium">{listing.manufacturerName}</span>
          </>
        )}
        {listing.partNumber && (
          <>
            <span className="text-gray-400">Ref#</span>
            <span className="text-gray-700 font-mono">{listing.partNumber}</span>
          </>
        )}
        {listing.price != null && (
          <>
            <span className="text-gray-400">Price</span>
            <span className="text-gray-700 font-medium">{formatPrice(listing.price)}</span>
          </>
        )}
        {listing.conditionName && (
          <>
            <span className="text-gray-400">Condition</span>
            <span className="text-gray-700">{listing.conditionName}</span>
          </>
        )}
      </div>

      {/* Description */}
      {listing.itemDescription && (
        <p className="mt-1.5 text-xs text-gray-500 line-clamp-2">
          {listing.itemDescription}
        </p>
      )}

      {/* Sold details */}
      {listing.status === 'sold' && (
        <div className="mt-1.5 text-xs text-red-600">
          {listing.soldAt && (
            <span>Sold {new Date(listing.soldAt).toLocaleDateString()}</span>
          )}
          {listing.buyerName && (
            <span>{listing.soldAt ? ' · ' : ''}Buyer: {listing.buyerName}</span>
          )}
        </div>
      )}
    </div>
  );
}

export function MessageBubble({
  message,
  quotedMessage,
  isSearchResult = false,
  highlightText,
}: MessageBubbleProps) {
  const [showRawJson, setShowRawJson] = useState(false);
  const [extracting, setExtracting] = useState(false);
  const [localListing, setLocalListing] = useState<ExtractedListingRef | null>(null);
  const [extractError, setExtractError] = useState<string | null>(null);
  const { user } = useAuth();

  const senderColor = getSenderColor(message.senderName);
  const avatarBg = getSenderAvatarBg(message.senderName);
  const initials = getSenderInitials(message.senderName);
  const listing = localListing ?? message.extractedListing;

  const isAdmin = user?.role === 'admin' || user?.role === 'uber_admin';
  const canExtract = isAdmin && !listing && message.messageBody && !extracting;

  const handleExtract = async () => {
    setExtracting(true);
    setExtractError(null);
    try {
      const result = await extractMessage(message.id);
      if (result) {
        setLocalListing(result);
      } else {
        setExtractError('No trade data found');
      }
    } catch {
      setExtractError('Extraction failed');
    } finally {
      setExtracting(false);
    }
  };

  return (
    <div
      className={clsx(
        'flex items-start gap-2 mb-3 max-w-[80%]',
        isSearchResult && 'animate-pulse-once'
      )}
    >
      {/* Sender avatar */}
      <div
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold text-gray-700 mt-0.5"
        style={{ backgroundColor: avatarBg }}
        aria-hidden="true"
      >
        {message.senderAvatar ? (
          <img
            src={message.senderAvatar}
            alt={message.senderName ?? ''}
            className="h-8 w-8 rounded-full object-cover"
          />
        ) : (
          initials
        )}
      </div>

      {/* Bubble */}
      <div
        className={clsx(
          'rounded-lg rounded-tl-none px-3 py-2 shadow-sm bg-white border border-gray-100',
          isSearchResult && 'ring-2 ring-blue-400'
        )}
      >
        {/* Header: sender name + time */}
        <div className="flex items-center gap-2 mb-1">
          <span
            className="text-sm font-semibold leading-none"
            style={{ color: senderColor }}
          >
            {message.senderName ?? message.senderPhone ?? 'Unknown'}
          </span>
          <span className="text-xs text-gray-400 leading-none">
            {formatDateTime(message.timestampWa)}
          </span>
          {message.isForwarded && (
            <span
              className="flex items-center gap-0.5 text-xs text-gray-400"
              title="Forwarded"
            >
              <Share2 className="h-3 w-3" />
              Forwarded
            </span>
          )}
          {message.replyToMsgId && (
            <span className="flex items-center gap-0.5 text-xs text-gray-400">
              <CornerUpRight className="h-3 w-3" />
              Reply
            </span>
          )}
        </div>

        {/* Quoted message */}
        {quotedMessage && (
          <div className="mb-2 rounded border-l-4 border-blue-400 bg-gray-50 px-3 py-2">
            <p className="text-xs font-semibold text-blue-600 mb-0.5">
              {quotedMessage.senderName ?? quotedMessage.senderPhone ?? 'Unknown'}
            </p>
            {quotedMessage.mediaUrl && quotedMessage.messageType === 'image' && (
              <img
                src={quotedMessage.mediaUrl}
                alt="Quoted media"
                className="mb-1 h-12 w-12 rounded object-cover"
              />
            )}
            <p className="text-xs text-gray-600 line-clamp-2">
              {quotedMessage.messageBody
                ? quotedMessage.messageBody
                : quotedMessage.messageType !== 'text'
                ? `[${quotedMessage.messageType}]`
                : '[message]'}
            </p>
          </div>
        )}
        {message.replyToMsgId && !quotedMessage && (
          <div className="mb-2 rounded border-l-4 border-gray-300 bg-gray-50 px-3 py-1">
            <p className="text-xs text-gray-400 italic">Quoted message not loaded</p>
          </div>
        )}

        {/* Media attachment */}
        {message.mediaUrl && <MediaPreview message={message} />}

        {/* Message body */}
        {message.messageBody && (
          <p className="text-sm text-gray-800 whitespace-pre-wrap leading-relaxed">
            {highlightText
              ? highlightMatches(message.messageBody, highlightText)
              : message.messageBody}
          </p>
        )}

        {/* Extracted listing card */}
        {listing && <ListingCard listing={listing} />}

        {/* Extract button for admins */}
        {canExtract && (
          <button
            type="button"
            onClick={handleExtract}
            className="mt-2 flex items-center gap-1 rounded-md border border-indigo-200 bg-indigo-50 px-2 py-1 text-xs text-indigo-600 hover:bg-indigo-100 transition-colors"
            title="Extract trade data from this message"
          >
            <Sparkles className="h-3 w-3" />
            Extract
          </button>
        )}
        {extracting && (
          <div className="mt-2 flex items-center gap-1 text-xs text-gray-400">
            <Loader2 className="h-3 w-3 animate-spin" />
            Extracting...
          </div>
        )}
        {extractError && (
          <p className="mt-2 text-xs text-amber-600">{extractError}</p>
        )}

        {/* Raw JSON toggle */}
        {message.rawJson && (
          <div className="mt-1">
            <button
              type="button"
              onClick={() => setShowRawJson(!showRawJson)}
              className="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition-colors"
              title="Toggle raw webhook JSON"
            >
              <Code2 className="h-3 w-3" />
              {showRawJson ? 'Hide' : 'Show'} raw JSON
            </button>
            {showRawJson && (
              <pre className="mt-1 max-h-64 overflow-auto rounded bg-gray-900 p-2 text-xs text-green-400 font-mono">
                {(() => {
                  try { return JSON.stringify(JSON.parse(message.rawJson!), null, 2); }
                  catch { return message.rawJson; }
                })()}
              </pre>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
