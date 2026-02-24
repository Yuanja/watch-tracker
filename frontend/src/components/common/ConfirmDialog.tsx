import { useEffect, useRef } from 'react';
import { X, AlertTriangle } from 'lucide-react';
import { clsx } from 'clsx';

interface ConfirmDialogProps {
  /** Whether the dialog is visible */
  isOpen: boolean;
  /** Dialog title */
  title: string;
  /** Descriptive message asking the user to confirm */
  message: string;
  /** Label for the confirm button (default: "Confirm") */
  confirmLabel?: string;
  /** Label for the cancel button (default: "Cancel") */
  cancelLabel?: string;
  /** Variant for the confirm button */
  variant?: 'danger' | 'primary';
  /** Called when the user confirms */
  onConfirm: () => void;
  /** Called when the user cancels or closes the dialog */
  onCancel: () => void;
  /** Whether the confirm action is in progress */
  isLoading?: boolean;
}

export function ConfirmDialog({
  isOpen,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'danger',
  onConfirm,
  onCancel,
  isLoading = false,
}: ConfirmDialogProps) {
  const confirmButtonRef = useRef<HTMLButtonElement>(null);

  // Focus the cancel button when dialog opens (safer default for destructive actions)
  useEffect(() => {
    if (isOpen) {
      confirmButtonRef.current?.focus();
    }
  }, [isOpen]);

  // Close on Escape key
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCancel();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onCancel]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
      aria-describedby="confirm-dialog-message"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onCancel}
        aria-hidden="true"
      />

      {/* Dialog panel */}
      <div className="relative z-10 w-full max-w-md rounded-lg bg-white shadow-xl">
        {/* Header */}
        <div className="flex items-start gap-4 p-6">
          <div
            className={clsx(
              'flex h-10 w-10 shrink-0 items-center justify-center rounded-full',
              variant === 'danger' ? 'bg-red-100' : 'bg-blue-100'
            )}
          >
            <AlertTriangle
              className={clsx(
                'h-5 w-5',
                variant === 'danger' ? 'text-red-600' : 'text-blue-600'
              )}
            />
          </div>
          <div className="flex-1">
            <h2
              id="confirm-dialog-title"
              className="text-base font-semibold text-gray-900"
            >
              {title}
            </h2>
            <p
              id="confirm-dialog-message"
              className="mt-1 text-sm text-gray-500"
            >
              {message}
            </p>
          </div>
          <button
            onClick={onCancel}
            className="rounded-md p-1 text-gray-400 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-gray-400"
            aria-label="Close dialog"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 border-t border-gray-100 px-6 py-4">
          <button
            onClick={onCancel}
            disabled={isLoading}
            className="rounded-md px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-400 disabled:opacity-50"
          >
            {cancelLabel}
          </button>
          <button
            ref={confirmButtonRef}
            onClick={onConfirm}
            disabled={isLoading}
            className={clsx(
              'rounded-md px-4 py-2 text-sm font-medium text-white focus:outline-none focus:ring-2 disabled:opacity-50',
              variant === 'danger'
                ? 'bg-red-600 hover:bg-red-700 focus:ring-red-500'
                : 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500'
            )}
          >
            {isLoading ? 'Processing...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
