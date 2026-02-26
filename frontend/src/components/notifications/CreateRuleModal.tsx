import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createNotificationRule,
  type CreateNotificationRuleRequest,
} from '../../api/notifications';

interface CreateRuleModalProps {
  onSuccess: () => void;
  onCancel: () => void;
}

export function CreateRuleModal({ onSuccess, onCancel }: CreateRuleModalProps) {
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
