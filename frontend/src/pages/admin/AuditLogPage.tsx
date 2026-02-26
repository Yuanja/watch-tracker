import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ScrollText, ChevronLeft, ChevronRight } from 'lucide-react';
import { getAuditLog, parseJsonValues } from '../../api/admin';
import type { AuditLogEntry } from '../../api/admin';
import { EmptyState } from '../../components/common/EmptyState';
import { LoadingOverlay } from '../../components/common/LoadingSpinner';

const PAGE_SIZE = 20;

function ValuePreview({
  label,
  values,
}: {
  label: string;
  values: Record<string, unknown> | null;
}) {
  if (!values || Object.keys(values).length === 0) return null;
  return (
    <details className="mt-1">
      <summary className="cursor-pointer text-xs font-medium text-gray-500 hover:text-gray-700">
        {label}
      </summary>
      <pre className="mt-1 rounded bg-gray-100 p-2 text-xs text-gray-700 overflow-x-auto max-w-xs">
        {JSON.stringify(values, null, 2)}
      </pre>
    </details>
  );
}

function AuditRow({ entry }: { entry: AuditLogEntry }) {
  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50 align-top">
      <td className="px-4 py-3 text-xs text-gray-500 whitespace-nowrap">
        {new Date(entry.createdAt).toLocaleString()}
      </td>
      <td className="px-4 py-3">
        <p className="text-xs font-medium text-gray-900">
          {entry.actorEmail ?? entry.actorId ?? 'System'}
        </p>
      </td>
      <td className="px-4 py-3">
        <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700 ring-1 ring-blue-200">
          {entry.action}
        </span>
      </td>
      <td className="px-4 py-3 text-xs text-gray-600">
        {entry.targetType && (
          <span className="font-medium">{entry.targetType}</span>
        )}
        {entry.targetId && (
          <span className="text-gray-400"> #{entry.targetId.slice(0, 8)}</span>
        )}
        <ValuePreview label="Old" values={parseJsonValues(entry.oldValues)} />
        <ValuePreview label="New" values={parseJsonValues(entry.newValues)} />
      </td>
      <td className="px-4 py-3 text-xs text-gray-400">
        {entry.ipAddress ?? '—'}
      </td>
    </tr>
  );
}

export function AuditLogPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['auditLog', page],
    queryFn: () => getAuditLog({ page, size: PAGE_SIZE }),
    placeholderData: (prev) => prev,
  });

  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gray-100">
            <ScrollText className="h-5 w-5 text-gray-600" />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-gray-900">Audit Log</h1>
            <p className="text-xs text-gray-500">
              Paginated record of all privileged actions
            </p>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {isLoading && <LoadingOverlay message="Loading audit log..." />}

        {isError && (
          <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            Failed to load audit log. Please refresh the page.
          </div>
        )}

        {!isLoading && !isError && data?.content.length === 0 && (
          <EmptyState
            icon={<ScrollText className="h-8 w-8" />}
            title="No audit events"
            description="Privileged actions will appear here once they occur."
          />
        )}

        {data && data.content.length > 0 && (
          <>
            <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-gray-200 bg-gray-50 text-xs font-semibold text-gray-600 uppercase tracking-wide">
                  <tr>
                    <th className="px-4 py-3 whitespace-nowrap">Timestamp</th>
                    <th className="px-4 py-3">Actor</th>
                    <th className="px-4 py-3">Action</th>
                    <th className="px-4 py-3">Target</th>
                    <th className="px-4 py-3">IP</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((entry) => (
                    <AuditRow key={entry.id} entry={entry} />
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
              <p>
                Page {page + 1} of {Math.max(totalPages, 1)} &mdash;{' '}
                {data.totalElements.toLocaleString()} total events
              </p>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={data.first}
                  className="flex items-center gap-1 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium hover:bg-gray-50 disabled:opacity-40"
                >
                  <ChevronLeft className="h-3.5 w-3.5" />
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={data.last}
                  className="flex items-center gap-1 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium hover:bg-gray-50 disabled:opacity-40"
                >
                  Next
                  <ChevronRight className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
