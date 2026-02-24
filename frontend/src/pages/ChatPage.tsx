import {
  useEffect,
  useRef,
  useState,
  useCallback,
  type KeyboardEvent,
  type FormEvent,
} from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Bot,
  Plus,
  Send,
  ChevronDown,
  ChevronRight,
  Wrench,
} from 'lucide-react';
import { clsx } from 'clsx';
import {
  createSession,
  getSessions,
  getSessionMessages,
  sendMessage,
} from '../api/chat';
import type { ChatMessage, ChatSession, ToolCall } from '../types/chat';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { formatRelativeTime, formatMicroCost } from '../utils/formatters';

// ---------------------------------------------------------------------------
// ToolCallCard — expandable card showing tool name, arguments, and result
// ---------------------------------------------------------------------------

interface ToolCallCardProps {
  toolCall: ToolCall;
}

function ToolCallCard({ toolCall }: ToolCallCardProps) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="mt-2 rounded-md border border-gray-200 bg-gray-50 text-xs">
      <button
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        className="flex w-full items-center gap-2 px-3 py-2 text-left text-gray-600 hover:bg-gray-100 rounded-md transition-colors"
        aria-expanded={expanded}
      >
        <Wrench className="h-3 w-3 shrink-0 text-gray-400" aria-hidden="true" />
        <span className="font-medium text-gray-700">{toolCall.name}</span>
        <span className="ml-auto shrink-0 text-gray-400">
          {expanded ? (
            <ChevronDown className="h-3 w-3" aria-hidden="true" />
          ) : (
            <ChevronRight className="h-3 w-3" aria-hidden="true" />
          )}
        </span>
      </button>

      {expanded && (
        <div className="border-t border-gray-200 px-3 py-2 space-y-2">
          {/* Arguments */}
          {toolCall.arguments &&
            Object.keys(toolCall.arguments).length > 0 && (
              <div>
                <p className="mb-1 font-semibold text-gray-500 uppercase tracking-wide text-[10px]">
                  Arguments
                </p>
                <pre className="overflow-x-auto rounded bg-white border border-gray-100 p-2 text-gray-700 leading-relaxed">
                  {JSON.stringify(toolCall.arguments, null, 2)}
                </pre>
              </div>
            )}

          {/* Result */}
          {toolCall.result !== undefined && (
            <div>
              <p className="mb-1 font-semibold text-gray-500 uppercase tracking-wide text-[10px]">
                Result
              </p>
              <pre className="overflow-x-auto rounded bg-white border border-gray-100 p-2 text-gray-700 leading-relaxed">
                {typeof toolCall.result === 'string'
                  ? toolCall.result
                  : JSON.stringify(toolCall.result, null, 2)}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// ChatBubble — single message bubble (user = right, assistant = left)
// ---------------------------------------------------------------------------

interface ChatBubbleProps {
  message: ChatMessage;
}

function ChatBubble({ message }: ChatBubbleProps) {
  const isUser = message.role === 'user';
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
      {/* Avatar — assistant only, on the left */}
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
        {/* Bubble */}
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

        {/* Tool calls */}
        {message.toolCalls && message.toolCalls.length > 0 && (
          <div className="w-full space-y-1 px-1">
            {message.toolCalls.map((tc) => (
              <ToolCallCard key={tc.id} toolCall={tc} />
            ))}
          </div>
        )}

        {/* Meta row: model badge + token counts + cost */}
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
// TypingIndicator — animated "..." shown while waiting for assistant response
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
// SessionListItem — single entry in the left sidebar
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
// MessageThread — scrollable area that renders all messages
// ---------------------------------------------------------------------------

interface MessageThreadProps {
  messages: ChatMessage[];
  isPending: boolean;
}

function MessageThread({ messages, isPending }: MessageThreadProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom whenever messages change or pending state toggles
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

      {/* Invisible anchor for auto-scroll */}
      <div ref={bottomRef} />
    </div>
  );
}

// ---------------------------------------------------------------------------
// MessageInput — textarea + send button at the bottom
// ---------------------------------------------------------------------------

interface MessageInputProps {
  onSend: (content: string) => void;
  disabled: boolean;
}

function MessageInput({ onSend, disabled }: MessageInputProps) {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = useCallback(
    (e?: FormEvent) => {
      e?.preventDefault();
      const trimmed = value.trim();
      if (!trimmed || disabled) return;
      onSend(trimmed);
      setValue('');
      // Reset textarea height
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
      }
    },
    [value, disabled, onSend]
  );

  // Submit on Enter (without Shift)
  const handleKeyDown = useCallback(
    (e: KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSubmit();
      }
    },
    [handleSubmit]
  );

  // Auto-resize textarea up to ~6 lines
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setValue(e.target.value);
      const el = e.target;
      el.style.height = 'auto';
      el.style.height = `${Math.min(el.scrollHeight, 144)}px`;
    },
    []
  );

  return (
    <form
      onSubmit={handleSubmit}
      className="flex items-end gap-2 border-t border-gray-200 bg-white px-4 py-3"
    >
      <textarea
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        rows={1}
        placeholder="Ask a question... (Enter to send, Shift+Enter for newline)"
        disabled={disabled}
        aria-label="Message input"
        className={clsx(
          'flex-1 resize-none rounded-xl border border-gray-200 bg-gray-50 px-4 py-2.5',
          'text-sm text-gray-900 placeholder-gray-400 outline-none',
          'transition-colors focus:border-blue-400 focus:bg-white focus:ring-2 focus:ring-blue-100',
          'disabled:cursor-not-allowed disabled:opacity-60',
          'min-h-[42px] max-h-36 leading-relaxed'
        )}
      />
      <button
        type="submit"
        disabled={disabled || !value.trim()}
        aria-label="Send message"
        className={clsx(
          'flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition-colors',
          'bg-blue-600 text-white hover:bg-blue-700',
          'disabled:cursor-not-allowed disabled:opacity-40',
          'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1'
        )}
      >
        {disabled ? (
          <LoadingSpinner size="sm" className="border-white border-t-blue-300" />
        ) : (
          <Send className="h-4 w-4" aria-hidden="true" />
        )}
      </button>
    </form>
  );
}

