import { Bot, Plus } from 'lucide-react';
import { clsx } from 'clsx';
import type { ChatMessage, ChatSession } from '../../types/chat';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ChatThread } from './ChatThread';
import { ChatInput } from './ChatInput';

interface ChatViewProps {
  activeSession: ChatSession | undefined;
  messages: ChatMessage[];
  isLoadingMessages: boolean;
  isSending: boolean;
  isCreating: boolean;
  onSend: (content: string) => void;
  onNewChat: () => void;
}

export function ChatView({
  activeSession,
  messages,
  isLoadingMessages,
  isSending,
  isCreating,
  onSend,
  onNewChat,
}: ChatViewProps) {
  if (!activeSession) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-100">
          <Bot className="h-8 w-8 text-blue-600" aria-hidden="true" />
        </div>
        <div>
          <h3 className="text-base font-semibold text-gray-800">
            Ask AI anything
          </h3>
          <p className="mt-1 max-w-sm text-sm text-gray-500">
            Search listings, analyze trends, and query the trade intelligence
            database using natural language.
          </p>
        </div>
        <button
          type="button"
          onClick={onNewChat}
          disabled={isCreating}
          className={clsx(
            'flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-medium text-white',
            'transition-colors hover:bg-blue-700',
            'disabled:cursor-not-allowed disabled:opacity-60',
            'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2'
          )}
        >
          {isCreating ? (
            <LoadingSpinner size="sm" className="border-white border-t-blue-300" />
          ) : (
            <Plus className="h-4 w-4" aria-hidden="true" />
          )}
          Start a new conversation
        </button>
      </div>
    );
  }

  return (
    <>
      {/* Thread header */}
      <div className="flex items-center gap-3 border-b border-gray-200 bg-white px-4 py-3">
        <div
          className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100"
          aria-hidden="true"
        >
          <Bot className="h-4 w-4 text-blue-600" />
        </div>
        <div className="min-w-0">
          <h2 className="truncate text-sm font-semibold text-gray-900">
            {activeSession.title ?? 'New conversation'}
          </h2>
          {activeSession.messageCount != null && (
            <p className="text-xs text-gray-400">
              {activeSession.messageCount}{' '}
              {activeSession.messageCount === 1 ? 'message' : 'messages'}
            </p>
          )}
        </div>
      </div>

      {/* Messages */}
      {isLoadingMessages ? (
        <div className="flex flex-1 items-center justify-center">
          <LoadingSpinner size="lg" />
        </div>
      ) : (
        <ChatThread messages={messages} isPending={isSending} />
      )}

      {/* Input */}
      <ChatInput onSend={onSend} disabled={isSending} />
    </>
  );
}
