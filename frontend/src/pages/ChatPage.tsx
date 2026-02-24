import { Bot } from 'lucide-react';

/**
 * ChatPage — AI query interface.
 * Placeholder implementation; full ChatView component to be implemented in Phase 4.
 */
export function ChatPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 p-8 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-100">
        <Bot className="h-8 w-8 text-blue-600" />
      </div>
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Ask AI</h1>
        <p className="mt-1 text-sm text-gray-500">
          Use natural language to query the trade intelligence database.
          <br />
          Full implementation coming in Phase 4.
        </p>
      </div>
    </div>
  );
}