// ---------------------------------------------------------------------------
// ChatPage — top-level two-panel layout
// ---------------------------------------------------------------------------

export function ChatPage() {
  const queryClient = useQueryClient();
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [optimisticMessages, setOptimisticMessages] = useState<ChatMessage[]>([]);

  // Fetch session list
  const {
    data: sessions = [],
    isLoading: isLoadingSessions,
  } = useQuery({
    queryKey: ['chatSessions'],
    queryFn: getSessions,
    staleTime: 30_000,
    select: (data) =>
      [...data].sort(
        (a, b) =>
          new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
      ),
  });

  // Fetch messages for the active session
  const {
    data: serverMessages = [],
    isLoading: isLoadingMessages,
  } = useQuery({
    queryKey: ['chatMessages', activeSessionId],
    queryFn: () => getSessionMessages(activeSessionId!),
    enabled: activeSessionId !== null,
    staleTime: 0,
  });

  // Merge server messages with any optimistic ones that haven't been confirmed yet
  const allMessages: ChatMessage[] = activeSessionId
    ? [
        ...serverMessages,
        ...optimisticMessages.filter(
          (om) => !serverMessages.some((sm) => sm.id === om.id)
        ),
      ]
    : [];

  // Create session mutation
  const createMutation = useMutation({
    mutationFn: createSession,
    onSuccess: (session) => {
      queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
      setActiveSessionId(session.id);
      setOptimisticMessages([]);
    },
  });

  // Send message mutation
  const sendMutation = useMutation({
    mutationFn: ({ sessionId, content }: { sessionId: string; content: string }) =>
      sendMessage(sessionId, { content }),
    onSuccess: (_data, variables) => {
      // Invalidate to get fresh server state (includes both user + assistant messages)
      queryClient.invalidateQueries({
        queryKey: ['chatMessages', variables.sessionId],
      });
      // Also refresh session list so title/updatedAt reflects the new message
      queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
      setOptimisticMessages([]);
    },
    onError: () => {
      // Remove optimistic messages on failure so the user can retry
      setOptimisticMessages([]);
    },
  });

  const handleNewChat = useCallback(() => {
    if (createMutation.isPending) return;
    createMutation.mutate();
  }, [createMutation]);

  const handleSelectSession = useCallback((id: string) => {
    setActiveSessionId(id);
    setOptimisticMessages([]);
  }, []);

  const handleSend = useCallback(
    (content: string) => {
      if (!activeSessionId || sendMutation.isPending) return;

      // Optimistic user message so the UI feels instant
      const optimisticUserMsg: ChatMessage = {
        id: `optimistic-user-${Date.now()}`,
        sessionId: activeSessionId,
        role: 'user',
        content,
        modelUsed: null,
        inputTokens: 0,
        outputTokens: 0,
        costUsd: 0,
        toolCalls: null,
        createdAt: new Date().toISOString(),
      };
      setOptimisticMessages([optimisticUserMsg]);

      sendMutation.mutate({ sessionId: activeSessionId, content });
    },
    [activeSessionId, sendMutation]
  );

  const activeSession = sessions.find((s) => s.id === activeSessionId);
  const isSending = sendMutation.isPending;

  return (
    <div className="flex h-full overflow-hidden">
      {/* ------------------------------------------------------------------ */}
      {/* Left sidebar: session list                                          */}
      {/* ------------------------------------------------------------------ */}
      <aside className="flex w-72 shrink-0 flex-col border-r border-gray-200 bg-white">
        {/* Header + New Chat button */}
        <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h2 className="text-sm font-semibold text-gray-900">Conversations</h2>
          <button
            type="button"
            onClick={handleNewChat}
            disabled={createMutation.isPending}
            aria-label="New chat"
            className={clsx(
              'flex items-center gap-1.5 rounded-lg bg-blue-600 px-2.5 py-1.5 text-xs font-medium text-white',
              'transition-colors hover:bg-blue-700',
              'disabled:cursor-not-allowed disabled:opacity-60',
              'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1'
            )}
          >
            {createMutation.isPending ? (
              <LoadingSpinner size="sm" className="border-white border-t-blue-300" />
            ) : (
              <Plus className="h-3.5 w-3.5" aria-hidden="true" />
            )}
            New Chat
          </button>
        </div>

        {/* Session list */}
        <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
          {isLoadingSessions ? (
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
                onSelect={handleSelectSession}
              />
            ))
          )}
        </div>
      </aside>

      {/* ------------------------------------------------------------------ */}
      {/* Right panel: message thread + input                                 */}
      {/* ------------------------------------------------------------------ */}
      <div className="flex flex-1 flex-col overflow-hidden bg-gray-50">
        {activeSessionId ? (
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
                  {activeSession?.title ?? 'New conversation'}
                </h2>
                {activeSession?.messageCount != null && (
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
              <MessageThread messages={allMessages} isPending={isSending} />
            )}

            {/* Input */}
            <MessageInput onSend={handleSend} disabled={isSending} />
          </>
        ) : (
          /* No session selected */
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
              onClick={handleNewChat}
              disabled={createMutation.isPending}
              className={clsx(
                'flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-medium text-white',
                'transition-colors hover:bg-blue-700',
                'disabled:cursor-not-allowed disabled:opacity-60',
                'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2'
              )}
            >
              {createMutation.isPending ? (
                <LoadingSpinner size="sm" className="border-white border-t-blue-300" />
              ) : (
                <Plus className="h-4 w-4" aria-hidden="true" />
              )}
              Start a new conversation
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
