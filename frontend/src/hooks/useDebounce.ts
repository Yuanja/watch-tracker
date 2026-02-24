import { useEffect, useState } from 'react';

/**
 * Debounces a value by the specified delay in milliseconds.
 * Useful for deferring search API calls until the user stops typing.
 *
 * @param value  The value to debounce
 * @param delay  Debounce delay in milliseconds (default: 300)
 * @returns      The debounced value
 */
export function useDebounce<T>(value: T, delay: number = 300): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);

  return debouncedValue;
}
