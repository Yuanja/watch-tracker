import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getAllUsers, getUserChats } from '../../api/admin';
import type { User } from '../../types/user';

interface ChatSessionRow {
  id: string;
  userId: string;
  userEmail: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
}

interface PagedChats {
  content: ChatSessionRow[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export function AllChatsPage() {
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const { data: users = [], isLoading: isLoadingUsers, isError: isUsersError } = useQuery({
    queryKey: ['adminUsers'],
    queryFn: getAllUsers,
    staleTime: 60_000,
  });

  const { data: chatsData, isLoading: isLoadingChats, isError: isChatsError } = useQuery<PagedChats>({
    queryKey: ['adminUserChats', selectedUserId, page],
    queryFn: () => getUserChats(selectedUserId!, page),
    enabled: selectedUserId !== null,
  });

  const chats: ChatSessionRow[] = chatsData?.content ?? (Array.isArray(chatsData) ? (chatsData as unknown as ChatSessionRow[]) : []);
  const totalPages = chatsData?.totalPages ?? 1;

  return (
    <div className="mx-auto max-w-6xl p-6">
      <h1 className="text-2xl font-bold text-gray-900">All Chats</h1>
      <p className="mt-1 text-sm text-gray-500">
        Browse chat sessions for any user.
      </p>

      <div className="mt-6 flex gap-6">
        {/* User list */}
        <div className="w-64 flex-shrink-0">
          <h2 className="mb-2 text-sm font-medium text-gray-700">Users</h2>
          {isLoadingUsers ? (
            <p className="text-sm text-gray-400">Loading users...</p>
          ) : isUsersError ? (
            <p className="text-sm text-red-600">Failed to load users.</p>
          ) : (
            <ul className="space-y-1 overflow-y-auto rounded border border-gray-200 bg-white" style={{ maxHeight: '70vh' }}>
              {users.map((u: User) => (
                <li key={u.id}>
                  <button
                    className={`w-full px-3 py-2 text-left text-sm hover:bg-blue-50 ${
                      selectedUserId === u.id
                        ? 'bg-blue-100 font-medium text-blue-900'
                        : 'text-gray-700'
                    }`}
                    onClick={() => {
                      setSelectedUserId(u.id);
                      setPage(0);
                    }}
                  >
                    <span className="block truncate">{u.email}</span>
                    <span className="block text-xs text-gray-400">{u.role}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Chat sessions */}
        <div className="flex-1">
          {!selectedUserId ? (
            <div className="flex h-64 items-center justify-center rounded border border-gray-200 bg-white">
              <p className="text-sm text-gray-400">Select a user to view their chat sessions.</p>
            </div>
          ) : isChatsError ? (
            <div className="flex h-64 items-center justify-center rounded border border-red-200 bg-red-50">
              <p className="text-sm text-red-600">Failed to load chat sessions.</p>
            </div>
          ) : isLoadingChats ? (
            <p className="text-sm text-gray-400">Loading chat sessions...</p>
          ) : chats.length === 0 ? (
            <div className="flex h-64 items-center justify-center rounded border border-gray-200 bg-white">
              <p className="text-sm text-gray-400">No chat sessions found for this user.</p>
            </div>
          ) : (
            <>
              <table className="w-full rounded border border-gray-200 bg-white text-sm">
                <thead>
                  <tr className="border-b bg-gray-50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    <th className="px-4 py-3">Title</th>
                    <th className="px-4 py-3">Created</th>
                    <th className="px-4 py-3">Updated</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {chats.map((chat) => (
                    <tr key={chat.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium text-gray-900">
                        {chat.title ?? 'Untitled session'}
                      </td>
                      <td className="px-4 py-3 text-gray-500">
                        {new Date(chat.createdAt).toLocaleString()}
                      </td>
                      <td className="px-4 py-3 text-gray-500">
                        {new Date(chat.updatedAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {totalPages > 1 && (
                <div className="mt-4 flex items-center gap-2">
                  <button
                    className="rounded border px-3 py-1 text-sm disabled:opacity-50"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    Previous
                  </button>
                  <span className="text-sm text-gray-600">
                    Page {page + 1} of {totalPages}
                  </span>
                  <button
                    className="rounded border px-3 py-1 text-sm disabled:opacity-50"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
