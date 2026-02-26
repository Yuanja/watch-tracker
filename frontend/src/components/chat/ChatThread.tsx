import { useEffect, useRef } from 'react';
import { Bot } from 'lucide-react';
import { clsx } from 'clsx';
import type { ChatMessage } from '../../types/chat';
import { parseToolCalls } from '../../types/chat';
import { EmptyState } from '../common/EmptyState';
import { formatMicroCost } from '../../utils/formatters';
import { ToolResultCard } from './ToolResultCard';

// ---------------------------------------------------------------------------
// ChatBubble — single message bubble (user = right, assistant = left)
// ---------------------------------------------------------------------------

function ChatBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === 'user';
  const toolCalls = parseToolCalls(message.toolCalls);
  const hasTokenInfo =
    (message.inputTokens > 0 || message.outputTokens > 0) &&
    message.role === 'assistant';

  return (
    <div
      className={clsx(
        'flex w-full gap-2 mb-4',
        isUser ? 'justify-end' : 'justify-start'
      )}
    >
      {!isUser && (
        <div
          className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-blue-100 mt-0.5"
          aria-hidden="true"
        >
          <Bot className="h-4 w-4 text-blue-600" />
        </div>
      )}

      <div
        className={clsx(
          'max-w-[75%] flex flex-col gap-1',
          isUser ? 'items-end' : 'items-start'
        )}
      >
        <div
          className={clsx(
            'rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap break-words shadow-sm',
            isUser
              ? 'rounded-br-sm bg-blue-600 text-white'
              : 'rounded-bl-sm bg-white text-gray-800 border border-gray-100'
          )}
        >
          {message.content}
        </div>

        {toolCalls.length > 0 && (
          <div className="w-full space-y-1 px-1">
            {toolCalls.map((tc, idx) => (
              <ToolResultCard key={`${message.id}-tool-${idx}`} toolCall={tc} />
            ))}
          </div>
        )}

        <div
          className={clsx(
            'flex items-center gap-2 px-1',
            isUser ? 'flex-row-reverse' : 'flex-row'
          )}
        >
          {message.modelUsed && (
            <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-medium text-gray-500 leading-none">
              {message.modelUsed}
            </span>
          )}
          {hasTokenInfo && (
            <span className="text-[10px] text-gray-400">
              {message.inputTokens.toLocaleString()} in /{' '}
              {message.outputTokens.toLocaleString()} out
            </span>
          )}
          {message.costUsd > 0 && (
            <span className="text-[10px] text-gray-400">
              {formatMicroCost(message.costUsd)}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// TypingIndicator
// ---------------------------------------------------------------------------

function TypingIndicator() {
  return (
    <div className="flex items-start gap-2 mb-4">
      <div
        className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-blue-100 mt-0.5"
        aria-hidden="true"
      >
        <Bot className="h-4 w-4 text-blue-600" />
      </div>
      <div className="rounded-2xl rounded-bl-sm bg-white border border-gray-100 px-4 py-3 shadow-sm">
        <span className="flex items-center gap-1" aria-label="Assistant is typing">
          <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:0ms]" />
          <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:150ms]" />
          <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:300ms]" />
        </span>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// ChatThread
// ---------------------------------------------------------------------------

interface ChatThreadProps {
  messages: ChatMessage[];
  isPending: boolean;
}

export function ChatThread({ messages, isPending }: ChatThreadProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length, isPending]);

  if (messages.length === 0 && !isPending) {
    return (
      <div className="flex flex-1 items-center justify-center p-8">
        <EmptyState
          icon={<Bot className="h-8 w-8 text-gray-400" />}
          title="Start the conversation"
          description="Ask anything about the trade intelligence database. The AI can search listings, analyze trends, and answer questions."
        />
      </div>
    );
  }

  return (
    <div
      className="flex flex-1 flex-col overflow-y-auto px-4 py-4"
      role="log"
      aria-label="Chat messages"
      aria-live="polite"
    >
      {messages.map((msg) => (
        <ChatBubble key={msg.id} message={msg} />
      ))}

      {isPending && <TypingIndicator />}

      <div ref={bottomRef} />
    </div>
  );
}
