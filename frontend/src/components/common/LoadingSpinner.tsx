import { clsx } from 'clsx';

interface LoadingSpinnerProps {
  /** Size variant */
  size?: 'sm' | 'md' | 'lg';
  /** Additional CSS classes */
  className?: string;
  /** Label for screen readers */
  label?: string;
}

const sizeClasses = {
  sm: 'h-4 w-4 border-2',
  md: 'h-8 w-8 border-2',
  lg: 'h-12 w-12 border-4',
};

export function LoadingSpinner({
  size = 'md',
  className,
  label = 'Loading...',
}: LoadingSpinnerProps) {
  return (
    <div
      role="status"
      aria-label={label}
      className={clsx(
        'inline-block animate-spin rounded-full border-gray-300 border-t-blue-600',
        sizeClasses[size],
        className
      )}
    />
  );
}

interface LoadingOverlayProps {
  message?: string;
}

export function LoadingOverlay({ message = 'Loading...' }: LoadingOverlayProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16">
      <LoadingSpinner size="lg" />
      <p className="text-sm text-gray-500">{message}</p>
    </div>
  );
}
