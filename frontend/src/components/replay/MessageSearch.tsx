import { useState, useCallback } from 'react';
import { Search, X } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { useDebounce } from '../../hooks/useDebounce';
import { searchMessages } from '../../api/messages';
import type { ReplayMessage } from '../../types/message';

type SearchMode = 'text' | 'semantic';

interface MessageSearchProps {
  groupId: string | null;
  onResultSelect: (message: ReplayMessage) => void;
  onClear: () => void;
}

export function MessageSearch({
  groupId,
  onResultSelect,
  onClear,
}: MessageSearchProps) {
  const [query, setQuery] = useState('');
  const [mode, setMode] = useState<SearchMode>('text');
  const debouncedQuery = useDebounce(query, 400);

  const { data, isFetching } = useQuery({
    queryKey: ['messageSearch', groupId, debouncedQuery, mode],
    queryFn: () =>
      searchMessages({
        groupId: groupId ?? undefined,
        textQuery: mode === 'text' ? debouncedQuery : undefined,
        semanticQuery: mode === 'semantic' ? debouncedQuery : undefined,
        size: 20,
      }),
    enabled: debouncedQuery.trim().length > 2,
    staleTime: 30_000,
  });

  const handleClear = useCallback(() => {
    setQuery('');
    onClear();
  }, [onClear]);

  const results = data?.content ?? [];
  const hasQuery = query.trim().length > 0;
  const hasResults = results.length > 0;

  return (
    <div className="relative">
      {/* Search input row */}
      <div className="flex items-center gap-2 border-b border-gray-200 bg-white px-3 py-2">
        <Search
          className={clsx(
            'h-4 w-4 shrink-0',
            isFetching ? 'text-blue-500 animate-pulse' : 'text-gray-400'
          )}
        />
        <input
          type="search"
          className="flex-1 bg-transparent text-sm outline-none placeholder:text-gray-400"
          placeholder={
            mode === 'semantic'
              ? 'Describe what you are looking for...'
              : 'Search messages...'
          }
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Search messages"
        />
        {hasQuery && (
          <button
            onClick={handleClear}
            className="shrink-0 text-gray-400 hover:text-gray-600"
            aria-label="Clear search"
          >
            <X className="h-4 w-4" />
          </button>
        )}
        <select
          value={mode}
          onChange={(e) => setMode(e.target.value as SearchMode)}
          className="shrink-0 rounded border border-gray-200 bg-gray-50 px-2 py-1 text-xs text-gray-600 focus:outline-none focus:ring-1 focus:ring-blue-400"
          aria-label="Search mode"
        >
          <option value="text">Text Match</option>
          <option value="semantic">Semantic</option>
        </select>
      </div>

      {/* Results dropdown */}
      {hasQuery && debouncedQuery.trim().length > 2 && (
        <div className="absolute left-0 right-0 top-full z-20 max-h-72 overflow-y-auto border-b border-gray-200 bg-white shadow-lg">
          {isFetching && !hasResults && (
            <div className="px-4 py-3 text-xs text-gray-400">Searching...</div>
          )}
          {!isFetching && !hasResults && (
            <div className="px-4 py-3 text-xs text-gray-400">
              No results found for "{debouncedQuery}"
            </div>
          )}
          {hasResults &&
            results.map((msg) => (
              <button
                key={msg.id}
                onClick={() => onResultSelect(msg)}
                className="flex w-full flex-col gap-0.5 px-4 py-2.5 text-left transition-colors hover:bg-blue-50"
              >
                <span className="text-xs font-semibold text-gray-700">
                  {msg.senderName ?? msg.senderPhone ?? 'Unknown'}
                </span>
                <span className="line-clamp-2 text-xs text-gray-600">
                  {msg.messageBody}
                </span>
              </button>
            ))}
        </div>
      )}
    </div>
  );
}
