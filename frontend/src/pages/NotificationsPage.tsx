import { Bell } from 'lucide-react';

/**
 * NotificationsPage — manage alert rules written in natural language.
 * Placeholder implementation; full NotificationRules component to be implemented in Phase 4.
 */
export function NotificationsPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 p-8 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-yellow-100">
        <Bell className="h-8 w-8 text-yellow-600" />
      </div>
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Notifications</h1>
        <p className="mt-1 text-sm text-gray-500">
          Create natural language alert rules for new listings.
          <br />
          Full implementation coming in Phase 4.
        </p>
      </div>
    </div>
  );
}
