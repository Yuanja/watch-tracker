import { clsx } from 'clsx';
import { CornerUpRight, Share2 } from 'lucide-react';
import type { ReplayMessage } from '../../types/message';
import { formatTime } from '../../utils/formatters';
import { getSenderColor, getSenderAvatarBg, getSenderInitials } from '../../utils/colors';
import { Badge } from '../common/Badge';
import { MediaPreview } from './MediaPreview';

interface MessageBubbleProps {
  message: ReplayMessage;
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

export function MessageBubble({
  message,
  isSearchResult = false,
  highlightText,
}: MessageBubbleProps) {
  const senderColor = getSenderColor(message.senderName);
  const avatarBg = getSenderAvatarBg(message.senderName);
  const initials = getSenderInitials(message.senderName);
  const listing = message.extractedListing;

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
            {formatTime(message.timestampWa)}
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

        {/* Extracted listing badge */}
        {listing && (
          <div className="mt-2 flex flex-wrap items-center gap-2">
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
            <span className="text-xs text-gray-500 line-clamp-1">
              {listing.itemDescription}
            </span>
            {listing.confidenceScore >= 0.8 && (
              <Badge variant="teal" size="sm">
                Auto-extracted
              </Badge>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
