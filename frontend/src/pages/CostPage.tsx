import { DollarSign } from 'lucide-react';

/**
 * CostPage — personal AI usage and cost tracking.
 * Placeholder implementation; full cost dashboard to be implemented in Phase 4.
 */
export function CostPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 p-8 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100">
        <DollarSign className="h-8 w-8 text-emerald-600" />
      </div>
      <div>
        <h1 className="text-xl font-semibold text-gray-900">My Usage</h1>
        <p className="mt-1 text-sm text-gray-500">
          Track your personal AI query costs and token usage.
          <br />
          Full implementation coming in Phase 4.
        </p>
      </div>
    </div>
  );
}
