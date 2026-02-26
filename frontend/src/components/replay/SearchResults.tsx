import { clsx } from 'clsx';
import { Search, ArrowRight } from 'lucide-react';
import type { ReplayMessage } from '../../types/message';
import { formatTime } from '../../utils/formatters';
import { getSenderColor } from '../../utils/colors';

interface SearchResultsProps {
  results: ReplayMessage[];
  highlightText: string;
  isLoading: boolean;
  onSelect: (message: ReplayMessage) => void;
}

/**
 * Displays search results as a scrollable list with highlighted matched text.
 * Each result shows sender, timestamp, and a preview of the message body
 * with the search term highlighted.
 */
export function SearchResults({
  results,
  highlightText,
  isLoading,
  onSelect,
}: SearchResultsProps) {
  if (isLoading) {
    return (
      <div className="flex items-center gap-2 px-4 py-6 text-sm text-gray-400">
        <Search className="h-4 w-4 animate-pulse" />
        Searching...
      </div>
    );
  }

  if (results.length === 0) {
    return (
      <div className="px-4 py-6 text-center text-sm text-gray-400">
        No results found
      </div>
    );
  }

  return (
    <div className="divide-y divide-gray-100">
      <div className="px-4 py-2 text-xs font-medium text-gray-500 bg-gray-50">
        {results.length} result{results.length !== 1 ? 's' : ''} found
      </div>
      {results.map((message) => (
        <SearchResultItem
          key={message.id}
          message={message}
          highlightText={highlightText}
          onSelect={onSelect}
        />
      ))}
    </div>
  );
}

interface SearchResultItemProps {
  message: ReplayMessage;
  highlightText: string;
  onSelect: (message: ReplayMessage) => void;
}

function SearchResultItem({
  message,
  highlightText,
  onSelect,
}: SearchResultItemProps) {
  const senderColor = getSenderColor(message.senderName);

  return (
    <button
      onClick={() => onSelect(message)}
      className={clsx(
        'flex w-full items-start gap-3 px-4 py-3 text-left',
        'transition-colors hover:bg-blue-50 focus:bg-blue-50 focus:outline-none'
      )}
    >
      <div className="flex-1 min-w-0">
        {/* Sender + time row */}
        <div className="flex items-center gap-2 mb-1">
          <span
            className="text-xs font-semibold truncate"
            style={{ color: senderColor }}
          >
            {message.senderName ?? message.senderPhone ?? 'Unknown'}
          </span>
          <span className="text-xs text-gray-400 shrink-0">
            {formatTime(message.timestampWa)}
          </span>
          {message.groupName && (
            <span className="text-xs text-gray-400 truncate">
              in {message.groupName}
            </span>
          )}
        </div>

        {/* Message preview with highlighting */}
        {message.messageBody && (
          <p className="text-xs text-gray-600 line-clamp-2">
            {highlightText
              ? highlightSearchText(message.messageBody, highlightText)
              : message.messageBody}
          </p>
        )}
      </div>

      <ArrowRight className="h-4 w-4 shrink-0 text-gray-300 mt-1" />
    </button>
  );
}

/**
 * Highlights occurrences of the search text within the message body.
 */
function highlightSearchText(
  text: string,
  search: string
): React.ReactNode {
  if (!search.trim()) return text;

  const escaped = search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const regex = new RegExp(`(${escaped})`, 'gi');
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
