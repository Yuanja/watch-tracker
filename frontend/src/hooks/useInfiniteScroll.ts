import { useCallback, useEffect, useRef } from 'react';

interface UseInfiniteScrollOptions {
  /** Called when the sentinel element enters the viewport */
  onLoadMore: () => void;
  /** Whether there are more items to load */
  hasMore: boolean;
  /** Whether a load is currently in progress */
  isLoading: boolean;
  /** Root margin for the intersection observer (default: "0px 0px 200px 0px") */
  rootMargin?: string;
  /** Threshold for the intersection observer (default: 0) */
  threshold?: number;
}

interface UseInfiniteScrollReturn {
  /** Attach this ref to the sentinel element at the bottom of your list */
  sentinelRef: React.RefObject<HTMLDivElement>;
}

/**
 * Hook for implementing infinite scroll in the message thread view.
 * Attach the `sentinelRef` to a div at the bottom of the scrollable list;
 * when that element becomes visible, `onLoadMore` is called.
 */
export function useInfiniteScroll({
  onLoadMore,
  hasMore,
  isLoading,
  rootMargin = '0px 0px 200px 0px',
  threshold = 0,
}: UseInfiniteScrollOptions): UseInfiniteScrollReturn {
  const sentinelRef = useRef<HTMLDivElement>(null) as React.RefObject<HTMLDivElement>;

  const handleIntersect = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const [entry] = entries;
      if (entry.isIntersecting && hasMore && !isLoading) {
        onLoadMore();
      }
    },
    [onLoadMore, hasMore, isLoading]
  );

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(handleIntersect, {
      rootMargin,
      threshold,
    });

    observer.observe(sentinel);

    return () => {
      observer.unobserve(sentinel);
      observer.disconnect();
    };
  }, [handleIntersect, rootMargin, threshold]);

  return { sentinelRef };
}
