import { List } from 'lucide-react';

/**
 * ListingsPage — browse and search extracted sell/want listings.
 * Placeholder implementation; full ListingsView component to be implemented in Phase 3.
 */
export function ListingsPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 p-8 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-green-100">
        <List className="h-8 w-8 text-green-600" />
      </div>
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Listings</h1>
        <p className="mt-1 text-sm text-gray-500">
          Browse and search extracted sell and want listings.
          <br />
          Full implementation coming in Phase 3.
        </p>
      </div>
    </div>
  );
}
