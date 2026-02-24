import { clsx } from 'clsx';
import { MessageSquare } from 'lucide-react';
import type { WhatsappGroup } from '../../types/message';
import { formatRelativeTime } from '../../utils/formatters';
import { LoadingSpinner } from '../common/LoadingSpinner';

interface GroupListProps {
  groups: WhatsappGroup[];
  selectedGroupId: string | null;
  onSelectGroup: (groupId: string) => void;
  isLoading: boolean;
}

export function GroupList({
  groups,
  selectedGroupId,
  onSelectGroup,
  isLoading,
}: GroupListProps) {
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <LoadingSpinner size="md" />
      </div>
    );
  }

  if (groups.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 px-4 py-8 text-center">
        <MessageSquare className="h-8 w-8 text-gray-300" />
        <p className="text-xs text-gray-400">No groups available</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col divide-y divide-gray-100">
      {groups.map((group) => (
        <button
          key={group.id}
          onClick={() => onSelectGroup(group.id)}
          className={clsx(
            'flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-gray-50',
            selectedGroupId === group.id && 'bg-blue-50 hover:bg-blue-50'
          )}
        >
          {/* Group avatar */}
          <div className="relative shrink-0">
            {group.avatarUrl ? (
              <img
                src={group.avatarUrl}
                alt={group.groupName}
                className="h-10 w-10 rounded-full object-cover"
              />
            ) : (
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100">
                <MessageSquare className="h-5 w-5 text-blue-600" />
              </div>
            )}
            {/* Active indicator */}
            {group.isActive && (
              <span className="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full bg-green-500 ring-2 ring-white" />
            )}
          </div>

          {/* Group info */}
          <div className="min-w-0 flex-1">
            <div className="flex items-center justify-between gap-1">
              <p
                className={clsx(
                  'truncate text-sm font-medium',
                  selectedGroupId === group.id
                    ? 'text-blue-700'
                    : 'text-gray-900'
                )}
              >
                {group.groupName}
              </p>
              {group.lastMessageAt && (
                <span className="shrink-0 text-xs text-gray-400">
                  {formatRelativeTime(group.lastMessageAt)}
                </span>
              )}
            </div>
            {group.description && (
              <p className="truncate text-xs text-gray-500">
                {group.description}
              </p>
            )}
            {group.messageCount != null && (
              <p className="text-xs text-gray-400">
                {group.messageCount.toLocaleString()} messages
              </p>
            )}
          </div>
        </button>
      ))}
    </div>
  );
}
