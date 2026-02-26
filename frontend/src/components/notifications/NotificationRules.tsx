import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Bell, Plus } from 'lucide-react';
import { getNotificationRules } from '../../api/notifications';
import { EmptyState } from '../common/EmptyState';
import { LoadingOverlay } from '../common/LoadingSpinner';
import { CreateRuleModal } from './CreateRuleModal';
import { RuleCard } from './RuleCard';

export function NotificationRules() {
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
            <CreateRuleModal
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
