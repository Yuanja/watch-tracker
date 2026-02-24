import type { ReactNode } from 'react';
import { clsx } from 'clsx';
import { Inbox } from 'lucide-react';

interface EmptyStateProps {
  /** Icon to display (defaults to Inbox icon) */
  icon?: ReactNode;
  /** Primary heading */
  title: string;
  /** Optional description text */
  description?: string;
  /** Optional action button or link */
  action?: ReactNode;
  /** Additional CSS classes */
  className?: string;
}

export function EmptyState({
  icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={clsx(
        'flex flex-col items-center justify-center gap-3 py-16 px-4 text-center',
        className
      )}
    >
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 text-gray-400">
        {icon ?? <Inbox className="h-8 w-8" />}
      </div>
      <div className="space-y-1">
        <h3 className="text-base font-semibold text-gray-900">{title}</h3>
        {description && (
          <p className="text-sm text-gray-500 max-w-sm">{description}</p>
        )}
      </div>
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
