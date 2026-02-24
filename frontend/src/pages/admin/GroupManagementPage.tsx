import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MessageSquare, Plus, Trash2 } from 'lucide-react';
import {
  getAdminGroups,
  addGroup,
  deleteGroup,
} from '../../api/admin';
import type { WhatsappGroup } from '../../types/message';
import { Badge } from '../../components/common/Badge';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { EmptyState } from '../../components/common/EmptyState';
import { LoadingOverlay } from '../../components/common/LoadingSpinner';

// ---- Add Group Form ----

interface AddGroupFormProps {
  onSuccess: () => void;
  onCancel: () => void;
}

function AddGroupForm({ onSuccess, onCancel }: AddGroupFormProps) {
  const queryClient = useQueryClient();
  const [whapiGroupId, setWhapiGroupId] = useState('');
  const [groupName, setGroupName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () =>
      addGroup({
        whapiGroupId: whapiGroupId.trim(),
        groupName: groupName.trim(),
        description: description.trim() || null,
        isActive: true,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminGroups'] });
      onSuccess();
    },
    onError: () => {
      setError('Failed to add group. Please check the Whapi Group ID and try again.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!whapiGroupId.trim()) {
      setError('Whapi Group ID is required.');
      return;
    }
    if (!groupName.trim()) {
      setError('Group name is required.');
      return;
    }
    mutation.mutate();
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-lg border border-green-200 bg-green-50 p-5 space-y-4"
    >
      <h3 className="text-sm font-semibold text-green-900">Add WhatsApp Group</h3>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-700">
            Whapi Group ID <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={whapiGroupId}
            onChange={(e) => setWhapiGroupId(e.target.value)}
            placeholder="e.g. 120363xxxxxx@g.us"
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>
        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-700">
            Display Name <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={groupName}
            onChange={(e) => setGroupName(e.target.value)}
            placeholder="e.g. Surplus Traders SA"
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>
      </div>

      <div className="space-y-1">
        <label className="block text-xs font-medium text-gray-700">
          Description (optional)
        </label>
        <input
          type="text"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
        />
      </div>

      {error && <p className="text-xs text-red-600">{error}</p>}

      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={mutation.isPending}
          className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-md bg-green-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Adding...' : 'Add Group'}
        </button>
      </div>
    </form>
  );
}

// ---- Group Row ----

function GroupRow({ group }: { group: WhatsappGroup }) {
  const queryClient = useQueryClient();
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  const deleteMutation = useMutation({
    mutationFn: () => deleteGroup(group.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminGroups'] });
      setDeleteTarget(null);
    },
  });

  return (
    <>
      <tr className="border-b border-gray-100 hover:bg-gray-50">
        <td className="px-4 py-3">
          <div className="flex items-center gap-3">
            {group.avatarUrl ? (
              <img
                src={group.avatarUrl}
                alt={group.groupName}
                className="h-8 w-8 rounded-full object-cover"
              />
            ) : (
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-green-100">
                <MessageSquare className="h-4 w-4 text-green-600" />
              </div>
            )}
            <div>
              <p className="text-sm font-medium text-gray-900">
                {group.groupName}
              </p>
              {group.description && (
                <p className="text-xs text-gray-500">{group.description}</p>
              )}
            </div>
          </div>
        </td>
        <td className="px-4 py-3 text-xs text-gray-500 font-mono">
          {group.whapiGroupId}
        </td>
        <td className="px-4 py-3">
          <Badge variant={group.isActive ? 'green' : 'gray'} size="sm">
            {group.isActive ? 'Active' : 'Inactive'}
          </Badge>
        </td>
        <td className="px-4 py-3 text-xs text-gray-500">
          {group.messageCount?.toLocaleString() ?? '—'}
        </td>
        <td className="px-4 py-3 text-xs text-gray-500">
          {group.lastMessageAt
            ? new Date(group.lastMessageAt).toLocaleDateString()
            : '—'}
        </td>
        <td className="px-4 py-3">
          <button
            onClick={() => setDeleteTarget(group.id)}
            title="Remove group"
            className="rounded-md p-1.5 text-gray-400 hover:text-red-600"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </td>
      </tr>

      <ConfirmDialog
        isOpen={deleteTarget === group.id}
        title="Remove WhatsApp group"
        message={`Remove "${group.groupName}" from monitoring? Existing messages will be retained.`}
        confirmLabel="Remove"
        variant="danger"
        onConfirm={() => deleteMutation.mutate()}
        onCancel={() => setDeleteTarget(null)}
        isLoading={deleteMutation.isPending}
      />
    </>
  );
}

// ---- GroupManagementPage ----

export function GroupManagementPage() {
  const [showAddForm, setShowAddForm] = useState(false);

  const { data: groups, isLoading, isError } = useQuery({
    queryKey: ['adminGroups'],
    queryFn: getAdminGroups,
  });

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-green-100">
              <MessageSquare className="h-5 w-5 text-green-600" />
            </div>
            <div>
              <h1 className="text-lg font-semibold text-gray-900">
                WhatsApp Groups
              </h1>
              <p className="text-xs text-gray-500">
                Manage monitored WhatsApp groups
              </p>
            </div>
          </div>
          {!showAddForm && (
            <button
              onClick={() => setShowAddForm(true)}
              className="flex items-center gap-1.5 rounded-md bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-700"
            >
              <Plus className="h-4 w-4" />
              Add Group
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6 space-y-4">
        {showAddForm && (
          <AddGroupForm
            onSuccess={() => setShowAddForm(false)}
            onCancel={() => setShowAddForm(false)}
          />
        )}

        {isLoading && <LoadingOverlay message="Loading groups..." />}

        {isError && (
          <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            Failed to load groups. Please refresh the page.
          </div>
        )}

        {!isLoading && !isError && groups?.length === 0 && !showAddForm && (
          <EmptyState
            icon={<MessageSquare className="h-8 w-8" />}
            title="No groups configured"
            description="Add a WhatsApp group to start monitoring trade messages."
            action={
              <button
                onClick={() => setShowAddForm(true)}
                className="flex items-center gap-1.5 rounded-md bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-700"
              >
                <Plus className="h-4 w-4" />
                Add your first group
              </button>
            }
          />
        )}

        {groups && groups.length > 0 && (
          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs font-semibold text-gray-600 uppercase tracking-wide">
                <tr>
                  <th className="px-4 py-3">Group</th>
                  <th className="px-4 py-3">Whapi ID</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Messages</th>
                  <th className="px-4 py-3">Last Message</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {groups.map((group) => (
                  <GroupRow key={group.id} group={group} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
