import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Pencil, Trash2, Mail, CheckCircle2, XCircle } from 'lucide-react';
import {
  updateNotificationRule,
  deleteNotificationRule,
  type NotificationRule,
} from '../../api/notifications';
import { Badge } from '../common/Badge';
import { ConfirmDialog } from '../common/ConfirmDialog';

// ---- Inline Edit Form ----

function EditRuleForm({
  rule,
  onSuccess,
  onCancel,
}: {
  rule: NotificationRule;
  onSuccess: () => void;
  onCancel: () => void;
}) {
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

export function RuleCard({ rule }: RuleCardProps) {
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
            <p className="text-sm font-medium text-gray-900 break-words">
              {rule.nlRule}
            </p>

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

            {rule.notifyEmail && (
              <div className="flex items-center gap-1.5 text-xs text-gray-500">
                <Mail className="h-3.5 w-3.5" />
                {rule.notifyEmail}
              </div>
            )}

            {rule.lastTriggered && (
              <p className="text-xs text-gray-400">
                Last triggered:{' '}
                {new Date(rule.lastTriggered).toLocaleDateString()}
              </p>
            )}
          </div>

          <div className="flex items-center gap-1 shrink-0">
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
        message="Are you sure you want to delete this rule? This action cannot be undone."
        confirmLabel="Delete"
        variant="danger"
        onConfirm={() => deleteMutation.mutate()}
        onCancel={() => setDeleteTarget(null)}
        isLoading={deleteMutation.isPending}
      />
    </>
  );
}
