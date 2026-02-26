import { useState } from 'react';
import { Wrench, ChevronDown, ChevronRight } from 'lucide-react';
import type { ToolCall } from '../../types/chat';

interface ToolResultCardProps {
  toolCall: ToolCall;
}

export function ToolResultCard({ toolCall }: ToolResultCardProps) {
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
