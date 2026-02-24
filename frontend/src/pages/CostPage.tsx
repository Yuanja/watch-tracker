import { useQuery } from '@tanstack/react-query';
import {
  DollarSign,
  Zap,
  MessageSquare,
  BarChart2,
  TrendingUp,
} from 'lucide-react';
import { getCostSummary } from '../api/chat';
import type { PeriodCost } from '../types/chat';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatUsd(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  }).format(value);
}

function formatTokens(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(2)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return n.toLocaleString();
}

function formatDate(iso: string): string {
  // periodDate is expected in YYYY-MM-DD format
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

// ── Summary Card ──────────────────────────────────────────────────────────────

interface SummaryCardProps {
  label: string;
  value: string;
  subValue?: string;
  icon: React.ReactNode;
  iconBg: string;
}

function SummaryCard({ label, value, subValue, icon, iconBg }: SummaryCardProps) {
  return (
    <div className="flex items-center gap-4 rounded-xl border border-gray-200 bg-white px-5 py-4 shadow-sm">
      <div
        className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${iconBg}`}
      >
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-sm text-gray-500">{label}</p>
        <p className="truncate text-2xl font-semibold text-gray-900">{value}</p>
        {subValue && <p className="text-xs text-gray-400">{subValue}</p>}
      </div>
    </div>
  );
}

// ── Bar Spark ─────────────────────────────────────────────────────────────────

/**
 * Tiny inline bar for relative cost visualization in the table.
 */
function CostBar({ value, max }: { value: number; max: number }) {
  const pct = max > 0 ? Math.max(2, (value / max) * 100) : 0;
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-20 overflow-hidden rounded-full bg-gray-100">
        <div
          className="h-full rounded-full bg-emerald-500 transition-all"
          style={{ width: `${pct}%` }}
          aria-hidden="true"
        />
      </div>
      <span className="text-xs tabular-nums text-gray-700">{formatUsd(value)}</span>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export function CostPage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['cost-summary'],
    queryFn: getCostSummary,
    staleTime: 60_000,
  });

  const maxDayCost = data
    ? Math.max(...data.byPeriod.map((p) => p.costUsd), 0)
    : 0;

  return (
    <div className="flex h-full flex-col gap-6 overflow-y-auto p-6">
      {/* Page header */}
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100">
          <DollarSign className="h-5 w-5 text-emerald-600" />
        </div>
        <div>
          <h1 className="text-xl font-semibold text-gray-900">My Usage</h1>
          <p className="text-sm text-gray-500">
            Track your personal AI query costs and token consumption
          </p>
        </div>
      </div>

      {isLoading ? (
        <LoadingOverlay message="Loading usage data..." />
      ) : isError ? (
        <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-red-200 bg-red-50 py-12">
          <TrendingUp className="h-8 w-8 text-red-400" />
          <p className="text-sm font-medium text-red-700">
            Failed to load usage data
          </p>
          <p className="text-xs text-red-500">
            {error instanceof Error ? error.message : 'Unknown error'}
          </p>
        </div>
      ) : !data ? (
        <EmptyState
          icon={<BarChart2 className="h-8 w-8 text-gray-400" />}
          title="No usage data yet"
          description="Start a chat session to see your token usage and cost breakdown here."
        />
      ) : (
        <>
          {/* Summary cards */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <SummaryCard
              label="Total Cost"
              value={formatUsd(data.totalCostUsd)}
              icon={<DollarSign className="h-6 w-6 text-emerald-600" />}
              iconBg="bg-emerald-100"
            />
            <SummaryCard
              label="Input Tokens"
              value={formatTokens(data.totalInputTokens)}
              subValue={`${data.totalInputTokens.toLocaleString()} tokens`}
              icon={<MessageSquare className="h-6 w-6 text-blue-600" />}
              iconBg="bg-blue-100"
            />
            <SummaryCard
              label="Output Tokens"
              value={formatTokens(data.totalOutputTokens)}
              subValue={`${data.totalOutputTokens.toLocaleString()} tokens`}
              icon={<Zap className="h-6 w-6 text-purple-600" />}
              iconBg="bg-purple-100"
            />
            <SummaryCard
              label="Sessions"
              value={data.sessionCount.toLocaleString()}
              icon={<BarChart2 className="h-6 w-6 text-orange-600" />}
              iconBg="bg-orange-100"
            />
          </div>

          {/* Daily breakdown */}
          {data.byPeriod.length === 0 ? (
            <div className="rounded-xl border border-gray-200 bg-white p-8 text-center shadow-sm">
              <p className="text-sm text-gray-400">
                No daily breakdown available yet.
              </p>
            </div>
          ) : (
            <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
              <div className="border-b border-gray-100 px-5 py-4">
                <h2 className="text-base font-semibold text-gray-900">
                  Daily Breakdown
                </h2>
                <p className="mt-0.5 text-sm text-gray-500">
                  {data.byPeriod.length} day{data.byPeriod.length !== 1 ? 's' : ''} of activity
                </p>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100 bg-gray-50 text-left">
                      <th className="px-5 py-3 font-medium text-gray-600">Date</th>
                      <th className="px-5 py-3 font-medium text-gray-600 text-right">
                        Input Tokens
                      </th>
                      <th className="px-5 py-3 font-medium text-gray-600 text-right">
                        Output Tokens
                      </th>
                      <th className="px-5 py-3 font-medium text-gray-600 text-right">
                        Sessions
                      </th>
                      <th className="px-5 py-3 font-medium text-gray-600">
                        Cost
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {data.byPeriod.map((period: PeriodCost) => (
                      <tr
                        key={period.periodDate}
                        className="transition-colors hover:bg-gray-50"
                      >
                        <td className="px-5 py-3 font-medium text-gray-800">
                          {formatDate(period.periodDate)}
                        </td>
                        <td className="px-5 py-3 text-right tabular-nums text-gray-700">
                          {period.inputTokens.toLocaleString()}
                        </td>
                        <td className="px-5 py-3 text-right tabular-nums text-gray-700">
                          {period.outputTokens.toLocaleString()}
                        </td>
                        <td className="px-5 py-3 text-right tabular-nums text-gray-700">
                          {period.sessionCount}
                        </td>
                        <td className="px-5 py-3">
                          <CostBar value={period.costUsd} max={maxDayCost} />
                        </td>
                      </tr>
                    ))}
                  </tbody>

                  {/* Totals row */}
                  <tfoot>
                    <tr className="border-t-2 border-gray-200 bg-gray-50 font-semibold">
                      <td className="px-5 py-3 text-gray-900">Total</td>
                      <td className="px-5 py-3 text-right tabular-nums text-gray-900">
                        {data.totalInputTokens.toLocaleString()}
                      </td>
                      <td className="px-5 py-3 text-right tabular-nums text-gray-900">
                        {data.totalOutputTokens.toLocaleString()}
                      </td>
                      <td className="px-5 py-3 text-right tabular-nums text-gray-900">
                        {data.sessionCount}
                      </td>
                      <td className="px-5 py-3 text-gray-900">
                        <span className="font-semibold text-emerald-700">
                          {formatUsd(data.totalCostUsd)}
                        </span>
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>

              {/* Pricing note */}
              <div className="border-t border-gray-100 px-5 py-3">
                <p className="text-xs text-gray-400">
                  Costs are calculated based on OpenAI API pricing for GPT-4o (chat) and
                  GPT-4o-mini (extraction). Prices may vary slightly from your OpenAI invoice
                  due to rounding.
                </p>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
