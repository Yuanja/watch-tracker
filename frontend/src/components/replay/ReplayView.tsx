import { useState, useCallback } from 'react';
import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
import { MessageSquare } from 'lucide-react';
import { getGroups, getGroupMessages } from '../../api/messages';
import type { ReplayMessage } from '../../types/message';
import { GroupList } from './GroupList';
import { MessageThread } from './MessageThread';
import { MessageSearch } from './MessageSearch';

const PAGE_SIZE = 40;

/**
 * ReplayView is the main container for the WhatsApp-style message replay UI.
 * Layout: group list on the left, message thread on the right.
 */
export function ReplayView() {
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(null);
  const [highlightMessageId, setHighlightMessageId] = useState<string | null>(null);
  const [searchHighlightText, setSearchHighlightText] = useState<string>('');

  // Fetch all groups
  const {
    data: groups = [],
    isLoading: isLoadingGroups,
  } = useQuery({
    queryKey: ['groups'],
    queryFn: getGroups,
    staleTime: 5 * 60_000,
  });

  // Infinite query for messages in the selected group
  const {
    data: messagesData,
    isLoading: isLoadingMessages,
    isFetchingNextPage,
    fetchNextPage,
    hasNextPage,
  } = useInfiniteQuery({
    queryKey: ['groupMessages', selectedGroupId],
    queryFn: ({ pageParam = 0 }) =>
      getGroupMessages(selectedGroupId!, { page: pageParam as number, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.last ? undefined : lastPage.number + 1,
    enabled: selectedGroupId !== null,
    staleTime: 60_000,
  });

  // Flatten all pages of messages into a single array, oldest first
  const allMessages: ReplayMessage[] =
    messagesData?.pages.flatMap((page) => page.content).reverse() ?? [];

  const handleGroupSelect = useCallback((groupId: string) => {
    setSelectedGroupId(groupId);
    setHighlightMessageId(null);
    setSearchHighlightText('');
  }, []);

  const handleSearchResultSelect = useCallback((message: ReplayMessage) => {
    // If the message belongs to a different group, switch to that group first
    if (message.groupId !== selectedGroupId) {
      setSelectedGroupId(message.groupId);
    }
    setHighlightMessageId(message.id);
    setSearchHighlightText(message.messageBody ?? '');
  }, [selectedGroupId]);

  const handleSearchClear = useCallback(() => {
    setHighlightMessageId(null);
    setSearchHighlightText('');
  }, []);

  const selectedGroup = groups.find((g) => g.id === selectedGroupId);

  return (
    <div className="flex h-full overflow-hidden">
      {/* Left panel: group list */}
      <div className="flex w-64 shrink-0 flex-col border-r border-gray-200 bg-white lg:w-72">
        <div className="border-b border-gray-200 px-4 py-3">
          <h2 className="text-sm font-semibold text-gray-900">Groups</h2>
        </div>
        <div className="flex-1 overflow-y-auto">
          <GroupList
            groups={groups}
            selectedGroupId={selectedGroupId}
            onSelectGroup={handleGroupSelect}
            isLoading={isLoadingGroups}
          />
        </div>
      </div>

      {/* Right panel: message thread */}
      <div className="flex flex-1 flex-col overflow-hidden bg-gray-50">
        {selectedGroupId ? (
          <>
            {/* Thread header */}
            <div className="flex items-center gap-3 border-b border-gray-200 bg-white px-4 py-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100">
                <MessageSquare className="h-4 w-4 text-blue-600" />
              </div>
              <div>
                <h2 className="text-sm font-semibold text-gray-900">
                  {selectedGroup?.groupName ?? 'Group'}
                </h2>
                {selectedGroup?.description && (
                  <p className="text-xs text-gray-500">
                    {selectedGroup.description}
                  </p>
                )}
              </div>
            </div>

            {/* Search bar */}
            <MessageSearch
              groupId={selectedGroupId}
              onResultSelect={handleSearchResultSelect}
              onClear={handleSearchClear}
            />

            {/* Messages */}
            <MessageThread
              messages={allMessages}
              isLoading={isLoadingMessages}
              isFetchingMore={isFetchingNextPage}
              hasMore={hasNextPage ?? false}
              onLoadMore={fetchNextPage}
              highlightMessageId={highlightMessageId}
              highlightText={searchHighlightText}
            />
          </>
        ) : (
          /* Empty state — no group selected */
          <div className="flex flex-1 flex-col items-center justify-center gap-3 text-center p-8">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-gray-100">
              <MessageSquare className="h-8 w-8 text-gray-400" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-gray-700">
                Select a group
              </h3>
              <p className="mt-1 text-sm text-gray-400">
                Choose a WhatsApp group from the list to view its messages.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
