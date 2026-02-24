import { Users } from 'lucide-react';

/**
 * AdminPage — general admin dashboard entry point.
 * Individual admin sections are reached via the sidebar links.
 * Placeholder implementation; full admin components to be implemented in Phase 2.
 */
export function AdminPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 p-8 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-purple-100">
        <Users className="h-8 w-8 text-purple-600" />
      </div>
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Admin Dashboard</h1>
        <p className="mt-1 text-sm text-gray-500">
          Use the sidebar to navigate to admin management sections.
          <br />
          Full implementation coming in Phase 2.
        </p>
      </div>
    </div>
  );
}
