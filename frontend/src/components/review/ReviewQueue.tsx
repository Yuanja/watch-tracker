import { useState, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ClipboardList, AlertCircle, CheckCircle, X } from 'lucide-react';
import { getReviewQueue, type ReviewQueueItem } from '../../api/review';
import { LoadingOverlay } from '../common/LoadingSpinner';
import { EmptyState } from '../common/EmptyState';
import { Pagination } from '../common/Pagination';
import { ReviewCard } from './ReviewCard';

// ── Toast ─────────────────────────────────────────────────────────────────────

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error';
}

let toastCounter = 0;

function useToasts() {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback((message: string, type: Toast['type']) => {
    const id = ++toastCounter;
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return { toasts, addToast, removeToast };
}

function ToastContainer({
  toasts,
  onRemove,
}: {
  toasts: Toast[];
  onRemove: (id: number) => void;
}) {
  if (toasts.length === 0) return null;
  return (
    <div
      className="fixed bottom-6 right-6 z-50 flex flex-col gap-2"
      aria-live="polite"
      aria-atomic="false"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-medium shadow-lg ${
            t.type === 'success'
              ? 'bg-green-600 text-white'
              : 'bg-red-600 text-white'
          }`}
          role="status"
        >
          {t.type === 'success' ? (
            <CheckCircle className="h-4 w-4 shrink-0" />
          ) : (
            <AlertCircle className="h-4 w-4 shrink-0" />
          )}
          <span>{t.message}</span>
          <button
            onClick={() => onRemove(t.id)}
            className="ml-2 rounded p-0.5 opacity-80 hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-white"
            aria-label="Dismiss notification"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      ))}
    </div>
  );
}

// ── ReviewQueue ─────────────────────────────────────────────────────────────

const PAGE_SIZE = 10;

export function ReviewQueue() {
  const [page, setPage] = useState(0);
  const { toasts, addToast, removeToast } = useToasts();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['review-queue', page],
    queryFn: () => getReviewQueue({ page, size: PAGE_SIZE }),
    placeholderData: (prev) => prev,
  });

  const invalidateQueue = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['review-queue'] });
  }, [queryClient]);

  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  return (
    <>
      <div className="flex h-full flex-col gap-6 overflow-y-auto p-6">
        {/* Page header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-100">
              <ClipboardList className="h-5 w-5 text-orange-600" />
            </div>
            <div>
              <h1 className="text-xl font-semibold text-gray-900">Review Queue</h1>
              <p className="text-sm text-gray-500">
                Review and correct low-confidence LLM extractions
              </p>
            </div>
          </div>

          {totalElements > 0 && (
            <div className="text-sm text-gray-500">
              <span className="font-semibold text-orange-600">{totalElements}</span>{' '}
              pending item{totalElements !== 1 ? 's' : ''}
            </div>
          )}
        </div>

        {/* Content area */}
        {isLoading ? (
          <LoadingOverlay message="Loading review queue..." />
        ) : isError ? (
          <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-red-200 bg-red-50 py-12">
            <AlertCircle className="h-8 w-8 text-red-500" />
            <p className="text-sm font-medium text-red-700">
              Failed to load review queue
            </p>
            <p className="text-xs text-red-500">
              {error instanceof Error ? error.message : 'Unknown error'}
            </p>
          </div>
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={<ClipboardList className="h-8 w-8 text-gray-400" />}
            title="Queue is empty"
            description="All extraction items have been reviewed. New items will appear here when the pipeline flags them for human review."
          />
        ) : (
          <>
            <div className="flex flex-col gap-4">
              {data.content.map((item: ReviewQueueItem) => (
                <ReviewCard
                  key={item.id}
                  item={item}
                  onResolveSuccess={invalidateQueue}
                  onSkipSuccess={invalidateQueue}
                  onSuccess={(msg) => addToast(msg, 'success')}
                  onError={(msg) => addToast(msg, 'error')}
                />
              ))}
            </div>

            <Pagination
              currentPage={page}
              totalPages={totalPages}
              totalElements={totalElements}
              onPageChange={setPage}
              showPageNumbers={false}
            />
          </>
        )}
      </div>

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
}
