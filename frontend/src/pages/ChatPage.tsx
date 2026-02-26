import { useState, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createSession,
  getSessions,
  getSessionMessages,
  sendMessage,
} from '../api/chat';
import type { ChatMessage } from '../types/chat';
import { ChatSidebar } from '../components/chat/ChatSidebar';
import { ChatView } from '../components/chat/ChatView';

export function ChatPage() {
  const queryClient = useQueryClient();
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [optimisticMessages, setOptimisticMessages] = useState<ChatMessage[]>([]);

  // Fetch session list
  const [mutationError, setMutationError] = useState<string | null>(null);

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

  // Merge server messages with optimistic ones
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
      setMutationError(null);
      queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
      setActiveSessionId(session.id);
      setOptimisticMessages([]);
    },
    onError: (err) => {
      setMutationError(err instanceof Error ? err.message : 'Failed to create session');
    },
  });

  // Send message mutation
  const sendMutation = useMutation({
    mutationFn: ({ sessionId, content }: { sessionId: string; content: string }) =>
      sendMessage(sessionId, { message: content }),
    onSuccess: (_data, variables) => {
      setMutationError(null);
      queryClient.invalidateQueries({
        queryKey: ['chatMessages', variables.sessionId],
      });
      queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
      setOptimisticMessages([]);
    },
    onError: (err) => {
      setOptimisticMessages([]);
      setMutationError(err instanceof Error ? err.message : 'Failed to send message');
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

      const optimisticUserMsg: ChatMessage = {
        id: `optimistic-user-${Date.now()}`,
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

  return (
    <div className="flex h-full overflow-hidden">
      <ChatSidebar
        sessions={sessions}
        activeSessionId={activeSessionId}
        isLoading={isLoadingSessions}
        isCreating={createMutation.isPending}
        onSelectSession={handleSelectSession}
        onNewChat={handleNewChat}
      />

      <div className="flex flex-1 flex-col overflow-hidden bg-gray-50">
        {mutationError && (
          <div className="mx-4 mt-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {mutationError}
            <button
              type="button"
              onClick={() => setMutationError(null)}
              className="ml-2 font-medium underline hover:text-red-800"
            >
              Dismiss
            </button>
          </div>
        )}
        <ChatView
          activeSession={activeSession}
          messages={allMessages}
          isLoadingMessages={isLoadingMessages}
          isSending={sendMutation.isPending}
          isCreating={createMutation.isPending}
          onSend={handleSend}
          onNewChat={handleNewChat}
        />
      </div>
    </div>
  );
}
