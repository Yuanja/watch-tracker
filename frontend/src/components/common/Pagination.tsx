import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalElements?: number;
  onPageChange: (page: number) => void;
  /** Show individual page number buttons (default true) */
  showPageNumbers?: boolean;
}

export function Pagination({
  currentPage,
  totalPages,
  totalElements,
  onPageChange,
  showPageNumbers = true,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between border-t border-gray-100 px-4 py-3">
      <p className="text-sm text-gray-500">
        Page {currentPage + 1} of {totalPages}
        {totalElements !== undefined && ` \u2014 ${totalElements} total`}
      </p>
      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
          disabled={currentPage === 0}
          className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>

        {showPageNumbers
          ? Array.from({ length: totalPages }).map((_, i) => {
              const show =
                i === 0 ||
                i === totalPages - 1 ||
                Math.abs(i - currentPage) <= 1;
              if (!show) {
                if (i === 1 && currentPage > 3) {
                  return (
                    <span key="ellipsis-start" className="px-1 text-gray-400">
                      &hellip;
                    </span>
                  );
                }
                if (i === totalPages - 2 && currentPage < totalPages - 4) {
                  return (
                    <span key="ellipsis-end" className="px-1 text-gray-400">
                      &hellip;
                    </span>
                  );
                }
                return null;
              }
              return (
                <button
                  key={i}
                  onClick={() => onPageChange(i)}
                  className={`inline-flex h-8 w-8 items-center justify-center rounded-md text-sm font-medium transition-colors ${
                    i === currentPage
                      ? 'bg-blue-600 text-white'
                      : 'border border-gray-300 text-gray-600 hover:bg-gray-50'
                  }`}
                  aria-label={`Go to page ${i + 1}`}
                  aria-current={i === currentPage ? 'page' : undefined}
                >
                  {i + 1}
                </button>
              );
            })
          : (
              <span className="px-2 text-sm text-gray-700">
                {currentPage + 1} / {totalPages}
              </span>
            )}

        <button
          onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
          disabled={currentPage >= totalPages - 1}
          className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
          aria-label="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
