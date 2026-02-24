import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Users } from 'lucide-react';
import {
  getAllUsers,
  updateUserRole,
  setUserActive,
} from '../../api/admin';
import type { User, UserRole } from '../../types/user';
import { RoleBadge } from '../../components/common/Badge';
import { EmptyState } from '../../components/common/EmptyState';
import { LoadingOverlay } from '../../components/common/LoadingSpinner';

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'user', label: 'User' },
  { value: 'admin', label: 'Admin' },
  { value: 'uber_admin', label: 'Uber Admin' },
];

function UserRow({ user }: { user: User }) {
  const queryClient = useQueryClient();

  const roleMutation = useMutation({
    mutationFn: (role: UserRole) => updateUserRole(user.id, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminUsers'] });
    },
  });

  const activeMutation = useMutation({
    mutationFn: (isActive: boolean) => setUserActive(user.id, isActive),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminUsers'] });
    },
  });

  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50">
      {/* Avatar + email */}
      <td className="px-4 py-3">
        <div className="flex items-center gap-3">
          {user.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt={user.displayName ?? user.email}
              className="h-8 w-8 rounded-full object-cover"
            />
          ) : (
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-200 text-xs font-semibold text-gray-600">
              {(user.displayName ?? user.email).charAt(0).toUpperCase()}
            </div>
          )}
          <div>
            <p className="text-sm font-medium text-gray-900">
              {user.displayName ?? '—'}
            </p>
            <p className="text-xs text-gray-500">{user.email}</p>
          </div>
        </div>
      </td>

      {/* Role */}
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <RoleBadge role={user.role} />
          <select
            value={user.role}
            onChange={(e) => roleMutation.mutate(e.target.value as UserRole)}
            disabled={roleMutation.isPending}
            className="rounded border border-gray-300 bg-white px-2 py-1 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
            aria-label={`Change role for ${user.email}`}
          >
            {ROLE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </td>

      {/* Active toggle */}
      <td className="px-4 py-3 text-center">
        <button
          role="switch"
          aria-checked={user.isActive}
          onClick={() => activeMutation.mutate(!user.isActive)}
          disabled={activeMutation.isPending}
          className={`relative inline-flex h-5 w-9 cursor-pointer rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 ${
            user.isActive ? 'bg-green-500' : 'bg-gray-300'
          }`}
          title={user.isActive ? 'Deactivate user' : 'Activate user'}
        >
          <span
            className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform mt-0.5 ${
              user.isActive ? 'translate-x-4.5' : 'translate-x-0.5'
            }`}
          />
        </button>
      </td>

      {/* Joined */}
      <td className="px-4 py-3 text-xs text-gray-500">
        {new Date(user.createdAt).toLocaleDateString()}
      </td>

      {/* Last login */}
      <td className="px-4 py-3 text-xs text-gray-500">
        {user.lastLoginAt
          ? new Date(user.lastLoginAt).toLocaleDateString()
          : '—'}
      </td>
    </tr>
  );
}

export function UserManagementPage() {
  const [search, setSearch] = useState('');

  const { data: users, isLoading, isError } = useQuery({
    queryKey: ['adminUsers'],
    queryFn: getAllUsers,
  });

  const filtered = users?.filter((u) => {
    const q = search.toLowerCase();
    return (
      u.email.toLowerCase().includes(q) ||
      (u.displayName ?? '').toLowerCase().includes(q)
    );
  });

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-purple-100">
              <Users className="h-5 w-5 text-purple-600" />
            </div>
            <div>
              <h1 className="text-lg font-semibold text-gray-900">
                User Management
              </h1>
              <p className="text-xs text-gray-500">
                Manage roles and account access
              </p>
            </div>
          </div>
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search users..."
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 w-52"
          />
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {isLoading && <LoadingOverlay message="Loading users..." />}

        {isError && (
          <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            Failed to load users. Please refresh the page.
          </div>
        )}

        {!isLoading && !isError && filtered?.length === 0 && (
          <EmptyState
            icon={<Users className="h-8 w-8" />}
            title="No users found"
            description={
              search ? 'Try adjusting your search.' : 'No users registered yet.'
            }
          />
        )}

        {filtered && filtered.length > 0 && (
          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs font-semibold text-gray-600 uppercase tracking-wide">
                <tr>
                  <th className="px-4 py-3">User</th>
                  <th className="px-4 py-3">Role</th>
                  <th className="px-4 py-3 text-center">Active</th>
                  <th className="px-4 py-3">Joined</th>
                  <th className="px-4 py-3">Last Login</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((user) => (
                  <UserRow key={user.id} user={user} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
