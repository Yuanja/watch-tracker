import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, Plus, Pencil, Trash2, Mail, CheckCircle2, XCircle } from 'lucide-react';
import {
  getNotificationRules,
  createNotificationRule,
  updateNotificationRule,
  deleteNotificationRule,
  type NotificationRule,
  type CreateNotificationRuleRequest,
} from '../api/notifications';
import { Badge } from '../components/common/Badge';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { EmptyState } from '../components/common/EmptyState';
import { LoadingOverlay } from '../components/common/LoadingSpinner';

// ---- Create Rule Form ----

interface CreateRuleFormProps {
  onSuccess: () => void;
  onCancel: () => void;
}

function CreateRuleForm({ onSuccess, onCancel }: CreateRuleFormProps) {
  const queryClient = useQueryClient();
  const [nlRule, setNlRule] = useState('');
  const [notifyEmail, setNotifyEmail] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (data: CreateNotificationRuleRequest) =>
      createNotificationRule(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notificationRules'] });
      onSuccess();
    },
    onError: () => {
      setError('Failed to create rule. Please try again.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!nlRule.trim()) {
      setError('Rule text is required.');
      return;
    }
    const payload: CreateNotificationRuleRequest = {
      nlRule: nlRule.trim(),
      notifyChannel: 'email',
      ...(notifyEmail.trim() ? { notifyEmail: notifyEmail.trim() } : {}),
    };
    mutation.mutate(payload);
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-lg border border-blue-200 bg-blue-50 p-5 space-y-4"
    >
      <h3 className="text-sm font-semibold text-blue-900">New Notification Rule</h3>

      <div className="space-y-1">
        <label
          htmlFor="nlRule"
          className="block text-xs font-medium text-gray-700"
        >
          Rule description <span className="text-red-500">*</span>
        </label>
        <textarea
          id="nlRule"
          rows={3}
          value={nlRule}
          onChange={(e) => setNlRule(e.target.value)}
          placeholder='e.g. "Notify me when someone sells a Siemens PLC under $500"'
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
        />
      </div>

      <div className="space-y-1">
        <label
          htmlFor="notifyEmail"
          className="block text-xs font-medium text-gray-700"
        >
          Override email (optional)
        </label>
        <input
          id="notifyEmail"
          type="email"
          value={notifyEmail}
          onChange={(e) => setNotifyEmail(e.target.value)}
          placeholder="Leave blank to use your account email"
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {error && (
        <p className="text-xs text-red-600">{error}</p>
      )}

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
          className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Creating...' : 'Create Rule'}
        </button>
      </div>
    </form>
  );
}

// ---- Edit Rule Form ----

interface EditRuleFormProps {
  rule: NotificationRule;
  onSuccess: () => void;
  onCancel: () => void;
}

