import { useEffect, useRef, useCallback } from 'react';
import { clsx } from 'clsx';
import type { ReplayMessage } from '../../types/message';
import { MessageBubble } from './MessageBubble';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useInfiniteScroll } from '../../hooks/useInfiniteScroll';

interface MessageThreadProps {
  messages: ReplayMessage[];
  isLoading: boolean;
  isFetchingMore: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
  highlightMessageId?: string | null;
  highlightText?: string;
}

/**
 * Groups messages by date for date separator display.
 */
function groupMessagesByDate(
  messages: ReplayMessage[]
): Array<{ dateLabel: string; messages: ReplayMessage[] }> {
  const groups: Record<string, ReplayMessage[]> = {};
  const order: string[] = [];

  for (const msg of messages) {
    const date = new Date(msg.timestampWa);
    const label = new Intl.DateTimeFormat('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    }).format(date);

    if (!groups[label]) {
      groups[label] = [];
      order.push(label);
    }
    groups[label].push(msg);
  }

  return order.map((label) => ({ dateLabel: label, messages: groups[label] }));
}

export function MessageThread({
  messages,
  isLoading,
  isFetchingMore,
  hasMore,
  onLoadMore,
  highlightMessageId,
  highlightText,
}: MessageThreadProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const highlightRef = useRef<HTMLDivElement>(null);

  // Infinite scroll sentinel at the top (load older messages when scrolled up)
  const { sentinelRef } = useInfiniteScroll({
    onLoadMore,
    hasMore,
    isLoading: isFetchingMore,
    rootMargin: '200px 0px 0px 0px',
  });

  // Scroll to highlighted message when it changes
  useEffect(() => {
    if (highlightMessageId && highlightRef.current) {
      highlightRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [highlightMessageId]);

  const handleScrollToBottom = useCallback(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, []);

  // Auto-scroll to bottom on initial load
  useEffect(() => {
    if (!isLoading && messages.length > 0 && !highlightMessageId) {
      handleScrollToBottom();
    }
  }, [isLoading, messages.length, highlightMessageId, handleScrollToBottom]);

  if (isLoading) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-gray-400">No messages in this group yet.</p>
      </div>
    );
  }

  const groups = groupMessagesByDate(messages);

  // Build a lookup map by whapiMsgId so we can resolve quoted messages
  const messageByWhapiId = new Map<string, ReplayMessage>();
  for (const msg of messages) {
    if (msg.whapiMsgId) {
      messageByWhapiId.set(msg.whapiMsgId, msg);
    }
  }

  return (
    <div
      ref={containerRef}
      className="flex flex-1 flex-col overflow-y-auto px-4 py-4"
      role="log"
      aria-label="Message thread"
      aria-live="polite"
    >
      {/* Sentinel for loading older messages at the top */}
      <div ref={sentinelRef} />

      {isFetchingMore && (
        <div className="flex justify-center py-3">
          <LoadingSpinner size="sm" />
        </div>
      )}

      {/* Message groups by date */}
      {groups.map(({ dateLabel, messages: dayMessages }) => (
        <div key={dateLabel}>
          {/* Date separator */}
          <div className="flex items-center gap-3 my-4">
            <div className="flex-1 h-px bg-gray-200" />
            <span className="shrink-0 rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-500">
              {dateLabel}
            </span>
            <div className="flex-1 h-px bg-gray-200" />
          </div>

          {/* Messages for this day */}
          {dayMessages.map((message) => {
            const isHighlighted = message.id === highlightMessageId;
            return (
              <div
                key={message.id}
                ref={isHighlighted ? highlightRef : undefined}
                className={clsx(
                  'transition-colors rounded-lg',
                  isHighlighted && 'bg-yellow-50'
                )}
              >
                <MessageBubble
                  message={message}
                  quotedMessage={message.replyToMsgId ? messageByWhapiId.get(message.replyToMsgId) ?? null : null}
                  isSearchResult={isHighlighted}
                  highlightText={isHighlighted ? highlightText : undefined}
                />
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
