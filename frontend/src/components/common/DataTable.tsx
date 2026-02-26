import type { ReactNode } from 'react';

export interface Column<T> {
  key: string;
  label: string;
  /** Custom renderer; receives the row object */
  render?: (row: T) => ReactNode;
  /** CSS classes for the <th> and <td> */
  className?: string;
  /** If true, column is hidden below the given breakpoint */
  hideBelow?: 'sm' | 'md' | 'lg';
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  /** Unique key extractor for each row */
  rowKey: (row: T) => string;
  /** Optional click handler for rows */
  onRowClick?: (row: T) => void;
  /** Optionally render an expanded detail panel below a row */
  expandedRowKey?: string | null;
  renderExpanded?: (row: T) => ReactNode;
}

const breakpointClass: Record<string, string> = {
  sm: 'hidden sm:table-cell',
  md: 'hidden md:table-cell',
  lg: 'hidden lg:table-cell',
};

export function DataTable<T>({
  columns,
  data,
  rowKey,
  onRowClick,
  expandedRowKey,
  renderExpanded,
}: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100 bg-gray-50 text-left">
            {columns.map((col) => (
              <th
                key={col.key}
                className={`px-4 py-3 font-medium text-gray-600 ${
                  col.hideBelow ? breakpointClass[col.hideBelow] : ''
                } ${col.className ?? ''}`}
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {data.map((row) => {
            const key = rowKey(row);
            const isExpanded = expandedRowKey === key;
            return (
              <>
                <tr
                  key={key}
                  className={
                    onRowClick
                      ? 'cursor-pointer transition-colors hover:bg-gray-50'
                      : ''
                  }
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  aria-expanded={
                    renderExpanded ? isExpanded : undefined
                  }
                  role={onRowClick ? 'button' : undefined}
                  tabIndex={onRowClick ? 0 : undefined}
                  onKeyDown={
                    onRowClick
                      ? (e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            onRowClick(row);
                          }
                        }
                      : undefined
                  }
                >
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className={`px-4 py-3 ${
                        col.hideBelow ? breakpointClass[col.hideBelow] : ''
                      } ${col.className ?? ''}`}
                    >
                      {col.render
                        ? col.render(row)
                        : String((row as Record<string, unknown>)[col.key] ?? '')}
                    </td>
                  ))}
                </tr>
                {isExpanded && renderExpanded && (
                  <tr key={`${key}-detail`}>
                    <td colSpan={columns.length} className="p-0">
                      {renderExpanded(row)}
                    </td>
                  </tr>
                )}
              </>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
