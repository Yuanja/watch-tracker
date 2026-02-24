/**
 * Format a date string or Date object to a human-readable date.
 * e.g. "March 15, 2026"
 */
export function formatDate(
  value: string | Date | null | undefined,
  options: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }
): string {
  if (!value) return '';
  const date = typeof value === 'string' ? new Date(value) : value;
  if (isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('en-US', options).format(date);
}

/**
 * Format a date string or Date object to a short date.
 * e.g. "Mar 15, 2026"
 */
export function formatShortDate(value: string | Date | null | undefined): string {
  return formatDate(value, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

/**
 * Format a date string or Date object to a time-only string.
 * e.g. "10:23 AM"
 */
export function formatTime(value: string | Date | null | undefined): string {
  if (!value) return '';
  const date = typeof value === 'string' ? new Date(value) : value;
  if (isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  }).format(date);
}

/**
 * Format a date string or Date object to a combined date-time string.
 * e.g. "Mar 15, 2026 at 10:23 AM"
 */
export function formatDateTime(value: string | Date | null | undefined): string {
  if (!value) return '';
  const date = typeof value === 'string' ? new Date(value) : value;
  if (isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  }).format(date);
}

/**
 * Format a number as currency.
 * e.g. formatCurrency(1500, 'USD') → "$1,500.00"
 */
export function formatCurrency(
  amount: number | null | undefined,
  currency: string = 'USD',
  fractionDigits: number = 2
): string {
  if (amount == null) return '';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(amount);
}

/**
 * Format a number with thousand separators.
 * e.g. 1234567 → "1,234,567"
 */
export function formatNumber(value: number | null | undefined): string {
  if (value == null) return '';
  return new Intl.NumberFormat('en-US').format(value);
}

/**
 * Format a cost in USD with up to 6 decimal places for micro-cost display.
 * e.g. 0.000123 → "$0.000123"
 */
export function formatMicroCost(amount: number | null | undefined): string {
  if (amount == null) return '';
  if (amount === 0) return '$0.00';
  if (amount < 0.01) {
    return `$${amount.toFixed(6)}`;
  }
  return formatCurrency(amount);
}

/**
 * Format a phone number for display.
 * Strips non-numeric chars and formats as +1 (XXX) XXX-XXXX for US numbers.
 */
export function formatPhone(phone: string | null | undefined): string {
  if (!phone) return '';
  const digits = phone.replace(/\D/g, '');
  if (digits.length === 11 && digits.startsWith('1')) {
    const d = digits.slice(1);
    return `+1 (${d.slice(0, 3)}) ${d.slice(3, 6)}-${d.slice(6)}`;
  }
  // Return as-is for international numbers
  return phone;
}

/**
 * Truncate a string to a maximum length, appending an ellipsis.
 */
export function truncate(text: string | null | undefined, maxLength: number): string {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return `${text.slice(0, maxLength - 3)}...`;
}

/**
 * Format a relative time string (e.g. "2 hours ago", "just now").
 */
export function formatRelativeTime(value: string | Date | null | undefined): string {
  if (!value) return '';
  const date = typeof value === 'string' ? new Date(value) : value;
  if (isNaN(date.getTime())) return '';

  const now = Date.now();
  const diffMs = now - date.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);

  if (diffSeconds < 60) return 'just now';
  if (diffSeconds < 3600) {
    const m = Math.floor(diffSeconds / 60);
    return `${m} ${m === 1 ? 'minute' : 'minutes'} ago`;
  }
  if (diffSeconds < 86400) {
    const h = Math.floor(diffSeconds / 3600);
    return `${h} ${h === 1 ? 'hour' : 'hours'} ago`;
  }
  const d = Math.floor(diffSeconds / 86400);
  if (d < 7) return `${d} ${d === 1 ? 'day' : 'days'} ago`;
  return formatShortDate(date);
}
