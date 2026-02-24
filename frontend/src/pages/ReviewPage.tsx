import { ClipboardList } from 'lucide-react';

/**
 * ReviewPage — human review queue for low-confidence LLM extractions.
 * Requires admin or uber_admin role.
 * Placeholder implementation; full ReviewQueue component to be implemented in Phase 3.
 */
export function ReviewPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 p-8 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-orange-100">
        <ClipboardList className="h-8 w-8 text-orange-600" />
      </div>
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Review Queue</h1>
        <p className="mt-1 text-sm text-gray-500">
          Review and correct low-confidence LLM extractions.
          <br />
          Full implementation coming in Phase 3.
        </p>
      </div>
    </div>
  );
}
