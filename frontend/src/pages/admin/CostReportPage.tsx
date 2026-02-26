import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BarChart2, Download } from 'lucide-react';
import { getAllCosts, exportCostsCsv } from '../../api/admin';
import type { AllUserCostRow } from '../../api/admin';
import { EmptyState } from '../../components/common/EmptyState';
import { LoadingOverlay } from '../../components/common/LoadingSpinner';

function formatCost(usd: number): string {
  return `$${usd.toFixed(4)}`;
}

function CostRow({ row }: { row: AllUserCostRow }) {
  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50">
      <td className="px-4 py-3">
        <p className="text-sm font-medium text-gray-900">
          {row.displayName ?? '—'}
        </p>
        <p className="text-xs text-gray-500">{row.email}</p>
      </td>
      <td className="px-4 py-3 text-right text-sm tabular-nums text-gray-700">
        {row.totalInputTokens.toLocaleString()}
      </td>
      <td className="px-4 py-3 text-right text-sm tabular-nums text-gray-700">
        {row.totalOutputTokens.toLocaleString()}
      </td>
      <td className="px-4 py-3 text-right text-sm tabular-nums text-gray-700">
        {row.sessionCount.toLocaleString()}
      </td>
      <td className="px-4 py-3 text-right text-sm tabular-nums font-semibold text-gray-900">
        {formatCost(row.totalCostUsd)}
      </td>
    </tr>
  );
}

export function CostReportPage() {
  const [isExporting, setIsExporting] = useState(false);
  const { data: costs, isLoading, isError } = useQuery({
    queryKey: ['adminCosts'],
    queryFn: getAllCosts,
  });

  async function handleExportCsv() {
    try {
      setIsExporting(true);
      const blob = await exportCostsCsv();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `cost-report-${new Date().toISOString().slice(0, 10)}.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch {
      // Silently fail - user can retry
    } finally {
      setIsExporting(false);
    }
  }

  const totals = costs?.reduce(
    (acc, row) => ({
      inputTokens: acc.inputTokens + row.totalInputTokens,
      outputTokens: acc.outputTokens + row.totalOutputTokens,
      sessions: acc.sessions + row.sessionCount,
      cost: acc.cost + row.totalCostUsd,
    }),
    { inputTokens: 0, outputTokens: 0, sessions: 0, cost: 0 }
  );

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-100">
              <BarChart2 className="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <h1 className="text-lg font-semibold text-gray-900">
                Cost Report
              </h1>
              <p className="text-xs text-gray-500">
                OpenAI token usage and costs by user
              </p>
            </div>
          </div>
          {costs && costs.length > 0 && (
            <button
              onClick={handleExportCsv}
              disabled={isExporting}
              className="flex items-center gap-1.5 rounded-md border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              <Download className="h-4 w-4" />
              {isExporting ? 'Exporting...' : 'Export CSV'}
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {isLoading && <LoadingOverlay message="Loading cost report..." />}

        {isError && (
          <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            Failed to load cost report. Please refresh the page.
          </div>
        )}

        {!isLoading && !isError && costs?.length === 0 && (
          <EmptyState
            icon={<BarChart2 className="h-8 w-8" />}
            title="No cost data"
            description="Cost data will appear here once users start using the chat features."
          />
        )}

        {costs && costs.length > 0 && (
          <div className="space-y-4">
            {/* Summary cards */}
            {totals && (
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                {[
                  {
                    label: 'Total Input Tokens',
                    value: totals.inputTokens.toLocaleString(),
                    color: 'text-blue-600',
                    bg: 'bg-blue-50',
                  },
                  {
                    label: 'Total Output Tokens',
                    value: totals.outputTokens.toLocaleString(),
                    color: 'text-purple-600',
                    bg: 'bg-purple-50',
                  },
                  {
                    label: 'Total Sessions',
                    value: totals.sessions.toLocaleString(),
                    color: 'text-teal-600',
                    bg: 'bg-teal-50',
                  },
                  {
                    label: 'Total Cost (USD)',
                    value: formatCost(totals.cost),
                    color: 'text-orange-600',
                    bg: 'bg-orange-50',
                  },
                ].map(({ label, value, color, bg }) => (
                  <div
                    key={label}
                    className={`rounded-lg ${bg} border border-gray-200 p-4`}
                  >
                    <p className="text-xs text-gray-500">{label}</p>
                    <p className={`mt-1 text-xl font-bold tabular-nums ${color}`}>
                      {value}
                    </p>
                  </div>
                ))}
              </div>
            )}

            {/* Table */}
            <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-gray-200 bg-gray-50 text-xs font-semibold text-gray-600 uppercase tracking-wide">
                  <tr>
                    <th className="px-4 py-3">User</th>
                    <th className="px-4 py-3 text-right">Input Tokens</th>
                    <th className="px-4 py-3 text-right">Output Tokens</th>
                    <th className="px-4 py-3 text-right">Sessions</th>
                    <th className="px-4 py-3 text-right">Total Cost</th>
                  </tr>
                </thead>
                <tbody>
                  {costs
                    .slice()
                    .sort((a, b) => b.totalCostUsd - a.totalCostUsd)
                    .map((row) => (
                      <CostRow key={row.userId} row={row} />
                    ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
