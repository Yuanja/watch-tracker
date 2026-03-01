import { clsx } from 'clsx';
import type { ReactNode } from 'react';

type BadgeVariant =
  | 'green'
  | 'blue'
  | 'red'
  | 'yellow'
  | 'gray'
  | 'purple'
  | 'orange'
  | 'teal';

type BadgeSize = 'sm' | 'md';

interface BadgeProps {
  variant?: BadgeVariant;
  size?: BadgeSize;
  children: ReactNode;
  className?: string;
}

const variantClasses: Record<BadgeVariant, string> = {
  green: 'bg-green-100 text-green-800 ring-green-600/20',
  blue: 'bg-blue-100 text-blue-800 ring-blue-600/20',
  red: 'bg-red-100 text-red-800 ring-red-600/20',
  yellow: 'bg-yellow-100 text-yellow-800 ring-yellow-600/20',
  gray: 'bg-gray-100 text-gray-700 ring-gray-600/20',
  purple: 'bg-purple-100 text-purple-800 ring-purple-600/20',
  orange: 'bg-orange-100 text-orange-800 ring-orange-600/20',
  teal: 'bg-teal-100 text-teal-800 ring-teal-600/20',
};

const sizeClasses: Record<BadgeSize, string> = {
  sm: 'px-1.5 py-0.5 text-xs',
  md: 'px-2.5 py-0.5 text-xs',
};

export function Badge({
  variant = 'gray',
  size = 'md',
  children,
  className,
}: BadgeProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1 rounded-full font-medium ring-1 ring-inset',
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
    >
      {children}
    </span>
  );
}

// Convenience exports for common badge types
export function IntentBadge({ intent }: { intent: 'sell' | 'want' | 'unknown' }) {
  const variantMap = {
    sell: 'green',
    want: 'blue',
    unknown: 'gray',
  } as const;

  const labelMap = {
    sell: 'SELL',
    want: 'WANT',
    unknown: 'UNKNOWN',
  };

  return (
    <Badge variant={variantMap[intent]}>{labelMap[intent]}</Badge>
  );
}

export function StatusBadge({
  status,
}: {
  status: 'active' | 'expired' | 'deleted' | 'pending_review' | 'sold';
}) {
  const variantMap = {
    active: 'green',
    expired: 'gray',
    deleted: 'red',
    pending_review: 'yellow',
    sold: 'red',
  } as const;

  const labelMap = {
    active: 'Active',
    expired: 'Expired',
    deleted: 'Deleted',
    pending_review: 'Pending Review',
    sold: 'Sold',
  };

  return (
    <Badge variant={variantMap[status]}>{labelMap[status]}</Badge>
  );
}

export function RoleBadge({ role }: { role: 'user' | 'admin' | 'uber_admin' }) {
  const variantMap = {
    user: 'gray',
    admin: 'blue',
    uber_admin: 'purple',
  } as const;

  const labelMap = {
    user: 'User',
    admin: 'Admin',
    uber_admin: 'Uber Admin',
  };

  return <Badge variant={variantMap[role]}>{labelMap[role]}</Badge>;
}