function EditRuleForm({ rule, onSuccess, onCancel }: EditRuleFormProps) {
  const queryClient = useQueryClient();
  const [nlRule, setNlRule] = useState(rule.nlRule);
  const [notifyEmail, setNotifyEmail] = useState(rule.notifyEmail ?? '');
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () =>
      updateNotificationRule(rule.id, {
        nlRule: nlRule.trim(),
        ...(notifyEmail.trim() ? { notifyEmail: notifyEmail.trim() } : {}),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notificationRules'] });
      onSuccess();
    },
    onError: () => {
      setError('Failed to update rule. Please try again.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!nlRule.trim()) {
      setError('Rule text is required.');
      return;
    }
    mutation.mutate();
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-lg border border-yellow-200 bg-yellow-50 p-5 space-y-4"
    >
      <h3 className="text-sm font-semibold text-yellow-900">Edit Rule</h3>

      <div className="space-y-1">
        <label
          htmlFor={`nlRule-edit-${rule.id}`}
          className="block text-xs font-medium text-gray-700"
        >
          Rule description <span className="text-red-500">*</span>
        </label>
        <textarea
          id={`nlRule-edit-${rule.id}`}
          rows={3}
          value={nlRule}
          onChange={(e) => setNlRule(e.target.value)}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500 resize-none"
        />
      </div>

      <div className="space-y-1">
        <label
          htmlFor={`notifyEmail-edit-${rule.id}`}
          className="block text-xs font-medium text-gray-700"
        >
          Override email (optional)
        </label>
        <input
          id={`notifyEmail-edit-${rule.id}`}
          type="email"
          value={notifyEmail}
          onChange={(e) => setNotifyEmail(e.target.value)}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500"
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
          className="rounded-md bg-yellow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-yellow-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Saving...' : 'Save Changes'}
        </button>
      </div>
    </form>
  );
}

// ---- Rule Card ----

interface RuleCardProps {
  rule: NotificationRule;
}

function RuleCard({ rule }: RuleCardProps) {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  const toggleMutation = useMutation({
    mutationFn: () =>
      updateNotificationRule(rule.id, { isActive: !rule.isActive }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notificationRules'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteNotificationRule(rule.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notificationRules'] });
      setDeleteTarget(null);
    },
  });

  const intentVariantMap = {
    sell: 'green',
    want: 'blue',
    unknown: 'gray',
  } as const;

  if (isEditing) {
    return (
      <EditRuleForm
        rule={rule}
        onSuccess={() => setIsEditing(false)}
        onCancel={() => setIsEditing(false)}
      />
    );
  }

  return (
    <>
      <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0 space-y-2">
            {/* Rule text */}
            <p className="text-sm font-medium text-gray-900 break-words">
              {rule.nlRule}
            </p>

            {/* Parsed metadata */}
            <div className="flex flex-wrap items-center gap-2">
              {rule.parsedIntent && (
                <Badge variant={intentVariantMap[rule.parsedIntent]}>
                  {rule.parsedIntent.toUpperCase()}
                </Badge>
              )}
              {rule.parsedKeywords.map((kw) => (
                <Badge key={kw} variant="teal" size="sm">
                  {kw}
                </Badge>
              ))}
              {rule.parsedPriceMin != null && (
                <Badge variant="orange" size="sm">
                  Min ${rule.parsedPriceMin.toLocaleString()}
                </Badge>
              )}
              {rule.parsedPriceMax != null && (
                <Badge variant="orange" size="sm">
                  Max ${rule.parsedPriceMax.toLocaleString()}
                </Badge>
              )}
            </div>

            {/* Email row */}
            {rule.notifyEmail && (
              <div className="flex items-center gap-1.5 text-xs text-gray-500">
                <Mail className="h-3.5 w-3.5" />
                {rule.notifyEmail}
              </div>
            )}

            {/* Last triggered */}
            {rule.lastTriggered && (
              <p className="text-xs text-gray-400">
                Last triggered:{' '}
                {new Date(rule.lastTriggered).toLocaleDateString()}
              </p>
            )}
          </div>

          {/* Actions */}
          <div className="flex items-center gap-1 shrink-0">
            {/* Active toggle */}
            <button
              onClick={() => toggleMutation.mutate()}
              disabled={toggleMutation.isPending}
              title={rule.isActive ? 'Deactivate rule' : 'Activate rule'}
              className="rounded-md p-1.5 text-gray-400 hover:text-gray-600 disabled:opacity-50"
            >
              {rule.isActive ? (
                <CheckCircle2 className="h-5 w-5 text-green-500" />
              ) : (
                <XCircle className="h-5 w-5 text-gray-400" />
              )}
            </button>

            <button
              onClick={() => setIsEditing(true)}
              title="Edit rule"
              className="rounded-md p-1.5 text-gray-400 hover:text-blue-600"
            >
              <Pencil className="h-4 w-4" />
            </button>

            <button
              onClick={() => setDeleteTarget(rule.id)}
              title="Delete rule"
              className="rounded-md p-1.5 text-gray-400 hover:text-red-600"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* Status strip */}
        <div className="mt-3 flex items-center gap-2 border-t border-gray-100 pt-3">
          <Badge variant={rule.isActive ? 'green' : 'gray'} size="sm">
            {rule.isActive ? 'Active' : 'Inactive'}
          </Badge>
          <span className="text-xs text-gray-400">
            Created {new Date(rule.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>

      <ConfirmDialog
        isOpen={deleteTarget === rule.id}
        title="Delete notification rule"
        message={`Are you sure you want to delete this rule? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={() => deleteMutation.mutate()}
        onCancel={() => setDeleteTarget(null)}
        isLoading={deleteMutation.isPending}
      />
    </>
  );
}

// ---- NotificationsPage ----

export function NotificationsPage() {
  const [showCreateForm, setShowCreateForm] = useState(false);

  const { data: rules, isLoading, isError } = useQuery({
    queryKey: ['notificationRules'],
    queryFn: getNotificationRules,
  });

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-yellow-100">
              <Bell className="h-5 w-5 text-yellow-600" />
            </div>
            <div>
              <h1 className="text-lg font-semibold text-gray-900">
                Notifications
              </h1>
              <p className="text-xs text-gray-500">
                Alert rules written in natural language
              </p>
            </div>
          </div>
          {!showCreateForm && (
            <button
              onClick={() => setShowCreateForm(true)}
              className="flex items-center gap-1.5 rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              Create Rule
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        <div className="mx-auto max-w-2xl space-y-4">
          {showCreateForm && (
            <CreateRuleForm
              onSuccess={() => setShowCreateForm(false)}
              onCancel={() => setShowCreateForm(false)}
            />
          )}

          {isLoading && <LoadingOverlay message="Loading notification rules..." />}

          {isError && (
            <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              Failed to load notification rules. Please refresh the page.
            </div>
          )}

          {!isLoading && !isError && rules?.length === 0 && !showCreateForm && (
            <EmptyState
              icon={<Bell className="h-8 w-8" />}
              title="No notification rules"
              description="Create a rule in plain English to receive email alerts when matching listings appear."
              action={
                <button
                  onClick={() => setShowCreateForm(true)}
                  className="flex items-center gap-1.5 rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
                >
                  <Plus className="h-4 w-4" />
                  Create your first rule
                </button>
              }
            />
          )}

          {rules && rules.length > 0 && (
            <div className="space-y-3">
              {rules.map((rule) => (
                <RuleCard key={rule.id} rule={rule} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
