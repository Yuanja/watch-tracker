import { Bot, Plus } from 'lucide-react';
import { clsx } from 'clsx';
import type { ChatSession } from '../../types/chat';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { formatRelativeTime } from '../../utils/formatters';

// ---------------------------------------------------------------------------
// SessionListItem
// ---------------------------------------------------------------------------

interface SessionListItemProps {
  session: ChatSession;
  isActive: boolean;
  onSelect: (id: string) => void;
}

function SessionListItem({ session, isActive, onSelect }: SessionListItemProps) {
  const title = session.title ?? 'New conversation';
  return (
    <button
      type="button"
      onClick={() => onSelect(session.id)}
      className={clsx(
        'flex w-full flex-col gap-0.5 px-3 py-3 text-left transition-colors hover:bg-gray-50',
        isActive && 'bg-blue-50 hover:bg-blue-50 border-r-2 border-blue-500'
      )}
    >
      <p
        className={clsx(
          'truncate text-sm font-medium leading-tight',
          isActive ? 'text-blue-700' : 'text-gray-800'
        )}
      >
        {title}
      </p>
      <p className="text-xs text-gray-400">
        {formatRelativeTime(session.updatedAt)}
      </p>
    </button>
  );
}

// ---------------------------------------------------------------------------
// ChatSidebar
// ---------------------------------------------------------------------------

interface ChatSidebarProps {
  sessions: ChatSession[];
  activeSessionId: string | null;
  isLoading: boolean;
  isCreating: boolean;
  onSelectSession: (id: string) => void;
  onNewChat: () => void;
}

export function ChatSidebar({
  sessions,
  activeSessionId,
  isLoading,
  isCreating,
  onSelectSession,
  onNewChat,
}: ChatSidebarProps) {
  return (
    <aside className="flex w-72 shrink-0 flex-col border-r border-gray-200 bg-white">
      <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <h2 className="text-sm font-semibold text-gray-900">Conversations</h2>
        <button
          type="button"
          onClick={onNewChat}
          disabled={isCreating}
          aria-label="New chat"
          className={clsx(
            'flex items-center gap-1.5 rounded-lg bg-blue-600 px-2.5 py-1.5 text-xs font-medium text-white',
            'transition-colors hover:bg-blue-700',
            'disabled:cursor-not-allowed disabled:opacity-60',
            'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1'
          )}
        >
          {isCreating ? (
            <LoadingSpinner size="sm" className="border-white border-t-blue-300" />
          ) : (
            <Plus className="h-3.5 w-3.5" aria-hidden="true" />
          )}
          New Chat
        </button>
      </div>

      <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
        {isLoading ? (
          <div className="flex items-center justify-center py-8">
            <LoadingSpinner size="md" />
          </div>
        ) : sessions.length === 0 ? (
          <div className="flex flex-col items-center gap-2 px-4 py-8 text-center">
            <Bot className="h-8 w-8 text-gray-300" aria-hidden="true" />
            <p className="text-xs text-gray-400">No conversations yet</p>
            <p className="text-xs text-gray-400">
              Click &ldquo;New Chat&rdquo; to get started
            </p>
          </div>
        ) : (
          sessions.map((session) => (
            <SessionListItem
              key={session.id}
              session={session}
              isActive={session.id === activeSessionId}
              onSelect={onSelectSession}
            />
          ))
        )}
      </div>
    </aside>
  );
}
